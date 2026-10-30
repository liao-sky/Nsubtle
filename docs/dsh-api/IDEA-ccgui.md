# 让 IDEA 里的 ccgui 插件用上 DSH

> 适用：IntelliJ IDEA 2026.1 + 插件 `idea-claude-code-gui` 0.6.2 + DeepSeek Harness 0.2.1-rc.3
> 结论：ccgui **自带** DSH 通道，不需要自己写桥。它默认连 `http://127.0.0.1:3080`，
> 只要该端口上有一个 `dsh web` 宿主，插件就能直接认领（adopt）并正常对话。

## 1. 现象与根因

| 现象 | 原因 |
| --- | --- |
| ccgui 里选 DSH 后一直连不上 | 插件默认找 `127.0.0.1:3080`，本机没有该端口的宿主 |
| 插件「启动宿主」按钮无效 | 插件用 `resolveCliSpawn` 拉起 `dsh web` 时把 `%~dp0` 解析成了插件自己的 `ai-bridge` 目录，`dsh.cmd` 根本没跑起来 |
| `~/.codemoss/dsh-web.log` 里的报错 | `'"...\plugins\idea-claude-code-gui\ai-bridge\..\..\..\..\DeepSeek Harness.exe"' 不是内部或外部命令` |
| 桌面端明明有个 `127.0.0.1:58269` | 那是桌面 App 自己起的宿主，端口由桌面 App 决定，插件的默认端口是 3080，两者对不上 |

插件的端口/自动拉起行为来自环境变量（`ai-bridge/services/dsh/supervisor.js`）：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DSH_HOST` | `127.0.0.1` | 宿主地址 |
| `DSH_PORT` | `3080` | 宿主端口 |
| `DSH_AUTO_START` | 开（`false` 才关） | 连不上时是否自己拉起 `dsh web` |
| `DSH_BIN` | 自动搜索 | `dsh` 可执行文件 |
| `DSH_HOME` | `~/.dsh` | DSH 数据目录（含 `.credentials.yaml`） |
| `DSH_PRESET` / `DSH_WIRE` | 空 / 自动 | 预设、wire 方言（`legacy`/`modern`） |

## 2. 已落地的修复（推荐，零环境变量）

在本机固定端口 3080 跑一个 `dsh web` 宿主，并入开机自启：

- 启动器：`C:\Users\KSCNYuan\AppData\Local\dsh-web\start-dsh-web.cmd`
  （阻塞式运行 `dsh web --host 127.0.0.1 --port 3080 --no-open`，日志追加到 `C:\Users\KSCNYuan\.dsh\dsh-web-3080.log`）
- 停止器：`C:\Users\KSCNYuan\AppData\Local\dsh-web\stop-dsh-web.cmd`
- 重启器：`C:\Users\KSCNYuan\AppData\Local\dsh-web\restart-dsh-web.cmd`（见 §4.1，卡住时用这个）
- 开机自启：`%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup\dsh-web-3080.vbs`
  （隐藏窗口调用上面的 cmd）

验证：

```powershell
netstat -ano | Select-String ':3080\s+.*LISTENING'   # 有监听即成功
```

插件侧状态（等价于 ccgui 的 `get_dsh_status`）：

```json
{"success":true,"provider":"dsh","installed":true,"bin":"...\\runtime\\cli\\bin\\dsh.cmd",
 "origin":"http://127.0.0.1:3080","hostRunning":true,"ownership":"adopted",
 "describe":{"provider":"deepseek-official","model":"deepseek-flash","reasoningEffort":"high"},
 "dialect":"modern"}
```

`ownership: "adopted"` 表示插件只是**认领**了已有宿主，没去拉起，正是我们要的。

## 3. 备选方案：直接复用桌面端那个宿主

不想常驻第二个实例时，可以让插件指向桌面 App 的端口，并禁止它自己拉起：

```powershell
setx DSH_PORT 58269
setx DSH_AUTO_START false
```

然后**完全退出并重启 IDEA**（环境变量只在进程启动时继承）。

- 端口以运行中的 GUI 地址为准（当前 `http://127.0.0.1:58269`）；桌面 App 重启后端口可能变化，届时改 `DSH_PORT` 即可。
- 桌面 App 关掉后 ccgui 就用不了了，这是该方案的主要代价。

## 4. 排障

| 症状 | 检查 |
| --- | --- |
| `host.describe` 返回 401 | 正常：modern wire 先用 `.credentials.yaml` 里的 `secret:` 签 `dsh-auth-*` cookie，签不出才会报 “requires browser-session authentication” |
| `requires browser-session authentication and no cookie could be minted` | `DSH_HOME` 指错，或该目录下 `.credentials.yaml` 没有 `secret:` |
| mux 连不上 | 宿主只监听 127.0.0.1，端口必须一致；`/api/remote.mux` 走 WebSocket |
| 想确认插件真能对话 | 看 ccgui 是否输出流式内容；等价手动测试见 `docs/dsh-api/README.md` 与 `DshClient.java` |
| `session "..." is already owned by an active write handle` | 宿主进程里那个会话的写句柄没释放，见 §4.1 |

### 4.1 `is already owned by an active write handle`

原文：

```
dsh session.prompt failed: session "session-xxxxxxxx-..." is already owned by an active write handle
```

机制（`app.asar` 里的 session 持久化层）：

- 每个宿主进程用一张 `writers` 表跟踪“谁正在写哪个会话”，重复取写句柄就抛 `SessionAlreadyOwnedError`（`claimWrite(id) { if (this.writers.has(id)) throw new SessionAlreadyOwnedError(id); }`）。
- 取写句柄时会同时向会话目录申请**跨进程文件租约**（`acquireLease(...)`），所以“跨进程抢同一会话”报的是租约错误，而**这条报错只可能来自宿主自身**：上一个 turn 没走到 `turn/end`，写句柄挂在那儿了。
- 常见诱因：回答途中关掉 IDEA / 停掉桥进程、宿主崩溃或热重启、在 GUI 里强杀正在跑的 turn。

处理：

```powershell
& "C:\Users\KSCNYuan\AppData\Local\dsh-web\restart-dsh-web.cmd"
```

重启宿主会清空进程内句柄表（不影响 `~/.dsh` 里的会话数据），随后在 ccgui 里重发即可。
会话本身并没有损坏：`session/list` 里 `running:false`、`session/projections` 也能正常读，只是写句柄被占。

## 5. 注意

- ccgui 0.6.2 的 DSH 通道**没有独立设置面板暴露全部参数**（消息资源里没有 dsh 文案），实际行为由环境变量与上面这些默认值决定。
- 两个宿主（桌面端 58269、本地 3080）共享同一个 `~/.dsh`，会话列表是共用的；在 ccgui 里删会话会删到真实会话，慎用删除。
- 宿主日志：`~/.dsh/dsh-web-3080.log`（stdout）与 `~/.dsh/dsh-web-3080.log.err`（stderr）。
