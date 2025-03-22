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
        path: 'resolve-view',
        name: 'ResolveView',
        component: () => import(/* webpackChunkName: "common" */ '../common/components/ResolveView.vue'),
        props: (route) => ({params: route.query})
      },

      {
        path: 'object-state-params-resolver',
        name: 'ResolveObjectStateParams',
        component: () => import(/* webpackChunkName: "common" */ '../common/components/ObjectStateParamsResolver.vue'),
        props: (route) => ({params: route.query})
      },

      {
        path: 'download-file',
        name: 'DownloadFileView',
        component: () => import(/* webpackChunkName: "common" */ '../common/components/DownloadFileView.vue'),
        props: (route) => ({downloadUrl: route.query.downloadUrl})
      },

      {
        path: 'home',
        name: 'HomePage',
        component: () => import(/* webpackChunkName: "common" */ '../home/views/Home.vue'),
      },

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
       * User roles module         *
       *****************************
       *****************************/
      {
        path: 'user-roles/:roleId',
        name: 'UserRolesList',
        component: () => import(/* webpackChunkName: "user-roles" */ '../administrative/roles/List.vue'),
        props: (route) => ({roleId: route.params && route.params.roleId, filters: route.query.filters}),
        children: [
          {
            path: '',
            name: 'UserRolesListItemDetail',
            component: () => import(/* webpackChunkName: "user-roles" */ '../administrative/roles/Detail.vue'),
            props: (route) => ({roleId: route.params && route.params.roleId, noNavButton: true}),
            children: [
              {
                path: 'overview',
                name: 'UserRolesListItemDetail.Overview',
                component: () => import(/* webpackChunkName: "user-roles" */ '../administrative/roles/Overview.vue')
              }
            ]
          }
        ]
      },

      {
        path: 'user-role-addedit/:roleId',
        name: 'UserRoleAddEdit',
        component: () => import(/* webpackChunkName: "user-roles" */ '../administrative/roles/AddEdit.vue'),
        props: (route) => ({roleId: route.params && route.params.roleId})
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
            shipmentType: route.query && route.query.shipmentType,
            action: route.query && route.query.action
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

      /*****************************
       *****************************
       * Containers module         *
       *****************************
       *****************************/
      {
        path: 'containers',
        name: 'ContainersList',
        component: () => import(/* webpackChunkName: "containers" */ '../administrative/containers/List.vue'),
        props: (route) => ({filters: route.query.filters, showStats: route.query.showStats}),
      },
      {
        path: 'containers/:containerId/detail',
        name: 'ContainerDetail',
        component: () => import(/* webpackChunkName: "containers" */ '../administrative/containers/Detail.vue'),
        props: (route) => ({containerId: route.params && route.params.containerId}),
        children: [
          {
            path: 'overview',
            name: 'ContainerDetail.Overview',
            component: () => import(/* webpackChunkName: "containers" */ '../administrative/containers/Overview.vue')
          },
          {
            path: 'locations',
            name: 'ContainerDetail.Locations',
            component: () => import(/* webpackChunkName: "containers" */ '../administrative/containers/Locations.vue')
          },
          {
            path: 'specimens',
            name: 'ContainerDetail.Specimens',
            component: () => import(/* webpackChunkName: "containers" */ '../administrative/containers/Specimens.vue')
          },
          {
            path: 'transfer-events',
            name: 'ContainerDetail.TransferEvents',
            component: () => import(/* webpackChunkName: "containers" */ '../administrative/containers/TransferEvents.vue')
          },
          {
            path: 'maintenance',
            name: 'ContainerDetail.Maintenance',
            component: () => import(/* webpackChunkName: "containers" */ '../administrative/containers/Maintenance.vue')
          }
        ]
      },
      {
        path: 'container-addedit/:containerId',
        name: 'ContainerAddEdit',
        component: () => import(/* webpackChunkName: "containers" */ '../administrative/containers/AddEdit.vue'),
        props: ({params, query}) => {
          if (!params) {
            return {}
          }

          query = query || {};
          return {
            containerId: params.containerId, parentContainerName: query.parentContainerName,
            row: query.row, column: query.column, position: query.position,
            mode: query.mode, typeId: query.typeId
          };
        }
      },
      {
        path: 'container-archive',
        name: 'BulkContainerArchive',
        component: () => import(/* webpackChunkName: "containers" */ '../administrative/containers/BulkArchive.vue'),
        props: () => ({})
      },
      {
        path: 'container-transfer',
        name: 'BulkContainerTransfer',
        component: () => import(/* webpackChunkName: "containers" */ '../administrative/containers/BulkTransfer.vue'),
        props: () => {
          return {checkout: false, checkin: false}
        }
      },
      {
        path: 'container-find-places',
        name: 'ContainerFindPlaces',
        component: () => import(/* webpackChunkName: "containers" */ '../administrative/containers/FindPlaces.vue'),
        props: () => { }
      },
      {
        path: 'container-checkout',
        name: 'BulkContainerCheckout',
        component: () => import(/* webpackChunkName: "containers" */ '../administrative/containers/BulkTransfer.vue'),
        props: () => {
          return {checkout: true, checkin: false}
        }
      },
      {
        path: 'container-checkin',
        name: 'BulkContainerCheckin',
        component: () => import(/* webpackChunkName: "containers" */ '../administrative/containers/BulkTransfer.vue'),
        props: () => {
          return {checkout: false, checkin: true}
        }
      },
      {
        path: 'scan-boxes',
        name: 'ScanBoxes',
        component: () => import(/* webpackChunkName: "containers" */ '../administrative/containers/ScanBoxes.vue')
      },
      {
        path: 'unblock-locations',
        name: 'UnblockLocations',
        component: () => import(/* webpackChunkName: "containers" */ '../administrative/containers/UnblockLocations.vue')
      },


      /**********************************
       **********************************
       * Container tasks module         *
       **********************************
       **********************************/
      {
        path: 'container-tasks',
        name: 'ContainerTasksList',
        component: () => import(/* webpackChunkName: "containers" */ '../administrative/container-tasks/List.vue'),
        props: (route) => ({filters: route.query.filters}),
      },

      {
        path: 'container-task-addedit/:taskId',
        name: 'ContainerTaskAddEdit',
        component: () => import(/* webpackChunkName: "containers" */ '../administrative/container-tasks/AddEdit.vue'),
        props: (route) => ({taskId: route.params && route.params.taskId})
      },

      /***********************************
       ***********************************
       * Containers types module         *
       ***********************************
       ***********************************/
      {
        path: 'container-types/:typeId',
        name: 'ContainerTypesList',
        component: () => import(/* webpackChunkName: "containers" */ '../administrative/container-types/List.vue'),
        props: (route) => ({typeId: route.params && route.params.typeId, filters: route.query.filters}),
        children: [
          {
            path: '',
            name: 'ContainerTypesListItemDetail',
            component: () => import(/* webpackChunkName: "containers" */ '../administrative/container-types/Detail.vue'),
            props: (route) => ({typeId: route.params && route.params.typeId, listItemDetailView: true}),
            children: [
              {
                path: 'overview',
                name: 'ContainerTypesListItemDetail.Overview',
                component: () => import(/* webpackChunkName: "containers" */ '../administrative/container-types/Overview.vue')
              },
            ]
          }
        ]
      },
      {
        path: 'container-types/:typeId/detail',
        name: 'ContainerTypeDetail',
        component: () => import(/* webpackChunkName: "containers" */ '../administrative/container-types/Detail.vue'),
        props: (route) => ({typeId: route.params && route.params.typeId, listItemDetailView: false}),
        children: [
          {
            path: 'overview',
            name: 'ContainerTypeDetail.Overview',
            component: () => import(/* webpackChunkName: "containers" */ '../administrative/container-types/Overview.vue')
          },
        ]
      },
      {
        path: 'container-type-addedit/:typeId',
        name: 'ContainerTypeAddEdit',
        component: () => import(/* webpackChunkName: "containers" */ '../administrative/container-types/AddEdit.vue'),
        props: (route) => ({typeId: route.params && route.params.typeId})
      },

      /*****************************
       *****************************
       * Specimen carts module     *
       *****************************
       *****************************/
      {
        path: 'carts/:cartId',
        name: 'SpecimenCartsList',
        component: () => import(/* webpackChunkName: "carts" */ '../biospecimen/carts/List.vue'),
        props: (route) => ({
          cartId: route.params && route.params.cartId,
          folderId: route.query.folderId,
          filters: route.query.filters
        }),
        children: [
          {
            path: 'specimens',
            name: 'CartSpecimensList',
            component: () => import(/* webpackChunkName: "carts" */ '../biospecimen/carts/Specimens.vue'),
            props: (route) => (
              {
                cartId: route.params && route.params.cartId,
                query: route.query && route.query.specimenFilters,
                listItemDetailView: true
              }
            ),
          }
        ]
      },
      {
        path: 'cart-addedit/:cartId',
        name: 'SpecimenCartAddEdit',
        component: () => import(/* webpackChunkName: "carts" */ '../biospecimen/carts/AddEdit.vue'),
        props: (route) => ({cartId: route.params && route.params.cartId})
      },

      {
        path: 'cart-folders',
        name: 'SpecimenCartsFoldersList',
        component: () => import(/* webpackChunkName: "carts" */ '../biospecimen/cart-folders/List.vue'),
        props: (route) => ({filters: route.query.filters})
      },

      {
        path: 'cart-folder-addedit/:folderId',
        name: 'SpecimenCartsFolderAddEdit',
        component: () => import(/* webpackChunkName: "carts" */ '../biospecimen/cart-folders/AddEdit.vue'),
        props: (route) => ({folderId: route.params && route.params.folderId})
      },

      {
        path: 'consent-statements',
        name: 'ConsentStatementsList',
        component: () => import(/* webpackChunkName: "consents" */ '../biospecimen/consents/List.vue'),
        props: (route) => ({filters: route.query.filters})
      },

      {
        path: 'consent-statement-addedit/:statementId',
        name: 'ConsentStatementAddEdit',
        component: () => import(/* webpackChunkName: "consents" */ '../biospecimen/consents/AddEdit.vue'),
        props: (route) => ({statementId: route.params && route.params.statementId})
      },


      /*****************************
       *****************************
       * CP module                 *
       *****************************
       *****************************/
      {
        path: 'cps',
        name: 'CpsList',
        component: () => import(/* webpackChunkName: "cps" */ '../biospecimen/cps/List.vue'),
        props: (route) => ({filters: route.query.filters, showStats: route.query.showStats})
      },

      {
        path: 'cps/:cpId/detail',
        name: 'CpDetail',
        component: () => import(/* webpackChunkName: "cps" */ '../biospecimen/cps/Detail.vue'),
        props: (route) => ({cpId: route.params && route.params.cpId}),
        children: [
          {
            path: 'overview',
            name: 'CpDetail.Overview',
            component: () => import(/* webpackChunkName: "cps" */ '../biospecimen/cps/Overview.vue')
          },
          {
            path: 'events',
            name: 'CpDetail.Events',
            component: () => import(/* webpackChunkName: "cps" */ '../biospecimen/cps/Events.vue'),
            props: ({query}) => ({eventId: query.eventId}),
            children: [
              {
                path: 'list',
                name: 'CpDetail.Events.List',
                component: () => import(/* webpackChunkName: "cps" */ '../biospecimen/cps/EventsList.vue'),
                props: ({query}) => ({reqId: query.reqId})
              },
              {
                path: 'addedit',
                name: 'CpDetail.Events.AddEdit',
                component: () => import(/* webpackChunkName: "cps" */ '../biospecimen/cps/AddEditEvent.vue'),
                props: ({query}) => ({copyOf: query.copyOf})
              },
              {
                path: 'addedit-sr',
                name: 'CpDetail.Events.AddEditReq',
                component: () => import(/* webpackChunkName: "cps" */ '../biospecimen/cps/AddEditReq.vue'),
                props: ({query}) => ({reqId: query.reqId, copyOf: query.copyOf})
              },
              {
                path: 'add-derived-sr',
                name: 'CpDetail.Events.CreateDerivedReq',
                component: () => import(/* webpackChunkName: "cps" */ '../biospecimen/cps/AddEditReq.vue'),
                props: ({query}) => ({parentReqId: query.parentReqId, lineage: 'Derived'})
              },
              {
                path: 'add-aliquots-sr',
                name: 'CpDetail.Events.CreateAliquots',
                component: () => import(/* webpackChunkName: "cps" */ '../biospecimen/cps/AddEditReq.vue'),
                props: ({query}) => ({parentReqId: query.parentReqId, lineage: 'Aliquot'})
              }
            ]
          },
          {
            path: 'consents',
            name: 'CpDetail.Consents',
            component: () => import(/* webpackChunkName: "cps" */ '../biospecimen/cps/Consents.vue'),
            props: ({query}) => ({filters: query.filters})
          },
          {
            path: 'import-jobs',
            name: 'CpDetail.ImportJobs',
            component: () => import(/* webpackChunkName: "cps" */ '../biospecimen/cps/ImportJobs.vue')
          },
          {
            path: 'revisions',
            name: 'CpDetail.Revisions',
            component: () => import(/* webpackChunkName: "cps" */ '../biospecimen/cps/Revisions.vue')
          },
          {
            path: 'settings',
            name: 'CpDetail.Settings',
            component: () => import(/* webpackChunkName: "cps" */ '../biospecimen/cps/Settings.vue'),
            children: [
              {
                path: 'label-formats',
                name: 'CpDetail.Settings.LabelFormats',
                component: () => import(/* webpackChunkName: "cps" */ '../biospecimen/cps/LabelFormats.vue')
              },
              {
                path: 'containers',
                name: 'CpDetail.Settings.Container',
                component: () => import(/* webpackChunkName: "cps" */ '../biospecimen/cps/ContainerSettings.vue')
              },
              {
                path: 'distribution-protocols',
                name: 'CpDetail.Settings.Distribution',
                component: () => import(/* webpackChunkName: "cps" */ '../biospecimen/cps/DistributionSettings.vue')
              },
              {
                path: 'forms',
                name: 'CpDetail.Settings.Forms',
                component: () => import(/* webpackChunkName: "cps" */ '../biospecimen/cps/Forms.vue')
              }
            ]
          }
        ]
      },

      {
        path: 'cp-addedit/:cpId',
        name: 'CpAddEdit',
        component: () => import(/* webpackChunkName: "cps" */ '../biospecimen/cps/AddEdit.vue'),
        props: (route) => ({cpId: route.params && route.params.cpId, copyOf: route.query.copyOf})
      },


      /** Biospecimen pages **/
      {
        path: 'cpr-resolver/:cprId',
        name: 'CprResolver',
        component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/CprResolver.vue'),
        props: (route) => ({cprId: route.params && route.params.cprId, query: route.query})
      },
      {
        path: 'visit-resolver/:visitId',
        name: 'VisitResolver',
        component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/visits/VisitResolver.vue'),
        props: (route) => ({visitId: route.params && route.params.visitId, query: route.query})
      },
      {
        path: 'specimen-resolver/:specimenId',
        name: 'SpecimenResolver',
        component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/specimens/SpecimenResolver.vue'),
        props: (route) => ({specimenId: route.params && route.params.specimenId, query: route.query})
      },
      {
        path: 'cp-view',
        name: 'CpViewRoot',
        component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/CpViewRoot.vue'),
        children: [
          {
            path: ':cpId',
            name: 'CpView',
            component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/CpView.vue'),
            props: (route) => ({cpId: route.params && route.params.cpId}),
            children: [
              {
                path: 'list-view',
                name: 'DefCpListView',
                component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/DefListView.vue'),
              },
              {
                path: 'participants',
                name: 'ParticipantsList',
                component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/List.vue'),
                props: (route) => ({
                  cprId: route.params && route.params.cprId,
                  specimenId: route.params && route.params.specimenId,
                  filters: route.query.filters,
                  view: route.query.view || 'participants_list'
                })
              },
              {
                path: 'participants/:cprId',
                name: 'ParticipantsListItemRoot',
                component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/DetailRoot.vue'),
                props: (route) => ({cprId: route.params && route.params.cprId}),
                children: [
                  {
                    path: '',
                    name: 'ParticipantsListItemDetail',
                    component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/Detail.vue'),
                    props: () => ({}),
                    children: [
                      {
                        path: 'overview',
                        name: 'ParticipantsListItemDetail.Overview',
                        component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/Overview.vue')
                      },
                      {
                        path: 'forms',
                        name: 'ParticipantsListItemDetail.Forms',
                        component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/Forms.vue'),
                        props: ({query}) => ({formId: query.formId, recordId: query.recordId})
                      },
                      {
                        path: 'consents',
                        name: 'ParticipantsListItemDetail.Consents',
                        component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/Consents.vue')
                      },
                      {
                        path: 'specimens',
                        name: 'ParticipantsListItemDetail.Specimens',
                        component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/Specimens.vue')
                      }
                    ]
                  },
                  {
                    path: 'visit/:visitId',
                    name: 'ParticipantsListItemVisitRoot',
                    component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/visits/DetailRoot.vue'),
                    props: ({params, query}) => ({ visitId: params && params.visitId, eventId: query.eventId}),
                    children: [
                      {
                        path: '',
                        name: 'ParticipantsListItemVisitDetail',
                        component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/visits/Detail.vue'),
                        props: () => ({noNavButton: true}),
                        children: [
                          {
                            path: 'overview',
                            name: 'ParticipantsListItemVisitDetail.Overview',
                            component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/visits/Overview.vue')
                          },
                          {
                            path: 'forms',
                            name: 'ParticipantsListItemVisitDetail.Forms',
                            component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/visits/Forms.vue'),
                            props: ({query}) => ({formId: query.formId, recordId: query.recordId})
                          },
                          {
                            path: 'consents',
                            name: 'ParticipantsListItemVisitDetail.Consents',
                            component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/visits/Consents.vue')
                          },
                          {
                            path: 'report',
                            name: 'ParticipantsListItemVisitDetail.Report',
                            component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/visits/Report.vue')
                          }
                        ]
                      },
                      {
                        path: 'specimen/:specimenId',
                        name: 'ParticipantsListItemSpecimenRoot',
                        component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/specimens/DetailRoot.vue'),
                        props: (route) => ({specimenId: route.params && route.params.specimenId, reqId: route.query.reqId}),
                        children: [
                          {
                            path: '',
                            name: 'ParticipantsListItemSpecimenDetail',
                            component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/specimens/Detail.vue'),
                            props: () => ({noNavButton: true}),
                            children: [
                              {
                                path: 'overview',
                                name: 'ParticipantsListItemSpecimenDetail.Overview',
                                component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/specimens/Overview.vue'),
                                props: ({query}) => ({action: query.action})
                              },
                              {
                                path: 'forms',
                                name: 'ParticipantsListItemSpecimenDetail.Forms',
                                component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/specimens/Forms.vue'),
                                props: ({query}) => ({formId: query.formId, recordId: query.recordId})
                              }
                            ]
                          }
                        ]
                      }
                    ]
                  }
                ]
              },
              {
                path: 'participants/:cprId',
                name: 'ParticipantDetailRoot',
                component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/DetailRoot.vue'),
                props: (route) => ({cprId: route.params && route.params.cprId, op: 'mutate'}),
                children: [
                  {
                    path: 'addedit',
                    name: 'ParticipantAddEdit',
                    component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/AddEdit.vue'),
                    props: (route) => ({participantId: route.query.participantId})
                  },
                  {
                    path: 'addedit-form',
                    name: 'ParticipantAddEditFormRecord',
                    component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/AddEditFormRecord.vue'),
                    props: (route) => ({...route.query})
                  },
                  {
                    path: 'visit/:visitId',
                    name: 'VisitRoot',
                    component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/visits/DetailRoot.vue'),
                    props: ({params, query}) => ({visitId: params && params.visitId, eventId: query.eventId}),
                    children: [
                      {
                        path: 'addedit',
                        name: 'VisitAddEdit',
                        component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/visits/AddEdit.vue')
                      },
                      {
                        path: 'addedit-form',
                        name: 'VisitAddEditFormRecord',
                        component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/visits/AddEditFormRecord.vue'),
                        props: (route) => ({...route.query})
                      },
                      {
                        path: 'specimen/:specimenId',
                        name: 'SpecimenRoot',
                        component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/specimens/DetailRoot.vue'),
                        props: (route) => ({specimenId: route.params && route.params.specimenId}),
                        children: [
                          {
                            path: 'addedit',
                            name: 'SpecimenAddEdit',
                            component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/specimens/AddEdit.vue')
                          },
                          {
                            path: 'addedit-event',
                            name: 'SpecimenEventAddEdit',
                            component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/specimens/AddEditEvent.vue'),
                            props: (route) => ({...route.query})
                          },
                          {
                            path: 'addedit-form',
                            name: 'SpecimenAddEditFormRecord',
                            component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/specimens/AddEditFormRecord.vue'),
                            props: (route) => ({...route.query})
                          }
                        ]
                      }
                    ]
                  }
                ]
              },
              {
                path: 'bulk-edit',
                name: 'ParticipantsBulkEdit',
                component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/BulkEdit.vue')
              }
            ]
          }
        ]
      },
      {
        path: 'bulk-edit-specimens',
        name: 'SpecimensBulkEdit',
        component: () => import(/* webpackChunkName: "cp-view" */ '../biospecimen/participants/specimens/BulkEdit.vue')
      },

      /*****************************
       *****************************
       * Scheduled jobs module     *
       *****************************
       *****************************/
      {
        path: 'scheduled-jobs',
        name: 'JobsList',
        component: () => import(/* webpackChunkName: "scheduled-jobs" */ '../administrative/jobs/List.vue'),
        props: (route) => ({filters: route.query.filters})
      },

      {
        path: 'scheduled-jobs/:jobId/runs',
        name: 'JobRunsList',
        component: () => import(/* webpackChunkName: "scheduled-jobs" */ '../administrative/jobs/RunsList.vue'),
        props: ({params, query}) => ({jobId: params && params.jobId, filters: query.filters})
      },

      {
        path: 'scheduled-jobs/:jobId/addedit',
        name: 'JobAddEdit',
        component: () => import(/* webpackChunkName: "scheduled-jobs" */ '../administrative/jobs/AddEdit.vue'),
        props: ({params, query}) => ({jobId: params && params.jobId, queryId: query.queryId})
      },

      /*****************************
       *****************************
       * Settings                  *
       *****************************
       *****************************/
      {
        path: 'settings',
        name: 'SettingsList',
        component: () => import(/* webpackChunkName: "settings" */ '../administrative/settings/List.vue'),
        props: ({query}) => ({module: query.module, property: query.property, search: query.search})
      },

      /*****************************
       *****************************
       * Forms                     *
       *****************************
       *****************************/
      {
        path: 'forms/:formId',
        name: 'FormsList',
        component: () => import(/* webpackChunkName: "forms" */ '../administrative/forms/List.vue'),
        props: ({params, query}) => ({formId: params.formId, filters: query.filters}),
        children: [
          {
            path: '',
            name: 'FormsListItemDetail',
            component: () => import(/* webpackChunkName: "forms" */ '../administrative/forms/Detail.vue'),
            children: [
              {
                path: 'overview',
                name: 'FormsListItemDetail.Overview',
                component: () => import(/* webpackChunkName: "forms" */ '../administrative/forms/Overview.vue')
              },
              {
                path: 'preview',
                name: 'FormsListItemDetail.Preview',
                component: () => import(/* webpackChunkName: "forms" */ '../administrative/forms/Preview.vue')
              },
              {
                path: 'associations',
                name: 'FormsListItemDetail.Associations',
                component: () => import(/* webpackChunkName: "forms" */ '../administrative/forms/Associations.vue')
              }
            ]
          }
        ]
      },

      /* TODO: This route/view should be discontinued after implementation of catalogs in the new UI */
      {
        path: 'distribution-requests',
        name: 'DistrbutionRequestViewRedirect',
        component: () => import(/* webpackChunkName: "common" */ '../common/components/DistrbutionRequestRedirect.vue'),
        props: ({query}) => ({catalogId: query.catalogId, requestId: query.requestId})
      }
    ]
  },

  {
    path: '/',
    name: 'NoLoginApp',
    component: () => import('../NoLoginApp.vue')
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
