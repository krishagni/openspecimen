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
            }
          ]
        }
      ]
    });
  }
}
