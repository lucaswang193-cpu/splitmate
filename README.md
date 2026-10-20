# Splitmate

一起記帳，輕鬆分帳。Splitmate 是適合朋友聚餐、旅遊與共同生活的群組分帳應用，讓每筆代墊、分攤金額與還款紀錄都能清楚追蹤。

採用 **Vue 3 + TypeScript** 與 **Spring Boot + SQL Server**，以前後端分離架構開發，支援 Docker 部署。

## 功能特色

- **帳號與登入**：電子郵件註冊、密碼登入、Google 登入，以及 Google 帳號綁定與解除綁定。
- **群組管理**：建立群組、透過邀請碼加入、查看成員、退出群組及移轉群組擁有者。
- **費用記錄**：新增、編輯與刪除支出，記錄付款人、分類、消費時間及備註。
- **成員分攤**：選擇參與成員並平均分攤，自動處理金額尾差。
- **餘額與結算**：查看成員應收、應付餘額，並記錄成員間的還款。
- **支出統計**：依費用分類查看群組支出統計。
- **權限控管**：使用 JWT 驗證身分，並檢查群組、費用與結算資料的存取權限。

## 技術架構

| 層級 | 技術 |
| --- | --- |
| 前端 | Vue 3、TypeScript、Vite、Vue Router、Pinia、Axios |
| 後端 | Java 21、Spring Boot 4.1、Spring Security、Spring Data JPA、Bean Validation |
| 身分驗證 | JWT、BCrypt、Google ID Token |
| 資料庫 | Microsoft SQL Server |
| 測試 | Spring Boot Test、Vitest |
| 部署與整合 | Docker、Docker Compose、Nginx、GitHub Actions、GitHub Container Registry |

```text
瀏覽器 → Vue / Nginx → Spring Boot REST API → SQL Server
```

Docker 環境由 Nginx 提供前端頁面，並將 `/api/` 請求轉送至後端。

## 本機開發

### 環境需求

- JDK 21
- Node.js `^22.18.0` 或 `>=24.12.0` 與 npm
- 可連線的 SQL Server，以及已建立的空白開發資料庫（例如 `splitmate`）
- Google OAuth Web Client ID（使用 Google 登入時需要）

專案附有 Maven Wrapper，無須另外安裝 Maven。

### 1. 設定後端

建立 `src/main/resources/application-local.properties`，填入自己的設定：

```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=splitmate;encrypt=true;trustServerCertificate=true
spring.datasource.username=YOUR_DB_USERNAME
spring.datasource.password=YOUR_DB_PASSWORD

# 僅限第一次初始化空白開發資料庫；建立完成後改為 validate。
spring.jpa.hibernate.ddl-auto=update

jwt.secret=REPLACE_WITH_A_RANDOM_SECRET_AT_LEAST_32_BYTES_LONG
google.client-id=YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com
```

預設啟用 `local` profile。上述本機設定檔已列入 `.gitignore`，請勿提交實際密碼或 JWT 金鑰。`trustServerCertificate=true` 適用於本機開發；正式環境請使用可信任的資料庫憑證。

首次啟動會依 JPA Entity 建立資料表，並補入預設幣別與費用分類。初始化完成後，將 `spring.jpa.hibernate.ddl-auto` 改為 `validate`。

在專案根目錄啟動後端：

```powershell
# Windows PowerShell
.\mvnw.cmd spring-boot:run
```

```sh
# macOS / Linux
./mvnw spring-boot:run
```

後端預設位於 `http://localhost:8080`。

### 2. 設定前端

建立 `frontend/.env.development`：

```dotenv
VITE_API_BASE_URL=http://localhost:8080/api
VITE_GOOGLE_CLIENT_ID=YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com
```

Google Client ID 需與後端一致，並在 Google OAuth 設定中加入前端來源 `http://localhost:5173`。

另開終端機啟動前端：

```sh
cd frontend
npm ci
npm run dev
```

開啟 [http://localhost:5173](http://localhost:5173) 即可使用。後端目前允許此來源的跨來源請求。

## Docker 啟動

需先準備 Docker、Docker Compose 與外部 SQL Server。現有 Compose 設定包含前端與後端服務，**不包含資料庫服務**。

在專案根目錄建立 `.env.docker`：

```dotenv
DB_URL=jdbc:sqlserver://YOUR_DB_HOST:1433;databaseName=splitmate;encrypt=true;trustServerCertificate=true
DB_USERNAME=YOUR_DB_USERNAME
DB_PASSWORD=YOUR_DB_PASSWORD
JWT_SECRET=REPLACE_WITH_A_RANDOM_SECRET_AT_LEAST_32_BYTES_LONG
GOOGLE_CLIENT_ID=YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com
DDL_AUTO=validate
```

`DB_URL` 必須使用後端容器能連線的位址。在 Docker Desktop 連線至主機上的 SQL Server 時，可使用 `host.docker.internal`；容器內的 `localhost` 指向容器本身。

`DDL_AUTO=validate` 要求資料表已存在；首次使用空白開發資料庫時，可暫設 `update` 初始化，再改回 `validate`。

```sh
docker compose --env-file .env.docker up -d --build
```

啟動後開啟 [http://localhost](http://localhost)。使用 Google 登入時，需另外將此來源加入 Google OAuth 設定。前端 Client ID 在建置時寫入，修改後需重新建置前端映像。

```sh
# 查看服務狀態與日誌
docker compose ps
docker compose logs -f

# 停止服務
docker compose down
```

## 測試與建置

### 後端

後端測試使用 SQL Server。請建立**獨立且可清空**的資料庫，例如 `splitmate_test`，並在執行測試的終端機設定：

```powershell
$env:SPRING_PROFILES_ACTIVE = "test"
$env:TEST_DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=splitmate_test;encrypt=true;trustServerCertificate=true"
$env:TEST_DB_USERNAME = "YOUR_DB_USERNAME"
$env:TEST_DB_PASSWORD = "YOUR_DB_PASSWORD"
.\mvnw.cmd clean verify
```

測試 profile 使用 `ddl-auto=create`，會重建資料表，請勿指向開發或正式資料庫。現有測試涵蓋 API 流程、群組與費用權限、結算權限、費用統計及手機驗證失敗回應。

### 前端

於 `frontend/` 目錄執行：

```sh
# 型別檢查與正式建置
npm run build

# 單次執行單元測試
npm run test:unit -- --run
```

### 持續整合

[GitHub Actions 工作流程](.github/workflows/ci.yml) 會在推送至 `main`、對 `main` 提出 Pull Request，或手動觸發時，執行後端測試、前端建置及 Docker 映像建置。`main` 分支的非 Pull Request 執行會將映像發布至 GHCR，使用 commit SHA 作為標籤。工作流程需設定 Repository Variable `GOOGLE_CLIENT_ID`。

## 專案結構

```text
splitmate/
├── frontend/
│   ├── src/
│   │   ├── api/           # API 請求
│   │   ├── components/    # 共用元件、餘額與統計
│   │   ├── router/        # 路由設定
│   │   ├── stores/        # 登入狀態
│   │   ├── types/         # TypeScript 型別
│   │   └── views/         # 頁面
│   ├── Dockerfile
│   └── nginx.conf
├── src/main/java/com/splitmate/
│   ├── config/           # 安全與初始化設定
│   ├── controller/       # REST API
│   ├── dto/              # 請求與回應資料
│   ├── entity/           # JPA Entity
│   ├── repository/       # 資料存取
│   ├── security/         # JWT 驗證
│   └── service/          # 商業邏輯
├── src/main/resources/   # 應用程式設定與 SQL 腳本
├── src/test/             # 後端測試
├── .github/workflows/    # CI 與映像發布
├── compose.yml
├── Dockerfile
└── pom.xml
```

## 開發狀態

專案持續開發中。目前前端費用流程以群組本位幣與平均分攤為主；後端已有幣別與匯率資料查詢，但尚未串接外部即時匯率來源。手機驗證已具備驗證碼與狀態管理流程，尚未串接簡訊發送服務。
