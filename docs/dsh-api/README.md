# DSH 本机 HTTP 接口（供 IDEA / 任意程序调用）

DeepSeek Harness（DSH）桌面端在启动时会同时起一个本地 Web 服务，默认只监听 `127.0.0.1`，端口是**每次启动随机分配的**。
当前实例：`http://127.0.0.1:58269`（以 DSH 运行时给出的 URL 为准，下文统一用 `BASE` 表示）。

本目录包含：协议说明（本文件）、IDEA HTTP Client 请求集 `dsh.http`、零依赖 Java 客户端 `DshClient.java`。

---

## 1. 一分钟上手

新建会话 → 发送问题 → 读回答，三步走完：

```bash
# ① 建会话（cwd 可省略，省略时用配置里的默认目录）
curl -s -X POST http://127.0.0.1:58269/api/session/create \
  -H "Content-Type: application/json" \
  -d '{"type":"client-request","rpcId":"'$(uuidgen)'","method":"session/create","payload":{"args":{"request":{"cwd":"C:\\Users\\KSCNYuan\\IdeaProjects\\Nsubtle"}}}}'

# ② 提问（requestId 自备 UUID，用于幂等去重）
curl -s -X POST http://127.0.0.1:58269/api/session/prompt \
  -H "Content-Type: application/json" \
  -d '{"type":"client-request","rpcId":"'$(uuidgen)'","method":"session/prompt","payload":{"args":{"request":{"requestId":"'$(uuidgen)'","sessionId":"session-xxxx","mode":"queue","content":[{"type":"text","text":"解释一下这段代码"}],"clientTimeZone":"Asia/Shanghai"}}}}'

# ③ 读回答（throughSeq = 该会话当前的 asOfSeq）
curl -s -X POST http://127.0.0.1:58269/api/session/page \
  -H "Content-Type: application/json" \
  -d '{"type":"client-request","rpcId":"'$(uuidgen)'","method":"session/page","payload":{"args":{"request":{"address":{"kind":"session","sessionId":"session-xxxx"},"throughSeq":21,"maxMessages":200}}}}'
```

> Windows PowerShell 下 `curl` 是 `Invoke-WebRequest` 的别名，请写 `curl.exe`；或直接用本目录的 `DshClient.java` / `dsh.http`。

---

## 2. 传输格式

### 2.1 请求

```
POST {BASE}/api/{namespace}/{method}
Content-Type: application/json; charset=utf-8
```

```json
{
  "type": "client-request",
  "rpcId": "8f2a1c34-5b6d-4e7f-8a90-b1c2d3e4f506",
  "method": "session/prompt",
  "payload": {
    "args": {
      "request": { }
    }
  }
}
```

| 字段 | 说明 |
| --- | --- |
| `type` | 固定 `"client-request"` |
| `rpcId` | 客户端生成的 UUID（v4），服务端原样回显，用于对齐响应 |
| `method` | 必须是 `{namespace}/{method}`，且与 URL 路径一致，否则 `gateway/bad-request` |
| `payload.args` | 参数对象，键名 = 端点参数的**描述符名**（见 2.2） |

### 2.2 args 的两种形状

- **单参数端点**（绝大多数）：参数名就是 `request`，所以是 `"args": {"request": { ...真实字段... }}`。
- **无参端点**：参数名是 `_request`，即 `"args": {"_request": {}}`。
  例如 `session/list` 传 `"request": {}` 会报
  `args fields do not match the descriptor: missing "_request"; unexpected "request"`。

参数对象里多余的字段默认被忽略（模式为 `additionalProperties: true`），所以不必担心多传。

### 2.3 响应

成功：

```json
{
  "type": "server-response",
  "rpcId": "8f2a1c34-...",
  "result": { "ok": true, "value": { } }
}
```

失败（**HTTP 仍是 200**，靠 `ok` 判断）：

```json
{
  "type": "server-response",
  "rpcId": "8f2a1c34-...",
  "result": {
    "ok": false,
    "error": {
      "code": "gateway/bad-request",
      "message": "session page through seq 30 is past cursor 21",
      "details": { "endpoint": "session/page" }
    }
  }
}
```

只有**不存在的端点**才返回真正的 HTTP 404（纯文本 `not found`）。

### 2.4 鉴权

没有鉴权。服务只绑定回环地址，主机侧把来自 `127.0.0.1` 的调用视为 owner，直接分派（非本机调用才需要经过 remote-access 策略判定）。

---

## 3. 端点总表

`session/*` 由 `@deepseek-ai/dsh-api-session-controller` 提供：

| 端点 | 参数（`request` 内） | 作用 |
| --- | --- | --- |
| `session/list` | （无参，用 `_request`） | 列出会话，带投影快照 |
| `session/create` | `cwd? / workspaceId? / sessionId? / agentPreset?` | 新建（或复用空白）会话 |
| `session/control` | 同上 | 显式恢复/切换会话的工作区与预设 |
| `session/prompt` | `requestId / sessionId / mode / content[] / clientTimeZone?` | 发送一轮提问 |
| `session/cancel` | `sessionId` | 取消当前回合 |
| `session/page` | `address / throughSeq / beforeSeq? / maxMessages? / turnWindow?` | 读历史事件（冷安全，按消息对齐） |
| `session/follow` | `address / maxMessages? / turnWindow? / assistantStream?` | 订阅后续事件（流式，走 WebSocket） |
| `session/projections` | `sessionId` | 读当前投影（标题、用量、todos…）与 `asOfSeq` |
| `session/rename` | `sessionId / title` | 改标题 |
| `session/fork` | 见描述符 | 从某个回合分叉 |
| `session/search` | `query` | 全库搜索会话 |
| `session/selectModel` | `sessionId / provider / model / reasoningEffort?` | 切模型 |
| `session/modelCatalog` | — | 可用模型清单 |
| `session/updateQueue` | `sessionId / itemId / action` | 编辑/撤回/转 steer（针对排队中的消息） |
| `session/attachment` | — | 附件 |
| `session/openWorkspacePath` / `session/canOpenWorkspacePath` / `session/workspacePathApplications` | `path` 等 | 用本机应用打开文件 |
| `session/initializeDefaultModel` | — | 初始化默认模型 |

其他命名空间（同一套信封，换个 namespace 即可）：`skills/list`、`fileReferences/list`、`userQuestions/answer`、`permissionPresets/catalog`、`workspace/*`、`terminal/*`、`goal/*`、`schedule/*` 等。

> 权威定义在 `app.asar` 里的 `*/lib/typert.host.js`：每个端点都带完整 `jsonSchema`，字段不明时就查它。

---

## 4. 常用端点详解

### 4.1 `session/create`

```json
{ "request": { "cwd": "C:\\path\\to\\project", "agentPreset": "standard" } }
```

返回：

```json
{ "sessionId": "session-1f0c...", "agentPreset": "standard" }
```

- `cwd` 与 `workspaceId` 互斥；都省略则用默认目录。
- 传入仍然空白的 `sessionId` 会**复用**该会话，否则新建。

### 4.2 `session/prompt`

```json
{
  "request": {
    "requestId": "b0f1...",
    "sessionId": "session-1f0c...",
    "mode": "queue",
    "content": [{ "type": "text", "text": "你好" }],
    "clientTimeZone": "Asia/Shanghai"
  }
}
```

- `mode`：`queue`（排队，正常提问）或 `steer`（插入当前回合、改变方向）。
- `content[]` 三种 part：`{"type":"text","text":...}`、`{"type":"image","mediaType":"image/png","data":"<base64>"}`、`{"type":"file","receiptId":...}`。
- `requestId` 会被持久化到用户消息上，**重试幂等**：同一个 `requestId` 再发一次直接返回原来的接受结果。
- 返回 `{"accepted": true}` —— 这只表示"已受理"，不是"已回答"。

### 4.3 `session/page`

```json
{
  "request": {
    "address": { "kind": "session", "sessionId": "session-1f0c..." },
    "throughSeq": 21,
    "maxMessages": 200
  }
}
```

- `address` 是**带 kind 的联合体**：`{"kind":"session","sessionId"}` 或 `{"kind":"subagent","parentSessionId","childSessionId","mode"}`。
  只写 `{"sessionId": ...}` 会报 `wire field "request" failed boundary validation`。
- `throughSeq` 必填，**不能超过当前 `asOfSeq`**，否则 `session page through seq N is past cursor M`。
  取法：先调 `session/projections` 拿 `asOfSeq`。
- `beforeSeq` 用于继续往前翻页（读更老的历史）。
- 返回：

```json
{
  "records": [ { "type": "event", "event": { "type": "turn/start", "seq": 4, "time": 1793363999092, "data": { "turn": 1 } } } ],
  "hasMore": false
}
```

### 4.4 `session/projections`

```json
{ "request": { "sessionId": "session-1f0c..." } }
```

返回 `{ "kind": "sequenced", "asOfSeq": 21, "values": { ... } }`，`values` 里有 `title`、`tokenUsage`、`contextPressure`、`turnOutline`、`todos`、`permissions`、`workingDirectory`、`modelSelection` 等。

### 4.5 `session/list`

返回 `{ "items": [ ... ] }`，每项：

```
sessionId, updatedAt, running, blank, cwd, agentAvailable, formatStatus,
projections: { kind, asOfSeq, values: { title, tokenUsage, turnOutline, ... } }
```

`running` 可直接用来判断某会话是否正在产生回答。

---

## 5. 事件流（`session/page` 的 records）

一次问答实际落库的事件序列（这是真实抓到的，会话内容"1+1 等于几"）：

| seq | type | data 要点 |
| --- | --- | --- |
| 0–2 | `permission/preset` / `sandbox/mode` / `approval/policy` | 会话初始策略 |
| 3,5 | `agent/inbox/spliced` | 收件箱变更 |
| 4 | `turn/start` | `{"turn":1}` |
| 6 | `step/start` | `{"turn":1,"step":1}` |
| 7 | `system/message` | 系统提示 |
| 8 | `user/message` | `{content, source:{kind:"user",rpcId,clientTimeZone}, role:"user", id}` |
| 9–11 | `user/message` | 上下文注入（AGENTS.md 等） |
| 12–13 | `request/header` / `request/context` | 发给模型前的快照 |
| 14–18 | `session/title` 等 | 自动起标题 |
| 19 | `assistant/message` | **回答正文在这里** |
| 20 | `step/end` | |
| 21 | `turn/end` | `{"turn":1,"reason":{"kind":"completed"}}` |

取回答正文：

```
records[].event
  找到 type == "assistant/message"
  data.turn / data.step 定位回合与步骤
  data.message.content[] 里 type == "text" 的 text 拼接起来
```

`assistant/message` 的 `data` 完整形状：

```json
{
  "turn": 1,
  "step": 1,
  "message": {
    "role": "assistant",
    "content": [
      { "type": "reasoning", "text": "" },
      { "type": "text", "text": "2" }
    ],
    "source": { "kind": "model", "provider": "deepseek-official", "model": "deepseek-flash" },
    "id": "123c1c5e-..."
  },
  "usage": { "inputTokens": 947, "outputTokens": 2, "cacheReadTokens": 13056, "totalTokens": 14005 },
  "stream": [ /* 逐块增量，含 reasoning / text-chunks / finish */ ]
}
```

`turn/end` 的 `reason.kind` 可能是 `completed`、`cancelled` 等——看到它就表示这一轮结束了。

---

## 6. 轮询式等待（不接 WebSocket 也能用）

```text
prompt()
oldEnd = 当前最大 turn/end 的 seq（没有就是 -1）
loop:
    asOf   = session/projections(sessionId).asOfSeq
    page   = session/page(sessionId, throughSeq = asOf)
    if 存在 turn/end 且 seq > oldEnd: 结束
    sleep 1s（最多 3~5 分钟）
```

`DshClient.java` 就是这么实现的：只读事件、不消费流，断线重连也只是重读一遍。

---

## 7. 错误码

| code | 含义 |
| --- | --- |
| `gateway/bad-request` | method 与路径不一致、资源状态不对（如 throughSeq 越界） |
| `gateway/arguments-invalid` | `payload.args` 的键名/字段与描述符不匹配 |
| `gateway/input-invalid` | 字段值没过边界校验（常见：`address` 少了 `kind`） |
| `gateway/internal` | 服务端内部错误 |
| `gateway/cancelled` | 调用被取消 |
| `remote-access/denied` | 非本机调用被策略拒绝 |

---

## 8. 坑与限制

1. **端口每次启动都可能变。** 客户端要把 `BASE` 做成配置项，不要在代码里写死。
2. **没有 HTTP 鉴权。** 只要本机能发请求就能用；不要把它暴露到公网。
3. **没有删除会话的端点。** `session/delete`、`session/dispose`、`session/remove` 都不存在（404），清理会话请用 GUI。
4. **交互式确认要额外处理。** 权限审批、提问会以 `userQuestions/answer` 之类的方式索要回执；纯问答（不触发写文件/跑命令）不会遇到。需要 headless 长跑时，建议给会话配一个宽松的权限预设。
5. **流式输出走 WebSocket。** `/api/remote.mux` 是独立通道，协议与一元调用不同；只想拿最终结果就不必碰它。
6. **`throughSeq` 只是"读到哪"的上界**，读历史每次都要自己给；它不会因为新事件自动变大。
7. **Java 的 `HttpClient` 必须钉死 HTTP/1.1。** 默认它会先尝试 h2c 升级，这个服务不认，直接表现为
   `java.io.IOException: HTTP/1.1 header parser received no bytes`（看起来很像"服务没起来"）。
   加一句 `.version(HttpClient.Version.HTTP_1_1)` 就好，`DshClient.java` 里已经这么做了。

---

## 9. 在 IDEA 里用

三种姿势，按需要挑：

### 9.1 直接用 IDEA 的 HTTP Client（最快验证）

用 IDEA 打开 `dsh.http`，改顶部两个变量，点行号左边的 ▶ 就能发请求。
`{{$uuid}}` 是 IDEA 内置的动态变量，每次发送自动生成；把返回的 `sessionId` 填进 `@sid` 再发下一条即可。

### 9.2 跑 Java 客户端（自带 JSON 解析，零依赖）

```bash
cd docs/dsh-api
java DshClient.java list
java DshClient.java ask "用一句话解释 NeoForge 的 DataGen 流程"
java DshClient.java ask -s session-1f0c... "继续，给出示例代码"
java DshClient.java history session-1f0c...
```

Java 21 可以直接跑单个 `.java` 文件（源码模式启动），所以在 IDEA 里右键 `Run 'DshClient.main()'` 即可，也可以把它 `install` 到你的 mod 工程里当工具类用（只依赖 JDK，不碰 Minecraft 的类加载器）。

### 9.3 自己写（任何语言）

记住三件事就够：URL 是 `/api/{ns}/{method}`；body 是 `{type, rpcId, method, payload.args}`；成功判断看 `result.ok`。
