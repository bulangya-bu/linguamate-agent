import { createApp } from "vue";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import App from "./App.vue";
import router from "./router";
import "./styles/app.css";
import "./styles/vue-overrides.css";
import "./styles/learning.css";
import "./styles/surfaces.css";
import "./styles/brand.css";
import "./styles/responsive.css";
import "./styles/layout.css";
import "./styles/typography.css";
import "./styles/quiz-workspace.css";

createApp(App).use(router).use(ElementPlus).mount("#app");
