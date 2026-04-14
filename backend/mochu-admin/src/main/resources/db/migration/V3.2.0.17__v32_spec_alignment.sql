-- V3.2.0.17__v32_spec_alignment.sql
-- MOCHU-OA V3.2 规格对齐：修复多项 P0 数据库问题
-- 涵盖：缺失角色、权限码、状态枚举、版本字段默认值、唯一索引、缺失表、字段宽度
SET NAMES utf8mb4;

-- ============================================================================
-- 1. 补齐规格要求的 6 个缺失角色
--    规格共 11 个角色，data.sql 已有: PROJ_MGR, BUDGET, PURCHASE, FINANCE, HR
--    此处补: GM, LEGAL, DATA, BASE, SOFT, TEAM_MEMBER
-- ============================================================================
INSERT IGNORE INTO `sys_role` (`role_code`, `role_name`, `data_scope`, `remark`, `status`, `deleted`)
VALUES
    ('GM',          '总经理',           1, '系统最高管理权限', 1, 0),
    ('LEGAL',       '法务',             2, '合同法律审核',     1, 0),
    ('DATA',        '资料员',           2, '文档管理和数据维护', 1, 0),
    ('BASE',        '基础业务部员工',   4, '基础业务部门',     1, 0),
    ('SOFT',        '软件业务部员工',   4, '软件业务部门',     1, 0),
    ('TEAM_MEMBER', '项目团队成员',     3, '项目执行层人员',   1, 0);

-- ============================================================================
-- 2. 补齐 V3.2 规格 57 项权限中缺失的 40 个权限码
--    perm_type = 1（功能权限）
-- ============================================================================
INSERT IGNORE INTO `sys_permission` (`perm_code`, `perm_name`, `module`, `perm_type`)
VALUES
    -- project 模块
    ('project:approve',           '项目审批',       'project',   1),
    ('project:convert',           '虚拟转实体',     'project',   1),
    ('project:terminate',         '项目终止',       'project',   1),
    ('project:resume',            '项目恢复',       'project',   1),
    -- contract 模块
    ('contract:sign-income',      '签订收入合同',   'contract',  1),
    ('contract:sign-expense',     '签订支出合同',   'contract',  1),
    ('contract:approve-finance',  '合同财务审批',   'contract',  1),
    ('contract:approve-legal',    '合同法务审批',   'contract',  1),
    ('contract:approve-gm',       '合同总经理审批', 'contract',  1),
    ('contract:link',             '合同关联',       'contract',  1),
    ('contract:template-manage',  '合同模板管理',   'contract',  1),
    -- purchase 模块
    ('purchase:list-manage',      '采购清单管理',   'purchase',  1),
    ('purchase:check-overbuy',    '超量采购检查',   'purchase',  1),
    -- material 模块
    ('material:inbound-approve',  '入库审批',       'material',  1),
    ('material:outbound-approve', '出库审批',       'material',  1),
    ('material:return',           '退库操作',       'material',  1),
    ('material:return-approve',   '退库审批',       'material',  1),
    -- progress 模块
    ('progress:view',             '进度查看',       'progress',  1),
    ('progress:correct',          '进度纠偏',       'progress',  1),
    -- change 模块
    ('change:apply',              '变更申请',       'change',    1),
    ('change:approve',            '变更审批',       'change',    1),
    -- statement 模块
    ('statement:apply',           '对账单申请',     'statement', 1),
    ('statement:approve',         '对账单审批',     'statement', 1),
    -- split 模块
    ('split:apply',               '收入拆分申请',   'split',     1),
    -- finance 模块
    ('finance:reimburse-approve', '报销审批',       'finance',   1),
    ('finance:payment-apply',     '付款申请',       'finance',   1),
    ('finance:report-view',       '财务报表查看',   'finance',   1),
    -- doc 模块
    ('doc:upload',                '文档上传',       'doc',       1),
    ('doc:download',              '文档下载',       'doc',       1),
    ('doc:manage',                '文档管理',       'doc',       1),
    -- report 模块
    ('report:view-all',           '查看所有报表',   'report',    1),
    ('report:view-project',       '查看项目报表',   'report',    1),
    -- system 模块
    ('system:log-view',           '审计日志查看',   'system',    1),
    -- hr 模块
    ('hr:entry-process',          '入职流程',       'hr',        1),
    ('hr:resign-process',         '离职流程',       'hr',        1),
    ('hr:salary-adjust',          '薪资调整',       'hr',        1),
    ('hr:salary-approve',         '薪资审批',       'hr',        1),
    ('hr:contract-view-own',      '查看本人合同',   'hr',        1),
    ('hr:social-insurance-config','社保配置',       'hr',        1),
    ('hr:tax-rate-config',        '税率配置',       'hr',        1);

-- ============================================================================
-- 3. 补齐缺失的业务状态枚举字典数据
-- ============================================================================
INSERT IGNORE INTO `sys_dict_data`
    (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `color_hex`, `status`, `deleted`)
VALUES
    ('biz_status', '跟踪中',     'tracking',              20, 'info',    '#909399', 1, 0),
    ('biz_status', '已转实体',   'converted',             21, 'success', '#67c23a', 1, 0),
    ('biz_status', '已完工验收', 'completion_accepted',   22, 'success', '#67c23a', 1, 0),
    ('biz_status', '已竣工验收', 'final_accepted',        23, 'success', '#67c23a', 1, 0),
    ('biz_status', '已完成审计', 'audit_done',            24, 'success', '#67c23a', 1, 0),
    ('biz_status', '归集失败',   'collect_failed',        25, 'danger',  '#f56c6c', 1, 0),
    ('biz_status', '已作废',     'voided',                26, 'info',    '#909399', 1, 0),
    ('biz_status', '已下线',     'offline',               27, 'info',    '#909399', 1, 0),
    ('biz_status', '已过期',     'expired',               28, 'info',    '#909399', 1, 0),
    ('biz_status', '已失效',     'inactive',              29, 'info',    '#909399', 1, 0);

-- ============================================================================
-- 4. 修正乐观锁 version 字段默认值（0 → 1）
--    修复 6 张业务表，首行不可为 0（乐观锁初始值应为 1）
-- ============================================================================
ALTER TABLE `biz_project`        ALTER COLUMN `version` SET DEFAULT 1;
ALTER TABLE `biz_contract`       ALTER COLUMN `version` SET DEFAULT 1;
ALTER TABLE `biz_inbound_order`  ALTER COLUMN `version` SET DEFAULT 1;
ALTER TABLE `biz_outbound_order` ALTER COLUMN `version` SET DEFAULT 1;
ALTER TABLE `biz_return_order`   ALTER COLUMN `version` SET DEFAULT 1;
ALTER TABLE `biz_payment_apply`  ALTER COLUMN `version` SET DEFAULT 1;

-- ============================================================================
-- 5. 添加唯一索引（先删除可能存在的普通索引，再创建唯一索引）
--    使用存储过程安全处理 "索引不存在" 的情况
-- ============================================================================

-- 辅助过程：安全删除索引（忽略不存在的索引）
DROP PROCEDURE IF EXISTS `_tmp_drop_index_if_exists`;
DELIMITER $$
CREATE PROCEDURE `_tmp_drop_index_if_exists`(
    IN p_table  VARCHAR(128),
    IN p_index  VARCHAR(128)
)
BEGIN
    DECLARE idx_count INT DEFAULT 0;
    SELECT COUNT(*) INTO idx_count
      FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME   = p_table
       AND INDEX_NAME   = p_index;
    IF idx_count > 0 THEN
        SET @sql_drop = CONCAT('ALTER TABLE `', p_table, '` DROP INDEX `', p_index, '`');
        PREPARE stmt FROM @sql_drop;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- 5-a. biz_project: uk_project_no
CALL `_tmp_drop_index_if_exists`('biz_project', 'idx_project_no');
CALL `_tmp_drop_index_if_exists`('biz_project', 'uk_project_no');
CREATE UNIQUE INDEX `uk_project_no` ON `biz_project` (`project_no`);

-- 5-b. biz_supplier: uk_supplier_name
CALL `_tmp_drop_index_if_exists`('biz_supplier', 'idx_supplier_name');
CALL `_tmp_drop_index_if_exists`('biz_supplier', 'uk_supplier_name');
CREATE UNIQUE INDEX `uk_supplier_name` ON `biz_supplier` (`supplier_name`);

-- 5-c. biz_material_base: uk_material_code
CALL `_tmp_drop_index_if_exists`('biz_material_base', 'idx_material_code');
CALL `_tmp_drop_index_if_exists`('biz_material_base', 'uk_material_code');
CREATE UNIQUE INDEX `uk_material_code` ON `biz_material_base` (`material_code`);

-- 5-d. biz_inventory: uk_inventory_project_material
CALL `_tmp_drop_index_if_exists`('biz_inventory', 'idx_inventory_project_material');
CALL `_tmp_drop_index_if_exists`('biz_inventory', 'uk_inventory_project_material');
CREATE UNIQUE INDEX `uk_inventory_project_material` ON `biz_inventory` (`project_id`, `material_id`);

-- 5-e. biz_salary: uk_salary_user_month
CALL `_tmp_drop_index_if_exists`('biz_salary', 'idx_salary_user_month');
CALL `_tmp_drop_index_if_exists`('biz_salary', 'uk_salary_user_month');
CREATE UNIQUE INDEX `uk_salary_user_month` ON `biz_salary` (`user_id`, `year_month`);

-- 清理辅助过程
DROP PROCEDURE IF EXISTS `_tmp_drop_index_if_exists`;

-- ============================================================================
-- 6. 创建缺失的表
-- ============================================================================

-- 6-a. 用户个性化配置表
CREATE TABLE IF NOT EXISTS `sys_user_config` (
    `id`           INT          AUTO_INCREMENT PRIMARY KEY,
    `user_id`      INT          NOT NULL,
    `config_key`   VARCHAR(100) NOT NULL,
    `config_value` TEXT,
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE  KEY `uk_user_config` (`user_id`, `config_key`),
    INDEX       `idx_user_id`   (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = '用户个性化配置';

-- 6-b. 报表订阅配置表
CREATE TABLE IF NOT EXISTS `biz_report_subscribe` (
    `id`          INT          AUTO_INCREMENT PRIMARY KEY,
    `user_id`     INT          NOT NULL,
    `report_type` VARCHAR(50)  NOT NULL COMMENT '报表类型',
    `frequency`   VARCHAR(20)  NOT NULL COMMENT 'daily/weekly/monthly',
    `params_json` TEXT         COMMENT '报表参数JSON',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用',
    `creator_id`  INT          NOT NULL,
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    INDEX `idx_user_id`     (`user_id`),
    INDEX `idx_report_type` (`report_type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = '报表订阅配置';

-- 6-c. 密码历史记录表（用于密码重复使用检测）
CREATE TABLE IF NOT EXISTS `sys_password_history` (
    `id`            INT          AUTO_INCREMENT PRIMARY KEY,
    `user_id`       INT          NOT NULL,
    `password_hash` VARCHAR(200) NOT NULL,
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = '密码历史记录';

-- ============================================================================
-- 7. 修正字段宽度
-- ============================================================================

-- 公告可见范围：扩展到 500 字符以支持多部门/角色列表
ALTER TABLE `sys_announcement` MODIFY COLUMN `scope` VARCHAR(500) DEFAULT 'all' COMMENT '可见范围';

-- 身份证号：AES 加密后 Base64 编码需要更大存储空间
ALTER TABLE `biz_hr_entry` MODIFY COLUMN `id_card_no` VARCHAR(200) COMMENT '身份证号(AES加密)';

-- ============================================================================
-- 8. 错误码 10002 文档备注（无 DDL，仅说明）
--    error_code 10002 = PARAM_VALIDATION_FAILED，已在后端 ErrorCode 枚举中定义
-- ============================================================================

-- ============================================================================
-- 9. 为 GM 角色分配全部权限（与 SUPER_ADMIN 同权）
--    使用 NOT EXISTS 子查询防止重复插入
-- ============================================================================
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
  FROM `sys_role` r, `sys_permission` p
 WHERE r.role_code = 'GM'
   AND NOT EXISTS (
       SELECT 1
         FROM `sys_role_permission` rp
        WHERE rp.role_id      = r.id
          AND rp.permission_id = p.id
   );
