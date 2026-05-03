# Task Manager REST API

## 專案概述

一個用 **Spring Boot 3.4 + Java 21** 實作的任務管理 REST API，**重點在於展示四個核心設計模式的實務應用與清潔架構設計**，而不是複雜的任務排程引擎。

### 設計理念

這個專案的目標是展示：
- ✅ 良好的軟體架構（三層式設計、職責分明）
- ✅ 設計模式的實際應用（Factory / Observer / Strategy / Singleton）
- ✅ RESTful API 設計原則
- ✅ Cloud-native 架構思維

### 關鍵特性

- ✅ **完整 REST API** — 任務的 CRUD 操作 + 統計功能
- ✅ **四大設計模式** — Factory / Observer / Strategy / Singleton
- ✅ **自動 API 文件** — Swagger UI (`/swagger-ui.html`)
- ✅ **容器化** — Docker image 可部署到 Kubernetes 或任何雲平台
- ✅ **統一錯誤處理** — `@RestControllerAdvice` 攔截所有例外

---

## 專案範圍與未來擴展

### 當前範圍（MVP）
本專案聚焦於**清潔架構 + 設計模式的實踐**：
- CRUD 操作（建立、查詢、更新、刪除）
- 基本的排序和過濾
- 簡單的統計（任務計數）
- 狀態流轉（TODO → IN_PROGRESS → DONE）

### 生產環境的擴展方向
如果要變成完整的任務管理系統，可以沿著同樣的架構新增：
```
1. 任務依賴關係
   ├── Task Entity 新增 dependsOn 欄位
   ├── 新增 DependencyValidator (Validator Pattern)
   └── Service 檢查依賴是否滿足才允許執行

2. 資源分配
   ├── Resource Entity（人員、機器、預算等）
   ├── 新增 ResourceAllocationStrategy (Strategy Pattern)
   └── Service 在建立任務時檢查資源可用性

3. 自動排程
   ├── 新增 Scheduler Component (時間觸發)
   ├── 新增 ReschedulingObserver (Observer Pattern)
   └── 監聽任務狀態變化自動調度

4. 衝突偵測
   ├── 新增 ConflictDetector Service
   ├── 使用圖論演算法檢測循環依賴
   └── 使用線性規劃最佳化資源分配
```

**設計的好處**：所有擴展都遵循相同的架構模式，無需重構核心。

---

```
Controller (HTTP 層)
    ↓
Service (業務邏輯)
    ├── Factory Pattern    → createTask() 根據 taskType 初始化
    ├── Observer Pattern   → updateTaskStatus() 發布事件 + 多個 Observer 監聽
    ├── Strategy Pattern   → getAllTasks(sortBy) 動態選擇排序策略
    └── Singleton Pattern  → TaskStatisticsCache 計算統計
    ↓
Repository (資料存取)
    ↓
Database (H2 開發 / PostgreSQL 正式)
```

---

## 設計模式說明

### 1️⃣ Factory Pattern
**位置**：`pattern/factory/`

**目的**：不同任務類型的初始化邏輯不同，使用 Factory 避免 Service 層的 if-else。

**效果**：
- `PERSONAL` 任務 → 預設 MEDIUM 優先級
- `WORK` 任務 → 若未指定優先級，自動 HIGH
- `RECURRING` 任務 → 在 description 前加 `[RECURRING]` 標記

**為什麼這樣設計**：Open/Closed Principle。未來新增 `URGENT` 類型，只需新增 `UrgentTaskCreator` class，Service 完全不用改。

---

### 2️⃣ Observer Pattern
**位置**：`pattern/observer/`

**目的**：任務狀態改變時，自動觸發多個獨立的行為（audit log + 通知）。

**效果**：
```
PATCH /api/tasks/{id}/status
  ↓ 發布 TaskStatusChangedEvent
    ├── LoggingObserver     → 寫入 audit log
    └── NotificationObserver → 模擬發送 Email/Push
```

**為什麼這樣設計**：Separation of Concerns。通知邏輯獨立不耦合到 Service，之後要加新的觸發行為只需新增 `@Component @EventListener`。

---

### 3️⃣ Strategy Pattern
**位置**：`pattern/strategy/`

**目的**：查詢任務時，支援多種排序方式，不用硬寫 if-else。

**效果**：
```
GET /api/tasks?sortBy=priority  → 按優先級排序（CRITICAL > HIGH > MEDIUM > LOW）
GET /api/tasks?sortBy=dueDate   → 按到期日期排序（最近的排前面）
GET /api/tasks?sortBy=status    → 按狀態排序（TODO > IN_PROGRESS > DONE）
```

**為什麼這樣設計**：Strategy Pattern 讓新增排序方式變成增加新 class，而不是修改現有邏輯。符合 Open/Closed Principle。

---

### 4️⃣ Singleton Pattern
**位置**：`config/AppConfig.java` + `config/TaskStatisticsCache.java`

**目的**：計算任務統計時，整個應用全程只用一個 cache 實例。

**實現方式**：
```java
@Bean
@Scope("singleton")  // Spring 預設就是 singleton
public TaskStatisticsCache taskStatisticsCache() {
    return new TaskStatisticsCache();
}
```

**為什麼這樣設計**：Singleton 確保全應用統一的狀態管理。相比手寫 `getInstance()`，用 Spring Bean 管理更符合依賴注入原則。

---

## 快速開始

### 方式一：直接執行 JAR

```bash
# 打包
mvn clean package

# 執行
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

訪問 `http://localhost:8080/swagger-ui.html`

### 方式二：Docker 執行（推薦）

```bash
# 建立 image
docker build -t task-manager:latest .

# 執行容器
docker run -p 8080:8080 task-manager:latest
```

### 方式三：Kubernetes 部署

```bash
# 使用 docker image 部署到 K8s
kubectl run task-manager --image=task-manager:latest --port=8080
kubectl expose pod task-manager --type=LoadBalancer --port=8080
```

---

## API 端點

| 方法 | 端點 | 說明 |
|------|------|------|
| POST | `/api/tasks` | 建立任務（使用 Factory Pattern）|
| GET | `/api/tasks` | 查詢所有任務（支援 `sortBy` 參數） |
| GET | `/api/tasks/{id}` | 查詢單一任務 |
| PUT | `/api/tasks/{id}` | 更新任務 |
| PATCH | `/api/tasks/{id}/status` | 更新狀態（觸發 Observer） |
| DELETE | `/api/tasks/{id}` | 刪除任務 |
| GET | `/api/tasks/stats` | 統計資料（使用 Singleton Cache） |

---

## 測試端點

### 1. 建立任務
```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Prepare Java Interview",
    "description": "Implement design patterns",
    "priority": "HIGH",
    "taskType": "WORK",
    "dueDate": "2026-05-09"
  }'
```

### 2. 更新狀態（觀察 Observer log）
```bash
curl -X PATCH http://localhost:8080/api/tasks/1/status \
  -H "Content-Type: application/json" \
  -d '{"status": "DONE"}'
```

查看終端機，應該看到：
```
[AUDIT] Task 'Prepare Java Interview' (id=1) status changed: TODO -> DONE
[NOTIFICATION] Task 'Prepare Java Interview' completed! Email notification sent.
```

### 3. 測試排序策略
```bash
curl http://localhost:8080/api/tasks?sortBy=priority
curl http://localhost:8080/api/tasks?sortBy=dueDate
curl http://localhost:8080/api/tasks?sortBy=status
```

### 4. 查看統計（Singleton Cache）
```bash
curl http://localhost:8080/api/tasks/stats
```

終端機會列印：
```
[SINGLETON] TaskStatisticsCache instance #123456789 computing stats
```

多次調用，instance 號碼永遠相同 ← 這就是 Singleton。

---

## 技術棧

- **Language**: Java 21
- **Framework**: Spring Boot 3.4.5
- **Database**: H2 (development) / PostgreSQL (production)
- **ORM**: Spring Data JPA + Hibernate
- **API Docs**: Springdoc OpenAPI (Swagger UI)
- **Build**: Maven
- **Container**: Docker

## OTHERS

- Swagger UI 可視化測試：`http://localhost:8080/swagger-ui.html`
- H2 Console 檢視資料庫：`http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:taskdb`)
- API 文件：`http://localhost:8080/api-docs`
