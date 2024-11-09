export default {
  install(app, {router}) {
    router.addRoute('App', {
      path: 'queries',
      name: 'QueriesRoot',
      component: () => import(/* webpackChunkName: "queries" */ './views/Root.vue'),
      children: [
        {
          path: 'list',
          name: 'QueriesList',
          component: () => import(/* webpackChunkName: "queries" */ './views/List.vue'),
          props: (route) => ({filters: route.query.filters, folderId: route.query.folderId})
        },

        {
          path: 'audit-logs',
          name: 'QueryAuditLogs',
          component: () => import(/* webpackChunkName: "queries" */ './views/AuditLogsList.vue'),
          props: (route) => ({filters: route.query.filters})
        },

        {
          path: ':queryId',
          name: 'QueryDetail',
          component: () => import(/* webpackChunkName: "queries" */ './views/Detail.vue'),
          props: (route) => ({queryId: route.params.queryId}),
          children: [
            {
              path: 'addedit',
              name: 'QueryDetail.AddEdit',
              component: () => import(/* webpackChunkName: "queries" */ './views/AddEdit.vue')
            },
            {
              path: 'results',
              name: 'QueryDetail.Results',
              component: () => import(/* webpackChunkName: "queries" */ './views/Results.vue')
            }
          ]
        }
      ]
    });
  }
}
