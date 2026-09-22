import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'                 // 组件库本体
import 'element-plus/dist/index.css'                   // 组件样式，不引则组件无样式
import { zhCn } from 'element-plus/es/locales.mjs'     // 中文语言包

import App from './App.vue'
import router from './router'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })  // 注册组件库，内置组件（分页、日期选择等）显示中文

app.mount('#app')
