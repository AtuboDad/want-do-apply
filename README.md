# 钱包请等等 · 消费审批小程序

> 想买的东西先丢进广场，让群友投个票。通过就买，驳回就省。
> 一个把「冲动消费」变成「公开审批」的小程序。

三个端、一套仓库：

| 子项目 | 说明 | 技术栈 |
| --- | --- | --- |
| `backend/` | 后端服务 | Spring Boot 4.1 + JDK 21 + MyBatis-Plus + Sa-Token + Redisson（基于 RuoYi-Vue-Plus 6.0） |
| `admin-ui/` | 管理后台 | Vue 3.5 + TS + Element Plus + Vite 8 + UnoCSS（基于 plus-ui） |
| `miniapp/` | 微信小程序 | uni-app Vue3 + TS + Vite 5 |

---

## 一、产品逻辑

```
用户发起审批单（商品 + 价格 + 为什么想买 + 不买的话用什么替代 + 审批多久）
        ↓
发到广场 / 审批群，所有人都能来「批阅」：通过 / 驳回，可写批语
        ↓
审批时长内自由表态，到点（或发起人手动结束）按「少数服从多数」出结果
        ↓
通过 → 允许买；驳回 → 省下一笔，记入「已省金额」
        ↓
批语可以送花、可以评论，热闹起来才有说服力
```

### 核心机制

- **双轨可见性**：`visibility=0` 进公开广场；`visibility=1` 只在审批群内可见（群成员凭邀请码加入）。
- **审批时长**：发起人下单时自选 `duration_hours`（1/2/6/12/24 小时，接口允许 1-72），写入 `expire_time`。
- **结算规则**：**只看时间，不看票数**——审批时长内谁都能表态，到点后按「少数服从多数」结算（平票算驳回，向省钱一侧倾斜）；0 票算流拍。发起人可「撤回审批」或「提前结束」（按当时票数立即结算，0 票时不允许）。结算用条件更新（`status=0` 才允许更新）防并发重复结算，另有每 5 分钟一次的定时兜底。
- **补充说明**：审批中发起人可以随时补一段说明（`extra_note`），显示在审批规则下方，结案后不能再改。
- **防刷**：每人每天最多发 10 单、批 200 次；不能批自己的单；同一单每人只能批一次；信用分低于 60 会被限制。
- **举报**：举报审批单 / 批条 / 评论，管理端可「同步下架」被举报内容。

### 双令牌体系

管理端与小程序端鉴权完全隔离，互不污染：

| | 管理端 | 小程序端 |
| --- | --- | --- |
| 令牌 | Sa-Token JWT | 独立 HS256 JWT（hutool-jwt） |
| Header | `Authorization: Bearer xxx` | `App-Token: xxx` |
| 有效期 | 由 Sa-Token 配置 | 30 天 |
| 拦截 | Sa-Token 拦截器 | `AppAuthInterceptor` 拦截 `/app/**` |

---

## 二、目录结构

```
want-do-apply/
├── backend/
│   ├── ruoyi-admin/                     # 启动模块（配置、主类）
│   ├── ruoyi-modules/
│   │   └── ruoyi-apply/                 # ★ 业务模块
│   │       └── src/main/java/org/dromara/apply/
│   │           ├── config/              # AppAuthInterceptor / AppWebConfig / ApplySettleJob
│   │           ├── constant/            # ApplyConstants
│   │           ├── controller/app/      # 小程序端接口（/app/**）
│   │           ├── controller/admin/    # 管理端接口（/apply/**）
│   │           ├── domain/{,bo,vo}/      # 实体 / 入参 / 出参
│   │           ├── mapper/              # Mapper 接口
│   │           ├── service/             # 业务逻辑
│   │           └── util/AppLoginHelper   # 小程序令牌签发与解析
│   └── script/sql/
│       ├── ry_apply.sql                 # 10 张业务表
│       └── ry_apply_menu.sql            # 菜单与按钮权限（menu_id 2000~2099）
├── admin-ui/
│   └── src/
│       ├── api/apply/                   # 管理端接口封装
│       ├── utils/apply-format.ts        # 状态/品类文案
│       └── views/apply/
│           ├── dashboard/               # 数据看板
│           ├── order/                   # 审批单管理
│           ├── vote/                    # 批条管理
│           ├── comment/                 # 评论管理
│           ├── group/                   # 审批群
│           ├── user/                    # 小程序用户
│           └── report/                  # 举报处理
├── miniapp/
│   └── src/
│       ├── api/                         # order / vote / group
│       ├── components/                  # NavBar / Empty / OrderCard / VoteCard / OrderSheet / OrderDetailPanel
│       ├── pages/                       # 广场 / 详情 / 发布 / 审批群 / 我的 / 通知
│       ├── static/tabbar/               # tabBar 图标（81x81 PNG，灰/橙两态）
│       ├── store/user.ts
│       └── utils/                       # request / auth / config / format
├── scripts/                             # 启动脚本、tabBar 图标生成器
└── docs/                                # 建库、探活、接口冒烟脚本
```

> tabBar 图标由 `scripts/gen_tabbar_icons.py`（Pillow 绘制）生成，改形状/配色后重跑即可，会同时输出一张 2×4 的预览图。

---

## 三、数据库表

| 表 | 说明 |
| --- | --- |
| `app_user` | 小程序用户（昵称、头像、信用分、封禁状态、发起/批阅/被采纳计数） |
| `apply_order` | 审批单（商品、价格、理由、替代品、票数、状态、可见性、审核状态、置顶） |
| `apply_vote` | 批条（结论 + 批语 + 获赞） |
| `apply_vote_like` | 批条点赞 |
| `apply_comment` | 评论（支持二级回复） |
| `apply_favorite` | 收藏 |
| `apply_group` | 审批群（邀请码、群公告、成员数） |
| `apply_group_member` | 群成员 |
| `apply_report` | 举报（审批单 / 批条 / 评论） |
| `app_notice` | 站内通知 |

所有业务表带 `del_flag` 逻辑删除；主键为雪花 ID（`ASSIGN_ID`）。

---

## 四、本地启动

### 1. 准备中间件

需要一个 MySQL 8 和一个 Redis 6+。

主机、库名等非敏感项在 `application-dev.yml` 里有默认值，可用环境变量覆盖；
**密码走 `application-local.yml`**（见下面「本地私有配置」），或临时用环境变量：

```bash
export DB_HOST=127.0.0.1
export DB_NAME=want_do_apply
export DB_USERNAME=root
export DB_PASSWORD=你的密码
export REDIS_HOST=127.0.0.1
```

> ⚠️ Redis 若未开启 AUTH，`application-dev.yml` 里的 `password` 必须**整行注释掉**，不能留空值——Redisson 见到空串仍会发 AUTH 命令而报错。

### 2. 建库导表

```bash
cd backend/script/sql
# 先建库
mysql -h$DB_HOST -u$DB_USERNAME -p -e "create database want_do_apply default charset utf8mb4"
# 基础框架表 + 业务表（ry_vue.sql 来自 RuoYi-Vue-Plus 主仓）
mysql -h$DB_HOST -u$DB_USERNAME -p want_do_apply < ry_vue.sql
mysql -h$DB_HOST -u$DB_USERNAME -p want_do_apply < ry_apply.sql
mysql -h$DB_HOST -u$DB_USERNAME -p want_do_apply < ry_apply_menu.sql
```

或直接跑 `docs/_db_import.py`（按行分号切分，幂等）。

### 3. 起后端

先按上一节创建 `application-local.yml`（数据库密码、微信 AppSecret），然后：

```bash
cd backend
mvn -pl ruoyi-admin -am package -DskipTests

# 方式一：用脚本（已封装端口 / profile / JDK 检查）
../scripts/start-backend.sh

# 方式二：手动起，注意必须带 dev,local 两个 profile
java -jar ruoyi-admin/target/ruoyi-admin.jar --server.port=8080 --spring.profiles.active=dev,local
```

> ⚠️ 必须显式指定 `--server.port=8080`。某些 IDE / 宿主进程会注入 `SERVER__PORT` 环境变量，Spring Boot 松散绑定会把它当成 `server.port` 覆盖掉。

> ⚠️ 本机如果装了多个 JDK，注意用 **JDK 21** 启动（`java -version` 先确认），JDK 8 会报 `UnsupportedClassVersionError`。脚本可用 `JAVA_BIN=/path/to/jdk-21/bin/java` 指定。

> ⚠️ 少了 `local` profile 就会连不上库（密码读不到），报 `Access denied ... (using password: NO)`。

启动后：
- 管理端接口：`http://127.0.0.1:8080`
- 小程序静态资源：`http://127.0.0.1:8080/app-file/**`（映射到 `upload/app`）

### 4. 起管理后台

```bash
cd admin-ui
corepack pnpm install
corepack pnpm dev
```

> 如果本机 `pnpm` 命令报 `no @pnpm/exe.win32-x64 native binary was found`，说明它是坏的独立安装包，
> 用 `corepack pnpm ...` 代替即可。国内网络可先 `export npm_config_registry=https://registry.npmmirror.com`
> （老的 `registry.npm.taobao.org` 已停止服务）。

登录后左侧会出现「消费审批」目录（7 个页面）。默认超管账号见 `ry_vue.sql` 的 `sys_user`（`admin` / `admin123`）。

### 5. 起小程序

```bash
cd miniapp
npm install
npm run dev:mp-weixin      # 产物在 dist/dev/mp-weixin，用微信开发者工具打开
```

**微信登录已接入真实 AppID**，凭据只存在本地 `application-local.yml`（不入库）：

- `miniapp/src/manifest.json` 的 `mp-weixin.appid` → 已填真实 AppID
- 后端 `WX_APPID` / `WX_SECRET` / `WX_MOCK=false` → 见 `application-local.yml`
- 上线前还要把 `miniapp/src/utils/config.ts` 的 `BASE_URL` 改成你的 HTTPS 域名
  （微信要求 request 合法域名必须是 HTTPS，`127.0.0.1` 只能在开发者工具「不校验合法域名」时用）

---

## 五、配置项

### 后端（`application.yml` 的 `apply` 段，均可用环境变量覆盖）

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `APPLY_JWT_SECRET` | `want-do-apply-2026-change-me-in-production` | 小程序令牌签名密钥，**生产必须改** |
| `APPLY_UPLOAD_DIR` | `upload/app` | 小程序图片上传目录 |
| `APPLY_UPLOAD_BASE_URL` | 空 | 图片外链前缀；留空则按当前请求域名拼接 |
| `WX_APPID` | 空 | 小程序 AppID |
| `WX_SECRET` | 空 | 小程序 AppSecret |
| `WX_MOCK` | `true` | 为 `true` 时完全不调微信接口，任何人都能登录——**仅用于没有 AppID 时本地联调** |
| `WX_ALLOW_DEV_CODE` | `false` | 是否放行前端伪造的 `dev:xxx` 凭证（H5 调试、接口冒烟测试用）——**生产必须 `false`** |

> 生产环境务必 `WX_MOCK=false` + `WX_ALLOW_DEV_CODE=false` + 填好 AppID/Secret，三者缺一都可能被伪造登录。
> AppSecret 不要写进 Git，放 `application-local.yml` 或环境变量。

### 本地私有配置 `application-local.yml`

数据库密码和微信 AppSecret 都放这里，文件名已被 `.gitignore` 忽略。

启用方式是**多 profile 叠加**：`--spring.profiles.active=dev,local`。
Spring Boot 中后激活的 profile 优先级更高，所以 `local` 会覆盖 `dev` 里的兜底值。

> ⚠️ 别用 `spring.config.import` 去加载它 —— 导入文件的优先级**低于**导入它的文件，
> `application-dev.yml` 里的 `${DB_PASSWORD:}` 空值会反过来盖掉本地密码，现象是
> `Access denied ... (using password: NO)`，排查起来很绕。

模板：

```yaml
spring:
  datasource:
    dynamic:
      datasource:
        master:
          username: your-db-user
          password: 'your-db-password'

apply:
  wx:
    app-id: wx1234567890abcdef
    secret: your-app-secret
    mock: false
    allow-dev-code: true   # 本地开发可开，公网部署前改 false
```

### 小程序登录的两种模式

| | `WX_MOCK=true` | 真实模式（`mock: false`） |
| --- | --- | --- |
| 登录凭证 | 任何 code 都行，`dev:xxx` → 固定用户 | 必须微信 `uni.login` 拿到的真 code |
| 换 openid | 本地按 md5 伪造 | 真实调 `sns/jscode2session` |
| 适用 | 没 AppID 时调页面 | 开发者工具 / 真机 / 生产 |
| 多账号联调 | 清缓存换一个 `wda_dev_id` | 换微信账号登录 |

真实模式下还有个 `allow-dev-code` 开关独立控制「是否接受前端伪造的 `dev:xxx`」：
本地把后端跑在 `mock=false + allow-dev-code=true`，就能一边走真实微信校验、一边让 H5 和接口冒烟测试用假 code。
**这个组合只能本地用**，公网部署前务必把 `allow-dev-code` 关掉。

小程序端也做了对应防护：`npm run build:*` 产出的正式包里 `DEV_LOGIN_FALLBACK` 为 `false`，
`uni.login` 失败时不再降级成假 code，而是直接报错——避免线上包把用户静默伪装成调试账号。

---

## 六、接口速览

### 小程序端 `/app/**`（`App-Token` 鉴权，登录/配置接口除外）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/app/auth/login` | 微信 code 登录，返回 token |
| GET | `/app/auth/config` | 小程序端开关配置 |
| GET | `/app/auth/me` | 我的信息 |
| PUT | `/app/auth/me` | 修改资料 |
| GET | `/app/order/square` | 广场列表（latest / hot / pending） |
| POST | `/app/order` | 发起审批单 |
| GET | `/app/order/{id}` | 审批单详情（含 myVoted / favorited / mine） |
| GET | `/app/order/mine` | 我的：`type=apply` 我提交的 / `vote` 我处理的 / `favorite` 我围观的 |
| POST | `/app/order/{id}/cancel` | 撤回审批 |
| POST | `/app/order/{id}/note` | 补充说明（发起人，仅审批中） |
| POST | `/app/order/{id}/finish` | 提前结束，按当前票数立即结算，返回结算后状态 |
| POST | `/app/order/{id}/favorite` | 围观 / 取消围观 |
| POST | `/app/vote` | 提交批阅 |
| GET | `/app/vote/list` | 某单的批条列表 |
| POST | `/app/vote/{id}/like` | 批条点赞 |
| GET/POST | `/app/comment` | 评论列表 / 发评论 |
| GET/POST | `/app/group` | 我的群 / 建群 |
| POST | `/app/group/join` | 凭邀请码加入 |
| GET/POST | `/app/notice` | 通知列表 / 已读 |
| POST | `/app/report` | 举报 |
| POST | `/app/file/upload` | 上传图片 |

### 管理端 `/apply/**`（Sa-Token 鉴权）

`/apply/dashboard/overview`、`/apply/order/{list,{id},audit,top}`、`/apply/vote/list`、
`/apply/comment/list`、`/apply/group/list`、`/apply/user/list`、`/apply/report/{list,pending,handle}` — 均为标准 RuoYi 风格（`R<T>` + `PageResult`）。

权限标识：`apply:order:{list,query,audit,edit,remove}`、`apply:vote:{list,remove}`、
`apply:comment:{list,remove}`、`apply:group:{list,remove}`、`apply:user:{list,edit}`、
`apply:report:{list,handle,remove}`、`apply:dashboard:view`。

---

## 七、验证

```bash
cd docs
cp .env.example .env      # 填好 WDA_DB_PWD 等连接信息（.env 不进 Git）

python _probe_mysql.py    # MySQL 连通性
python _probe_redis.py    # Redis 连通性

python _smoke_api.py      # 小程序端 29 条接口断言
python _smoke_admin.py    # 管理端 37 条接口断言
```

**`_smoke_api.py`**（小程序端）覆盖：登录两个用户 → 发单 → 广场列表 → 详情 → 批阅 → 重复批阅拦截 → 自批拦截 → 批条列表与点赞 → 评论 → 个人中心 → 通知 → 建群与邀请码入群 → 举报 → 收藏 → 未登录 401。

**`_smoke_admin.py`**（管理端）覆盖：加密登录 → 看板指标与趋势 → 六个列表接口 → 造测试单 → 按 ID 查询 → 置顶/取消 → 下架/恢复 → 举报并同步下架 → 编辑用户信用分 → 删除审批单 → 未登录 401。

关于管理端测试的三个坑（脚本里都已经处理）：

| 坑 | 说明 |
| --- | --- |
| 请求体加密 | `/auth/login` 带 `@ApiEncrypt`，body 必须 AES 加密、AES 秘钥再用 RSA 公钥加密放进 `encrypt-key` 头。脚本从 `application.yml` 的 `api-decrypt.privateKey` 推导公钥，不用手工填。 |
| `clientid` 头 | RuoYi 会校验 header 里的 `clientid` 与 token 里的是否一致，漏了就是「登录状态异常」。 |
| 验证码 | `captcha.enable: true` 时脚本登录不了。启动后端时加 `CAPTCHA_ENABLE=false` 即可（`_toggle_captcha.py` 是改库的备选方案）。 |

---

## 八、已知限制

- 小程序为个人未认证主体能力范围内实现，**不含支付**；「通过就买」是行为约束，不是支付动作。
- 图片存本地磁盘（`upload/app`），多实例部署需换成对象存储。
- 结算依赖「定时任务 + 请求触发」双保险，若服务长时间停机，重启后首次访问或下个 5 分钟周期会补结算。
