/**
 * 异步路由 — 根据用户权限动态注入
 * meta.permission: 控制路由可见性，值为权限码字符串或数组(OR)
 * meta.icon: 侧边栏图标名称（@element-plus/icons-vue 组件名）
 * meta.hidden: true 时不在菜单中显示
 * meta.title: 菜单标题 / 面包屑标题
 */
export default [
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/home',
    meta: { hidden: true },
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/home/index.vue'),
        meta: { title: '首页', icon: 'HomeFilled', affix: true }
      },
      {
        path: 'todos',
        name: 'TodoCenter',
        component: () => import('@/views/todo/index.vue'),
        meta: { title: '待办中心', icon: 'List' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人信息', hidden: true }
      },

      // ========== 项目管理 ==========
      {
        path: 'projects',
        name: 'ProjectManage',
        component: () => import('@/views/project/index.vue'),
        meta: {
          title: '项目管理',
          icon: 'Folder',
          permission: ['project:create', 'project:view-all', 'project:view-own']
        }
      },
      {
        path: 'projects/:id',
        name: 'ProjectDetail',
        component: () => import('@/views/project/detail.vue'),
        meta: {
          title: '项目详情',
          hidden: true,
          permission: ['project:view-all', 'project:view-own']
        }
      },

      // ========== 供应商管理 ==========
      {
        path: 'suppliers',
        name: 'SupplierManage',
        component: () => import('@/views/supplier/index.vue'),
        meta: {
          title: '供应商管理',
          icon: 'Van',
          permission: ['supplier:view', 'supplier:edit']
        }
      },
      {
        path: 'supplier/rating',
        name: 'SupplierRating',
        component: () => import('@/views/supplier/rating.vue'),
        meta: {
          title: '供应商评价',
          icon: 'Star',
          permission: 'supplier:rating'
        }
      },

      // ========== 材料管理 ==========
      {
        path: 'materials',
        name: 'MaterialManage',
        component: () => import('@/views/material/index.vue'),
        meta: {
          title: '材料管理',
          icon: 'Box',
          permission: ['material:inbound', 'material:outbound', 'material:view']
        }
      },

      // ========== 合同管理 ==========
      {
        path: 'contracts',
        name: 'ContractManage',
        component: () => import('@/views/contract/index.vue'),
        meta: {
          title: '合同管理',
          icon: 'Tickets',
          permission: [
            'contract:create', 'contract:view-all', 'contract:view-own',
            'contract:edit', 'system:tpl-manage'
          ]
        }
      },
      {
        path: 'contracts/:id',
        name: 'ContractDetail',
        component: () => import('@/views/contract/detail.vue'),
        meta: {
          title: '合同详情',
          hidden: true,
          permission: [
            'contract:view-all', 'contract:view-own'
          ]
        }
      },

      // ========== 采购管理 ==========
      {
        path: 'purchases',
        name: 'PurchaseManage',
        component: () => import('@/views/purchase/index.vue'),
        meta: {
          title: '采购管理',
          icon: 'ShoppingCart',
          permission: ['purchase:create', 'purchase:view', 'purchase:edit']
        }
      },

      // ========== 库存管理 ==========
      {
        path: 'inventory',
        name: 'InventoryManage',
        component: () => import('@/views/inventory/index.vue'),
        meta: {
          title: '库存管理',
          icon: 'House',
          permission: [
            'inventory:inbound', 'inventory:outbound', 'inventory:return',
            'inventory:stock-view', 'inventory:check', 'inventory:check-approve'
          ]
        }
      },
      {
        path: 'inventory/enhance',
        name: 'InventoryEnhance',
        component: () => import('@/views/inventory/enhance.vue'),
        meta: {
          title: '库存增强',
          icon: 'SetUp',
          permission: ['inventory:alert-manage', 'inventory:transfer']
        }
      },

      // ========== 进度变更 ==========
      {
        path: 'progress',
        name: 'ProgressManage',
        component: () => import('@/views/progress/index.vue'),
        meta: {
          title: '进度变更',
          icon: 'DataLine',
          permission: [
            'progress:gantt-manage', 'progress:milestone-manage',
            'progress:change-manage', 'progress:report'
          ]
        }
      },

      // ========== 财务管理 ==========
      {
        path: 'finance',
        name: 'FinanceManage',
        component: () => import('@/views/finance/index.vue'),
        meta: {
          title: '财务管理',
          icon: 'Money',
          permission: [
            'finance:statement-manage', 'finance:payment-create', 'finance:payment-confirm',
            'finance:invoice-manage', 'finance:reimburse-manage',
            'finance:cost-view', 'finance:cost-summary', 'finance:receipt-create'
          ]
        }
      },

      // ========== 人力资源 ==========
      {
        path: 'hr',
        name: 'HrManage',
        component: () => import('@/views/hr/index.vue'),
        meta: {
          title: '人力资源',
          icon: 'Avatar',
          permission: [
            'hr:salary-manage', 'hr:contract-manage', 'hr:certificate-manage',
            'hr:entry-manage', 'hr:resign-manage',
            'hr:salary-config', 'hr:social-insurance', 'hr:asset-transfer'
          ]
        }
      },

      // ========== 竣工劳务 ==========
      {
        path: 'completion',
        name: 'CompletionManage',
        component: () => import('@/views/completion/index.vue'),
        meta: {
          title: '竣工劳务',
          icon: 'Finished',
          permission: [
            'completion:finish-manage', 'completion:labor-manage',
            'completion:drawing-manage', 'completion:doc-manage'
          ]
        }
      },

      // ========== 流程审批 ==========
      {
        path: 'approval',
        name: 'ApprovalManage',
        component: () => import('@/views/approval/index.vue'),
        meta: { title: '流程审批', icon: 'Stamp', permission: ['approval:flow-manage', 'approval:operate', 'approval:view'] }
      },

      // ========== 报表分析 ==========
      {
        path: 'reports',
        name: 'ReportManage',
        component: () => import('@/views/report/index.vue'),
        meta: {
          title: '报表分析',
          icon: 'TrendCharts',
          permission: ['report:view']
        }
      },
      {
        path: 'report/template',
        name: 'ReportTemplate',
        component: () => import('@/views/report/template.vue'),
        meta: {
          title: '报表模板',
          icon: 'DocumentCopy',
          permission: 'report:template-manage'
        }
      },
      {
        path: 'report/dynamic',
        name: 'ReportDynamic',
        component: () => import('@/views/report/dynamic.vue'),
        meta: { title: '动态报表', hidden: true, permission: 'report:view' }
      },

      // ========== 通讯录 ==========
      {
        path: 'contacts',
        name: 'ContactManage',
        component: () => import('@/views/contact/index.vue'),
        meta: { title: '通讯录', icon: 'Phone', permission: ['system:user-manage'] }
      },

      // ========== 文档管理 ==========
      {
        path: 'documents',
        name: 'DocumentManage',
        component: () => import('@/views/document/index.vue'),
        meta: {
          title: '文档管理',
          icon: 'FolderOpened',
          permission: ['completion:doc-manage']
        }
      },

      // ========== 系统管理 ==========
      {
        path: 'system',
        name: 'SystemManage',
        component: () => import('@/layouts/PassThrough.vue'),
        redirect: '/system/users',
        meta: {
          title: '系统管理',
          icon: 'Setting',
          permission: [
            'system:user-manage', 'system:role-manage', 'system:dept-manage',
            'system:announcement-manage', 'system:audit-log',
            'system:config', 'system:config:edit',
            'system:delegation', 'system:delegation:edit',
            'system:tpl-manage', 'system:dict-manage'
          ]
        },
        children: [
          {
            path: 'users',
            name: 'UserManage',
            component: () => import('@/views/system/user/index.vue'),
            meta: { title: '用户管理', icon: 'User', permission: 'system:user-manage' }
          },
          {
            path: 'roles',
            name: 'RoleManage',
            component: () => import('@/views/system/role/index.vue'),
            meta: { title: '角色管理', icon: 'Key', permission: 'system:role-manage' }
          },
          {
            path: 'depts',
            name: 'DeptManage',
            component: () => import('@/views/system/dept/index.vue'),
            meta: { title: '部门管理', icon: 'OfficeBuilding', permission: 'system:dept-manage' }
          },
          {
            path: 'announcements',
            name: 'AnnouncementManage',
            component: () => import('@/views/system/announcement/index.vue'),
            meta: { title: '公告管理', icon: 'Bell', permission: 'system:announcement-manage' }
          },
          {
            path: 'audit-logs',
            name: 'AuditLogManage',
            component: () => import('@/views/system/audit-log/index.vue'),
            meta: { title: '审计日志', icon: 'Document', permission: 'system:audit-log' }
          },
          {
            path: 'configs',
            name: 'ConfigManage',
            component: () => import('@/views/system/config/index.vue'),
            meta: { title: '系统配置', icon: 'Tools', permission: ['system:config', 'system:config:edit'] }
          },
          {
            path: 'delegations',
            name: 'DelegationManage',
            component: () => import('@/views/system/delegation/index.vue'),
            meta: { title: '委托代理', icon: 'Switch', permission: ['system:delegation', 'system:delegation:edit'] }
          },
          {
            path: 'contract-tpl',
            name: 'ContractTplManage',
            component: () => import('@/views/system/contract-tpl/index.vue'),
            meta: { title: '合同模板', icon: 'Tickets', permission: 'system:tpl-manage' }
          },
          {
            path: 'dict',
            name: 'DictManage',
            component: () => import('@/views/system/dict/index.vue'),
            meta: { title: '字典管理', icon: 'Collection', permission: 'system:dict-manage' }
          },
          {
            path: 'dict/:dictType/data',
            name: 'DictDataManage',
            component: () => import('@/views/system/dict/data.vue'),
            meta: { title: '字典数据', hidden: true, permission: 'system:dict-manage' }
          }
        ]
      },

      // ========== 基础设施 ==========
      {
        path: 'infra',
        name: 'InfraManage',
        component: () => import('@/layouts/PassThrough.vue'),
        redirect: '/infra/codegen',
        meta: {
          title: '基础设施',
          icon: 'Cpu',
          permission: ['infra:codegen']
        },
        children: [
          {
            path: 'codegen',
            name: 'CodegenManage',
            component: () => import('@/views/infra/codegen/index.vue'),
            meta: { title: '代码生成', icon: 'MagicStick', permission: 'infra:codegen' }
          },
          {
            path: 'codegen/:id/edit',
            name: 'CodegenEdit',
            component: () => import('@/views/infra/codegen/edit.vue'),
            meta: { title: '编辑生成配置', hidden: true, permission: 'infra:codegen' }
          }
        ]
      },

      // ========== 流程管理 ==========
      {
        path: 'bpm',
        name: 'BpmManage',
        component: () => import('@/layouts/PassThrough.vue'),
        redirect: '/bpm/task/todo',
        meta: {
          title: '流程管理',
          icon: 'Stamp',
          permission: ['bpm:process-manage', 'bpm:task-operate', 'bpm:instance-view', 'bpm:rule-manage']
        },
        children: [
          {
            path: 'task/todo',
            name: 'BpmTodoTask',
            component: () => import('@/views/bpm/task/todo.vue'),
            meta: { title: '我的待办', icon: 'Bell', permission: 'bpm:task-operate' }
          },
          {
            path: 'process-def',
            name: 'BpmProcessDef',
            component: () => import('@/views/bpm/process-def/index.vue'),
            meta: { title: '流程定义', icon: 'Share', permission: 'bpm:process-manage' }
          },
          {
            path: 'instance',
            name: 'BpmInstance',
            component: () => import('@/views/bpm/instance/index.vue'),
            meta: { title: '流程监控', icon: 'DataAnalysis', permission: 'bpm:instance-view' }
          }
        ]
      }
    ]
  }
]
