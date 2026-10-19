-- Run manually against the intended database when preserving existing data.
-- Do not use create_table.sql or ddl-auto=create when preserving data.
-- Existing DATE values become midnight; actual historical times are unknown.
SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF COL_LENGTH('dbo.expense', 'expense_date') IS NOT NULL
   AND COL_LENGTH('dbo.expense', 'expense_at') IS NOT NULL
BEGIN
    THROW 50001, 'Both expense_date and expense_at exist; reconcile data before migration.', 1;
END;

IF COL_LENGTH('dbo.expense', 'expense_date') IS NOT NULL
BEGIN
    IF EXISTS (SELECT 1 FROM sys.indexes
               WHERE object_id = OBJECT_ID('dbo.expense') AND name = 'ix_expense_group_date')
        DROP INDEX ix_expense_group_date ON dbo.expense;

    EXEC sys.sp_rename N'dbo.expense.expense_date', N'expense_at', N'COLUMN';
    EXEC(N'ALTER TABLE dbo.expense ALTER COLUMN expense_at DATETIME2(6) NOT NULL;');

    IF NOT EXISTS (SELECT 1 FROM sys.indexes
                   WHERE object_id = OBJECT_ID('dbo.expense') AND name = 'ix_expense_group_at')
        EXEC(N'CREATE INDEX ix_expense_group_at ON dbo.expense(group_id, expense_at);');
END;

COMMIT TRANSACTION;
