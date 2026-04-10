import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import { permissionDirective } from './directives/permission'
import './styles/global.scss'

// 全局业务组件
import StatusTag from './components/StatusTag.vue'
import MoneyInput from './components/MoneyInput.vue'
import QuantityInput from './components/QuantityInput.vue'
import MoneyText from './components/MoneyText.vue'
import UserPicker from './components/UserPicker.vue'
import DeptPicker from './components/DeptPicker.vue'
import ProjectSelect from './components/ProjectSelect.vue'
import ContractSelect from './components/ContractSelect.vue'
import SupplierSelect from './components/SupplierSelect.vue'
import MaterialSelect from './components/MaterialSelect.vue'
import FileUpload from './components/FileUpload.vue'
import ApprovalFlow from './components/ApprovalFlow.vue'
import ApprovalTimeline from './components/ApprovalTimeline.vue'
import NumberingDisplay from './components/NumberingDisplay.vue'
import SearchForm from './components/SearchForm.vue'
import PageHeader from './components/PageHeader.vue'
import DictTag from './components/DictTag.vue'

const app = createApp(App)

// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 注册全局业务组件
const globalComponents = {
  StatusTag, MoneyInput, QuantityInput, MoneyText,
  UserPicker, DeptPicker, ProjectSelect, ContractSelect,
  SupplierSelect, MaterialSelect, FileUpload, ApprovalFlow,
  ApprovalTimeline, NumberingDisplay, SearchForm, PageHeader,
  DictTag
}
Object.entries(globalComponents).forEach(([name, comp]) => {
  app.component(name, comp)
})

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.directive('permission', permissionDirective)
app.mount('#app')
