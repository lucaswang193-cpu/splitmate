-- SQL Server schema for Splitmate.
-- Re-runnable DDL: drop tables first, then create the schema again.
-- Spring JPA ddl-auto=create is configured in src/main/resources/application.properties.

DROP TABLE IF EXISTS expense_share;
DROP TABLE IF EXISTS settlement;
DROP TABLE IF EXISTS expense;
DROP TABLE IF EXISTS exchange_rate;
DROP TABLE IF EXISTS group_member;
DROP TABLE IF EXISTS phone_verification;
DROP TABLE IF EXISTS expense_category;
DROP TABLE IF EXISTS expense_group;
DROP TABLE IF EXISTS currency;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id UNIQUEIDENTIFIER NOT NULL CONSTRAINT pk_users PRIMARY KEY DEFAULT NEWID(), -- 使用者主鍵，系統自動產生的唯一識別碼
    name NVARCHAR(100) NOT NULL, -- 使用者名稱
    email NVARCHAR(255) NOT NULL, -- 使用者電子郵件，作為登入或識別帳號使用
    phone_number NVARCHAR(30) NULL, -- 使用者手機號碼，未來可用於簡訊驗證
    phone_verified BIT NOT NULL CONSTRAINT df_users_phone_verified DEFAULT 0, -- 使用者手機號碼是否已完成驗證
    phone_verified_at DATETIME2 NULL, -- 使用者手機號碼完成驗證的時間
    password NVARCHAR(255) NOT NULL, -- 使用者密碼雜湊值
    avatar_url NVARCHAR(255) NULL, -- 使用者頭像圖片網址，可為空
    created_at DATETIME2 NOT NULL CONSTRAINT df_users_created_at DEFAULT SYSUTCDATETIME(), -- 資料建立時間
    updated_at DATETIME2 NOT NULL CONSTRAINT df_users_updated_at DEFAULT SYSUTCDATETIME(), -- 資料最後更新時間
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE UNIQUE INDEX uq_users_phone_number
    ON users(phone_number)
    WHERE phone_number IS NOT NULL;

CREATE TABLE phone_verification (
    id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_phone_verification PRIMARY KEY, -- 手機驗證紀錄主鍵，自動遞增
    user_id UNIQUEIDENTIFIER NOT NULL, -- 要驗證手機的使用者 ID
    phone_number NVARCHAR(30) NOT NULL, -- 本次驗證的手機號碼
    code NVARCHAR(10) NOT NULL, -- 手機驗證碼，開發階段可回傳給前端測試
    expires_at DATETIME2 NOT NULL, -- 驗證碼過期時間
    verified_at DATETIME2 NULL, -- 驗證成功時間，可為空
    attempt_count INT NOT NULL CONSTRAINT df_phone_verification_attempt_count DEFAULT 0, -- 驗證嘗試次數
    status NVARCHAR(16) NOT NULL, -- 驗證狀態，例如 PENDING、VERIFIED、EXPIRED、FAILED
    created_at DATETIME2 NOT NULL CONSTRAINT df_phone_verification_created_at DEFAULT SYSUTCDATETIME(), -- 資料建立時間
    updated_at DATETIME2 NOT NULL CONSTRAINT df_phone_verification_updated_at DEFAULT SYSUTCDATETIME(), -- 資料最後更新時間
    CONSTRAINT fk_phone_verification_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT ck_phone_verification_attempt_count CHECK (attempt_count >= 0),
    CONSTRAINT ck_phone_verification_status CHECK (status IN ('PENDING', 'VERIFIED', 'EXPIRED', 'FAILED'))
);

CREATE TABLE currency (
    code CHAR(3) NOT NULL CONSTRAINT pk_currency PRIMARY KEY, -- 貨幣代碼，使用 ISO 4217 三碼格式，例如 TWD、USD
    name NVARCHAR(50) NOT NULL, -- 貨幣名稱
    symbol NVARCHAR(8) NULL, -- 貨幣符號，例如 NT$、$
    decimal_places INT NOT NULL, -- 貨幣小數位數
    created_at DATETIME2 NOT NULL CONSTRAINT df_currency_created_at DEFAULT SYSUTCDATETIME(), -- 資料建立時間
    updated_at DATETIME2 NOT NULL CONSTRAINT df_currency_updated_at DEFAULT SYSUTCDATETIME(), -- 資料最後更新時間
    CONSTRAINT ck_currency_decimal_places CHECK (decimal_places >= 0)
);

CREATE TABLE expense_group (
    id UNIQUEIDENTIFIER NOT NULL CONSTRAINT pk_expense_group PRIMARY KEY DEFAULT NEWID(), -- 群組主鍵，系統自動產生的唯一識別碼
    name NVARCHAR(100) NOT NULL, -- 群組名稱
    description NVARCHAR(MAX) NULL, -- 群組描述或備註，可為空
    owner_id UNIQUEIDENTIFIER NOT NULL, -- 群組擁有者的使用者 ID
    base_currency CHAR(3) NOT NULL, -- 群組基準貨幣代碼，用於統一換算金額
    invite_code NVARCHAR(12) NOT NULL, -- 群組邀請碼，用於邀請其他使用者加入群組
    created_at DATETIME2 NOT NULL CONSTRAINT df_expense_group_created_at DEFAULT SYSUTCDATETIME(), -- 資料建立時間
    updated_at DATETIME2 NOT NULL CONSTRAINT df_expense_group_updated_at DEFAULT SYSUTCDATETIME(), -- 資料最後更新時間
    CONSTRAINT fk_expense_group_owner
        FOREIGN KEY (owner_id) REFERENCES users(id),
    CONSTRAINT fk_expense_group_base_currency
        FOREIGN KEY (base_currency) REFERENCES currency(code),
    CONSTRAINT uq_expense_group_invite_code
        UNIQUE(invite_code)
);

CREATE TABLE group_member (
    id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_group_member PRIMARY KEY, -- 群組成員主鍵，自動遞增
    group_id UNIQUEIDENTIFIER NOT NULL, -- 所屬群組 ID
    user_id UNIQUEIDENTIFIER NOT NULL, -- 群組成員的使用者 ID
    role NVARCHAR(16) NOT NULL, -- 成員角色，例如 OWNER 或 MEMBER
    joined_at DATETIME2 NOT NULL CONSTRAINT df_group_member_joined_at DEFAULT SYSUTCDATETIME(), -- 加入群組時間
    created_at DATETIME2 NOT NULL CONSTRAINT df_group_member_created_at DEFAULT SYSUTCDATETIME(), -- 資料建立時間
    updated_at DATETIME2 NOT NULL CONSTRAINT df_group_member_updated_at DEFAULT SYSUTCDATETIME(), -- 資料最後更新時間
    CONSTRAINT fk_group_member_group
        FOREIGN KEY (group_id) REFERENCES expense_group(id),
    CONSTRAINT fk_group_member_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uq_group_member_group_user UNIQUE (group_id, user_id),
    CONSTRAINT ck_group_member_role CHECK (role IN ('OWNER', 'MEMBER'))
);

CREATE TABLE expense_category (
    id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_expense_category PRIMARY KEY, -- 支出分類主鍵，自動遞增
    name NVARCHAR(100) NOT NULL, -- 支出分類名稱
    created_at DATETIME2 NOT NULL CONSTRAINT df_expense_category_created_at DEFAULT SYSUTCDATETIME(), -- 資料建立時間
    updated_at DATETIME2 NOT NULL CONSTRAINT df_expense_category_updated_at DEFAULT SYSUTCDATETIME(), -- 資料最後更新時間
    CONSTRAINT uq_expense_category_name UNIQUE (name)
);

CREATE TABLE expense (
    id UNIQUEIDENTIFIER NOT NULL CONSTRAINT pk_expense PRIMARY KEY DEFAULT NEWID(), -- 支出主鍵，支援離線產生的唯一識別碼
    group_id UNIQUEIDENTIFIER NOT NULL, -- 所屬群組 ID
    payer_id UNIQUEIDENTIFIER NOT NULL, -- 實際付款人的使用者 ID
    title NVARCHAR(200) NOT NULL, -- 支出標題或品項名稱
    category_id BIGINT NULL, -- 支出分類 ID，可為空
    currency CHAR(3) NOT NULL, -- 原始支出貨幣代碼
    amount DECIMAL(20,2) NOT NULL, -- 原始貨幣金額
    exchange_rate DECIMAL(18,6) NOT NULL, -- 記帳當下匯率，表示 1 原始貨幣可換算多少基準貨幣
    base_amount DECIMAL(20,2) NOT NULL, -- 換算為群組基準貨幣後的金額
    expense_at DATETIME2(6) NOT NULL, -- 實際消費日期時間（不含時區）
    note NVARCHAR(MAX) NULL, -- 支出備註，可為空
    created_at DATETIME2 NOT NULL CONSTRAINT df_expense_created_at DEFAULT SYSUTCDATETIME(), -- 資料建立時間
    updated_at DATETIME2 NOT NULL CONSTRAINT df_expense_updated_at DEFAULT SYSUTCDATETIME(), -- 資料最後更新時間
    CONSTRAINT fk_expense_group
        FOREIGN KEY (group_id) REFERENCES expense_group(id),
    CONSTRAINT fk_expense_payer
        FOREIGN KEY (payer_id) REFERENCES users(id),
    CONSTRAINT fk_expense_category
        FOREIGN KEY (category_id) REFERENCES expense_category(id),
    CONSTRAINT fk_expense_currency
        FOREIGN KEY (currency) REFERENCES currency(code),
    CONSTRAINT ck_expense_amount CHECK (amount >= 0),
    CONSTRAINT ck_expense_exchange_rate CHECK (exchange_rate > 0),
    CONSTRAINT ck_expense_base_amount CHECK (base_amount >= 0)
);

CREATE TABLE expense_share (
    id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_expense_share PRIMARY KEY, -- 分帳項主鍵，自動遞增
    expense_id UNIQUEIDENTIFIER NOT NULL, -- 對應的支出 ID
    user_id UNIQUEIDENTIFIER NOT NULL, -- 分帳對象的使用者 ID
    share_amount DECIMAL(20,2) NOT NULL, -- 該使用者應分攤的基準貨幣金額
    paid_amount DECIMAL(20,2) NOT NULL CONSTRAINT df_expense_share_paid_amount DEFAULT 0, -- 該使用者已支付的基準貨幣金額
    status NVARCHAR(16) NOT NULL, -- 分帳狀態，例如 OWING 或 PAID
    created_at DATETIME2 NOT NULL CONSTRAINT df_expense_share_created_at DEFAULT SYSUTCDATETIME(), -- 資料建立時間
    updated_at DATETIME2 NOT NULL CONSTRAINT df_expense_share_updated_at DEFAULT SYSUTCDATETIME(), -- 資料最後更新時間
    CONSTRAINT fk_expense_share_expense
        FOREIGN KEY (expense_id) REFERENCES expense(id),
    CONSTRAINT fk_expense_share_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uq_expense_share_expense_user UNIQUE (expense_id, user_id),
    CONSTRAINT ck_expense_share_amount CHECK (share_amount >= 0),
    CONSTRAINT ck_expense_share_paid_amount CHECK (paid_amount >= 0),
    CONSTRAINT ck_expense_share_status CHECK (status IN ('OWING', 'PAID'))
);

CREATE TABLE settlement (
    id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_settlement PRIMARY KEY, -- 還款紀錄主鍵，自動遞增
    group_id UNIQUEIDENTIFIER NOT NULL, -- 所屬群組 ID
    from_user UNIQUEIDENTIFIER NOT NULL, -- 還款人的使用者 ID
    to_user UNIQUEIDENTIFIER NOT NULL, -- 收款人的使用者 ID
    amount DECIMAL(20,2) NOT NULL, -- 還款金額
    currency CHAR(3) NOT NULL, -- 還款貨幣代碼
    note NVARCHAR(500) NULL, -- 還款備註，可為空
    settled_at DATETIME2 NOT NULL CONSTRAINT df_settlement_settled_at DEFAULT SYSUTCDATETIME(), -- 還款完成時間
    created_at DATETIME2 NOT NULL CONSTRAINT df_settlement_created_at DEFAULT SYSUTCDATETIME(), -- 資料建立時間
    updated_at DATETIME2 NOT NULL CONSTRAINT df_settlement_updated_at DEFAULT SYSUTCDATETIME(), -- 資料最後更新時間
    CONSTRAINT fk_settlement_group
        FOREIGN KEY (group_id) REFERENCES expense_group(id),
    CONSTRAINT fk_settlement_from_user
        FOREIGN KEY (from_user) REFERENCES users(id),
    CONSTRAINT fk_settlement_to_user
        FOREIGN KEY (to_user) REFERENCES users(id),
    CONSTRAINT fk_settlement_currency
        FOREIGN KEY (currency) REFERENCES currency(code),
    CONSTRAINT ck_settlement_amount CHECK (amount > 0),
    CONSTRAINT ck_settlement_different_users CHECK (from_user <> to_user)
);

CREATE TABLE exchange_rate (
    id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_exchange_rate PRIMARY KEY, -- 匯率紀錄主鍵，自動遞增
    base_currency CHAR(3) NOT NULL, -- 基準貨幣代碼，表示 1 單位來源貨幣
    target_currency CHAR(3) NOT NULL, -- 目標貨幣代碼，表示換算後貨幣
    rate DECIMAL(18,6) NOT NULL, -- 匯率，表示 1 基準貨幣可換算多少目標貨幣
    effective_date DATE NOT NULL, -- 匯率生效日期
    source NVARCHAR(32) NULL, -- 匯率資料來源，可為空
    created_at DATETIME2 NOT NULL CONSTRAINT df_exchange_rate_created_at DEFAULT SYSUTCDATETIME(), -- 資料建立時間
    updated_at DATETIME2 NOT NULL CONSTRAINT df_exchange_rate_updated_at DEFAULT SYSUTCDATETIME(), -- 資料最後更新時間
    CONSTRAINT fk_exchange_rate_base_currency
        FOREIGN KEY (base_currency) REFERENCES currency(code),
    CONSTRAINT fk_exchange_rate_target_currency
        FOREIGN KEY (target_currency) REFERENCES currency(code),
    CONSTRAINT uq_exchange_rate_pair_date UNIQUE (base_currency, target_currency, effective_date),
    CONSTRAINT ck_exchange_rate_rate CHECK (rate > 0),
    CONSTRAINT ck_exchange_rate_different_currencies CHECK (base_currency <> target_currency)
);

CREATE INDEX ix_expense_group_id ON expense(group_id);
CREATE INDEX ix_expense_payer_id ON expense(payer_id);
CREATE INDEX ix_expense_group_at ON expense(group_id, expense_at);

CREATE INDEX ix_expense_share_user_id ON expense_share(user_id);

CREATE INDEX ix_settlement_from_user ON settlement(from_user);
CREATE INDEX ix_settlement_to_user ON settlement(to_user);

CREATE INDEX ix_group_member_group_id ON group_member(group_id);
CREATE INDEX ix_group_member_user_id ON group_member(user_id);

CREATE INDEX ix_phone_verification_lookup
    ON phone_verification(user_id, phone_number, status, created_at DESC);

CREATE INDEX ix_exchange_rate_lookup
    ON exchange_rate(base_currency, target_currency, effective_date DESC);
