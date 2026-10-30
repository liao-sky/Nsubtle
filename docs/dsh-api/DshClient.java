import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DSH（DeepSeek Harness）本机 HTTP 接口的最小客户端：只依赖 JDK，不引入任何第三方库。
 *
 * <p>用法（Java 21 可直接以源码模式运行）：
 * <pre>
 *   java DshClient.java list
 *   java DshClient.java ask "解释一下 NeoForge 的 DataGen 流程"
 *   java DshClient.java ask -s session-xxxx "继续，给个例子"
 *   java DshClient.java history session-xxxx
 *   java DshClient.java --base http://127.0.0.1:58269 list
 * </pre>
 *
 * <p>协议摘要见同目录 README.md：POST {base}/api/{namespace}/{method}，
 * 请求体 {type:"client-request", rpcId, method, payload:{args}}，
 * 响应体 {type:"server-response", rpcId, result:{ok, value|error}}。
 */
public final class DshClient {

    private final String base;
    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1) // 服务端不认 h2c 升级，必须钉死 1.1
            .connectTimeout(Duration.ofSeconds(5))
            .proxy(HttpClient.Builder.NO_PROXY)
            .build();

    public DshClient(String base) {
        this.base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    // ---------------------------------------------------------------- 端点封装

    /** 列出会话，每项含 sessionId / running / cwd / projections 等。 */
    public List<Map<String, Object>> listSessions() {
        Map<String, Object> value = asMap(rpc("session/list", args("_request", Map.of())));
        return asList(value.get("items")).stream().map(DshClient::asMap).toList();
    }

    /** 新建（或复用空白）会话，返回 sessionId。cwd 传 null 用默认目录。 */
    public String createSession(String cwd) {
        Map<String, Object> request = new LinkedHashMap<>();
        if (cwd != null) {
            request.put("cwd", cwd);
        }
        Map<String, Object> value = asMap(rpc("session/create", args("request", request)));
        return asString(value.get("sessionId"));
    }

    /** 发送一轮提问。返回时只代表"已受理"，不代表"已回答"。 */
    public void prompt(String sessionId, String text, String mode) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("requestId", UUID.randomUUID().toString());
        request.put("sessionId", sessionId);
        request.put("mode", mode == null ? "queue" : mode);
        request.put("content", List.of(Map.of("type", "text", "text", text)));
        request.put("clientTimeZone", "Asia/Shanghai");
        rpc("session/prompt", args("request", request));
    }

    /** 取消当前回合。 */
    public void cancel(String sessionId) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("sessionId", sessionId);
        rpc("session/cancel", args("request", request));
    }

    /** 会话当前的事件游标；读历史时 throughSeq 不能超过它。 */
    public int asOfSeq(String sessionId) {
        Map<String, Object> request = Map.of("sessionId", sessionId);
        Map<String, Object> value = asMap(rpc("session/projections", args("request", request)));
        return intOf(value.get("asOfSeq"));
    }

    /** 读一页事件（返回的是"截至 throughSeq 的最近一页"）。 */
    public List<Map<String, Object>> page(String sessionId, int throughSeq, int maxMessages) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("address", Map.of("kind", "session", "sessionId", sessionId));
        request.put("throughSeq", throughSeq);
        request.put("maxMessages", maxMessages);
        Map<String, Object> value = asMap(rpc("session/page", args("request", request)));
        return asList(value.get("records")).stream().map(DshClient::asMap).toList();
    }

    // ---------------------------------------------------------------- 组合流程

    /**
     * 提问并等待回答：轮询事件，直到出现新一轮的 turn/end 为止。
     *
     * @return 最后一个 assistant/message 里的纯文本
     */
    public String ask(String sessionId, String question) {
        int baseline = lastTurnEndSeq(sessionId);
        prompt(sessionId, question, "queue");
        System.out.println("[dsh] 已发送，会话 " + sessionId + "，等待回答…");

        long deadline = System.currentTimeMillis() + Duration.ofMinutes(5).toMillis();
        int printedUpTo = baseline;
        while (System.currentTimeMillis() < deadline) {
            sleep(1000);
            int asOf = asOfSeq(sessionId);
            List<Map<String, Object>> records = page(sessionId, asOf, 500);

            // 增量打印本轮事件，方便在 IDEA 控制台看进度
            for (Map<String, Object> record : records) {
                Map<String, Object> event = asMap(record.get("event"));
                int seq = intOf(event.get("seq"));
                if (seq > printedUpTo) {
                    printedUpTo = seq;
                    if (isInteresting(str(event.get("type")))) {
                        System.out.println("  [" + seq + "] " + str(event.get("type")));
                    }
                }
            }

            Integer end = lastTurnEnd(records);
            if (end != null && end > baseline) {
                return extractAnswer(records);
            }
        }
        throw new IllegalStateException("等待回答超时（5 分钟）");
    }

    private int lastTurnEndSeq(String sessionId) {
        int asOf = asOfSeq(sessionId);
        if (asOf <= 0) {
            return -1;
        }
        Integer end = lastTurnEnd(page(sessionId, asOf, 500));
        return end == null ? -1 : end;
    }

    private static Integer lastTurnEnd(List<Map<String, Object>> records) {
        Integer max = null;
        for (Map<String, Object> record : records) {
            Map<String, Object> event = asMap(record.get("event"));
            if ("turn/end".equals(str(event.get("type")))) {
                int seq = intOf(event.get("seq"));
                max = max == null ? seq : Math.max(max, seq);
            }
        }
        return max;
    }

    /** 取最后一个 assistant/message 的纯文本块。 */
    private static String extractAnswer(List<Map<String, Object>> records) {
        String answer = "";
        for (Map<String, Object> record : records) {
            Map<String, Object> event = asMap(record.get("event"));
            if (!"assistant/message".equals(str(event.get("type")))) {
                continue;
            }
            Map<String, Object> data = asMap(event.get("data"));
            Map<String, Object> message = asMap(data.get("message"));
            StringBuilder sb = new StringBuilder();
            for (Object part : asList(message.get("content"))) {
                Map<String, Object> p = asMap(part);
                if ("text".equals(str(p.get("type")))) {
                    sb.append(str(p.get("text")));
                }
            }
            if (!sb.isEmpty()) {
                answer = sb.toString();
            }
        }
        return answer;
    }

    private static boolean isInteresting(String type) {
        if (type == null) {
            return false;
        }
        return type.startsWith("turn/") || type.startsWith("step/")
                || type.equals("assistant/message") || type.contains("tool");
    }

    /** 打印整段历史（每条事件一行，消息类事件带正文）。 */
    public void printHistory(String sessionId) {
        int asOf = asOfSeq(sessionId);
        List<Map<String, Object>> records = page(sessionId, asOf, 500);
        for (Map<String, Object> record : records) {
            Map<String, Object> event = asMap(record.get("event"));
            String type = str(event.get("type"));
            Map<String, Object> data = asMap(event.get("data"));
            String preview = "";
            if ("user/message".equals(type) || "assistant/message".equals(type)) {
                Map<String, Object> message = "user/message".equals(type) ? data : asMap(data.get("message"));
                StringBuilder sb = new StringBuilder();
                for (Object part : asList(message.get("content"))) {
                    Map<String, Object> p = asMap(part);
                    if ("text".equals(str(p.get("type")))) {
                        sb.append(str(p.get("text")));
                    }
                }
                preview = abbreviate(sb.toString().replace("\n", " "), 90);
            }
            System.out.printf("%3d  %-18s %s%n", intOf(event.get("seq")), type, preview);
        }
    }

    // ---------------------------------------------------------------- RPC 底座

    private static Map<String, Object> args(String name, Object value) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put(name, value);
        return args;
    }

    private Object rpc(String method, Map<String, Object> args) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("type", "client-request");
        envelope.put("rpcId", UUID.randomUUID().toString());
        envelope.put("method", method);
        envelope.put("payload", Map.of("args", args));
        String body = Json.write(envelope);

        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/api/" + method))
                .header("Content-Type", "application/json; charset=utf-8")
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (java.io.IOException e) {
            throw new IllegalStateException("连不上 " + base + "，DSH 是否在运行？", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("调用被中断", e);
        }
        if (response.statusCode() == 404) {
            throw new IllegalStateException("端点不存在：" + method);
        }
        Map<String, Object> result = asMap(asMap(Json.parse(response.body())).get("result"));
        if (!Boolean.TRUE.equals(result.get("ok"))) {
            Map<String, Object> error = asMap(result.get("error"));
            throw new IllegalStateException(error.get("code") + ": " + error.get("message"));
        }
        return result.get("value");
    }

    // ---------------------------------------------------------------- 小工具

    @SuppressWarnings("unchecked")
    static Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Map.of();
    }

    @SuppressWarnings("unchecked")
    static List<Object> asList(Object value) {
        return value instanceof List ? (List<Object>) value : List.of();
    }

    static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    static String asString(Object value) {
        return str(value);
    }

    static int intOf(Object value) {
        return value instanceof Number n ? n.intValue() : 0;
    }

    private static String abbreviate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待被中断", e);
        }
    }

    // ---------------------------------------------------------------- main

    public static void main(String[] args) throws Exception {
        String base = System.getenv().getOrDefault("DSH_BASE", "http://127.0.0.1:58269");
        List<String> rest = new ArrayList<>(List.of(args));
        if (!rest.isEmpty() && rest.get(0).equals("--base")) {
            base = rest.get(1);
            rest = rest.subList(2, rest.size());
        }
        if (rest.isEmpty()) {
            System.out.println("""
                    用法：
                      java DshClient.java list
                      java DshClient.java ask [-s sessionId] [-c cwd] <问题…>
                      java DshClient.java history <sessionId>
                      java DshClient.java --base http://127.0.0.1:58269 list
                    """);
            return;
        }
        DshClient client = new DshClient(base);
        String command = rest.get(0);
        switch (command) {
            case "list" -> {
                for (Map<String, Object> row : client.listSessions()) {
                    Map<String, Object> projections = asMap(row.get("projections"));
                    Map<String, Object> values = asMap(projections.get("values"));
                    System.out.printf("%-46s %-6s %s%n",
                            row.get("sessionId"),
                            Boolean.TRUE.equals(row.get("running")) ? "忙" : "闲",
                            values.get("title"));
                }
            }
            case "ask" -> {
                String sessionId = null;
                String cwd = null;
                List<String> words = new ArrayList<>();
                for (int i = 1; i < rest.size(); i++) {
                    switch (rest.get(i)) {
                        case "-s" -> sessionId = rest.get(++i);
                        case "-c" -> cwd = rest.get(++i);
                        default -> words.add(rest.get(i));
                    }
                }
                String question = String.join(" ", words);
                if (question.isBlank()) {
                    throw new IllegalArgumentException("缺少问题文本");
                }
                if (sessionId == null) {
                    sessionId = client.createSession(cwd);
                    System.out.println("[dsh] 新建会话 " + sessionId);
                }
                String answer = client.ask(sessionId, question);
                System.out.println();
                System.out.println("=== 回答 ===");
                System.out.println(answer);
                System.out.println();
                System.out.println("[dsh] 继续追问：java DshClient.java ask -s " + sessionId + " \"...\"");
            }
            case "cancel" -> client.cancel(rest.get(1));
            case "history" -> client.printHistory(rest.get(1));
            default -> throw new IllegalArgumentException("未知命令：" + command);
        }
    }

    // ---------------------------------------------------------------- 极简 JSON

    /** 够用就行的 JSON 读写：对象→Map，数组→List，数字→Long/Double。 */
    static final class Json {

        static String write(Object value) {
            StringBuilder sb = new StringBuilder();
            write(value, sb);
            return sb.toString();
        }

        private static void write(Object value, StringBuilder sb) {
            if (value == null) {
                sb.append("null");
            } else if (value instanceof String s) {
                escape(s, sb);
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else if (value instanceof Map<?, ?> map) {
                sb.append('{');
                boolean first = true;
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    if (!first) {
                        sb.append(',');
                    }
                    first = false;
                    escape(String.valueOf(e.getKey()), sb);
                    sb.append(':');
                    write(e.getValue(), sb);
                }
                sb.append('}');
            } else if (value instanceof Iterable<?> list) {
                sb.append('[');
                boolean first = true;
                for (Object item : list) {
                    if (!first) {
                        sb.append(',');
                    }
                    first = false;
                    write(item, sb);
                }
                sb.append(']');
            } else {
                escape(String.valueOf(value), sb);
            }
        }

        private static void escape(String s, StringBuilder sb) {
            sb.append('"');
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '"' -> sb.append("\\\"");
                    case '\\' -> sb.append("\\\\");
                    case '\n' -> sb.append("\\n");
                    case '\r' -> sb.append("\\r");
                    case '\t' -> sb.append("\\t");
                    default -> {
                        if (c < 0x20) {
                            sb.append(String.format("\\u%04x", (int) c));
                        } else {
                            sb.append(c);
                        }
                    }
                }
            }
            sb.append('"');
        }

        static Object parse(String text) {
            Parser parser = new Parser(text);
            Object value = parser.value();
            parser.skipSpace();
            return value;
        }

        private static final class Parser {
            private final String s;
            private int i;

            Parser(String s) {
                this.s = s;
            }

            void skipSpace() {
                while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
                    i++;
                }
            }

            Object value() {
                skipSpace();
                char c = s.charAt(i);
                return switch (c) {
                    case '{' -> object();
                    case '[' -> array();
                    case '"' -> string();
                    case 't' -> literal("true", Boolean.TRUE);
                    case 'f' -> literal("false", Boolean.FALSE);
                    case 'n' -> literal("null", null);
                    default -> number();
                };
            }

            Object literal(String word, Object value) {
                if (!s.startsWith(word, i)) {
                    throw new IllegalArgumentException("JSON 解析失败，位置 " + i);
                }
                i += word.length();
                return value;
            }

            Map<String, Object> object() {
                Map<String, Object> map = new LinkedHashMap<>();
                i++; // {
                skipSpace();
                if (s.charAt(i) == '}') {
                    i++;
                    return map;
                }
                while (true) {
                    skipSpace();
                    String key = string();
                    skipSpace();
                    i++; // :
                    map.put(key, value());
                    skipSpace();
                    char c = s.charAt(i++);
                    if (c == '}') {
                        return map;
                    }
                }
            }

            List<Object> array() {
                List<Object> list = new ArrayList<>();
                i++; // [
                skipSpace();
                if (s.charAt(i) == ']') {
                    i++;
                    return list;
                }
                while (true) {
                    list.add(value());
                    skipSpace();
                    char c = s.charAt(i++);
                    if (c == ']') {
                        return list;
                    }
                }
            }

            String string() {
                StringBuilder sb = new StringBuilder();
                i++; // "
                while (true) {
                    char c = s.charAt(i++);
                    if (c == '"') {
                        return sb.toString();
                    }
                    if (c != '\\') {
                        sb.append(c);
                        continue;
                    }
                    char e = s.charAt(i++);
                    switch (e) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                            i += 4;
                        }
                        default -> throw new IllegalArgumentException("非法转义 \\" + e);
                    }
                }
            }

            Object number() {
                int start = i;
                while (i < s.length() && "+-.eE0123456789".indexOf(s.charAt(i)) >= 0) {
                    i++;
                }
                String raw = s.substring(start, i);
                if (raw.contains(".") || raw.contains("e") || raw.contains("E")) {
                    return Double.parseDouble(raw);
                }
                return Long.parseLong(raw);
            }
        }
    }
}
