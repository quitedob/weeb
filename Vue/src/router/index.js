// /router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import Login from '@/auth/Login.vue' // 确认路径
import Register from '@/auth/Register.vue'
import video from '@/video/Video.vue'
import value from '@/value/value.vue'
import logout from '@/auth/logout.vue'
import usermain from '@/auth/usermain.vue'
import Articlewrite from '@/article/ArticleWrite.vue'
import Articlemanage from "@/article/ArticleManage.vue"
import Articleedit from "@/article/ArticleEdit.vue"
import ArticleRead from "@/article/ArticleRead.vue";
import ArticleMain from "@/article/ArticleMain.vue"
import HelpCenter from "@/auth/HelpCenter.vue";
import ChatPage from "@/Chat/ChatPage.vue";
import UserInform from "@/auth/UserInform.vue";
import UserValue from "@/value/usevalue.vue";

// New component imports
import ContactPage from '@/contact/ContactPage.vue';
import GroupPage from '@/group/GroupPage.vue';
import SearchPage from '@/search/SearchPage.vue';

const routes = [
    { path: '/login', component: Login },
    { path: '/register', component: Register },
    { path: '/video', component: video },
    { path: '/value', component: value},
    { path: '/logout', component: logout},
    { path: '/usermain', component: usermain },
    { path: '/articlewrite', component: Articlewrite},
    { path: '/articlemanage', component: Articlemanage},
    { path: '/articleedit', component: Articleedit},
    { path: '/articleread',component: ArticleRead},
    { path: '/articlemain', component: ArticleMain},
    { path: '/helpcenter' , component: HelpCenter},
    { path: '/chatpage' , component: ChatPage},
    { path: '/userinform', component: UserInform},
    { path: '/uservalue', component: UserValue},

    // New routes
    { path: '/contacts', component: ContactPage },
    { path: '/groups', component: GroupPage },
    { path: '/search', component: SearchPage },
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router
