import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/users',
    name: 'UsersList',
    // route level code-splitting
    // this generates a separate chunk (about.[hash].js) for this route
    // which is lazy-loaded when the route is visited.
    component: () => import(/* webpackChunkName: "users" */ '../administrative/users/List.vue'),
    props: (route) => ({filters: route.query.filters, groupId: route.query.groupId})
  },
  {
    path: '/user-addedit/:userId',
    name: 'UserAddEdit',
    // route level code-splitting
    // this generates a separate chunk (about.[hash].js) for this route
    // which is lazy-loaded when the route is visited.
    component: () => import(/* webpackChunkName: "users" */ '../administrative/users/AddEdit.vue'),
    props: (route) => ({userId: route.params && route.params.userId})
  },
  {
    path: '/user-edit-profile/:userId',
    name: 'UserEditProfile',
    component: () => import(/* webpackChunkName: "users" */ '../administrative/users/AddEdit.vue'),
    props: (route) => ({userId: route.params && route.params.userId, editProfile: true})
  },
  {
    path: '/user-password-change/:userId',
    name: 'UserChangePassword',
    component: () => import(/* webpackChunkName: "users" */ '../administrative/users/ChangePassword.vue'),
    props: (route) => ({userId: route.params && route.params.userId})
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
