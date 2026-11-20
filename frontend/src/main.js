import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './assets/styles/tokens.css' // variables de diseño (primero)
import './assets/styles/base.css'   // reset y elementos globales

createApp(App).use(router).mount('#app')
