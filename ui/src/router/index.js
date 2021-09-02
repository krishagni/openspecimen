import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'App',
    component: () => import("../App.vue"),
    props: (route) => {
      return {token: route.query.token};
    },
    children: [
      {
        path: 'users',
        name: 'UsersList',
        // route level code-splitting
        // this generates a separate chunk (about.[hash].js) for this route
        // which is lazy-loaded when the route is visited.
        component: () => import(/* webpackChunkName: "users" */ '../administrative/users/List.vue'),
        props: (route) => ({filters: route.query.filters, groupId: route.query.groupId})
      },
      {
        path: 'user-addedit/:userId',
        name: 'UserAddEdit',
        // route level code-splitting
        // this generates a separate chunk (about.[hash].js) for this route
        // which is lazy-loaded when the route is visited.
        component: () => import(/* webpackChunkName: "users" */ '../administrative/users/AddEdit.vue'),
        props: (route) => ({userId: route.params && route.params.userId})
      },
      {
        path: 'user-bulkedit',
        name: 'UserBulkEdit',
        // route level code-splitting
        // this generates a separate chunk (about.[hash].js) for this route
        // which is lazy-loaded when the route is visited.
        component: () => import(/* webpackChunkName: "users" */ '../administrative/users/AddEdit.vue')
      },
      {
        path: 'user-edit-profile/:userId',
        name: 'UserEditProfile',
        component: () => import(/* webpackChunkName: "users" */ '../administrative/users/AddEdit.vue'),
        props: (route) => ({userId: route.params && route.params.userId, editProfile: true})
      },
      {
        path: 'user-password-change/:userId',
        name: 'UserChangePassword',
        component: () => import(/* webpackChunkName: "users" */ '../administrative/users/ChangePassword.vue'),
        props: (route) => ({userId: route.params && route.params.userId})
      },
      {
        path: 'users/:userId',
        name: 'UserDetail',
        component: () => import(/* webpackChunkName: "users" */ '../administrative/users/Detail.vue'),
        props: (route) => ({userId: route.params && route.params.userId}),
        children: [
          {
            path: 'overview',
            name: 'UserOverview',
            component: () => import(/* webpackChunkName: "users" */ '../administrative/users/Overview.vue')
          },
          {
            path: 'roles',
            name: 'UserRoles',
            component: () => import(/* webpackChunkName: "users" */ '../administrative/users/Roles.vue')
          },
          {
            path: 'forms',
            name: 'UserForms',
            component: () => import(/* webpackChunkName: "users" */ '../administrative/users/Forms.vue'),
            children: [
              {
                path: 'list',
                name: 'UserFormsList',
                component: () => import(/* webpackChunkName: "users" */ '../administrative/users/FormsList.vue'),
                props: (route) => {
                  return {
                    formId: route.query.formId,
                    formCtxtId: route.query.formCtxtId,
                    recordId: route.query.recordId
                  };
                }
              },
              {
                path: 'addedit',
                name: 'UserFormAddEdit',
                component: () => import(/* webpackChunkName: "users" */ '../administrative/users/AddEditFormRecord.vue'),
                props: (route) => {
                  return {
                    formId: route.query.formId,
                    formCtxtId: route.query.formCtxtId,
                    recordId: route.query.recordId
                  };
                }
              }
            ]
          }
        ]
      },
      {
        path: 'user-groups',
        name: 'UserGroupsList',
        // route level code-splitting
        // this generates a separate chunk (about.[hash].js) for this route
        // which is lazy-loaded when the route is visited.
        component: () => import(/* webpackChunkName: "user-groups" */ '../administrative/user-groups/List.vue'),
        props: (route) => ({filters: route.query.filters})
      },
      {
        path: 'user-group-addedit/:groupId',
        name: 'UserGroupAddEdit',
        // route level code-splitting
        // this generates a separate chunk (about.[hash].js) for this route
        // which is lazy-loaded when the route is visited.
        component: () => import(/* webpackChunkName: "user-groups" */ '../administrative/user-groups/AddEdit.vue'),
        props: (route) => ({groupId: route.params && route.params.groupId})
      },
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
