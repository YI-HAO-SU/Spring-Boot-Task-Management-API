# Task Manager — 架構與設計模式完整說明

## 目錄

1. [技術棧與基礎建置](#一技術棧與基礎建置)
2. [專案目錄結構](#二專案目錄結構)
3. [資料模型設計](#三資料模型設計)
4. [分層架構](#四分層架構)
5. [API 端點總覽](#五api-端點總覽)
6. [DTO 隔離層](#六dto-隔離層)
7. [輸入驗證與錯誤處理](#七輸入驗證與錯誤處理)
8. [設計模式實現](#八設計模式實現)
   - [工廠模式 Factory Pattern](#81-工廠模式-factory-pattern)
   - [策略模式 Strategy Pattern](#82-策略模式-strategy-pattern)
   - [觀察者模式 Observer Pattern](#83-觀察者模式-observer-pattern)
   - [單例模式 Singleton Pattern](#84-單例模式-singleton-pattern)
9. [完整資料流程](#九完整資料流程)

---

## 一、技術棧與基礎建置

**Spring Boot 3.4.5 + Java 21**

| 依賴 | 用途 |
|------|------|
| `spring-boot-starter-web` | 建立 REST API，處理 HTTP 路由與 JSON 序列化 |
| `spring-boot-starter-data-jpa` | ORM 框架，將 Java 物件對應到資料庫表格 |
| `spring-boot-starter-validation` | 請求參數驗證（`@NotBlank`、`@Valid` 等） |
| `h2` | 內嵌記憶體資料庫，應用啟動時自動建立，不需安裝外部 DB |
| `lombok` | 編譯期自動產生 getter/setter/builder，減少樣板程式碼 |
| `springdoc-openapi` | 自動產生 Swagger UI，可在 `/swagger-ui.html` 互動測試 API |

---

## 二、專案目錄結構

```
src/main/java/com/example/demo/
│
├── DemoApplication.java              # Spring Boot 入口點
│
├── model/                            # 資料庫實體（Entity）
│   ├── Task.java                     # 核心資料模型
│   ├── TaskStatus.java               # 列舉：TODO / IN_PROGRESS / DONE / CANCELLED
│   ├── Priority.java                 # 列舉：LOW / MEDIUM / HIGH / CRITICAL
│   └── TaskType.java                 # 列舉：PERSONAL / WORK / RECURRING
│
├── dto/                              # 資料傳輸物件，隔離外部與內部
│   ├── TaskRequest.java              # 建立/更新請求的輸入格式
│   ├── TaskResponse.java             # 回傳給客戶端的輸出格式
│   ├── TaskStatusUpdateRequest.java  # 更新狀態的專用請求
│   ├── TaskStatsResponse.java        # 統計資料的回傳格式
│   └── ErrorResponse.java           # 統一錯誤回應格式
│
├── repository/
│   └── TaskRepository.java           # JPA 資料庫操作介面
│
├── service/
│   └── TaskService.java              # 業務邏輯核心，協調所有設計模式
│
├── controller/
│   └── TaskController.java           # HTTP 入口，接收請求、回傳回應
│
├── pattern/
│   ├── factory/                      # 工廠模式：任務建立
│   │   ├── TaskCreator.java          # 介面（含 default buildBase 方法）
│   │   ├── PersonalTaskCreator.java
│   │   ├── WorkTaskCreator.java
│   │   ├── RecurringTaskCreator.java
│   │   └── TaskFactory.java          # 靜態入口，根據 taskType 選擇 Creator
│   │
│   ├── observer/                     # 觀察者模式：狀態變更通知
│   │   ├── TaskStatusChangedEvent.java  # 事件資料容器
│   │   ├── LoggingObserver.java         # 記錄 Audit Log
│   │   └── NotificationObserver.java   # 發送 Email / Push 通知
│   │
│   └── strategy/                    # 策略模式：任務排序
│       ├── TaskSortingStrategy.java  # 策略介面
│       ├── PrioritySortingStrategy.java
│       ├── DueDateSortingStrategy.java
│       └── StatusSortingStrategy.java
│
├── config/
│   ├── AppConfig.java                # 定義 Spring Bean（Singleton）
│   ├── TaskStatisticsCache.java      # 單例統計計算器
│   └── OpenApiConfig.java            # Swagger 設定
│
└── exception/
    ├── TaskNotFoundException.java    # 自訂例外
    └── GlobalExceptionHandler.java   # 全域錯誤攔截器
```

---

## 三、資料模型設計

### Task 實體（`model/Task.java`）

```java
@Entity
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                   // 主鍵，DB 自動遞增

    @Column(nullable = false)
    private String title;              // 必填

    @Column(columnDefinition = "TEXT")
    private String description;        // 可空，TEXT 型別支援長文字

    @Enumerated(EnumType.STRING)       // 存字串（"TODO"）而非數字（0）
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    private TaskType taskType;

    private LocalDate dueDate;

    @CreationTimestamp                 // Hibernate 自動填入建立時間
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp                   // Hibernate 自動更新修改時間
    private LocalDateTime updatedAt;
}
```

**關鍵設計決策：**
- `@Enumerated(EnumType.STRING)`：資料庫存 `"HIGH"` 而非 `2`，資料可讀性高，且新增 enum 值不影響舊資料。
- `@CreationTimestamp` / `@UpdateTimestamp`：時間戳由 Hibernate 自動管理，不需在程式碼中手動設定。

### 狀態與優先度列舉

```
TaskStatus:  TODO → IN_PROGRESS → DONE
                              └→ CANCELLED

Priority:    LOW < MEDIUM < HIGH < CRITICAL
             （enum 宣告順序決定比較順序，排序策略直接利用此特性）
```

---

## 四、分層架構

```
HTTP 請求
    │
    ▼
┌─────────────────────────────────────┐
│  Controller 層                       │  只負責：接收 HTTP、呼叫 Service、回傳 HTTP
│  TaskController                      │
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│  Service 層                          │  只負責：業務邏輯、協調設計模式
│  TaskService                         │
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│  Repository 層                       │  只負責：資料庫 CRUD
│  TaskRepository                      │
└─────────────────────────────────────┘
    │
    ▼
H2 記憶體資料庫
```

**原則：每層只依賴下一層，嚴禁越層呼叫。**

---

## 五、API 端點總覽

| HTTP 方法 | 路徑 | 功能 | 使用的設計模式 |
|-----------|------|------|----------------|
| `POST` | `/api/tasks` | 建立任務 | Factory Pattern |
| `GET` | `/api/tasks?sortBy=priority` | 取得全部任務 | Strategy Pattern |
| `GET` | `/api/tasks/{id}` | 取得單一任務 | — |
| `PUT` | `/api/tasks/{id}` | 更新任務完整內容 | — |
| `PATCH` | `/api/tasks/{id}/status` | 只更新任務狀態 | Observer Pattern |
| `DELETE` | `/api/tasks/{id}` | 刪除任務 | — |
| `GET` | `/api/tasks/stats` | 取得統計資料 | Singleton Pattern |

**`PUT` vs `PATCH` 的語義差異：**
- `PUT`：取代整筆資料，客戶端需傳完整欄位
- `PATCH`：局部更新，這裡只更新 `status` 欄位

`sortBy` 參數可選值：`priority`、`dueDate`、`status`（不傳則不排序）

---

## 六、DTO 隔離層

### 為何不直接回傳 Entity？

```java
// ❌ 危險做法：直接暴露 @Entity
public Task createTask(...) { ... }
// 問題：洩漏 JPA 內部欄位、Hibernate 代理物件可能序列化失敗、
//       修改 Entity 欄位會直接影響 API 格式（破壞向後相容）

// ✅ 正確做法：透過 DTO 控制對外格式
public TaskResponse createTask(...) { ... }
```

### TaskResponse 的靜態工廠方法

```java
// TaskResponse.java
public static TaskResponse from(Task task) {
    return TaskResponse.builder()
            .id(task.getId())
            .title(task.getTitle())
            // 只暴露需要對外的欄位，可自由增減不影響 Entity
            .build();
}
```

呼叫方只需一行 `TaskResponse.from(task)`，轉換邏輯集中管理。

---

## 七、輸入驗證與錯誤處理

### 驗證流程

```
HTTP 請求 Body
    │
    ▼ @Valid 觸發驗證
TaskRequest（@NotBlank title 等）
    │
    ├── 驗證通過 → 進入 Service
    └── 驗證失敗 → MethodArgumentNotValidException
                    → GlobalExceptionHandler 攔截 → 400 Bad Request
```

### GlobalExceptionHandler（`exception/GlobalExceptionHandler.java`）

```java
@RestControllerAdvice   // 全域攔截所有 @RestController 的例外
public class GlobalExceptionHandler {

    @ExceptionHandler(TaskNotFoundException.class)
    // → 404 Not Found

    @ExceptionHandler(MethodArgumentNotValidException.class)
    // → 400 Bad Request（把所有欄位驗證錯誤訊息合併成一行）

    @ExceptionHandler(Exception.class)
    // → 500 Internal Server Error（兜底）
}
```

統一錯誤格式（`ErrorResponse`）：
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Task with id 99 not found"
}
```

---

## 八、設計模式實現

### 8.1 工廠模式 Factory Pattern

**解決的問題：** 不同類型的任務有不同的初始化邏輯，避免 Service 層充斥 `if/else`。

**類別關係：**

```
TaskFactory（靜態入口）
    └── switch(taskType) 決定使用哪個 Creator
            │
            ├── PERSONAL  → PersonalTaskCreator  （用預設值，無特殊邏輯）
            ├── WORK      → WorkTaskCreator       （未指定 priority 時強制 HIGH）
            └── RECURRING → RecurringTaskCreator  （在 description 前加 [RECURRING]）
                              │
                              ▼ 全部實作 TaskCreator 介面
                              create(TaskRequest) : Task
```

**共用邏輯放在介面 `default` 方法（`TaskCreator.java`）：**

```java
default Task buildBase(TaskRequest request) {
    return Task.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .priority(request.getPriority() != null ? request.getPriority() : Priority.MEDIUM)
            .taskType(request.getTaskType() != null ? request.getTaskType() : TaskType.PERSONAL)
            .dueDate(request.getDueDate())
            .status(TaskStatus.TODO)    // 新任務一律從 TODO 開始
            .build();
}
```

**各 Creator 的差異化邏輯：**

```java
// WorkTaskCreator：未指定優先度時，工作任務預設 HIGH
public Task create(TaskRequest request) {
    Task task = buildBase(request);
    if (request.getPriority() == null) {
        task.setPriority(Priority.HIGH);
    }
    return task;
}

// RecurringTaskCreator：在描述加上識別標記
public Task create(TaskRequest request) {
    Task task = buildBase(request);
    String original = task.getDescription() != null ? task.getDescription() : "";
    task.setDescription("[RECURRING] " + original);
    return task;
}
```

**可擴展性：** 新增 `MEETING` 類型只需：
1. 在 `TaskType` enum 加 `MEETING`
2. 新增 `MeetingTaskCreator implements TaskCreator`
3. 在 `TaskFactory` 的 switch 加一行

**不需修改任何現有程式碼（開閉原則）。**

---

### 8.2 策略模式 Strategy Pattern

**解決的問題：** 排序邏輯有多種且可能繼續擴充，呼叫方不應知道排序細節。

**介面定義行為契約：**

```java
@FunctionalInterface  // 只有一個抽象方法，可用 Lambda 實作
public interface TaskSortingStrategy {
    List<Task> sort(List<Task> tasks);
}
```

**三種具體策略的演算法：**

```java
// PrioritySortingStrategy：CRITICAL > HIGH > MEDIUM > LOW
// 利用 enum 宣告順序（ordinal），reversed() 讓大的排前面
tasks.stream()
     .sorted(Comparator.comparing(Task::getPriority).reversed())
     .toList();

// DueDateSortingStrategy：最早到期排最前，null（無期限）排最後
tasks.stream()
     .sorted(Comparator.comparing(Task::getDueDate,
             Comparator.nullsLast(Comparator.naturalOrder())))
     .toList();

// StatusSortingStrategy：依 enum 宣告順序（TODO=0, IN_PROGRESS=1, DONE=2, CANCELLED=3）
tasks.stream()
     .sorted(Comparator.comparing(Task::getStatus))
     .toList();
```

**Service 動態選擇策略：**

```java
// TaskService.java
private TaskSortingStrategy resolveSortingStrategy(String sortBy) {
    if (sortBy == null) return tasks -> tasks;   // Lambda：直接回傳，不排序
    return switch (sortBy.toLowerCase()) {
        case "priority" -> new PrioritySortingStrategy();
        case "duedate"  -> new DueDateSortingStrategy();
        case "status"   -> new StatusSortingStrategy();
        default         -> tasks -> tasks;
    };
}
```

呼叫端只傳字串 `"priority"`，具體排序邏輯完全封裝在策略類別內。

---

### 8.3 觀察者模式 Observer Pattern

**解決的問題：** 狀態改變時要做多件事（寫 log、發通知），但不想讓 Service 直接依賴每個動作（否則新增通知管道就要改 Service）。

**Spring 事件機制實現解耦：**

```
TaskService.updateTaskStatus()
    │
    │  task.setStatus(newStatus)
    │  taskRepository.save(task)
    │
    ▼
eventPublisher.publishEvent(
    new TaskStatusChangedEvent(this, savedTask, previousStatus)
)
    │
    │  Spring 自動分派給所有監聽者
    │
    ├──▶ LoggingObserver.onStatusChanged()
    │        log.info("[AUDIT] Task '{}' status: {} -> {}", ...)
    │
    └──▶ NotificationObserver.onStatusChanged()
             if (status == DONE)  → [EMAIL] Task completed!
             else                 → [PUSH]  Task updated to IN_PROGRESS
```

**事件資料容器（`TaskStatusChangedEvent.java`）：**

```java
public class TaskStatusChangedEvent extends ApplicationEvent {
    private final Task task;           // 帶有新狀態的任務
    private final TaskStatus previousStatus;  // 舊狀態（用於 log 比對）
}
```

**觀察者的 `@EventListener` 自動綁定：**

```java
@Component
public class LoggingObserver {
    @EventListener
    public void onStatusChanged(TaskStatusChangedEvent event) {
        // Spring 根據方法參數型別自動匹配事件
    }
}
```

**可擴展性：** 新增 Slack 通知只需新建 `SlackObserver` 加 `@EventListener`，完全不動 Service。

---

### 8.4 單例模式 Singleton Pattern

**解決的問題：** 統計計算是共享邏輯，整個應用程式只需一個實例。

**Spring Bean 預設即為 Singleton：**

```java
// AppConfig.java
@Configuration
public class AppConfig {
    @Bean
    @Scope("singleton")   // 明確宣告以表示設計意圖（實際上是預設值）
    public TaskStatisticsCache taskStatisticsCache() {
        return new TaskStatisticsCache();
    }
}
```

Spring IoC 容器保證 `TaskStatisticsCache` 全應用生命週期只被 `new` 一次。呼叫 `/api/tasks/stats` 時 log 印出的 `System.identityHashCode(this)` 每次都相同，可以驗證此特性。

**統計計算邏輯（`TaskStatisticsCache.java`）：**

```java
public TaskStatsResponse compute(List<Task> tasks) {
    return TaskStatsResponse.builder()
            .total(tasks.size())
            .todo(countByStatus(tasks, TaskStatus.TODO))
            .inProgress(countByStatus(tasks, TaskStatus.IN_PROGRESS))
            .done(countByStatus(tasks, TaskStatus.DONE))
            .cancelled(countByStatus(tasks, TaskStatus.CANCELLED))
            .highPriority(countByPriority(tasks, Priority.HIGH))
            .criticalPriority(countByPriority(tasks, Priority.CRITICAL))
            .build();
}

// 用 Stream filter + count 做分組統計
private long countByStatus(List<Task> tasks, TaskStatus status) {
    return tasks.stream().filter(t -> t.getStatus() == status).count();
}
```

---

## 九、完整資料流程

### 建立任務（POST /api/tasks）

```
Request Body: {"title":"開會","taskType":"WORK"}
         │
         ▼
TaskController.createTask()
         │ @Valid 驗證 title 不為空
         ▼
TaskService.createTask()
         │
         ▼
TaskFactory.createTask()          ← Factory Pattern
         │ switch(WORK) → WorkTaskCreator
         ▼
WorkTaskCreator.create()
         │ buildBase() 建立基礎任務
         │ priority 未傳 → 強制設為 HIGH
         ▼
taskRepository.save(task)         ← JPA 寫入 H2
         │
         ▼
TaskResponse.from(savedTask)      ← Entity 轉 DTO
         │
         ▼
HTTP 201 Created
{"id":1,"title":"開會","priority":"HIGH","status":"TODO","taskType":"WORK",...}
```

### 更新任務狀態（PATCH /api/tasks/1/status）

```
Request Body: {"status":"DONE"}
         │
         ▼
TaskService.updateTaskStatus()
         │ previousStatus = task.getStatus()  （記住舊狀態）
         │ task.setStatus(DONE)
         │ taskRepository.save(task)
         ▼
eventPublisher.publishEvent(      ← Observer Pattern 觸發點
    new TaskStatusChangedEvent(task, previousStatus)
)
         │
         ├──▶ LoggingObserver
         │    [AUDIT] Task '開會'(id=1) status: TODO -> DONE
         │
         └──▶ NotificationObserver
              status==DONE → [EMAIL] Task '開會' completed!
         │
         ▼
HTTP 200 OK {"status":"DONE",...}
```

### 取得排序任務（GET /api/tasks?sortBy=priority）

```
Query Param: sortBy=priority
         │
         ▼
TaskService.getAllTasks("priority")
         │
         ▼
resolveSortingStrategy("priority")  ← Strategy Pattern 選擇
         │ → new PrioritySortingStrategy()
         ▼
PrioritySortingStrategy.sort(tasks)
         │ CRITICAL > HIGH > MEDIUM > LOW
         ▼
List<TaskResponse>（已排序）
         │
         ▼
HTTP 200 OK [...]
```

### 取得統計（GET /api/tasks/stats）

```
         ▼
TaskService.getStats()
         │
         ▼
statsCache.compute(tasks)          ← Singleton Pattern（同一個實例）
         │ Stream filter + count 分組統計
         ▼
HTTP 200 OK
{
  "total": 10,
  "todo": 3,
  "inProgress": 4,
  "done": 2,
  "cancelled": 1,
  "highPriority": 5,
  "criticalPriority": 2
}
```

---

## 設計模式速查表

| 模式 | 檔案位置 | 觸發時機 | 核心價值 |
|------|----------|----------|----------|
| **Factory** | `pattern/factory/` | `POST /api/tasks` | 封裝初始化差異，新增類型不改舊程式 |
| **Strategy** | `pattern/strategy/` | `GET /api/tasks?sortBy=` | 排序算法可替換，呼叫方不感知細節 |
| **Observer** | `pattern/observer/` | `PATCH /api/tasks/{id}/status` | 狀態變更的副作用解耦，新增通知不改 Service |
| **Singleton** | `config/TaskStatisticsCache.java` | `GET /api/tasks/stats` | 共享實例，由 Spring IoC 容器保證唯一性 |
