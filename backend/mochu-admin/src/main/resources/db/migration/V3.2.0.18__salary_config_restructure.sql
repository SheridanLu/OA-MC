-- V3.2.0.18__salary_config_restructure.sql
-- MOCHU-OA V3.2 Spec alignment: Restructure biz_salary_config from per-grade to per-employee model
-- Target schema (V3.2 P.54):
--   user_id, base_salary, position_salary, performance_base, allowance,
--   effective_date, status, remark, version
SET NAMES utf8mb4;

-- ============================================================================
-- Helper procedure: safely add column if it does not already exist
-- ============================================================================
DROP PROCEDURE IF EXISTS `_tmp_add_column_if_not_exists`;
DELIMITER $$
CREATE PROCEDURE `_tmp_add_column_if_not_exists`(
    IN p_table  VARCHAR(128),
    IN p_column VARCHAR(128),
    IN p_definition TEXT
)
BEGIN
    DECLARE col_count INT DEFAULT 0;
    SELECT COUNT(*) INTO col_count
      FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME   = p_table
       AND COLUMN_NAME  = p_column;
    IF col_count = 0 THEN
        SET @sql_add = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE stmt FROM @sql_add;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- Helper procedure: safely add index if it does not already exist
DROP PROCEDURE IF EXISTS `_tmp_add_index_if_not_exists`;
DELIMITER $$
CREATE PROCEDURE `_tmp_add_index_if_not_exists`(
    IN p_table VARCHAR(128),
    IN p_index VARCHAR(128),
    IN p_columns TEXT
)
BEGIN
    DECLARE idx_count INT DEFAULT 0;
    SELECT COUNT(*) INTO idx_count
      FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME   = p_table
       AND INDEX_NAME   = p_index;
    IF idx_count = 0 THEN
        SET @sql_idx = CONCAT('CREATE INDEX `', p_index, '` ON `', p_table, '` (', p_columns, ')');
        PREPARE stmt FROM @sql_idx;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- ============================================================================
-- 1. Add per-employee column: user_id
-- ============================================================================
CALL `_tmp_add_column_if_not_exists`('biz_salary_config', 'user_id',
    'INT NULL COMMENT ''员工ID'' AFTER `id`');

-- ============================================================================
-- 2. Add position_salary column
-- ============================================================================
CALL `_tmp_add_column_if_not_exists`('biz_salary_config', 'position_salary',
    'DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT ''岗位工资'' AFTER `base_salary`');

-- ============================================================================
-- 3. Add performance column (performance_base in spec)
-- ============================================================================
CALL `_tmp_add_column_if_not_exists`('biz_salary_config', 'performance',
    'DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT ''绩效基数'' AFTER `position_salary`');

-- ============================================================================
-- 4. Add effective_date column
-- ============================================================================
CALL `_tmp_add_column_if_not_exists`('biz_salary_config', 'effective_date',
    'DATE NULL COMMENT ''生效日期'' AFTER `allowance`');

-- ============================================================================
-- 5. Add version column (optimistic locking)
-- ============================================================================
CALL `_tmp_add_column_if_not_exists`('biz_salary_config', 'version',
    'INT NOT NULL DEFAULT 1 COMMENT ''乐观锁版本号'' AFTER `remark`');

-- ============================================================================
-- 6. Add indexes for per-employee queries
-- ============================================================================
CALL `_tmp_add_index_if_not_exists`('biz_salary_config', 'idx_sc_user', '`user_id`');
CALL `_tmp_add_index_if_not_exists`('biz_salary_config', 'idx_sc_status_date', '`status`, `effective_date`');

-- ============================================================================
-- Cleanup helper procedures
-- ============================================================================
DROP PROCEDURE IF EXISTS `_tmp_add_column_if_not_exists`;
DROP PROCEDURE IF EXISTS `_tmp_add_index_if_not_exists`;
