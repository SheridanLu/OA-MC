SET NAMES utf8mb4;

-- ============================================================
-- 清空系统种子表，彻底修复乱码问题
-- （已有数据可能在错误字符集下写入，无法通过 UPDATE 修复）
-- ============================================================
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE sys_role_permission;
TRUNCATE TABLE sys_user_role;
TRUNCATE TABLE sys_permission;
TRUNCATE TABLE sys_role;
TRUNCATE TABLE sys_dept;
DELETE FROM sys_user WHERE username = 'admin';
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- Roles
-- ============================================================
INSERT INTO sys_role (role_code, role_name, data_scope, remark, status, deleted) VALUES
('SUPER_ADMIN', '超级管理员', 1, '拥有所有权限', 1, 0),
('PROJ_MGR', '项目经理', 3, '管理所属项目', 1, 0),
('PURCHASE', '采购专员', 2, '采购管理', 1, 0),
('FINANCE', '财务人员', 1, '财务管理', 1, 0),
('HR', '人力资源', 1, '人事管理', 1, 0),
('INVENTORY', '仓库管理员', 2, '库存管理', 1, 0),
('BUDGET', '预算专员', 2, '预算审核', 1, 0),
('VIEWER', '查看者', 4, '只读权限', 1, 0);

-- ============================================================
-- Default department
-- ============================================================
INSERT INTO sys_dept (name, parent_id, level, path, sort, status, deleted)
VALUES ('总公司', 0, 1, '/1', 1, 1, 0);

-- ============================================================
-- Admin user (password: admin123 BCrypt hash)
-- ============================================================
INSERT INTO sys_user (username, real_name, phone, email, dept_id, position, password_hash, status, deleted)
VALUES ('admin', '系统管理员', '13800000000', 'admin@mochu.com', 1, '系统管理员',
        '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 1, 0);

-- ============================================================
-- Admin -> SUPER_ADMIN role binding
-- ============================================================
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username = 'admin' AND r.role_code = 'SUPER_ADMIN';

-- ============================================================
-- V3.2 Fine-grained permissions (67 codes)
-- ============================================================

-- System (10)
INSERT INTO sys_permission (perm_code, perm_name, module, perm_type) VALUES
('system:user-manage', '用户管理', 'system', 1),
('system:role-manage', '角色管理', 'system', 1),
('system:dept-manage', '部门管理', 'system', 1),
('system:announcement-manage', '公告管理', 'system', 1),
('system:audit-log', '审计日志查看', 'system', 1),
('system:config', '系统配置查看', 'system', 1),
('system:config:edit', '系统配置编辑', 'system', 1),
('system:delegation', '委托代理查看', 'system', 1),
('system:delegation:edit', '委托代理编辑', 'system', 1),
('system:tpl-manage', '合同模板管理', 'system', 1);

-- Project (6)
INSERT INTO sys_permission (perm_code, perm_name, module, perm_type) VALUES
('project:create', '项目创建', 'project', 1),
('project:view-all', '查看所有项目', 'project', 1),
('project:view-own', '查看本人项目', 'project', 1),
('project:edit', '项目编辑', 'project', 1),
('project:delete', '项目删除', 'project', 1),
('project:suspend', '项目暂停/恢复', 'project', 1);

-- Contract (6)
INSERT INTO sys_permission (perm_code, perm_name, module, perm_type) VALUES
('contract:create', '合同创建', 'contract', 1),
('contract:view-all', '查看所有合同', 'contract', 1),
('contract:view-own', '查看本人合同', 'contract', 1),
('contract:edit', '合同编辑', 'contract', 1),
('contract:delete', '合同删除', 'contract', 1),
('contract:terminate', '合同终止', 'contract', 1);

-- Purchase (4)
INSERT INTO sys_permission (perm_code, perm_name, module, perm_type) VALUES
('purchase:create', '采购单创建', 'purchase', 1),
('purchase:view', '采购单查看', 'purchase', 1),
('purchase:edit', '采购单编辑', 'purchase', 1),
('purchase:delete', '采购单删除', 'purchase', 1);

-- Material (4)
INSERT INTO sys_permission (perm_code, perm_name, module, perm_type) VALUES
('material:view', '物资查看', 'material', 1),
('material:edit', '物资编辑', 'material', 1),
('material:inbound', '入库操作', 'material', 1),
('material:outbound', '出库操作', 'material', 1);

-- Inventory (6)
INSERT INTO sys_permission (perm_code, perm_name, module, perm_type) VALUES
('inventory:inbound', '入库单管理', 'inventory', 1),
('inventory:outbound', '出库单管理', 'inventory', 1),
('inventory:return', '退库单管理', 'inventory', 1),
('inventory:stock-view', '库存查看', 'inventory', 1),
('inventory:check', '盘点管理', 'inventory', 1),
('inventory:check-approve', '盘点审批', 'inventory', 1);

-- Finance (8)
INSERT INTO sys_permission (perm_code, perm_name, module, perm_type) VALUES
('finance:statement-manage', '对账单管理', 'finance', 1),
('finance:payment-create', '付款申请创建', 'finance', 1),
('finance:payment-confirm', '付款确认', 'finance', 1),
('finance:invoice-manage', '发票管理', 'finance', 1),
('finance:reimburse-manage', '报销管理', 'finance', 1),
('finance:receipt-create', '收款登记', 'finance', 1),
('finance:cost-view', '成本台账查看', 'finance', 1),
('finance:cost-summary', '成本汇总查看', 'finance', 1);

-- HR (8)
INSERT INTO sys_permission (perm_code, perm_name, module, perm_type) VALUES
('hr:salary-manage', '薪资管理', 'hr', 1),
('hr:salary-config', '薪资配置', 'hr', 1),
('hr:contract-manage', '劳动合同管理', 'hr', 1),
('hr:certificate-manage', '证书管理', 'hr', 1),
('hr:entry-manage', '入职管理', 'hr', 1),
('hr:resign-manage', '离职管理', 'hr', 1),
('hr:social-insurance', '社保配置', 'hr', 1),
('hr:asset-transfer', '资产交接', 'hr', 1);

-- Progress (4)
INSERT INTO sys_permission (perm_code, perm_name, module, perm_type) VALUES
('progress:gantt-manage', '甘特图管理', 'progress', 1),
('progress:milestone-manage', '里程碑管理', 'progress', 1),
('progress:change-manage', '变更单管理', 'progress', 1),
('progress:report', '进度汇报', 'progress', 1);

-- Completion (4)
INSERT INTO sys_permission (perm_code, perm_name, module, perm_type) VALUES
('completion:finish-manage', '完工验收管理', 'completion', 1),
('completion:labor-manage', '劳务结算管理', 'completion', 1),
('completion:drawing-manage', '竣工图纸管理', 'completion', 1),
('completion:doc-manage', '竣工资料管理', 'completion', 1);

-- Approval (3)
INSERT INTO sys_permission (perm_code, perm_name, module, perm_type) VALUES
('approval:flow-manage', '审批流程管理', 'approval', 1),
('approval:operate', '审批操作', 'approval', 1),
('approval:view', '审批查看', 'approval', 1);

-- Report (2)
INSERT INTO sys_permission (perm_code, perm_name, module, perm_type) VALUES
('report:view', '报表查看', 'report', 1),
('report:export', '报表导出', 'report', 1);

-- Supplier (2)
INSERT INTO sys_permission (perm_code, perm_name, module, perm_type) VALUES
('supplier:view', '供应商查看', 'supplier', 1),
('supplier:edit', '供应商编辑', 'supplier', 1);

-- Infra (2)
INSERT INTO sys_permission (perm_code, perm_name, module, perm_type) VALUES
('system:dict-manage', '字典管理', 'system', 1),
('infra:codegen', '代码生成', 'infra', 1);

-- BPM (4)
INSERT INTO sys_permission (perm_code, perm_name, module, perm_type) VALUES
('bpm:process-manage', '流程定义管理', 'bpm', 1),
('bpm:task-operate', '流程任务操作', 'bpm', 1),
('bpm:instance-view', '流程实例查看', 'bpm', 1),
('bpm:rule-manage', '流程规则管理', 'bpm', 1);

-- ERP 增强 (3)
INSERT INTO sys_permission (perm_code, perm_name, module, perm_type) VALUES
('supplier:rating', '供应商评价', 'supplier', 1),
('inventory:transfer', '库存调拨', 'inventory', 1),
('inventory:alert-manage', '库存预警管理', 'inventory', 1);

-- Report 增强 (1)
INSERT INTO sys_permission (perm_code, perm_name, module, perm_type) VALUES
('report:template-manage', '报表模板管理', 'report', 1);

-- ============================================================
-- SUPER_ADMIN -> ALL permissions (cross join)
-- ============================================================
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.role_code = 'SUPER_ADMIN';

-- ============================================================
-- 字典类型种子数据
-- ============================================================
TRUNCATE TABLE sys_dict_data;
TRUNCATE TABLE sys_dict_type;

INSERT INTO sys_dict_type (dict_type, dict_name, status, deleted) VALUES
('biz_status', '业务状态', 1, 0),
('contract_type', '合同类型', 1, 0),
('change_type', '变更类型', 1, 0),
('material_category', '物料分类', 1, 0),
('payment_type', '付款类型', 1, 0),
('invoice_type', '发票类型', 1, 0),
('reimburse_type', '报销类型', 1, 0),
('outbound_type', '出库类型', 1, 0),
('cert_type', '证书类型', 1, 0),
('education_level', '学历', 1, 0),
('hr_contract_type', '劳动合同类型', 1, 0),
('resign_type', '离职类型', 1, 0),
('exception_biz_type', '异常工单类型', 1, 0),
('case_type', '案例类型', 1, 0),
('tax_rate', '税率', 1, 0);

-- ============================================================
-- 字典数据种子数据
-- ============================================================

-- biz_status (19)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, dict_sort, list_class, color_hex, status, deleted) VALUES
('biz_status', '草稿', 'draft', 1, 'info', '#909399', 1, 0),
('biz_status', '待审批', 'pending', 2, 'warning', '#e6a23c', 1, 0),
('biz_status', '已审批', 'approved', 3, 'success', '#67c23a', 1, 0),
('biz_status', '已驳回', 'rejected', 4, 'danger', '#f56c6c', 1, 0),
('biz_status', '已取消', 'cancelled', 5, 'info', '#909399', 1, 0),
('biz_status', '已确认', 'confirmed', 6, 'success', '#67c23a', 1, 0),
('biz_status', '已领取', 'collected', 7, 'success', '#67c23a', 1, 0),
('biz_status', '已退回', 'returned', 8, 'warning', '#e6a23c', 1, 0),
('biz_status', '已关闭', 'closed', 9, 'info', '#909399', 1, 0),
('biz_status', '进行中', 'active', 10, '', '#409eff', 1, 0),
('biz_status', '已暂停', 'suspended', 11, 'warning', '#e6a23c', 1, 0),
('biz_status', '已终止', 'terminated', 12, 'danger', '#f56c6c', 1, 0),
('biz_status', '已完成', 'completed', 13, 'success', '#67c23a', 1, 0),
('biz_status', '虚拟', 'virtual', 14, 'info', '#909399', 1, 0),
('biz_status', '实体', 'entity', 15, '', '#409eff', 1, 0),
('biz_status', '逾期', 'overdue', 16, 'danger', '#f56c6c', 1, 0),
('biz_status', '正常', 'normal', 17, 'success', '#67c23a', 1, 0),
('biz_status', '已付款', 'paid', 18, 'success', '#67c23a', 1, 0),
('biz_status', '未付款', 'unpaid', 19, 'warning', '#e6a23c', 1, 0);

-- contract_type (7)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, dict_sort, list_class, color_hex, status, deleted) VALUES
('contract_type', '收入合同', 'income', 1, '', NULL, 1, 0),
('contract_type', '支出合同', 'expense', 2, '', NULL, 1, 0),
('contract_type', '劳务合同', 'labor', 3, '', NULL, 1, 0),
('contract_type', '材料合同', 'material', 4, '', NULL, 1, 0),
('contract_type', '设备租赁合同', 'equipment_lease', 5, '', NULL, 1, 0),
('contract_type', '专业分包合同', 'professional', 6, '', NULL, 1, 0),
('contract_type', '其他合同', 'other', 7, '', NULL, 1, 0);

-- change_type (4)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, dict_sort, list_class, color_hex, status, deleted) VALUES
('change_type', '设计变更', 'design', 1, '', NULL, 1, 0),
('change_type', '工程变更', 'engineering', 2, '', NULL, 1, 0),
('change_type', '签证变更', 'visa', 3, '', NULL, 1, 0),
('change_type', '其他变更', 'other', 4, '', NULL, 1, 0);

-- material_category (9)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, dict_sort, list_class, color_hex, status, deleted) VALUES
('material_category', '钢材', 'steel', 1, '', NULL, 1, 0),
('material_category', '水泥', 'cement', 2, '', NULL, 1, 0),
('material_category', '砂石', 'sand', 3, '', NULL, 1, 0),
('material_category', '混凝土', 'concrete', 4, '', NULL, 1, 0),
('material_category', '木材', 'wood', 5, '', NULL, 1, 0),
('material_category', '电气材料', 'electric', 6, '', NULL, 1, 0),
('material_category', '水暖材料', 'plumbing', 7, '', NULL, 1, 0),
('material_category', '装饰材料', 'decor', 8, '', NULL, 1, 0),
('material_category', '其他', 'other', 9, '', NULL, 1, 0);

-- payment_type (5)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, dict_sort, list_class, color_hex, status, deleted) VALUES
('payment_type', '预付款', 'advance', 1, '', NULL, 1, 0),
('payment_type', '进度款', 'progress', 2, '', NULL, 1, 0),
('payment_type', '结算款', 'final', 3, '', NULL, 1, 0),
('payment_type', '质保金', 'retention', 4, '', NULL, 1, 0),
('payment_type', '其他', 'other', 5, '', NULL, 1, 0);

-- invoice_type (3)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, dict_sort, list_class, color_hex, status, deleted) VALUES
('invoice_type', '增值税专用发票', 'special', 1, '', NULL, 1, 0),
('invoice_type', '增值税普通发票', 'ordinary', 2, '', NULL, 1, 0),
('invoice_type', '收据', 'receipt', 3, '', NULL, 1, 0);

-- reimburse_type (4)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, dict_sort, list_class, color_hex, status, deleted) VALUES
('reimburse_type', '差旅费', 'travel', 1, '', NULL, 1, 0),
('reimburse_type', '办公费', 'office', 2, '', NULL, 1, 0),
('reimburse_type', '材料费', 'material', 3, '', NULL, 1, 0),
('reimburse_type', '其他', 'other', 4, '', NULL, 1, 0);

-- outbound_type (4)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, dict_sort, list_class, color_hex, status, deleted) VALUES
('outbound_type', '施工领用', 'construction', 1, '', NULL, 1, 0),
('outbound_type', '调拨出库', 'transfer', 2, '', NULL, 1, 0),
('outbound_type', '报废出库', 'scrap', 3, '', NULL, 1, 0),
('outbound_type', '其他', 'other', 4, '', NULL, 1, 0);

-- cert_type (4)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, dict_sort, list_class, color_hex, status, deleted) VALUES
('cert_type', '安全证书', 'safety', 1, '', NULL, 1, 0),
('cert_type', '资质证书', 'qualification', 2, '', NULL, 1, 0),
('cert_type', '技能证书', 'skill', 3, '', NULL, 1, 0),
('cert_type', '特种作业证', 'special', 4, '', NULL, 1, 0);

-- education_level (5)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, dict_sort, list_class, color_hex, status, deleted) VALUES
('education_level', '高中/中专', 'high_school', 1, '', NULL, 1, 0),
('education_level', '大专', 'associate', 2, '', NULL, 1, 0),
('education_level', '本科', 'bachelor', 3, '', NULL, 1, 0),
('education_level', '硕士', 'master', 4, '', NULL, 1, 0),
('education_level', '博士', 'doctor', 5, '', NULL, 1, 0);

-- hr_contract_type (3)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, dict_sort, list_class, color_hex, status, deleted) VALUES
('hr_contract_type', '固定期限', 'fixed', 1, '', NULL, 1, 0),
('hr_contract_type', '无固定期限', 'unfixed', 2, '', NULL, 1, 0),
('hr_contract_type', '以完成工作任务为期限', 'task', 3, '', NULL, 1, 0);

-- resign_type (4)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, dict_sort, list_class, color_hex, status, deleted) VALUES
('resign_type', '主动离职', 'voluntary', 1, '', NULL, 1, 0),
('resign_type', '辞退', 'involuntary', 2, '', NULL, 1, 0),
('resign_type', '退休', 'retirement', 3, '', NULL, 1, 0),
('resign_type', '合同到期', 'expiry', 4, '', NULL, 1, 0);

-- exception_biz_type (4)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, dict_sort, list_class, color_hex, status, deleted) VALUES
('exception_biz_type', '质量问题', 'quality', 1, '', NULL, 1, 0),
('exception_biz_type', '安全问题', 'safety', 2, '', NULL, 1, 0),
('exception_biz_type', '进度问题', 'progress', 3, '', NULL, 1, 0),
('exception_biz_type', '成本问题', 'cost', 4, '', NULL, 1, 0);

-- case_type (4)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, dict_sort, list_class, color_hex, status, deleted) VALUES
('case_type', '质量案例', 'quality', 1, '', NULL, 1, 0),
('case_type', '安全案例', 'safety', 2, '', NULL, 1, 0),
('case_type', '纠纷案例', 'dispute', 3, '', NULL, 1, 0),
('case_type', '其他案例', 'other', 4, '', NULL, 1, 0);

-- tax_rate (6)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, dict_sort, list_class, color_hex, status, deleted) VALUES
('tax_rate', '0%', '0', 1, '', NULL, 1, 0),
('tax_rate', '1%', '1', 2, '', NULL, 1, 0),
('tax_rate', '3%', '3', 3, '', NULL, 1, 0),
('tax_rate', '6%', '6', 4, '', NULL, 1, 0),
('tax_rate', '9%', '9', 5, '', NULL, 1, 0),
('tax_rate', '13%', '13', 6, '', NULL, 1, 0);

-- ============================================================
-- V3.2 审批流程定义种子数据 (18条)
-- ============================================================
DELETE FROM sys_flow_def WHERE deleted = 0;

-- 4.1 项目立项: 采购员→财务→总经理
INSERT INTO sys_flow_def (biz_type, flow_name, nodes_json, condition_json, status, version, creator_id, deleted) VALUES
('project', '项目立项审批',
 '[{"node_order":1,"node_name":"采购员审核","approver_type":"role","approver_id":3},{"node_order":2,"node_name":"财务审核","approver_type":"role","approver_id":4},{"node_order":3,"node_name":"总经理审批","approver_type":"role","approver_id":1}]',
 NULL, 1, 1, NULL, 0);

-- 4.2 合同(常规): 采购员→财务→法务→总经理
INSERT INTO sys_flow_def (biz_type, flow_name, nodes_json, condition_json, status, version, creator_id, deleted) VALUES
('contract', '常规合同审批',
 '[{"node_order":1,"node_name":"采购员审核","approver_type":"role","approver_id":3},{"node_order":2,"node_name":"财务审核","approver_type":"role","approver_id":4},{"node_order":3,"node_name":"法务审核","approver_type":"role","approver_id":4},{"node_order":4,"node_name":"总经理审批","approver_type":"role","approver_id":1}]',
 NULL, 1, 1, NULL, 0);

-- 4.3 合同(支出超量): 采购员→预算员→财务→法务→总经理
INSERT INTO sys_flow_def (biz_type, flow_name, nodes_json, condition_json, status, version, creator_id, deleted) VALUES
('contract', '支出合同超量审批',
 '[{"node_order":1,"node_name":"采购员审核","approver_type":"role","approver_id":3},{"node_order":2,"node_name":"预算员审核","approver_type":"role","approver_id":7},{"node_order":3,"node_name":"财务审核","approver_type":"role","approver_id":4},{"node_order":4,"node_name":"法务审核","approver_type":"role","approver_id":4},{"node_order":5,"node_name":"总经理审批","approver_type":"role","approver_id":1}]',
 '{"field":"contract_type","op":"eq","value":"expense","and":{"field":"over_budget","op":"eq","value":true}}',
 1, 2, NULL, 0);

-- 4.4 采购清单: 预算员→财务→总经理
INSERT INTO sys_flow_def (biz_type, flow_name, nodes_json, condition_json, status, version, creator_id, deleted) VALUES
('purchase', '采购清单审批',
 '[{"node_order":1,"node_name":"预算员审核","approver_type":"role","approver_id":7},{"node_order":2,"node_name":"财务审核","approver_type":"role","approver_id":4},{"node_order":3,"node_name":"总经理审批","approver_type":"role","approver_id":1}]',
 NULL, 1, 1, NULL, 0);

-- 4.5 零星采购(常规): 采购员→总经理
INSERT INTO sys_flow_def (biz_type, flow_name, nodes_json, condition_json, status, version, creator_id, deleted) VALUES
('spot_purchase', '零星采购审批',
 '[{"node_order":1,"node_name":"采购员审核","approver_type":"role","approver_id":3},{"node_order":2,"node_name":"总经理审批","approver_type":"role","approver_id":1}]',
 NULL, 1, 1, NULL, 0);

-- 4.6 零星采购(超阈值): 采购员→预算员→总经理
INSERT INTO sys_flow_def (biz_type, flow_name, nodes_json, condition_json, status, version, creator_id, deleted) VALUES
('spot_purchase', '零星采购超阈值审批',
 '[{"node_order":1,"node_name":"采购员审核","approver_type":"role","approver_id":3},{"node_order":2,"node_name":"预算员审核","approver_type":"role","approver_id":7},{"node_order":3,"node_name":"总经理审批","approver_type":"role","approver_id":1}]',
 '{"field":"amount","op":"gt","value":5000}',
 1, 2, NULL, 0);

-- 4.7 入库单: 采购员→财务
INSERT INTO sys_flow_def (biz_type, flow_name, nodes_json, condition_json, status, version, creator_id, deleted) VALUES
('inbound', '入库单审批',
 '[{"node_order":1,"node_name":"采购员审核","approver_type":"role","approver_id":3},{"node_order":2,"node_name":"财务审核","approver_type":"role","approver_id":4}]',
 NULL, 1, 1, NULL, 0);

-- 4.8 出库单: 项目经理→采购员确认→财务→总经理
INSERT INTO sys_flow_def (biz_type, flow_name, nodes_json, condition_json, status, version, creator_id, deleted) VALUES
('outbound', '出库单审批',
 '[{"node_order":1,"node_name":"项目经理审核","approver_type":"role","approver_id":2},{"node_order":2,"node_name":"采购员确认","approver_type":"role","approver_id":3},{"node_order":3,"node_name":"财务审核","approver_type":"role","approver_id":4},{"node_order":4,"node_name":"总经理审批","approver_type":"role","approver_id":1}]',
 NULL, 1, 1, NULL, 0);

-- 4.9 退库单: 项目经理→采购员确认→财务→总经理
INSERT INTO sys_flow_def (biz_type, flow_name, nodes_json, condition_json, status, version, creator_id, deleted) VALUES
('return_order', '退库单审批',
 '[{"node_order":1,"node_name":"项目经理审核","approver_type":"role","approver_id":2},{"node_order":2,"node_name":"采购员确认","approver_type":"role","approver_id":3},{"node_order":3,"node_name":"财务审核","approver_type":"role","approver_id":4},{"node_order":4,"node_name":"总经理审批","approver_type":"role","approver_id":1}]',
 NULL, 1, 1, NULL, 0);

-- 4.10 盘点单: 采购员→财务→总经理
INSERT INTO sys_flow_def (biz_type, flow_name, nodes_json, condition_json, status, version, creator_id, deleted) VALUES
('inventory_check', '盘点单审批',
 '[{"node_order":1,"node_name":"采购员审核","approver_type":"role","approver_id":3},{"node_order":2,"node_name":"财务审核","approver_type":"role","approver_id":4},{"node_order":3,"node_name":"总经理审批","approver_type":"role","approver_id":1}]',
 NULL, 1, 1, NULL, 0);

-- 4.11 里程碑进度: 项目经理→总经理
INSERT INTO sys_flow_def (biz_type, flow_name, nodes_json, condition_json, status, version, creator_id, deleted) VALUES
('gantt_task', '里程碑进度审批',
 '[{"node_order":1,"node_name":"项目经理审核","approver_type":"role","approver_id":2},{"node_order":2,"node_name":"总经理审批","approver_type":"role","approver_id":1}]',
 NULL, 1, 1, NULL, 0);

-- 4.12 变更单: 项目经理→预算员→总经理
INSERT INTO sys_flow_def (biz_type, flow_name, nodes_json, condition_json, status, version, creator_id, deleted) VALUES
('change_order', '变更单审批',
 '[{"node_order":1,"node_name":"项目经理审核","approver_type":"role","approver_id":2},{"node_order":2,"node_name":"预算员审核","approver_type":"role","approver_id":7},{"node_order":3,"node_name":"总经理审批","approver_type":"role","approver_id":1}]',
 NULL, 1, 1, NULL, 0);

-- 4.13 对账单: 项目经理→采购员→预算员→财务→总经理
INSERT INTO sys_flow_def (biz_type, flow_name, nodes_json, condition_json, status, version, creator_id, deleted) VALUES
('statement', '对账单审批',
 '[{"node_order":1,"node_name":"项目经理确认","approver_type":"role","approver_id":2},{"node_order":2,"node_name":"采购员审核","approver_type":"role","approver_id":3},{"node_order":3,"node_name":"预算员审核","approver_type":"role","approver_id":7},{"node_order":4,"node_name":"财务审核","approver_type":"role","approver_id":4},{"node_order":5,"node_name":"总经理审批","approver_type":"role","approver_id":1}]',
 NULL, 1, 1, NULL, 0);

-- 4.14 付款申请: 项目经理→采购员→财务→总经理
INSERT INTO sys_flow_def (biz_type, flow_name, nodes_json, condition_json, status, version, creator_id, deleted) VALUES
('payment', '付款申请审批',
 '[{"node_order":1,"node_name":"项目经理审核","approver_type":"role","approver_id":2},{"node_order":2,"node_name":"采购员审核","approver_type":"role","approver_id":3},{"node_order":3,"node_name":"财务审核","approver_type":"role","approver_id":4},{"node_order":4,"node_name":"总经理审批","approver_type":"role","approver_id":1}]',
 NULL, 1, 1, NULL, 0);

-- 4.15 报销: 员工提交→主管审批→财务审批→财务付款确认
INSERT INTO sys_flow_def (biz_type, flow_name, nodes_json, condition_json, status, version, creator_id, deleted) VALUES
('reimburse', '报销审批',
 '[{"node_order":1,"node_name":"主管审批","approver_type":"dept_leader","approver_id":null},{"node_order":2,"node_name":"财务审批","approver_type":"role","approver_id":4},{"node_order":3,"node_name":"财务付款确认","approver_type":"role","approver_id":4}]',
 NULL, 1, 1, NULL, 0);

-- 4.16 完工验收: 项目经理→预算员→采购员→总经理
INSERT INTO sys_flow_def (biz_type, flow_name, nodes_json, condition_json, status, version, creator_id, deleted) VALUES
('completion', '完工验收审批',
 '[{"node_order":1,"node_name":"项目经理审核","approver_type":"role","approver_id":2},{"node_order":2,"node_name":"预算员审核","approver_type":"role","approver_id":7},{"node_order":3,"node_name":"采购员审核","approver_type":"role","approver_id":3},{"node_order":4,"node_name":"总经理审批","approver_type":"role","approver_id":1}]',
 NULL, 1, 1, NULL, 0);

-- 4.17 劳务结算: 项目经理→预算员→采购员→财务→总经理
INSERT INTO sys_flow_def (biz_type, flow_name, nodes_json, condition_json, status, version, creator_id, deleted) VALUES
('labor_settlement', '劳务结算审批',
 '[{"node_order":1,"node_name":"项目经理审核","approver_type":"role","approver_id":2},{"node_order":2,"node_name":"预算员审核","approver_type":"role","approver_id":7},{"node_order":3,"node_name":"采购员审核","approver_type":"role","approver_id":3},{"node_order":4,"node_name":"财务审核","approver_type":"role","approver_id":4},{"node_order":5,"node_name":"总经理审批","approver_type":"role","approver_id":1}]',
 NULL, 1, 1, NULL, 0);

-- 4.18 工资表: HR→财务→总经理
INSERT INTO sys_flow_def (biz_type, flow_name, nodes_json, condition_json, status, version, creator_id, deleted) VALUES
('salary', '工资表审批',
 '[{"node_order":1,"node_name":"HR审核","approver_type":"role","approver_id":5},{"node_order":2,"node_name":"财务审核","approver_type":"role","approver_id":4},{"node_order":3,"node_name":"总经理审批","approver_type":"role","approver_id":1}]',
 NULL, 1, 1, NULL, 0);
