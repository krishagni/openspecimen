export default {
  install(app, {router}) {
    router.addRoute('App', {
      path: 'cp-groups/:cpgId',
      name: 'CpgsList',
      component: () => import(/* webpackChunkName: "cp-groups" */ './List.vue'),
      props: (route) => ({cpgId: route.params && route.params.cpgId, filters: route.query.filters}),
      children: [
        {
          path: '',
          name: 'CpgDetail',
          component: () => import(/* webpackChunkName: "cp-groups" */ './Detail.vue'),
          props: (route) => ({filters: route.query.filters}),
          children: [
            {
              path: 'overview',
              name: 'CpgDetail.Overview',
              component: () => import(/* webpackChunkName: "cp-groups" */ './Overview.vue'),
            },
            {
              path: 'forms',
              name: 'CpgDetail.Forms',
              component: () => import(/* webpackChunkName: "cp-groups" */ './Forms.vue'),
            }
          ]
        }
      ]
    });

    router.addRoute('App', {
      path: 'cp-groups/:cpgId/addedit',
      name: 'CpgAddEdit',
      component: () => import(/* webpackChunkName: "cp-groups" */ './AddEdit.vue'),
      props: (route) => ({cpgId: route.params.cpgId})
    });
  }
}
