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
            },
            {
              path: 'settings',
              name: 'CpgDetail.Settings',
              component: () => import(/* webpackChunkName: "cp-groups" */ './Settings.vue'),
              beforeEnter: (to, from, next) => {
                const {global: {appProps: {plugins}}} = window.osUi;
                if (plugins instanceof Array && plugins.indexOf('sc') >= 0) {
                  next();
                } else {
                  alert('Specimen Catalog plugin is not installed. Navigating to the Overview page');
                  next({name: 'CpgDetail.Overview', params: {cpgId: to.params.cpgId}});
                }
              }
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
