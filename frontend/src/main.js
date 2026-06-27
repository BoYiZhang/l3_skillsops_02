import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import './utils/echarts'
import VChart from 'vue-echarts'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.component('VChart', VChart)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.mount('#app')
