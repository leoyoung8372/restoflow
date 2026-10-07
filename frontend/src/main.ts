import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'                 // 组件库本体
import 'element-plus/dist/index.css'                   // 组件样式，不引则组件无样式
import { zhCn } from 'element-plus/es/locales.mjs'     // 中文语言包
import '@/assets/iconfont/iconfont.css'                // 图标字体，供 <i class="iconfont icon-xxx"> 使用
import '@/styles/global.css'                           // 全局样式：全屏布局，禁止页面滚动

import App from './App.vue'
import router from './router'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })  // 注册组件库，内置组件（分页、日期选择等）显示中文

app.mount('#app')
