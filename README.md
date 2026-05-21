# 动态数据源示例

本项目演示 MyBatis Plus 下实现动态数据源切换的两种方案：

- **方案一**：使用 Baomidou 官方 `dynamic-datasource-spring-boot-starter`，零代码，纯配置驱动
- **方案二**：基于 Spring 原生 `AbstractRoutingDataSource` + AOP + ThreadLocal 手写实现

两种方案共用一套业务代码，通过切换 Spring profile 即可体验。

## 技术栈

| 组件 | 版本 |
|---|---|
| Spring Boot | 2.7.18 |
| MyBatis Plus | 3.5.5 |
| MySQL | 8.x |
| HikariCP | 内置 |

## 快速开始

### 1. 初始化数据库

在 MySQL 中执行 `sql/init.sql`（用户名 `root`，密码 `123456`）：

```bash
mysql -uroot -p123456 < sql/init.sql
```

会创建两个库，各有一张 `user` 表并写入不同数据：

| 数据库 | 数据 |
|---|---|
| `db_master` | master_张三、master_李四 |
| `db_slave` | slave_王五、slave_赵六 |

### 2. 启动

**方案一（Baomidou 官方）：**

```bash
# application.yml 中设置 spring.profiles.active=baomidou
mvn spring-boot:run
```

**方案二（自定义实现）：**

```bash
# application.yml 中设置 spring.profiles.active=custom
mvn spring-boot:run
```

### 3. 验证

```bash
# 查询全部用户 —— 命中的是 slave 库（只看到 slave_王五、slave_赵六）
curl http://localhost:8080/users

# 新增用户 —— 写入的是 master 库
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"username": "新用户", "email": "new@test.com"}'
```

观察控制台的 SQL 日志即可确认数据源切换是否生效。

## 项目结构

```
src/main/java/com/example/dynamicdatasource/
├── DynamicDatasourceApplication.java     # 启动类
├── entity/User.java                      # 实体
├── mapper/UserMapper.java                # MyBatis Plus Mapper
├── service/
│   ├── UserService.java                  # 服务接口
│   └── impl/
│       ├── UserServiceBaomidouImpl.java  # 方案一实现（@Profile("baomidou")）
│       └── UserServiceCustomImpl.java    # 方案二实现（@Profile("custom")）
├── controller/UserController.java        # REST 接口
└── custom/                               # ★ 自定义实现全部在这一包内
    ├── annotation/DS.java                # 自定义数据源注解
    ├── context/DynamicDataSourceContextHolder.java  # ThreadLocal 上下文
    ├── datasource/DynamicDataSource.java # 动态数据源核心
    ├── aspect/DynamicDataSourceAspect.java          # AOP 切面
    └── config/CustomDataSourceConfig.java           # 多数据源 Bean 装配
```

## 方案一：Baomidou 官方 Starter

**原理**：引入 `dynamic-datasource-spring-boot-starter` 后，框架自动解析 `application-baomidou.yml` 中的多数据源配置，并代理所有数据库连接。方法上标注 `@DS("数据源名")` 即可切换。

**代码只需要一行注解**：

```java
@DS("slave")
public List<User> listUsers() {
    return userMapper.selectList(null);  // 走从库
}

@DS("master")
public User createUser(User user) {
    userMapper.insert(user);             // 走主库
    return user;
}
```

**优点**：开箱即用，支持嵌套切换、负载均衡、注解/配置/Druid 等高级特性，生产环境首选。

---

## 方案二：自定义实现 —— 详细原理

当切换 profile 为 `custom` 时，框架的自动配置被排除，数据源全部由手写代码管理。整个链路分为 5 个组件，逐层说明如下。

### 第一步：注册多个物理数据源

```
CustomDataSourceConfig
├── masterDataSource()  →  db_master 连接池
├── slaveDataSource()   →  db_slave 连接池
└── dynamicDataSource() →  路由 DataSource（@Primary）
```

`CustomDataSourceConfig` 创建了两个独立的 HikariCP 连接池，分别指向 `db_master` 和 `db_slave`。然后创建一个 `DynamicDataSource` 实例，内部维护一个路由表 `Map<String, DataSource>`，将 `"master"` → `masterDataSource`、`"slave"` → `slaveDataSource`。最后用 `@Primary` 让 MyBatis Plus 将这个路由 DataSource 作为默认数据源使用。

### 第二步：注解声明数据源

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DS {
    String value();  // "master" 或 "slave"
}
```

业务方法上标注 `@DS("slave")`，声明该方法希望使用的数据源名称。注解保留到运行时是为了让 AOP 在运行时能读到它。

### 第三步：ThreadLocal 保存数据源标识

```java
public class DynamicDataSourceContextHolder {
    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    public static void set(String ds) { CONTEXT.set(ds); }
    public static String get()          { return CONTEXT.get(); }
    public static void clear()          { CONTEXT.remove(); }
}
```

每个请求线程独立持有自己的数据源标识。选 `ThreadLocal` 的原因：

- 一次请求 → 一个线程 → 一个数据库连接，数据源标识跟随线程是最自然的映射
- 多线程并发请求时互不干扰
- `finally` 块中必须 `remove()`，因为 Tomcat 使用线程池，线程会被复用。不清理会导致后续请求拿到错误的数据源标识

### 第四步：AbstractRoutingDataSource 路由决策

```java
public class DynamicDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        return DynamicDataSourceContextHolder.get();  // → "master" 或 "slave"
    }
}
```

这是整个动态数据源的**核心机制**。`AbstractRoutingDataSource` 是 Spring 提供的抽象类，内部维护了第二步提到的 `Map<Object, DataSource>` 路由表。每次需要获取数据库连接时，Spring 都会调用 `determineCurrentLookupKey()` 来确定一个路由 key，然后用该 key 从路由表中找到真正的数据源，返回其连接。

**关键时序**：`determineCurrentLookupKey()` 是**延迟调用**的 —— 不是在方法开始时调用，而是在第一次执行 SQL 时才调用。这意味着 `@DS` 注解 → ThreadLocal → `determineCurrentLookupKey()` 这条链路上的值必须在整个方法执行期间保持有效，直到 finally 才清除。

### 第五步：AOP 切面串联整个流程

```java
@Aspect
@Component
@Order(-1)  // ★ 必须在 @Transactional 之前执行
public class DynamicDataSourceAspect {

    @Around("@annotation(ds)")
    public Object around(ProceedingJoinPoint pjp, DS ds) throws Throwable {
        DynamicDataSourceContextHolder.set(ds.value());   // 1. 设置数据源标识
        try {
            return pjp.proceed();                          // 2. 执行业务方法
        } finally {
            DynamicDataSourceContextHolder.clear();         // 3. 清除标识
        }
    }
}
```

切面拦截所有标注 `@DS` 的方法，在方法执行前将数据源名称写入 ThreadLocal，方法执行完毕后清除。

**`@Order(-1)` 至关重要**。Spring 的 `@Transactional` 切面默认 order 为 `Integer.MAX_VALUE`。如果数据源切面晚于事务切面执行，事务管理器已经在切面到达之前获取了数据库连接并开启了事务，此时再切换 ThreadLocal 中的数据源标识已经来不及了 —— 连接已经被绑定。将 order 设为 -1 确保数据源切换在事务开启之前完成。

### 完整调用链路

```
请求进入 UserController.createUser()
  │
  ▼
UserServiceCustomImpl.createUser()
  │ 标注了 @DS("master")
  │
  ▼
DynamicDataSourceAspect.around()        ← AOP 拦截
  │ DynamicDataSourceContextHolder.set("master")
  │
  ▼
userMapper.insert(user)                 ← 业务方法执行
  │ MyBatis Plus 获取 Connection
  │   → DynamicDataSource.getConnection()
  │     → determineCurrentLookupKey()
  │       → DynamicDataSourceContextHolder.get() = "master"
  │     → targetDataSources.get("master") → 主库连接池
  │
  ▼
DynamicDataSourceContextHolder.clear()  ← finally 清理
```

### 两种方案对比

| | 方案一（Baomidou） | 方案二（自定义） |
|---|---|---|
| 实现方式 | 引入 starter，纯配置 | 手写 5 个类 |
| 数据源配置 | `spring.datasource.dynamic.*` | 自定义 `CustomDataSourceConfig` |
| @DS 注解 | Starter 提供 | 自定义注解 |
| 切换原理 | Starter 内部 AOP | 自写 AOP |
| 高级特性 | 多从库负载均衡、嵌套切换、加密 | 无 |
| 适用场景 | 生产环境 | 学习原理、特殊定制需求 |

### 为什么自定义实现中要排除自动配置

```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
      - com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceAutoConfiguration
```

- `DataSourceAutoConfiguration`：Spring Boot 根据 `spring.datasource.url` 自动创建 DataSource。自定义方案用的是 `custom.datasource.*` 前缀，Spring Boot 找不到默认配置会报错，必须排除
- `DynamicDataSourceAutoConfiguration`：Baomidou starter 根据 `spring.datasource.dynamic.*` 自动创建动态数据源。自定义方案不用这套配置，同样会报错，必须排除

排除后，数据源生命周期完全由 `CustomDataSourceConfig` 接管。
