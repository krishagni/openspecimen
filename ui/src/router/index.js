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
        path: 'users/:userId',
        name: 'UsersList',
        // route level code-splitting
        // this generates a separate chunk (about.[hash].js) for this route
        // which is lazy-loaded when the route is visited.
        component: () => import(/* webpackChunkName: "users" */ '../administrative/users/List.vue'),
        props: (route) => (
          {
            userId: route.params && route.params.userId,
            filters: route.query.filters,
            groupId: route.query.groupId
          }
        ),
        children: [
          {
            path: '',
            name: 'UsersListItemDetail',
            component: () => import(/* webpackChunkName: "users" */ '../administrative/users/Detail.vue'),
            props: (route) => ({userId: route.params && route.params.userId}),
            children: [
              {
                path: 'overview',
                name: 'UsersListItemDetail.Overview',
                component: () => import(/* webpackChunkName: "users" */ '../administrative/users/Overview.vue')
              },
              {
                path: 'roles',
                name: 'UsersListItemDetail.Roles',
                component: () => import(/* webpackChunkName: "users" */ '../administrative/users/Roles.vue')
              },
              {
                path: 'forms',
                name: 'UsersListItemDetail.Forms',
                component: () => import(/* webpackChunkName: "users" */ '../administrative/users/Forms.vue'),
                props: () => ({
                  entityType: 'User',
                  listView: 'UsersListItemDetail.Forms.List',
                  addEditView: 'UsersListItemDetail.Forms.AddEdit'
                }),
                children: [
                  {
                    path: 'list',
                    name: 'UsersListItemDetail.Forms.List',
                    component: () => import(/* webpackChunkName: "users" */ '../administrative/users/FormsList.vue'),
                    props: (route) => ({
                      formId: route.query.formId,
                      formCtxtId: route.query.formCtxtId,
                      recordId: route.query.recordId
                    })
                  },
                  {
                    path: 'addedit',
                    name: 'UsersListItemDetail.Forms.AddEdit',
                    component: () => import(/* webpackChunkName: "users" */ '../administrative/users/AddEditFormRecord.vue'),
                    props: (route) => ({
                      formId: route.query.formId,
                      formCtxtId: route.query.formCtxtId,
                      recordId: route.query.recordId
                    })
                  }
                ]
              },
              {
                path: 'profile-forms',
                name: 'UsersListItemDetail.ProfileForms',
                component: () => import(/* webpackChunkName: "users" */ '../administrative/users/Forms.vue'),
                props: () => ({
                  entityType: 'UserProfile',
                  listView: 'UsersListItemDetail.ProfileForms.List',
                  addEditView: 'UsersListItemDetail.ProfileForms.AddEdit'
                }),
                children: [
                  {
                    path: 'list',
                    name: 'UsersListItemDetail.ProfileForms.List',
                    component: () => import(/* webpackChunkName: "users" */ '../administrative/users/FormsList.vue'),
                    props: (route) => ({
                      formId: route.query.formId,
                      formCtxtId: route.query.formCtxtId,
                      recordId: route.query.recordId
                    })
                  },
                  {
                    path: 'addedit',
                    name: 'UsersListItemDetail.ProfileForms.AddEdit',
                    component: () => import(/* webpackChunkName: "users" */ '../administrative/users/AddEditFormRecord.vue'),
                    props: (route) => ({
                      formId: route.query.formId,
                      formCtxtId: route.query.formCtxtId,
                      recordId: route.query.recordId,
                    })
                  }
                ]
              }
            ]
          }
        ]
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
        path: 'users/:userId/detail',
        name: 'UserDetail',
        component: () => import(/* webpackChunkName: "users" */ '../administrative/users/Detail.vue'),
        props: (route) => ({userId: route.params && route.params.userId}),
        children: [
          {
            path: 'overview',
            name: 'UserDetail.Overview',
            component: () => import(/* webpackChunkName: "users" */ '../administrative/users/Overview.vue')
          },
          {
            path: 'roles',
            name: 'UserDetail.Roles',
            component: () => import(/* webpackChunkName: "users" */ '../administrative/users/Roles.vue')
          },
          {
            path: 'forms',
            name: 'UserDetail.Forms',
            component: () => import(/* webpackChunkName: "users" */ '../administrative/users/Forms.vue'),
            props: () => ({
              entityType: 'User',
              listView: 'UserDetail.Forms.List',
              addEditView: 'UserDetail.Forms.AddEdit'
            }),
            children: [
              {
                path: 'list',
                name: 'UserDetail.Forms.List',
                component: () => import(/* webpackChunkName: "users" */ '../administrative/users/FormsList.vue'),
                props: (route) => ({
                  formId: route.query.formId,
                  formCtxtId: route.query.formCtxtId,
                  recordId: route.query.recordId
                })
              },
              {
                path: 'addedit',
                name: 'UserDetail.Forms.AddEdit',
                component: () => import(/* webpackChunkName: "users" */ '../administrative/users/AddEditFormRecord.vue'),
                props: (route) => ({
                  formId: route.query.formId,
                  formCtxtId: route.query.formCtxtId,
                  recordId: route.query.recordId
                })
              }
            ]
          },
          {
            path: 'profile-forms',
            name: 'UserDetail.ProfileForms',
            component: () => import(/* webpackChunkName: "users" */ '../administrative/users/Forms.vue'),
            props: () => ({
              entityType: 'UserProfile',
              listView: 'UserDetail.ProfileForms.List',
              addEditView: 'UserDetail.ProfileForms.AddEdit'
            }),
            children: [
              {
                path: 'list',
                name: 'UserDetail.ProfileForms.List',
                component: () => import(/* webpackChunkName: "users" */ '../administrative/users/FormsList.vue'),
                props: (route) => ({
                  formId: route.query.formId,
                  formCtxtId: route.query.formCtxtId,
                  recordId: route.query.recordId
                })
              },
              {
                path: 'addedit',
                name: 'UserDetail.ProfileForms.AddEdit',
                component: () => import(/* webpackChunkName: "users" */ '../administrative/users/AddEditFormRecord.vue'),
                props: (route) => ({
                  formId: route.query.formId,
                  formCtxtId: route.query.formCtxtId,
                  recordId: route.query.recordId,
                })
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

      /*****************************
       *****************************
       * Sites module              *
       *****************************
       *****************************/
      {
        path: 'sites/:siteId',
        name: 'SitesList',
        component: () => import(/* webpackChunkName: "sites" */ '../administrative/sites/List.vue'),
        props: (route) => ({siteId: route.params && route.params.siteId, filters: route.query.filters}),
        children: [
          {
            path: '',
            name: 'SitesListItemDetail',
            component: () => import(/* webpackChunkName: "sites" */ '../administrative/sites/Detail.vue'),
            props: (route) => ({siteId: route.params && route.params.siteId, noNavButton: true}),
            children: [
              {
                path: 'overview',
                name: 'SitesListItemDetail.Overview',
                component: () => import(/* webpackChunkName: "sites" */ '../administrative/sites/Overview.vue')
              }
            ]
          }
        ]
      },
      {
        path: 'site-addedit/:siteId',
        name: 'SiteAddEdit',
        component: () => import(/* webpackChunkName: "sites" */ '../administrative/sites/AddEdit.vue'),
        props: (route) => ({siteId: route.params && route.params.siteId})
      },
      {
        path: 'sites/:siteId/detail',
        name: 'SiteDetail',
        component: () => import(/* webpackChunkName: "sites" */ '../administrative/sites/Detail.vue'),
        props: (route) => ({siteId: route.params && route.params.siteId}),
        children: [
          {
            path: 'overview',
            name: 'SiteDetail.Overview',
            component: () => import(/* webpackChunkName: "sites" */ '../administrative/sites/Overview.vue')
          }
        ]
      },

      /*****************************
       *****************************
       * Institutes module         *
       *****************************
       *****************************/
      {
        path: 'institutes/:instituteId',
        name: 'InstitutesList',
        component: () => import(/* webpackChunkName: "institutes" */ '../administrative/institutes/List.vue'),
        props: (route) => ({filters: route.query.filters, instituteId: route.params && route.params.instituteId}),
        children: [
          {
            path: '',
            name: 'InstitutesListItemDetail',
            component: () => import(/* webpackChunkName: "institutes" */ '../administrative/institutes/Detail.vue'),
            props: (route) => ({instituteId: route.params && route.params.instituteId, noNavButton: true}),
            children: [
              {
                path: 'overview',
                name: 'InstitutesListItemDetail.Overview',
                component: () => import(/* webpackChunkName: "institutes" */ '../administrative/institutes/Overview.vue')
              }
            ]
          }
        ]
      },
      {
        path: 'institute-addedit/:instituteId',
        name: 'InstituteAddEdit',
        component: () => import(/* webpackChunkName: "institutes" */ '../administrative/institutes/AddEdit.vue'),
        props: (route) => ({instituteId: route.params && route.params.instituteId})
      },
      {
        path: 'institutes/:instituteId/detail',
        name: 'InstituteDetail',
        component: () => import(/* webpackChunkName: "institutes" */ '../administrative/institutes/Detail.vue'),
        props: (route) => ({instituteId: route.params && route.params.instituteId}),
        children: [
          {
            path: 'overview',
            name: 'InstituteDetail.Overview',
            component: () => import(/* webpackChunkName: "institutes" */ '../administrative/institutes/Overview.vue')
          }
        ]
      },

      /*****************************
       *****************************
       * Shipments module          *
       *****************************
       *****************************/
      {
        path: 'shipments/:shipmentId',
        name: 'ShipmentsList',
        component: () => import(/* webpackChunkName: "shipments" */ '../administrative/shipments/List.vue'),
        props: (route) => ({filters: route.query.filters, shipmentId: route.params && route.params.shipmentId}),
        children: [
          {
            path: '',
            name: 'ShipmentsListItemDetail',
            component: () => import(/* webpackChunkName: "shipments" */ '../administrative/shipments/Detail.vue'),
            props: (route) => ({shipmentId: route.params && route.params.shipmentId, noNavButton: true}),
            children: [
              {
                path: 'overview',
                name: 'ShipmentsListItemDetail.Overview',
                component: () => import(/* webpackChunkName: "shipments" */ '../administrative/shipments/Overview.vue')
              },
              {
                path: 'specimens',
                name: 'ShipmentsListItemDetail.Specimens',
                component: () => import(/* webpackChunkName: "shipments" */ '../administrative/shipments/Specimens.vue')
              },
              {
                path: 'containers',
                name: 'ShipmentsListItemDetail.Containers',
                component: () => import(/* webpackChunkName: "shipments" */ '../administrative/shipments/Containers.vue')
              }
            ]
          }
        ]
      },
      {
        path: 'shipments/:shipmentId/detail',
        name: 'ShipmentDetail',
        component: () => import(/* webpackChunkName: "shipments" */ '../administrative/shipments/Detail.vue'),
        props: (route) => ({shipmentId: route.params && route.params.shipmentId}),
        children: [
          {
            path: 'overview',
            name: 'ShipmentDetail.Overview',
            component: () => import(/* webpackChunkName: "shipments" */ '../administrative/shipments/Overview.vue')
          },
          {
            path: 'specimens',
            name: 'ShipmentDetail.Specimens',
            component: () => import(/* webpackChunkName: "shipments" */ '../administrative/shipments/Specimens.vue')
          },
          {
            path: 'containers',
            name: 'ShipmentDetail.Containers',
            component: () => import(/* webpackChunkName: "shipments" */ '../administrative/shipments/Containers.vue')
          }
        ]
      },
      {
        path: 'shipment-addedit/:shipmentId',
        name: 'ShipmentAddEdit',
        component: () => import(/* webpackChunkName: "shipments" */ '../administrative/shipments/AddEdit.vue'),
        props: (route) => (
          {
            shipmentId: route.params && route.params.shipmentId,
            shipmentType: route.query && route.query.shipmentType
          }
        )
      },
      {
        path: 'shipment-receive/:shipmentId',
        name: 'ShipmentReceive',
        component: () => import(/* webpackChunkName: "shipments" */ '../administrative/shipments/AddEdit.vue'),
        props: (route) => (
          {
            shipmentId: route.params && route.params.shipmentId,
            shipmentType: route.query && route.query.shipmentType,
            receive: true
          }
        )
      },

      /*****************************
       *****************************
       * Orders module              *
       *****************************
       *****************************/
      {
        path: 'orders/:orderId',
        name: 'OrdersList',
        component: () => import(/* webpackChunkName: "orders" */ '../administrative/orders/List.vue'),
        props: (route) => ({orderId: route.params && route.params.orderId, filters: route.query.filters}),
        children: [
          {
            path: '',
            name: 'OrdersListItemDetail',
            component: () => import(/* webpackChunkName: "orders" */ '../administrative/orders/Detail.vue'),
            props: (route) => ({orderId: route.params && route.params.orderId, listItemDetailView: true}),
            children: [
              {
                path: 'overview',
                name: 'OrdersListItemDetail.Overview',
                component: () => import(/* webpackChunkName: "orders" */ '../administrative/orders/Overview.vue')
              },
              {
                path: 'specimens',
                name: 'OrdersListItemDetail.Specimens',
                component: () => import(/* webpackChunkName: "orders" */ '../administrative/orders/Specimens.vue')
              }
            ]
          }
        ]
      },
      {
        path: 'orders/:orderId/detail',
        name: 'OrderDetail',
        component: () => import(/* webpackChunkName: "orders" */ '../administrative/orders/Detail.vue'),
        props: (route) => ({orderId: route.params && route.params.orderId, listItemDetailView: false}),
        children: [
          {
            path: 'overview',
            name: 'OrderDetail.Overview',
            component: () => import(/* webpackChunkName: "orders" */ '../administrative/orders/Overview.vue')
          },
          {
            path: 'specimens',
            name: 'OrderDetail.Specimens',
            component: () => import(/* webpackChunkName: "orders" */ '../administrative/orders/Specimens.vue')
          }
        ]
      },
      {
        path: 'order-addedit/:orderId',
        name: 'OrderAddEdit',
        component: () => import(/* webpackChunkName: "orders" */ '../administrative/orders/AddEdit.vue'),
        props: (route) => ({orderId: route.params && route.params.orderId})
      },
      {
        path: 'order-return-specimens',
        name: 'OrderReturnSpecimens',
        component: () => import(/* webpackChunkName: "orders" */ '../administrative/orders/ReturnSpecimens.vue'),
      },

      /*****************************
       *****************************
       * DPs module                *
       *****************************
       *****************************/
      {
        path: 'dps/:dpId',
        name: 'DpsList',
        component: () => import(/* webpackChunkName: "dps" */ '../administrative/dps/List.vue'),
        props: (route) => ({filters: route.query.filters, dpId: route.params && route.params.dpId}),
        children: [
          {
            path: '',
            name: 'DpsListItemDetail',
            component: () => import(/* webpackChunkName: "dps" */ '../administrative/dps/Detail.vue'),
            props: (route) => ({dpId: route.params && route.params.dpId, noNavButton: true}),
            children: [
              {
                path: 'overview',
                name: 'DpsListItemDetail.Overview',
                component: () => import(/* webpackChunkName: "dps" */ '../administrative/dps/Overview.vue')
              },
              {
                path: 'consents',
                name: 'DpsListItemDetail.Consents',
                component: () => import(/* webpackChunkName: "dps" */ '../administrative/dps/Consents.vue')
              },
              {
                path: 'reserved-specimens',
                name: 'DpsListItemDetail.ReservedSpecimens',
                component: () => import(/* webpackChunkName: "dps" */ '../administrative/dps/ReservedSpecimens.vue')
              },
              {
                path: 'requirements',
                name: 'DpsListItemDetail.Requirements',
                component: () => import(/* webpackChunkName: "dps" */ '../administrative/dps/Requirements.vue'),
                children: [
                  {
                    path: 'list',
                    name: 'DpsListItemDetail.Requirements.List',
                    component: () => import(/* webpackChunkName: "dps" */ '../administrative/dps/RequirementsList.vue')
                  },
                  {
                    path: 'addedit/:reqId',
                    name: 'DpsListItemDetail.Requirements.AddEdit',
                    component: () => import(/* webpackChunkName: "dps" */ '../administrative/dps/RequirementsAddEdit.vue'),
                    props: (route) => ({reqId: route.params && route.params.reqId}),
                  }
                ]
              }
            ]
          }
        ]
      },
      {
        path: 'dps/:dpId/detail',
        name: 'DpDetail',
        component: () => import(/* webpackChunkName: "dps" */ '../administrative/dps/Detail.vue'),
        props: (route) => ({dpId: route.params && route.params.dpId}),
        children: [
          {
            path: 'overview',
            name: 'DpDetail.Overview',
            component: () => import(/* webpackChunkName: "dps" */ '../administrative/dps/Overview.vue')
          },
          {
            path: 'consents',
            name: 'DpDetail.Consents',
            component: () => import(/* webpackChunkName: "dps" */ '../administrative/dps/Consents.vue')
          },
          {
            path: 'reserved-specimens',
            name: 'DpDetail.ReservedSpecimens',
            component: () => import(/* webpackChunkName: "dps" */ '../administrative/dps/ReservedSpecimens.vue')
          },
          {
            path: 'requirements',
            name: 'DpDetail.Requirements',
            component: () => import(/* webpackChunkName: "dps" */ '../administrative/dps/Requirements.vue'),
            children: [
              {
                path: 'list',
                name: 'DpDetail.Requirements.List',
                component: () => import(/* webpackChunkName: "dps" */ '../administrative/dps/RequirementsList.vue')
              },
              {
                path: 'addedit/:reqId',
                name: 'DpDetail.Requirements.AddEdit',
                component: () => import(/* webpackChunkName: "dps" */ '../administrative/dps/RequirementsAddEdit.vue'),
                props: (route) => ({reqId: route.params && route.params.reqId}),
              }
            ]
          }
        ]
      },
      {
        path: 'dp-addedit/:dpId',
        name: 'DpAddEdit',
        component: () => import(/* webpackChunkName: "dps" */ '../administrative/dps/AddEdit.vue'),
        props: (route) => ({dpId: route.params && route.params.dpId})
      },
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
