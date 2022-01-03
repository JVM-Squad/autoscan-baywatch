import store from './store'
import router from "./router";
import App from './App.vue';
import '@/assets/styles/index.css';
import {createApp} from "vue";
import {plugin as alertDialogPlugin} from "@/components/shared/alertdialog/plugin";

createApp(App)
    .use(alertDialogPlugin)
    .use(router)
    .use(store)
    .mount('#app');