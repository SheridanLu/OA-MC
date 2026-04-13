SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- Force database charset to utf8mb4 (fixes garbled Chinese on existing databases)
ALTER DATABASE mochu_oa CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- ============================================================
-- 1. sys_user
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_user (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50),
    real_name       VARCHAR(50),
    phone           VARCHAR(20),
    email           VARCHAR(100),
    dept_id         INT,
    position        VARCHAR(50),
    password_hash   VARCHAR(100),
    avatar          VARCHAR(255),
    status          TINYINT DEFAULT 1,
    flag_contact    TINYINT DEFAULT 1,
    privacy_mode    TINYINT DEFAULT 0,
    login_attempts  INT DEFAULT 0,
    last_login_time DATETIME,
    lock_until      DATETIME,
    force_change_pwd TINYINT DEFAULT 0,
    wx_userid       VARCHAR(100),
    creator_id      INT,
    created_at      DATETIME,
    updated_at      DATETIME,
    deleted         TINYINT DEFAULT 0,
    UNIQUE INDEX uk_username (username),
    INDEX idx_dept_id (dept_id),
    INDEX idx_status (status),
    INDEX idx_phone (phone)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 2. sys_role
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_role (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    role_code   VARCHAR(50),
    role_name   VARCHAR(50),
    data_scope  TINYINT DEFAULT 4,
    remark      VARCHAR(255),
    status      TINYINT DEFAULT 1,
    creator_id  INT,
    created_at  DATETIME,
    updated_at  DATETIME,
    deleted     TINYINT DEFAULT 0,
    UNIQUE INDEX uk_role_code (role_code),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 3. sys_user_role (junction)
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    INDEX idx_role_id (role_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 4. sys_permission
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_permission (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    perm_code   VARCHAR(100),
    perm_name   VARCHAR(100),
    module      VARCHAR(50),
    perm_type   TINYINT,
    UNIQUE INDEX uk_perm_code (perm_code),
    INDEX idx_module (module)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 5. sys_role_permission (junction)
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_role_permission (
    role_id       INT NOT NULL,
    permission_id INT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    INDEX idx_permission_id (permission_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 6. sys_dept
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_dept (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100),
    parent_id   INT DEFAULT 0,
    level       INT DEFAULT 1,
    path        VARCHAR(255),
    sort        INT DEFAULT 0,
    leader_id   INT,
    phone       VARCHAR(20),
    remark      VARCHAR(255),
    status      TINYINT DEFAULT 1,
    creator_id  INT,
    created_at  DATETIME,
    updated_at  DATETIME,
    deleted     TINYINT DEFAULT 0,
    INDEX idx_parent_id (parent_id),
    INDEX idx_leader_id (leader_id),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 7. sys_config
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_config (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    config_key    VARCHAR(100),
    config_value  TEXT,
    config_desc   VARCHAR(255),
    config_group  VARCHAR(50),
    status        TINYINT DEFAULT 1,
    creator_id    INT,
    created_at    DATETIME,
    updated_at    DATETIME,
    deleted       TINYINT DEFAULT 0,
    UNIQUE INDEX uk_config_key (config_key),
    INDEX idx_config_group (config_group)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 8. sys_audit_log
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_audit_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT,
    user_name       VARCHAR(50),
    operate_type    VARCHAR(50),
    operate_module  VARCHAR(50),
    biz_type        VARCHAR(50),
    biz_id          INT,
    before_data     TEXT,
    after_data      TEXT,
    ip_address      VARCHAR(50),
    request_id      VARCHAR(100),
    created_at      DATETIME,
    INDEX idx_user_id (user_id),
    INDEX idx_biz_type_biz_id (biz_type, biz_id),
    INDEX idx_created_at (created_at)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 9. sys_todo
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_todo (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT,
    biz_type    VARCHAR(50),
    biz_id      INT,
    title       VARCHAR(200),
    content     TEXT,
    status      TINYINT DEFAULT 0,
    created_at  DATETIME,
    updated_at  DATETIME,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_biz_type_biz_id (biz_type, biz_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 10. sys_announcement
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_announcement (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(200),
    content         TEXT,
    type            VARCHAR(20),
    publish_time    DATETIME,
    expire_time     DATETIME,
    publisher_id    INT,
    status          VARCHAR(30) DEFAULT 'draft',
    is_top          TINYINT DEFAULT 0,
    scope           VARCHAR(20) DEFAULT 'all',
    creator_id      INT,
    created_at      DATETIME,
    updated_at      DATETIME,
    deleted         TINYINT DEFAULT 0,
    INDEX idx_publisher_id (publisher_id),
    INDEX idx_status (status),
    INDEX idx_publish_time (publish_time)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 11. sys_delegation
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_delegation (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    delegator_id      INT,
    delegatee_id      INT,
    permission_codes  TEXT,
    start_time        DATETIME,
    end_time          DATETIME,
    remark            VARCHAR(255),
    status            TINYINT DEFAULT 1,
    creator_id        INT,
    created_at        DATETIME,
    updated_at        DATETIME,
    deleted           TINYINT DEFAULT 0,
    INDEX idx_delegator_id (delegator_id),
    INDEX idx_delegatee_id (delegatee_id),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 12. biz_project
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_project (
    id                      INT AUTO_INCREMENT PRIMARY KEY,
    project_no              VARCHAR(50),
    project_name            VARCHAR(200),
    project_alias           VARCHAR(100),
    project_type            INT,
    contract_type           VARCHAR(20),
    location                VARCHAR(255),
    amount_with_tax         DECIMAL(18,2),
    amount_without_tax      DECIMAL(18,2),
    tax_amount              DECIMAL(18,2),
    tax_rate                DECIMAL(5,2),
    client_name             VARCHAR(100),
    plan_start_date         DATE,
    plan_end_date           DATE,
    warranty_date           DATE,
    status                  VARCHAR(30) DEFAULT 'draft',
    manager_id              INT,
    invest_limit            DECIMAL(18,2),
    bid_time                DATE,
    source_project_id       INT,
    cost_target_project_id  INT,
    remark                  TEXT,
    version                 INT DEFAULT 1,
    creator_id              INT,
    created_at              DATETIME,
    updated_at              DATETIME,
    deleted                 TINYINT DEFAULT 0,
    UNIQUE KEY uk_project_no (project_no),
    INDEX idx_manager_id (manager_id),
    INDEX idx_status (status),
    INDEX idx_source_project_id (source_project_id),
    INDEX idx_cost_target_project_id (cost_target_project_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 13. biz_project_member
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_project_member (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    project_id  INT,
    user_id     INT,
    role        VARCHAR(50),
    join_date   DATE,
    creator_id  INT,
    created_at  DATETIME,
    updated_at  DATETIME,
    deleted     TINYINT DEFAULT 0,
    INDEX idx_project_id (project_id),
    INDEX idx_user_id (user_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 14. biz_contract
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_contract (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    contract_no         VARCHAR(50),
    contract_name       VARCHAR(200),
    contract_type       VARCHAR(20),
    project_id          INT,
    supplier_id         INT,
    template_id         INT,
    tpl_version_id      INT,
    amount_with_tax     DECIMAL(18,2),
    amount_without_tax  DECIMAL(18,2),
    tax_rate            DECIMAL(5,2),
    tax_amount          DECIMAL(18,2),
    sign_date           DATE,
    start_date          DATE,
    end_date            DATE,
    party_a             VARCHAR(100),
    party_b             VARCHAR(100),
    status              VARCHAR(30) DEFAULT 'draft',
    parent_contract_id  INT,
    purchase_list_id    INT,
    terminate_reason    TEXT,
    terminate_time      DATETIME,
    terminator_id       INT,
    remark              TEXT,
    version             INT DEFAULT 1,
    creator_id          INT,
    created_at          DATETIME,
    updated_at          DATETIME,
    deleted             TINYINT DEFAULT 0,
    UNIQUE KEY uk_contract_no (contract_no),
    INDEX idx_project_id (project_id),
    INDEX idx_supplier_id (supplier_id),
    INDEX idx_template_id (template_id),
    INDEX idx_tpl_version_id (tpl_version_id),
    INDEX idx_parent_contract_id (parent_contract_id),
    INDEX idx_purchase_list_id (purchase_list_id),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 15. biz_contract_field_value
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_contract_field_value (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    contract_id   INT,
    field_key     VARCHAR(100),
    field_value   TEXT,
    created_at    DATETIME,
    updated_at    DATETIME,
    INDEX idx_contract_id (contract_id),
    INDEX idx_field_key (field_key)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 16. biz_supplier
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_supplier (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    supplier_name   VARCHAR(200),
    contact_person  VARCHAR(50),
    contact_phone   VARCHAR(20),
    address         VARCHAR(255),
    bank_name       VARCHAR(100),
    bank_account    VARCHAR(50),
    tax_no          VARCHAR(50),
    status          VARCHAR(30) DEFAULT 'active',
    remark          TEXT,
    creator_id      INT,
    created_at      DATETIME,
    updated_at      DATETIME,
    deleted         TINYINT DEFAULT 0,
    INDEX idx_status (status),
    INDEX idx_tax_no (tax_no)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 17. biz_material_base
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_material_base (
    id                   INT AUTO_INCREMENT PRIMARY KEY,
    material_code        VARCHAR(50),
    material_name        VARCHAR(200),
    spec_model           VARCHAR(200),
    unit                 VARCHAR(20),
    category             VARCHAR(50),
    base_price_with_tax  DECIMAL(18,2),
    tax_rate             INT,
    status               VARCHAR(30) DEFAULT 'active',
    creator_id           INT,
    created_at           DATETIME,
    updated_at           DATETIME,
    deleted              TINYINT DEFAULT 0,
    INDEX idx_material_code (material_code),
    INDEX idx_category (category),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 18. biz_purchase_list
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_purchase_list (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    list_no       VARCHAR(50),
    project_id    INT,
    total_amount  DECIMAL(18,2),
    status        VARCHAR(30) DEFAULT 'draft',
    remark        TEXT,
    creator_id    INT,
    created_at    DATETIME,
    updated_at    DATETIME,
    deleted       TINYINT DEFAULT 0,
    UNIQUE KEY uk_list_no (list_no),
    INDEX idx_project_id (project_id),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 19. biz_purchase_list_item
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_purchase_list_item (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    list_id         INT,
    material_id     INT,
    material_name   VARCHAR(200),
    spec_model      VARCHAR(200),
    unit            VARCHAR(20),
    quantity        DECIMAL(18,4),
    estimated_price DECIMAL(18,2),
    subtotal        DECIMAL(18,2),
    remark          VARCHAR(255),
    created_at      DATETIME,
    INDEX idx_list_id (list_id),
    INDEX idx_material_id (material_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 20. biz_spot_purchase
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_spot_purchase (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    purchase_no     VARCHAR(50),
    project_id      INT,
    item_name       VARCHAR(200),
    spec_model      VARCHAR(200),
    quantity        DECIMAL(18,4),
    unit_price      DECIMAL(18,2),
    amount          DECIMAL(18,2),
    supplier_name   VARCHAR(200),
    status          VARCHAR(30) DEFAULT 'draft',
    remark          TEXT,
    creator_id      INT,
    created_at      DATETIME,
    updated_at      DATETIME,
    deleted         TINYINT DEFAULT 0,
    UNIQUE KEY uk_purchase_no (purchase_no),
    INDEX idx_project_id (project_id),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 21. biz_inbound_order
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_inbound_order (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    inbound_no    VARCHAR(50),
    contract_id   INT,
    project_id    INT,
    warehouse     VARCHAR(100),
    inbound_date  DATE,
    status        VARCHAR(30) DEFAULT 'draft',
    remark        TEXT,
    version       INT DEFAULT 1,
    creator_id    INT,
    created_at    DATETIME,
    updated_at    DATETIME,
    deleted       TINYINT DEFAULT 0,
    UNIQUE KEY uk_inbound_no (inbound_no),
    INDEX idx_contract_id (contract_id),
    INDEX idx_project_id (project_id),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 22. biz_inbound_order_item
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_inbound_order_item (
    id                   INT AUTO_INCREMENT PRIMARY KEY,
    inbound_id           INT,
    material_id          INT,
    material_name        VARCHAR(200),
    spec_model           VARCHAR(200),
    unit                 VARCHAR(20),
    quantity             DECIMAL(18,4),
    unit_price           DECIMAL(18,2),
    subtotal             DECIMAL(18,2),
    contract_material_id INT,
    created_at           DATETIME,
    INDEX idx_inbound_id (inbound_id),
    INDEX idx_material_id (material_id),
    INDEX idx_contract_material_id (contract_material_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 23. biz_outbound_order
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_outbound_order (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    outbound_no     VARCHAR(50),
    project_id      INT,
    outbound_type   VARCHAR(20),
    outbound_date   DATE,
    status          VARCHAR(30) DEFAULT 'draft',
    remark          TEXT,
    version         INT DEFAULT 1,
    creator_id      INT,
    created_at      DATETIME,
    updated_at      DATETIME,
    deleted         TINYINT DEFAULT 0,
    UNIQUE KEY uk_outbound_no (outbound_no),
    INDEX idx_project_id (project_id),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 24. biz_outbound_order_item
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_outbound_order_item (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    outbound_id     INT,
    material_id     INT,
    material_name   VARCHAR(200),
    unit            VARCHAR(20),
    quantity        DECIMAL(18,4),
    avg_price       DECIMAL(18,2),
    subtotal        DECIMAL(18,2),
    created_at      DATETIME,
    INDEX idx_outbound_id (outbound_id),
    INDEX idx_material_id (material_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 25. biz_return_order
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_return_order (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    return_no         VARCHAR(50),
    project_id        INT,
    dispose_method    VARCHAR(20),
    target_project_id INT,
    return_date       DATE,
    status            VARCHAR(30) DEFAULT 'draft',
    remark            TEXT,
    version           INT DEFAULT 1,
    creator_id        INT,
    created_at        DATETIME,
    updated_at        DATETIME,
    deleted           TINYINT DEFAULT 0,
    UNIQUE KEY uk_return_no (return_no),
    INDEX idx_project_id (project_id),
    INDEX idx_target_project_id (target_project_id),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 26. biz_return_order_item
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_return_order_item (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    return_id       INT,
    material_id     INT,
    material_name   VARCHAR(200),
    unit            VARCHAR(20),
    quantity        DECIMAL(18,4),
    unit_price      DECIMAL(18,2),
    subtotal        DECIMAL(18,2),
    created_at      DATETIME,
    INDEX idx_return_id (return_id),
    INDEX idx_material_id (material_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 27. biz_inventory
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_inventory (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    project_id        INT,
    material_id       INT,
    material_name     VARCHAR(200),
    unit              VARCHAR(20),
    current_quantity  DECIMAL(18,4),
    avg_price         DECIMAL(18,2),
    total_amount      DECIMAL(18,2),
    creator_id        INT,
    created_at        DATETIME,
    updated_at        DATETIME,
    deleted           TINYINT DEFAULT 0,
    INDEX idx_project_id (project_id),
    INDEX idx_material_id (material_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 28. biz_inventory_check
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_inventory_check (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    check_no    VARCHAR(50),
    project_id  INT,
    check_date  DATE,
    gain_count  INT,
    loss_count  INT,
    remark      TEXT,
    status      VARCHAR(30) DEFAULT 'draft',
    creator_id  INT,
    created_at  DATETIME,
    updated_at  DATETIME,
    deleted     TINYINT DEFAULT 0,
    UNIQUE KEY uk_check_no (check_no),
    INDEX idx_project_id (project_id),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 29. biz_gantt_task
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_gantt_task (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    project_id          INT,
    parent_id           INT,
    task_name           VARCHAR(200),
    task_type           INT,
    plan_start_date     DATE,
    plan_end_date       DATE,
    actual_start_date   DATE,
    actual_end_date     DATE,
    progress_pct        DECIMAL(5,2),
    dependency_type     VARCHAR(10),
    dependency_task_id  INT,
    sort_order          INT,
    status              VARCHAR(30) DEFAULT 'pending',
    creator_id          INT,
    created_at          DATETIME,
    updated_at          DATETIME,
    deleted             TINYINT DEFAULT 0,
    INDEX idx_project_id (project_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_dependency_task_id (dependency_task_id),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 30. biz_milestone_dep
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_milestone_dep (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    milestone_id      INT,
    dep_milestone_id  INT,
    created_at        DATETIME,
    INDEX idx_milestone_id (milestone_id),
    INDEX idx_dep_milestone_id (dep_milestone_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 31. biz_change_order
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_change_order (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    change_no     VARCHAR(50),
    change_type   VARCHAR(20),
    project_id    INT,
    contract_id   INT,
    title         VARCHAR(200),
    description   TEXT,
    total_amount  DECIMAL(18,2),
    status        VARCHAR(30) DEFAULT 'draft',
    creator_id    INT,
    created_at    DATETIME,
    updated_at    DATETIME,
    deleted       TINYINT DEFAULT 0,
    UNIQUE KEY uk_change_no (change_no),
    INDEX idx_project_id (project_id),
    INDEX idx_contract_id (contract_id),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 32. biz_change_detail
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_change_detail (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    change_id       INT,
    item_name       VARCHAR(200),
    spec_model      VARCHAR(200),
    unit            VARCHAR(20),
    plan_quantity   DECIMAL(18,4),
    actual_quantity DECIMAL(18,4),
    diff_quantity   DECIMAL(18,4),
    unit_price      DECIMAL(18,2),
    subtotal        DECIMAL(18,2),
    created_at      DATETIME,
    INDEX idx_change_id (change_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 33. biz_statement
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_statement (
    id                      INT AUTO_INCREMENT PRIMARY KEY,
    statement_no            VARCHAR(50),
    project_id              INT,
    contract_id             INT,
    period                  VARCHAR(20),
    contract_amount         DECIMAL(18,2),
    progress_ratio          DECIMAL(5,2),
    current_output          DECIMAL(18,2),
    cumulative_output       DECIMAL(18,2),
    current_collection      DECIMAL(18,2),
    cumulative_collection   DECIMAL(18,2),
    status                  VARCHAR(30) DEFAULT 'draft',
    creator_id              INT,
    created_at              DATETIME,
    updated_at              DATETIME,
    deleted                 TINYINT DEFAULT 0,
    UNIQUE KEY uk_statement_no (statement_no),
    INDEX idx_project_id (project_id),
    INDEX idx_contract_id (contract_id),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 34. biz_payment_apply
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_payment_apply (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    payment_no      VARCHAR(50),
    payment_type    VARCHAR(20),
    project_id      INT,
    contract_id     INT,
    statement_id    INT,
    amount          DECIMAL(18,2),
    payee_name      VARCHAR(100),
    payee_bank      VARCHAR(100),
    payee_account   VARCHAR(50),
    payment_method  VARCHAR(20),
    status          VARCHAR(30) DEFAULT 'draft',
    confirm_time    DATETIME,
    confirmer_id    INT,
    confirm_remark  TEXT,
    remark          TEXT,
    version         INT DEFAULT 1,
    creator_id      INT,
    created_at      DATETIME,
    updated_at      DATETIME,
    deleted         TINYINT DEFAULT 0,
    UNIQUE KEY uk_payment_no (payment_no),
    INDEX idx_project_id (project_id),
    INDEX idx_contract_id (contract_id),
    INDEX idx_statement_id (statement_id),
    INDEX idx_confirmer_id (confirmer_id),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 35. biz_invoice
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_invoice (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    invoice_no      VARCHAR(50),
    invoice_type    VARCHAR(20),
    amount          DECIMAL(18,2),
    tax_rate        DECIMAL(5,2),
    tax_amount      DECIMAL(18,2),
    invoice_date    DATE,
    invoice_party   VARCHAR(100),
    biz_type        VARCHAR(20),
    biz_id          INT,
    attachment_id   INT,
    is_certified    TINYINT DEFAULT 0,
    certified_date  DATE,
    status          VARCHAR(30) DEFAULT 'draft',
    creator_id      INT,
    created_at      DATETIME,
    updated_at      DATETIME,
    deleted         TINYINT DEFAULT 0,
    UNIQUE KEY uk_invoice_no (invoice_no),
    INDEX idx_biz_type_biz_id (biz_type, biz_id),
    INDEX idx_attachment_id (attachment_id),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 36. biz_reimburse
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_reimburse (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    reimburse_no    VARCHAR(50),
    reimburse_type  VARCHAR(20),
    amount          DECIMAL(18,2),
    dept_id         INT,
    project_id      INT,
    description     TEXT,
    status          VARCHAR(30) DEFAULT 'draft',
    creator_id      INT,
    created_at      DATETIME,
    updated_at      DATETIME,
    deleted         TINYINT DEFAULT 0,
    UNIQUE KEY uk_reimburse_no (reimburse_no),
    INDEX idx_dept_id (dept_id),
    INDEX idx_project_id (project_id),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 37. biz_receipt
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_receipt (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    receipt_no      VARCHAR(50),
    project_id      INT,
    contract_id     INT,
    amount          DECIMAL(18,2),
    receipt_date    DATE,
    payer           VARCHAR(100),
    receipt_method  VARCHAR(20),
    remark          TEXT,
    status          VARCHAR(30) DEFAULT 'draft',
    creator_id      INT,
    created_at      DATETIME,
    updated_at      DATETIME,
    deleted         TINYINT DEFAULT 0,
    UNIQUE KEY uk_receipt_no (receipt_no),
    INDEX idx_project_id (project_id),
    INDEX idx_contract_id (contract_id),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 38. biz_cost_ledger
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_cost_ledger (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    project_id    INT,
    cost_type     VARCHAR(50),
    cost_subtype  VARCHAR(50),
    amount        DECIMAL(18,2),
    biz_type      VARCHAR(20),
    biz_id        INT,
    collect_time  DATETIME,
    creator_id    INT,
    created_at    DATETIME,
    INDEX idx_project_id (project_id),
    INDEX idx_cost_type (cost_type),
    INDEX idx_biz_type_biz_id (biz_type, biz_id),
    INDEX idx_collect_time (collect_time)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 39. biz_salary
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_salary (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    user_id           INT,
    salary_month      VARCHAR(10),
    base_salary       DECIMAL(18,2),
    position_salary   DECIMAL(18,2),
    performance       DECIMAL(18,2),
    allowance         DECIMAL(18,2),
    bonus             DECIMAL(18,2),
    deduction         DECIMAL(18,2),
    social_insurance  DECIMAL(18,2),
    tax               DECIMAL(18,2),
    net_salary        DECIMAL(18,2),
    status            VARCHAR(30) DEFAULT 'draft',
    creator_id        INT,
    created_at        DATETIME,
    updated_at        DATETIME,
    deleted           TINYINT DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_salary_month (salary_month),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 40. biz_salary_config
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_salary_config (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    grade         VARCHAR(20),
    grade_name    VARCHAR(50),
    base_salary   DECIMAL(18,2),
    allowance     DECIMAL(18,2),
    remark        TEXT,
    status        VARCHAR(30) DEFAULT 'active',
    creator_id    INT,
    created_at    DATETIME,
    updated_at    DATETIME,
    deleted       TINYINT DEFAULT 0,
    INDEX idx_grade (grade),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 41. biz_social_insurance
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_social_insurance (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    user_id             INT,
    pension_base        DECIMAL(18,2),
    medical_base        DECIMAL(18,2),
    unemployment_base   DECIMAL(18,2),
    injury_base         DECIMAL(18,2),
    maternity_base      DECIMAL(18,2),
    housing_base        DECIMAL(18,2),
    remark              TEXT,
    status              VARCHAR(30) DEFAULT 'active',
    creator_id          INT,
    created_at          DATETIME,
    updated_at          DATETIME,
    deleted             TINYINT DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 42. biz_tax_rate
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_tax_rate (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    level       INT,
    min_income  DECIMAL(18,2),
    max_income  DECIMAL(18,2),
    rate        DECIMAL(5,4),
    deduction   DECIMAL(18,2),
    creator_id  INT,
    created_at  DATETIME,
    updated_at  DATETIME,
    deleted     TINYINT DEFAULT 0,
    INDEX idx_level (level)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 43. biz_hr_entry
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_hr_entry (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    entry_no        VARCHAR(50),
    applicant_name  VARCHAR(50),
    phone           VARCHAR(20),
    dept_id         INT,
    position        VARCHAR(50),
    entry_date      DATE,
    education       VARCHAR(20),
    work_years      INT,
    id_card_no      VARCHAR(20),
    status          VARCHAR(30) DEFAULT 'draft',
    creator_id      INT,
    created_at      DATETIME,
    updated_at      DATETIME,
    deleted         TINYINT DEFAULT 0,
    UNIQUE KEY uk_entry_no (entry_no),
    INDEX idx_dept_id (dept_id),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 44. biz_hr_resign
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_hr_resign (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    resign_no       VARCHAR(50),
    user_id         INT,
    resign_type     VARCHAR(20),
    resign_date     DATE,
    resign_reason   TEXT,
    handover_status VARCHAR(30),
    handover_to     INT,
    status          VARCHAR(30) DEFAULT 'draft',
    creator_id      INT,
    created_at      DATETIME,
    updated_at      DATETIME,
    deleted         TINYINT DEFAULT 0,
    UNIQUE KEY uk_resign_no (resign_no),
    INDEX idx_user_id (user_id),
    INDEX idx_handover_to (handover_to),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 45. biz_hr_contract
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_hr_contract (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    user_id           INT,
    contract_type     VARCHAR(20),
    start_date        DATE,
    end_date          DATE,
    status            VARCHAR(30) DEFAULT 'active',
    renewal_id        INT,
    contract_file_id  INT,
    creator_id        INT,
    created_at        DATETIME,
    updated_at        DATETIME,
    deleted           TINYINT DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_renewal_id (renewal_id),
    INDEX idx_contract_file_id (contract_file_id),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 46. biz_hr_certificate
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_hr_certificate (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    cert_type       VARCHAR(20),
    user_id         INT,
    cert_name       VARCHAR(100),
    cert_category   VARCHAR(50),
    cert_no         VARCHAR(50),
    issue_date      DATE,
    expire_date     DATE,
    attachment_id   INT,
    warn_status     VARCHAR(30),
    status          VARCHAR(30) DEFAULT 'active',
    creator_id      INT,
    created_at      DATETIME,
    updated_at      DATETIME,
    deleted         TINYINT DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_cert_no (cert_no),
    INDEX idx_attachment_id (attachment_id),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 47. biz_asset_transfer
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_asset_transfer (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    transfer_no     VARCHAR(50),
    user_id         INT,
    asset_name      VARCHAR(200),
    asset_code      VARCHAR(50),
    transfer_type   VARCHAR(20),
    transfer_date   DATE,
    remark          TEXT,
    status          VARCHAR(30) DEFAULT 'draft',
    creator_id      INT,
    created_at      DATETIME,
    updated_at      DATETIME,
    deleted         TINYINT DEFAULT 0,
    UNIQUE KEY uk_transfer_no (transfer_no),
    INDEX idx_user_id (user_id),
    INDEX idx_asset_code (asset_code),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 48. biz_labor_settlement
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_labor_settlement (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    settlement_no       VARCHAR(50),
    project_id          INT,
    contract_id         INT,
    settlement_amount   DECIMAL(18,2),
    paid_amount         DECIMAL(18,2),
    apply_pay_amount    DECIMAL(18,2),
    status              VARCHAR(30) DEFAULT 'draft',
    creator_id          INT,
    created_at          DATETIME,
    updated_at          DATETIME,
    deleted             TINYINT DEFAULT 0,
    UNIQUE KEY uk_settlement_no (settlement_no),
    INDEX idx_project_id (project_id),
    INDEX idx_contract_id (contract_id),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 49. biz_case
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_case (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    project_id      INT,
    case_name       VARCHAR(200),
    case_type       VARCHAR(20),
    summary         VARCHAR(500),
    content         TEXT,
    cover_image_id  INT,
    display_order   INT,
    status          VARCHAR(30) DEFAULT 'draft',
    creator_id      INT,
    created_at      DATETIME,
    updated_at      DATETIME,
    deleted         TINYINT DEFAULT 0,
    INDEX idx_project_id (project_id),
    INDEX idx_cover_image_id (cover_image_id),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 50. biz_completion_finish
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_completion_finish (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    project_id          INT,
    title               VARCHAR(200),
    plan_finish_date    DATE,
    finish_content      TEXT,
    self_check_result   TEXT,
    remaining_issues    TEXT,
    status              VARCHAR(30) DEFAULT 'draft',
    creator_id          INT,
    created_at          DATETIME,
    updated_at          DATETIME,
    deleted             TINYINT DEFAULT 0,
    INDEX idx_project_id (project_id),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 51. biz_completion_doc
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_completion_doc (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    doc_no      VARCHAR(50),
    project_id  INT,
    doc_name    VARCHAR(200),
    doc_type    VARCHAR(50),
    file_url    VARCHAR(500),
    remark      TEXT,
    status      VARCHAR(30) DEFAULT 'draft',
    creator_id  INT,
    created_at  DATETIME,
    updated_at  DATETIME,
    deleted     TINYINT DEFAULT 0,
    UNIQUE KEY uk_doc_no (doc_no),
    INDEX idx_project_id (project_id),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 52. biz_drawing
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_drawing (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    drawing_no      VARCHAR(50),
    project_id      INT,
    drawing_name    VARCHAR(200),
    drawing_type    VARCHAR(50),
    file_url        VARCHAR(500),
    version         VARCHAR(20),
    remark          TEXT,
    status          VARCHAR(30) DEFAULT 'draft',
    creator_id      INT,
    created_at      DATETIME,
    updated_at      DATETIME,
    deleted         TINYINT DEFAULT 0,
    UNIQUE KEY uk_drawing_no (drawing_no),
    INDEX idx_project_id (project_id),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 53. biz_progress_report
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_progress_report (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    report_no       VARCHAR(50),
    project_id      INT,
    report_date     DATE,
    content         TEXT,
    progress_rate   DECIMAL(5,2),
    issues          TEXT,
    next_plan       TEXT,
    remark          TEXT,
    status          VARCHAR(30) DEFAULT 'draft',
    creator_id      INT,
    created_at      DATETIME,
    updated_at      DATETIME,
    deleted         TINYINT DEFAULT 0,
    UNIQUE KEY uk_report_no (report_no),
    INDEX idx_project_id (project_id),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 54. biz_progress_statement
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_progress_statement (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    statement_no    VARCHAR(50),
    project_id      INT,
    period          VARCHAR(20),
    planned_amount  DECIMAL(18,2),
    actual_amount   DECIMAL(18,2),
    completion_rate DECIMAL(5,2),
    remark          TEXT,
    status          VARCHAR(30) DEFAULT 'draft',
    creator_id      INT,
    created_at      DATETIME,
    updated_at      DATETIME,
    deleted         TINYINT DEFAULT 0,
    UNIQUE KEY uk_statement_no (statement_no),
    INDEX idx_project_id (project_id),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 55. biz_income_split
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_income_split (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    split_no    VARCHAR(50),
    project_id  INT,
    contract_id INT,
    split_item  VARCHAR(200),
    amount      DECIMAL(18,2),
    ratio       DECIMAL(5,4),
    remark      TEXT,
    status      VARCHAR(30) DEFAULT 'draft',
    creator_id  INT,
    created_at  DATETIME,
    updated_at  DATETIME,
    deleted     TINYINT DEFAULT 0,
    UNIQUE KEY uk_split_no (split_no),
    INDEX idx_project_id (project_id),
    INDEX idx_contract_id (contract_id),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 56. biz_external_contact
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_external_contact (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(50),
    company         VARCHAR(200),
    phone           VARCHAR(20),
    email           VARCHAR(100),
    contact_type    VARCHAR(20),
    position        VARCHAR(50),
    remark          TEXT,
    creator_id      INT,
    created_at      DATETIME,
    updated_at      DATETIME,
    deleted         TINYINT DEFAULT 0,
    INDEX idx_contact_type (contact_type),
    INDEX idx_company (company)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 57. biz_exception_task
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_exception_task (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    biz_type        VARCHAR(50),
    biz_id          INT,
    fail_reason     TEXT,
    handler_id      INT,
    resolve_remark  TEXT,
    status          TINYINT DEFAULT 0,
    creator_id      INT,
    created_at      DATETIME,
    updated_at      DATETIME,
    INDEX idx_biz_type_biz_id (biz_type, biz_id),
    INDEX idx_handler_id (handler_id),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 58. biz_no_seed (composite primary key)
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_no_seed (
    prefix      VARCHAR(20)  NOT NULL,
    date_part   VARCHAR(10)  NOT NULL,
    current_seq INT DEFAULT 0,
    PRIMARY KEY (prefix, date_part)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 59. biz_approval_instance
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_approval_instance (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    biz_type        VARCHAR(50),
    biz_id          INT,
    flow_def_id     INT,
    current_node    INT,
    status          VARCHAR(30),
    initiator_id    INT,
    deadline_at     DATETIME,
    reminder_level  INT DEFAULT 0,
    created_at      DATETIME,
    updated_at      DATETIME,
    INDEX idx_biz_type_biz_id (biz_type, biz_id),
    INDEX idx_flow_def_id (flow_def_id),
    INDEX idx_initiator_id (initiator_id),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 60. biz_approval_record
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_approval_record (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    instance_id       INT,
    node_order        INT,
    node_name         VARCHAR(100),
    approver_id       INT,
    action            VARCHAR(20),
    opinion           TEXT,
    delegate_from_id  INT,
    created_at        DATETIME,
    INDEX idx_instance_id (instance_id),
    INDEX idx_approver_id (approver_id),
    INDEX idx_delegate_from_id (delegate_from_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 61. biz_approval_cc
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_approval_cc (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    instance_id INT,
    user_id     INT,
    cc_type     VARCHAR(20),
    is_read     TINYINT DEFAULT 0,
    is_handled  TINYINT DEFAULT 0,
    created_at  DATETIME,
    handled_at  DATETIME,
    INDEX idx_instance_id (instance_id),
    INDEX idx_user_id (user_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 62. biz_approval_cosign
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_approval_cosign (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    instance_id     INT,
    node_order      INT,
    cosigner_id     INT,
    status          VARCHAR(30),
    opinion         TEXT,
    created_at      DATETIME,
    completed_at    DATETIME,
    INDEX idx_instance_id (instance_id),
    INDEX idx_cosigner_id (cosigner_id),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 63. biz_attachment
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_attachment (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    file_name   VARCHAR(200),
    file_path   VARCHAR(500),
    file_size   BIGINT,
    file_type   VARCHAR(100),
    file_ext    VARCHAR(10),
    md5         VARCHAR(50),
    biz_type    VARCHAR(50),
    biz_id      INT,
    status      TINYINT DEFAULT 1,
    creator_id  INT,
    created_at  DATETIME,
    updated_at  DATETIME,
    deleted     TINYINT DEFAULT 0,
    INDEX idx_biz_type_biz_id (biz_type, biz_id),
    INDEX idx_md5 (md5),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 64. sys_flow_def
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_flow_def (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    biz_type        VARCHAR(50),
    flow_name       VARCHAR(100),
    nodes_json      TEXT,
    condition_json  TEXT,
    status          TINYINT DEFAULT 1,
    version         INT DEFAULT 1,
    creator_id      INT,
    created_at      DATETIME,
    updated_at      DATETIME,
    deleted         TINYINT DEFAULT 0,
    INDEX idx_biz_type (biz_type),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 65. sys_contract_tpl
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_contract_tpl (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    contract_type   VARCHAR(20),
    tpl_name        VARCHAR(200),
    description     TEXT,
    status          TINYINT DEFAULT 1,
    creator_id      INT,
    created_at      DATETIME,
    updated_at      DATETIME,
    deleted         TINYINT DEFAULT 0,
    INDEX idx_contract_type (contract_type),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 66. sys_contract_tpl_version
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_contract_tpl_version (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    tpl_id          INT,
    version_no      INT,
    file_path       VARCHAR(500),
    file_name       VARCHAR(200),
    file_md5        VARCHAR(50),
    html_cache      LONGTEXT,
    status          TINYINT DEFAULT 1,
    effective_from  DATETIME,
    effective_until DATETIME,
    creator_id      INT,
    created_at      DATETIME,
    INDEX idx_tpl_id (tpl_id),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 67. sys_contract_tpl_field
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_contract_tpl_field (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    version_id      INT,
    field_key       VARCHAR(100),
    field_name      VARCHAR(100),
    field_type      VARCHAR(20),
    required        TINYINT DEFAULT 0,
    options_json    TEXT,
    default_value   VARCHAR(255),
    sort_order      INT DEFAULT 0,
    placeholder     VARCHAR(200),
    max_length      INT,
    validation_rule VARCHAR(255),
    INDEX idx_version_id (version_id),
    INDEX idx_field_key (field_key)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 68. sys_contract_tpl_audit
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_contract_tpl_audit (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    tpl_id      INT,
    version_id  INT,
    action      VARCHAR(20),
    detail      TEXT,
    operator_id INT,
    operated_at DATETIME,
    INDEX idx_tpl_id (tpl_id),
    INDEX idx_version_id (version_id),
    INDEX idx_operator_id (operator_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 69. sys_dict_type
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_dict_type (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    dict_type   VARCHAR(100) NOT NULL COMMENT '字典类型编码',
    dict_name   VARCHAR(100) NOT NULL COMMENT '字典名称',
    status      TINYINT DEFAULT 1 COMMENT '0=停用 1=正常',
    remark      VARCHAR(500),
    creator_id  INT,
    created_at  DATETIME,
    updated_at  DATETIME,
    deleted     TINYINT DEFAULT 0,
    UNIQUE INDEX uk_dict_type (dict_type),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- 70. sys_dict_data
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_dict_data (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    dict_type   VARCHAR(100) NOT NULL COMMENT '关联 sys_dict_type.dict_type',
    dict_label  VARCHAR(100) NOT NULL COMMENT '字典标签',
    dict_value  VARCHAR(100) NOT NULL COMMENT '字典值',
    dict_sort   INT DEFAULT 0 COMMENT '排序',
    css_class   VARCHAR(100) COMMENT '样式属性',
    list_class  VARCHAR(100) COMMENT 'Element Plus Tag type',
    color_hex   VARCHAR(20) COMMENT '颜色值',
    is_default  TINYINT DEFAULT 0 COMMENT '是否默认',
    status      TINYINT DEFAULT 1 COMMENT '0=停用 1=正常',
    remark      VARCHAR(500),
    creator_id  INT,
    created_at  DATETIME,
    updated_at  DATETIME,
    deleted     TINYINT DEFAULT 0,
    INDEX idx_dict_type (dict_type),
    INDEX idx_status (status)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ENGINE=InnoDB;

-- ============================================================
-- Force-convert ALL existing tables to utf8mb4 (fixes garbled
-- Chinese text when tables were created with latin1/utf8mb3)
-- ============================================================
ALTER TABLE sys_user CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE sys_role CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE sys_user_role CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE sys_permission CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE sys_role_permission CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE sys_dept CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE sys_config CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE sys_audit_log CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE sys_todo CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE sys_announcement CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE sys_delegation CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_project CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_project_member CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_contract CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_contract_field_value CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_supplier CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_material_base CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_purchase_list CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_purchase_list_item CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_spot_purchase CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_inbound_order CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_inbound_order_item CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_outbound_order CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_outbound_order_item CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_return_order CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_return_order_item CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_inventory CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_inventory_check CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_gantt_task CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_milestone_dep CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_change_order CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_change_detail CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_statement CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_payment_apply CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_invoice CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_reimburse CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_receipt CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_cost_ledger CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_salary CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_salary_config CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_social_insurance CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_tax_rate CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_hr_entry CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_hr_resign CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_hr_contract CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_hr_certificate CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_asset_transfer CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_labor_settlement CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_case CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_completion_finish CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_completion_doc CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_drawing CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_progress_report CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_progress_statement CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_income_split CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_external_contact CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_exception_task CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_no_seed CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_approval_instance CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_approval_record CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_approval_cc CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_approval_cosign CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_attachment CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE sys_flow_def CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE sys_contract_tpl CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE sys_contract_tpl_version CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE sys_contract_tpl_field CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE sys_contract_tpl_audit CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE sys_dict_type CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE sys_dict_data CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- ============================================================
-- 71. infra_codegen_table — 代码生成-表配置
-- ============================================================
CREATE TABLE IF NOT EXISTS infra_codegen_table (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    table_name      VARCHAR(200)  NOT NULL COMMENT '表名',
    table_comment   VARCHAR(500)  DEFAULT '' COMMENT '表描述',
    module_name     VARCHAR(100)  NOT NULL COMMENT '所属模块(如 system / business)',
    biz_name        VARCHAR(100)  NOT NULL COMMENT '业务名(如 contract)',
    class_name      VARCHAR(200)  NOT NULL COMMENT 'Java类名(如 Contract)',
    template_type   TINYINT       DEFAULT 1 COMMENT '模板类型 1单表 2主子 3树表',
    author          VARCHAR(100)  DEFAULT '' COMMENT '作者',
    remark          VARCHAR(500)  DEFAULT '' COMMENT '备注',
    creator_id      INT,
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT       DEFAULT 0,
    UNIQUE KEY uk_table_name (table_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ============================================================
-- 72. infra_codegen_column — 代码生成-列配置
-- ============================================================
CREATE TABLE IF NOT EXISTS infra_codegen_column (
    id                      INT AUTO_INCREMENT PRIMARY KEY,
    table_id                INT           NOT NULL COMMENT '关联表ID',
    column_name             VARCHAR(200)  NOT NULL COMMENT '列名',
    column_comment          VARCHAR(500)  DEFAULT '' COMMENT '列描述',
    data_type               VARCHAR(100)  NOT NULL COMMENT '数据库类型(varchar/int/datetime等)',
    java_type               VARCHAR(100)  NOT NULL COMMENT 'Java类型(String/Integer/LocalDateTime等)',
    java_field              VARCHAR(200)  NOT NULL COMMENT 'Java字段名(camelCase)',
    dict_type               VARCHAR(200)  DEFAULT '' COMMENT '关联字典类型',
    html_type               VARCHAR(50)   DEFAULT 'input' COMMENT '前端组件(input/select/datetime/textarea/upload)',
    pk_flag                 TINYINT       DEFAULT 0 COMMENT '是否主键',
    nullable_flag           TINYINT       DEFAULT 1 COMMENT '是否允许空',
    create_operation        TINYINT       DEFAULT 1 COMMENT '新增时显示',
    update_operation        TINYINT       DEFAULT 1 COMMENT '编辑时显示',
    list_operation          TINYINT       DEFAULT 1 COMMENT '列表时显示',
    query_operation         TINYINT       DEFAULT 0 COMMENT '是否查询条件',
    query_condition         VARCHAR(20)   DEFAULT 'EQ' COMMENT '查询方式(EQ/NE/LIKE/GT/GTE/LT/LTE/BETWEEN)',
    column_sort             INT           DEFAULT 0 COMMENT '排序',
    creator_id              INT,
    created_at              DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                 TINYINT       DEFAULT 0,
    KEY idx_table_id (table_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

ALTER TABLE infra_codegen_table CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE infra_codegen_column CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- ============================================================
-- 73. bpm_process_definition_ext — Flowable 流程定义扩展
-- （Flowable 自动创建 ACT_* 表，此为业务扩展信息）
-- ============================================================
CREATE TABLE IF NOT EXISTS bpm_process_definition_ext (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    process_def_key     VARCHAR(255) NOT NULL COMMENT 'Flowable流程定义Key',
    process_def_id      VARCHAR(64)  NOT NULL COMMENT 'Flowable流程定义ID（带版本）',
    biz_type            VARCHAR(50)  DEFAULT '' COMMENT '关联业务类型',
    form_config         TEXT         COMMENT '表单配置JSON',
    candidate_strategy  TINYINT      DEFAULT 1 COMMENT '候选人策略 1指定用户 2指定角色 3部门主管 4发起人自选',
    candidate_param     VARCHAR(500) DEFAULT '' COMMENT '候选人参数（用户ID列表或角色code）',
    status              TINYINT      DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark              VARCHAR(500) DEFAULT '',
    creator_id          INT,
    created_at          DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT      DEFAULT 0,
    UNIQUE KEY uk_proc_def_id (process_def_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ============================================================
-- 74. bpm_task_ext — Flowable 任务扩展（关联业务数据）
-- ============================================================
CREATE TABLE IF NOT EXISTS bpm_task_ext (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    process_inst_id     VARCHAR(64)  NOT NULL COMMENT 'Flowable流程实例ID',
    task_id             VARCHAR(64)  DEFAULT '' COMMENT 'Flowable任务ID',
    biz_type            VARCHAR(50)  NOT NULL COMMENT '业务类型',
    biz_id              INT          NOT NULL COMMENT '业务数据ID',
    biz_no              VARCHAR(100) DEFAULT '' COMMENT '业务单号',
    initiator_id        INT          COMMENT '发起人ID',
    result              TINYINT      DEFAULT 0 COMMENT '审批结果 0进行中 1通过 2驳回 3撤回 4取消',
    end_time            DATETIME     COMMENT '结束时间',
    creator_id          INT,
    created_at          DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT      DEFAULT 0,
    KEY idx_process_inst_id (process_inst_id),
    KEY idx_biz (biz_type, biz_id),
    KEY idx_initiator (initiator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ============================================================
-- 75. bpm_oa_rule — 业务类型到Flowable流程定义的映射
-- ============================================================
CREATE TABLE IF NOT EXISTS bpm_oa_rule (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    biz_type            VARCHAR(50)  NOT NULL COMMENT '业务类型',
    biz_name            VARCHAR(100) DEFAULT '' COMMENT '业务名称',
    process_def_key     VARCHAR(255) NOT NULL COMMENT 'Flowable流程定义Key',
    enabled             TINYINT      DEFAULT 1 COMMENT '是否启用',
    creator_id          INT,
    created_at          DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT      DEFAULT 0,
    UNIQUE KEY uk_biz_type (biz_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

ALTER TABLE bpm_process_definition_ext CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE bpm_task_ext CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE bpm_oa_rule CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- ============================================================
-- Phase 4: ERP 增强
-- ============================================================

-- 76. biz_supplier_rating — 供应商评价
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_supplier_rating (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    supplier_id     INT           NOT NULL COMMENT '供应商ID',
    purchase_id     INT           COMMENT '关联采购单ID',
    project_id      INT           COMMENT '所属项目',
    quality_score   TINYINT       DEFAULT 5 COMMENT '质量评分 1-5',
    delivery_score  TINYINT       DEFAULT 5 COMMENT '交货评分 1-5',
    service_score   TINYINT       DEFAULT 5 COMMENT '服务评分 1-5',
    price_score     TINYINT       DEFAULT 5 COMMENT '价格评分 1-5',
    total_score     DECIMAL(4,2)  COMMENT '综合评分',
    comment_text    TEXT          COMMENT '评价内容',
    creator_id      INT,
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT       DEFAULT 0,
    KEY idx_supplier_id (supplier_id),
    KEY idx_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ============================================================
-- 77. biz_inventory_alert — 库存预警配置
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_inventory_alert (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    project_id      INT           NOT NULL COMMENT '项目ID',
    material_id     INT           NOT NULL COMMENT '物料ID',
    material_name   VARCHAR(200)  DEFAULT '' COMMENT '物料名称',
    unit            VARCHAR(20)   DEFAULT '' COMMENT '单位',
    safety_qty      DECIMAL(18,2) DEFAULT 0 COMMENT '安全库存量',
    min_qty         DECIMAL(18,2) DEFAULT 0 COMMENT '最低库存量',
    alert_enabled   TINYINT       DEFAULT 1 COMMENT '预警开关',
    creator_id      INT,
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT       DEFAULT 0,
    UNIQUE KEY uk_project_material (project_id, material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ============================================================
-- 78. biz_inventory_transfer — 库存调拨单
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_inventory_transfer (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    transfer_no     VARCHAR(50)   NOT NULL COMMENT '调拨单号',
    from_project_id INT           NOT NULL COMMENT '源项目',
    to_project_id   INT           NOT NULL COMMENT '目标项目',
    material_id     INT           NOT NULL COMMENT '物料ID',
    material_name   VARCHAR(200)  DEFAULT '',
    unit            VARCHAR(20)   DEFAULT '',
    qty             DECIMAL(18,2) NOT NULL COMMENT '调拨数量',
    avg_price       DECIMAL(18,2) DEFAULT 0 COMMENT '调拨单价（加权均价）',
    total_amount    DECIMAL(18,2) DEFAULT 0 COMMENT '调拨金额',
    status          VARCHAR(30)   DEFAULT 'draft' COMMENT 'draft/confirmed/cancelled',
    remark          TEXT,
    confirm_time    DATETIME      COMMENT '确认时间',
    creator_id      INT,
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT       DEFAULT 0,
    KEY idx_from_project (from_project_id),
    KEY idx_to_project (to_project_id),
    UNIQUE KEY uk_transfer_no (transfer_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ============================================================
-- Phase 5: 报表引擎增强
-- ============================================================

-- 79. sys_report_template — 可配置报表模板
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_report_template (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    report_name     VARCHAR(200)  NOT NULL COMMENT '报表名称',
    category        VARCHAR(50)   DEFAULT 'custom' COMMENT '分类',
    chart_type      VARCHAR(20)   DEFAULT 'table' COMMENT 'table/line/bar/pie/radar',
    sql_text        TEXT          NOT NULL COMMENT '参数化SQL（:param_name 占位）',
    params_json     TEXT          COMMENT '参数定义JSON [{"name":"startDate","label":"开始日期","type":"date"}]',
    x_field         VARCHAR(100)  DEFAULT '' COMMENT '图表X轴字段名',
    y_fields        VARCHAR(500)  DEFAULT '' COMMENT '图表Y轴字段名（逗号分隔）',
    description     TEXT          COMMENT '说明',
    status          TINYINT       DEFAULT 1,
    creator_id      INT,
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT       DEFAULT 0,
    KEY idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

ALTER TABLE biz_supplier_rating CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_inventory_alert CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE biz_inventory_transfer CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE sys_report_template CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
