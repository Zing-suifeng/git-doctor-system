Git仓库接入体检工具

技术栈：
后端：Spring Boot 3.3.5 + Java 17 + WebClient
前端：Vue 3 + Vite 6 + Element Plus
无数据库、无 Redis——纯 API 调用+静态分析，开箱即用
双平台：GitHub API (`api.github.com`) + Gitee API (`gitee.com/api/v5`)，结构对齐，按网址前缀自动切换
AI建议：可选接DeepSeek（OpenAI兼容），没配key就完全不调用，不影响开箱即用

启用AI建议（可选）：
	set DEEPSEEK_API_KEY=你的key
	set AI_ENABLED=true
	mvn spring-boot:run
不设这两个变量就是纯规则模式，报告里没有aiAdvice字段内容。调用失败或超时自动跳过，不影响评分。

检测项与评分

代码结构：

```
src/main/java/com/intern/doctor/
├── InternTestApplication.java     启动类
├── common/                        统一返回、业务异常、全局异常处理
├── config/                        RepoProps（GitHub/Gitee）、WebClient、AiProps（DeepSeek）配置
├── controller/                    体检接口
├── service/                       RepoApiClient（GitHub/Gitee 双平台外部调用）+ DoctorService（编排）
├── service/impl/                  体检流程实现
├── checker/                       四个检测器，统一实现 Checker 接口
├── dto/                           请求对象、仓库文件
└── vo/                            报告、维度、风险条目
```

总分 100，四项加权：
| 检测项                       | 分值 | 说明                                                                                                   |
|----------------------|-----|--------------------------------------------------------------------|
| 文档与脚本完整度      |  40  | README（13）、依赖清单（13）、启动脚本（14）                      |
| Windows脚本兼容    |  15  | 发现 `.bat`/`.cmd`/`.ps1`/`.vbs` 每个扣 5 分                                     |
| 硬编码路径                 |  15  | 发现 `C:\`、`/Users/`、`/home/` 每个扣 3 分                                   |
| 密钥泄露                    |  30  | `.env` 入库、sk-、AKID、Bearer、私钥、明文口令，每个扣 10 分 |

评分结论：80以上可接入，60-79 需整改，60以下高风险。

更新跟踪设计

下面是设计说明，当前demo没做持久化，报告只在接口返回里，不落库。
体检报告存数据库的时候带一个`base_commit`，就是仓库当前HEAD的SHA，下次检测先拿这个SHA跟仓库现在的比一下，一样就说明没变，不用重跑。判断是否改变有几种做法：
	1. 定时轮询，每隔一小时拉一次HEAD commit跟存的SHA比，不一样就重新体检。实现最简单，不用用户管，但大部分仓库根本没更新，白拉一堆请求。测试时实实在在把API配额打爆了，高频轮询需要避免多次查询。
	2. Webhook，在仓库配个webhook，push的时候平台主动POST过来，收到就重新检测。实时、几乎不浪费请求，但要公网地址，本地开发没法测，还得配secret验签，GitHub和Gitee的配法还不一样。
	3. 手动点重新检测，页面上放个按钮，用户修改了代码就点击。零额外请求、不用部署，最省事，但不自动，用户不点就永远看不到更新。
	4. 条件请求，带上次的ETag或时间，服务端没变返回304，GitHub上304不扣速率限制。比无脑轮询省很多，但Gitee支持不一定靠谱，而且304本身也得发一次请求。

报告存储也有几种选法：
	1. Map本地内存，用`ConcurrentHashMap`，key用platform加owner加repo拼。零依赖、写起来最快、进程内读写也快，但服务一重启全没了，多起一个实例就各存各的，不设上限一定会打爆内存。
	2. MySQL，建一张`repo_check_report`表，唯一键是platform加owner加repo，存`base_commit`、`report_json`、`score`、`checked_at`，命中就`ON DUPLICATE KEY UPDATE`覆盖。重启不丢、多个实例读同一份、还能翻历史看分数变化，但要建表，报告得序列化成JSON塞进去，比Map麻烦一点。
	3. Redis，key拼成`doctor:report:{platform}:{owner}:{repo}`，value存JSON，加个TTL比如24小时自动过期。读写比MySQL快、过期不用自己写定时任务、多实例也能共享，但多一个中间件要维护，Redis挂了得降级成直接重新体检，TTL一到历史也就没了。

避免全量重算：
	确认变了之后可以用GitHub的Compare API `GET /repos/{owner}/{repo}/compare/{oldSha}...{newSha}` 拿变更文件列表，只重扫变更文件，不用整个仓库重来；请求限流或者超时就返回上次报告并标注数据可能过期，仓库删了或者转私有就保留历史报告，状态标成源不可访问。

实际如果我做的话我会看场景再选，没有一刀切的方案：
	检测方式上，自己用或者人少的时候手动点一下最省事，接了公网再说Webhook，仓库多又不能漏更新再考虑轮询或者条件请求，限流这坎得先解决。存的地方也一样，单测和本地跑用Map就够了，要留档、多个人看就上MySQL，量大了、机器多、能接受缓存过期就换Redis，不过挂了得有降级方案，不能让它变成单点。
