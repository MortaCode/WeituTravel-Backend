# 微旅旅行 (WeituTravel) 后端服务

基于 Spring Boot 3.5 + Spring AI 的智能旅游平台后端，集成 AI 对话、高并发秒杀、热点点赞等技术模块。

## 技术栈

| 技术          | 版本       | 用途                        |
|---------------|-----------|-----------------------------|
| Spring Boot   | 3.5.14    | 应用框架                     |
| Spring AI     | 1.1.5     | AI 模型集成                  |
| MyBatis Plus  | 3.5.15    | ORM / 数据库操作              |
| MySQL         | -         | 业务数据存储                  |
| Redis         | -         | 缓存 / 分布式锁 / 库存预扣     |
| Redisson      | 4.3.1     | Redis 分布式客户端            |
| Lucene        | 9.6.0     | BM25 全文检索                |
| Kryo          | 5.5.0     | 高性能序列化                  |
| Caffeine      | -         | 本地缓存                     |
| Hutool        | 5.8.38    | 工具库                       |
| Lombok        | -         | 代码简化                     |

## 项目结构

```
src/main/java/com/myy/weitutravel/
├── WeituTravelBackendApplication.java   # 启动类
├── chat/                                # AI 对话模块
│   ├── controller/
│   │   ├── ChatController.java          # 对话接口
│   │   ├── ChatMemoryController.java    # 记忆管理接口
│   │   └── ModelController.java         # 模型列表接口
│   ├── entity/                          # 实体：ChatSession, ChatMessage, ChatSnapshot
│   ├── mapper/                          # MyBatis Mapper
│   ├── service/
│   │   ├── ChatSessionService.java      # 会话服务
│   │   ├── ChatMessageService.java      # 消息服务
│   │   ├── ChatSnapshotService.java     # 快照服务
│   │   ├── ModelSelectService.java      # 模型选择（策略模式）
│   │   ├── advisor/
│   │   │   ├── MemoryAdvisor.java       # 对话记忆顾问（上下文管理）
│   │   │   └── RAGAdvisor.java          # RAG 知识检索顾问（预留）
│   │   ├── handler/
│   │   │   ├── MarkdownReaderHandler.java  # Markdown 文档加载器
│   │   │   └── RecursiveSplitHandler.java  # 递归文本分块
│   │   ├── memory/
│   │   │   ├── ChatMemoryService.java      # 三层记忆管理（本地/Redis/MySQL）
│   │   │   ├── ChatMemoryCompressService.java # 记忆压缩编排
│   │   │   ├── CompressionService.java     # AI 摘要压缩核心
│   │   │   └── KryoSerializer.java         # 序列化工具
│   │   └── retriever/
│   │       ├── Bm25Retriever.java          # BM25 关键词检索（预留）
│   │       ├── HybridSearchService.java    # 混合检索融合（预留）
│   │       ├── HierarchicalRetrieverService.java  # 分层检索（预留）
│   │       └── HierarchicalRetrieverSingleService.java
│   └── vo/                              # ChatMessageVo, ChatModel
├── flashSale/                           # 秒杀模块
│   ├── controller/
│   │   └── BookingController.java       # 秒杀预订接口
│   ├── entity/Stock.java                # 库存实体
│   ├── mapper/
│   ├── service/
│   │   ├── RedisPreDeductService.java   # Redis Lua 原子预扣
│   │   ├── DatabaseUpdateService.java   # MySQL 乐观锁兜底
│   │   ├── MqConsumerService.java       # RocketMQ 消息生产者（预留）
│   │   └── OrderConsumerService.java    # RocketMQ 消息消费者（预留）
│   └── vo/BookingRequest.java
├── thumb/                               # 点赞模块
│   ├── controller/BlogController.java   # 博客 & 点赞接口
│   ├── entity/Blog.java, Thumb.java
│   ├── job/
│   │   ├── SyncThumb2DBJob.java         # Redis → MySQL 定时同步
│   │   └── SyncThumb2DBCompensateJob.java # 补偿任务
│   ├── manage/CacheManager.java         # 多级缓存 + 热点检测
│   ├── mapper/
│   ├── service/
│   │   ├── BlogService.java
│   │   ├── ThumbService.java            # 传统点赞（直接写库）
│   │   ├── ThumbUPService.java          # 优化点赞（Redis Lua → 定时入库）
│   │   ├── HeavyKeeper.java             # 热点 Key 检测算法
│   │   └── TopK.java                    # TopK 接口
│   └── vo/                             # BlogVo, MsgVo, AddResult, Item 等
├── user/                                # 用户模块
│   ├── controller/UserController.java   # 登录 / 获取当前用户
│   ├── entity/User.java
│   ├── service/UserService.java         # Session 管理
│   └── vo/UserloginVo.java
└── common/                              # 公共模块
    ├── api/Result.java, CodeEnum.java   # 统一响应体
    ├── config/
    │   ├── ChatConfig.java              # ChatClient Bean 配置（DeepSeek + Qwen）
    │   ├── RedisConfig.java             # RedisTemplate 配置
    │   ├── RedissonConfig.java          # Redisson 配置
    │   ├── RedisScriptConfig.java       # Lua 脚本加载
    │   ├── DataSourceConfig.java        # 多数据源配置
    │   └── HybridRetrievalConfig.java   # 混合检索参数配置
    ├── constants/Constants.java
    ├── exception/
    │   ├── BizException.java
    │   └── GlobalExceptionHandler.java
    └── utils/RedisUtil.java
```

## 核心模块详解

### 1. AI 对话模块 (`chat/`)

支持 **DeepSeek** 和 **Qwen（通义千问）** 双模型，通过策略模式灵活切换。

**对话接口** — `POST /chat/ai`

```json
{
  "sessionId": "xxx",
  "userInput": "帮我规划北京3日游",
  "modelName": "deepseek"
}
```

**架构设计：**

```
用户请求 → ChatController → ModelSelectService(模型选择)
                                  ↓
                            ChatClient.prompt()
                              .advisors(memoryAdvisor)
                              .call()
                                  ↓
                            MemoryAdvisor（顾问链）
                              ├── 加载历史消息
                              ├── 增强 Prompt（拼接历史）
                              ├── 执行 LLM 调用
                              └── 保存对话
```

**对话记忆 (MemoryAdvisor)：**

- 实现 Spring AI 的 `CallAdvisor` + `StreamAdvisor` 接口，自动拦截每次对话
- 自动管理会话 ID，首次对话自动生成
- 每次请求自动拼接历史消息到 Prompt 中
- 每次响应自动保存用户消息和助手回复

**三层记忆管理 (ChatMemoryService)：**

| 层级   | 存储                       | 特点                          |
|--------|---------------------------|-------------------------------|
| L1     | ConcurrentHashMap（本地）  | 最快，JVM 内直接访问            |
| L2     | Redis（Kryo 序列化）       | 分布式缓存，24h TTL            |
| L3     | MySQL（t_session/t_message）| 持久化，可恢复                 |

读取优先级：L1 → L2 → L3（命中即返回并回填上层缓存）

**记忆压缩 (CompressionService)：**

当会话消息超过 30 条时触发压缩：
1. 提取新消息（上次压缩后的部分）
2. 调用 AI 生成对话摘要（≤500字）
3. 与历史摘要合并
4. 返回结构：`[摘要 SystemMessage] + [最近10条完整消息]`

有效解决长对话下的 Token 超限问题。

**RAG 知识检索（已预留，默认注释）：**

- BM25 关键词检索（基于 Lucene 9.6 内存索引）
- PGVector 语义向量检索（PostgreSQL + pgvector）
- 混合检索融合（加权分数 + RRF 倒数排序融合）
- Markdown 文档加载与递归分块

### 2. 秒杀模块 (`flashSale/`)

**预订接口** — `POST /booking/book`

```
请求 → Redis Lua 原子预扣 → MySQL 乐观锁兜底 → 返回
         (第一道防线)         (第三道防线)
```

**三道防线架构：**

```
┌─────────────────────────────────────┐
│  第一道：Redis 预扣减（Lua 脚本）      │  ← 抗 98%+ 并发
│  redis.call('decr', key)             │    原子操作，无锁化
├─────────────────────────────────────┤
│  第二道：RocketMQ 消息队列（预留）      │  ← 削峰填谷
│  异步发送订单消息，限流消费             │
├─────────────────────────────────────┤
│  第三道：MySQL 乐观锁兜底               │  ← 绝不超卖
│  UPDATE stock SET stock = stock - 1   │    version 版本号控制
│  WHERE stock_id = ? AND stock > 0     │
└─────────────────────────────────────┘
```

**Redis 预扣核心（Lua 脚本）：**

```lua
local stock = redis.call('get', key)
if stock and tonumber(stock) > 0 then
    return redis.call('decr', key)
else
    return -1
end
```

**库存预热：** 应用启动时通过 `@PostConstruct` 将库存加载到 Redis。

**统计接口** — `GET /booking/stats`：实时查看成功/失败次数和剩余库存。

### 3. 点赞模块 (`thumb/`)

**点赞/取消点赞** — `GET /blog/thumb?blogId=xxx`

**优化方案 (ThumbUPService)：**

```
用户操作 → Redis Lua 原子执行
              ├── 写入临时计数桶（10秒粒度）
              └── 更新用户点赞状态
                    ↓
          定时任务每 10s 执行
              ├── 读取上一时间片的临时数据
              ├── 批量插入/删除点赞记录
              ├── 批量更新博客点赞数
              └── 清理 Redis 临时 Key
                    ↓
          凌晨 2 点补偿任务
              └── 扫描所有残留临时 Key 并处理
```

**Lua 脚本保证原子性：** 点赞/取消点赞操作在 Redis 中一步完成（检查状态 + 更新计数 + 标记用户），避免并发问题。

**热点 Key 检测 (HeavyKeeper)：**

基于 HeavyKeeper 算法实现 TopK 热点检测：

- 二维桶数组 (depth × width) 进行频率估算
- 指数衰减机制处理哈希冲突
- 最小堆维护 Top 100 热点 Key
- 驱逐队列记录被淘汰的热点

**多级缓存 (CacheManager)：**

```
请求 → Caffeine 本地缓存（命中即返回 + 热度+1）
         ↓ 未命中
       Redis（命中后判断是否热点，是则写入本地缓存）
         ↓ 未命中
       MySQL
         ↓ 后台
       HeavyKeeper 每 20s 衰减一次（fading）
```

### 4. 用户模块 (`user/`)

基于 Session 的简单登录系统。

- `POST /user/login` — 登录（传入 userId，校验存在后存入 Session）
- `GET /user/login/getCur` — 获取当前登录用户

## 数据库表

| 表名                   | 用途         | 关键字段                                     |
|-----------------------|-------------|--------------------------------------------|
| `user`                | 用户表       | id, username                               |
| `t_order`             | 订单主表      | order_id, status, version（乐观锁）          |
| `t_payment_record`    | 支付记录表    | payment_no, channel, status                |
| `t_idempotent_record` | 幂等控制表    | idempotent_key, business_type              |
| `t_stock`             | 库存表       | stock_id, stock, version（乐观锁）           |
| `t_blog`              | 博客内容表    | title, content, thumbCount                 |
| `t_thumb`             | 点赞记录表    | userId, blogId（联合唯一索引）                |
| `t_session`           | 对话会话表    | user_id, title, model_name, message_count  |
| `t_message`           | 对话消息表    | session_id, role, content                  |
| `t_snapshot`          | 会话快照表    | session_id, snapshot_data（序列化二进制）     |

## Redis 缓存设计

| Key 模式                 | 用途                | TTL      |
|--------------------------|--------------------|----------|
| `chat:memory:{sessionId}` | 对话记忆缓存         | 24h      |
| `stock:{stockId}`         | 秒杀库存缓存         | 永久      |
| `thumb:{userId}`          | 用户点赞状态（Hash）  | 永久      |
| `thumb:temp:{timeSlice}`  | 临时点赞计数（Hash）  | 定时清除  |

## 启动说明

### 环境要求

- JDK 17+
- MySQL 8.0+
- Redis 6.0+

### 快速启动

```bash
# 1. 初始化数据库
mysql -u root -p < sql/travel.sql

# 2. 确保 Redis 已启动（默认 127.0.0.1:6379）

# 3. 修改 application-de.yml 中的数据库密码

# 4. 启动项目
mvn spring-boot:run
```

服务启动端口：`8080`

### 配置说明

- `application.yml` — 公共配置
- `application-de.yml` — 开发环境配置（激活配置文件为 `de`）
- API Key 在 `application.yml` 中配置（DeepSeek / DashScope）

## API 接口总览

### AI 对话

| 方法   | 路径                   | 说明          |
|--------|----------------------|--------------|
| POST   | `/chat/ai`           | AI 对话       |
| GET    | `/chat/memory/clear/cache` | 清除会话缓存 |
| GET    | `/model/list`        | 获取可用模型列表 |

### 秒杀

| 方法   | 路径              | 说明         |
|--------|------------------|-------------|
| POST   | `/booking/book`  | 秒杀预订     |
| GET    | `/booking/stats` | 查询秒杀状态 |

### 点赞

| 方法   | 路径                         | 说明           |
|--------|-----------------------------|---------------|
| GET    | `/blog/searchById`          | 查询单个博客    |
| GET    | `/blog/searchByIds`         | 批量查询博客    |
| GET    | `/blog/thumb`               | 点赞/取消点赞   |
| GET    | `/blog/clearThumbData`      | 清空点赞缓存    |

### 用户

| 方法   | 路径                  | 说明          |
|--------|---------------------|--------------|
| POST   | `/user/login`       | 用户登录      |
| GET    | `/user/login/getCur` | 获取当前用户  |
