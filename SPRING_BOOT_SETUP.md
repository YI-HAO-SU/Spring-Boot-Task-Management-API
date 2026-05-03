# Spring Boot 新專案建置流程

## 第一步：從 start.spring.io 下載骨架

前往 https://start.spring.io 選擇以下設定：

| 欄位 | 建議值 |
|------|--------|
| Project | Maven |
| Language | Java |
| Spring Boot | 最新穩定版（**不要選 SNAPSHOT**，不要選 RC/M 開頭的版本） |
| Java | **21**（LTS，最穩定） |
| Packaging | Jar |

Dependencies 勾選：
- **Spring Web** — 建立 REST API
- **Spring Data JPA** — 資料庫 ORM
- **H2 Database** — 開發用記憶體資料庫
- **Validation** — @Valid 輸入驗證

下載解壓後得到 Maven 專案骨架。

> **已下載但選錯了？** 不需要重新下載，直接改 `pom.xml` 即可：
> - Java 版本：`<java.version>25</java.version>` → 改成 `21`
> - Spring Boot SNAPSHOT：`<version>3.x.x-SNAPSHOT</version>` → 改成穩定版（例如 `3.4.5`）
> - 同時刪除 pom.xml 裡的 `<repositories>` 和 `<pluginRepositories>` 區塊（SNAPSHOT 才需要）

---

## 第二步：手動補加 pom.xml 的 dependency

start.spring.io **不包含**以下兩個，需手動加入 `<dependencies>` 區塊：

```xml
<!-- 自動產生 Getter/Setter/Builder，減少重複程式碼 -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<!-- 自動產生 Swagger UI 互動文件 -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.0</version>
</dependency>
```

在 `<build>` 的 `spring-boot-maven-plugin` 加上 Lombok 排除（防止 Lombok 被打包進 jar）：

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <excludes>
                    <exclude>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                    </exclude>
                </excludes>
            </configuration>
        </plugin>
    </plugins>
</build>
```

---

## 第三步：設定 application.properties

開發階段使用 H2（不需要安裝任何資料庫）：

```properties
spring.application.name=your-app-name

# H2 記憶體資料庫
spring.datasource.url=jdbc:h2:mem:yourdbname;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect

# H2 網頁管理介面（http://localhost:8080/h2-console）
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# JPA 設定
spring.jpa.hibernate.ddl-auto=create-drop   # 每次啟動建表，關閉時刪除
spring.jpa.show-sql=true                     # 印出 SQL 語句（debug 用）
spring.jpa.properties.hibernate.format_sql=true

# Swagger UI（http://localhost:8080/swagger-ui.html）
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.api-docs.path=/api-docs
```

---

## 第四步：建立標準專案結構

```
src/main/java/com/example/yourapp/
├── YourAppApplication.java      ← 入口點（start.spring.io 自動產生）
├── controller/                  ← REST 端點，只負責接收請求和回傳結果
├── service/                     ← 業務邏輯
├── repository/                  ← JPA 資料存取（extends JpaRepository）
├── model/                       ← Entity（對應資料庫表）+ Enum
├── dto/                         ← Request / Response 資料格式
├── config/                      ← Spring Bean 設定
└── exception/                   ← 自訂例外 + 統一錯誤處理
```

開發順序：`model` → `repository` → `service` → `controller`

---

## 各層的職責與關鍵 Annotation

### Model（Entity）
```java
@Entity
@Table(name = "your_table")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class YourEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp  // Hibernate 自動設定建立時間
    private LocalDateTime createdAt;

    @UpdateTimestamp    // Hibernate 自動設定更新時間
    private LocalDateTime updatedAt;
}
```

### Repository
```java
@Repository
public interface YourRepository extends JpaRepository<YourEntity, Long> {
    // Spring Data JPA 自動實作基本的 CRUD
    // 也可以自訂查詢方法，例如：
    List<YourEntity> findByStatus(Status status);
}
```

### Service
```java
@Service
@Transactional
public class YourService {
    private final YourRepository repository;

    public YourService(YourRepository repository) {  // 建構子注入（推薦）
        this.repository = repository;
    }
}
```

### Controller
```java
@RestController
@RequestMapping("/api/your-resource")
public class YourController {
    @PostMapping   → 建立
    @GetMapping    → 查詢
    @PutMapping    → 全量更新
    @PatchMapping  → 部分更新
    @DeleteMapping → 刪除
}
```

### 統一錯誤處理
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(YourNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(YourNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }
}
```

---

## 日後換成真實資料庫（PostgreSQL）

### pom.xml 換掉 H2，加入 PostgreSQL driver：
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

### application.properties 換成 PostgreSQL 設定：
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/yourdbname
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.username=youruser
spring.datasource.password=yourpassword
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update   # 只更新表結構，不刪資料
```

---

## Docker 容器化

### Dockerfile
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### docker-compose.yml（app + PostgreSQL）
```yaml
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/yourdbname
      SPRING_DATASOURCE_USERNAME: user
      SPRING_DATASOURCE_PASSWORD: password
    depends_on:
      - db

  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: yourdbname
      POSTGRES_USER: user
      POSTGRES_PASSWORD: password
    ports:
      - "5432:5432"
```

啟動指令：
```bash
# 打包
mvn clean package -DskipTests

# 一鍵啟動
docker-compose up --build
```

---

## 常用 Lombok Annotation 速查

| Annotation | 作用 |
|------------|------|
| `@Getter` | 自動產生所有欄位的 getter |
| `@Setter` | 自動產生所有欄位的 setter |
| `@Builder` | 產生 Builder pattern（`Task.builder().title("x").build()`） |
| `@NoArgsConstructor` | 產生無參數建構子（JPA 必須有） |
| `@AllArgsConstructor` | 產生全參數建構子 |
| `@Data` | `@Getter + @Setter + @ToString + @EqualsAndHashCode`（Entity 不建議用） |

---

## 快速驗證清單

啟動後確認以下 URL 可以存取：

- `http://localhost:8080/swagger-ui.html` → Swagger 互動文件
- `http://localhost:8080/h2-console` → H2 資料庫管理介面（JDBC URL 填 `jdbc:h2:mem:yourdbname`）
- `http://localhost:8080/api-docs` → OpenAPI JSON 格式文件
