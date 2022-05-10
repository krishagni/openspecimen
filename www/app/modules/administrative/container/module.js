
angular.module('os.administrative.container',
  [
    'ui.router',
    'os.administrative.container.list',
    'os.administrative.container.addedit',
    'os.administrative.container.detail',
    'os.administrative.container.overview',
    'os.administrative.container.locations',
    'os.administrative.container.replicate',
    'os.administrative.container.util',
    'os.administrative.container.map'
  ])

  .config(function($stateProvider) {
    $stateProvider
      .state('container-root', {
        abstract: true,
        template: '<div ui-view></div>',
        controller: function($scope, AuthorizationService) {
          // Storage Container Authorization Options
          $scope.containerResource = {
            createOpts: {resource: 'StorageContainer', operations: ['Create']},
            updateOpts: {resource: 'StorageContainer', operations: ['Update']},
            deleteOpts: {resource: 'StorageContainer', operations: ['Delete']},
            importOpts: {resource: 'StorageContainer', operations: ['Export Import']},
            deleteAllowed: AuthorizationService.isAllowed({resource: 'StorageContainer', operations: ['Delete']})
          }
        },
        parent: 'signed-in'
      })
      .state('container-list', {
        url: '/containers?filters',
        template: '<div>Discontinued</div>',
        controller: function(VueApp) {
          VueApp.setVueView('containers');
        },
        parent: 'signed-in'
      })
      .state('container-addedit', {
        url: '/container-addedit/:containerId?posOne&posTwo&pos&siteName&parentContainerId&parentContainerName&mode&containerTypeId',
        templateUrl: 'modules/administrative/container/addedit.html',
        resolve: {
          container: function($stateParams, Container) {
            if ($stateParams.containerId) {
              return Container.getById($stateParams.containerId);
            }

            return new Container({
              allowedCollectionProtocols: [],
              allowedSpecimenClasses: [],
              allowedSpecimenTypes: [],
              allowedDistributionProtocols: []
            });
          },
          containerType: function($stateParams, ContainerType) {
            if ($stateParams.containerTypeId) {
              return ContainerType.getById($stateParams.containerTypeId);
            }
            
            return null;
          },
          barcodingEnabled: function(CollectionProtocol) {
            return CollectionProtocol.getBarcodingEnabled();
          },
          extensionCtxt: function(Container) {
            return Container.getExtensionCtxt();
          }
        },
        controller: 'ContainerAddEditCtrl',
        parent: 'container-root'
      })
      .state('container-import', {
        url: '/containers-import',
        templateUrl: 'modules/common/import/add.html',
        controller: 'ImportObjectCtrl',
        resolve: {
          importDetail: function() {
            return {
              breadcrumbs: [{state: 'container-list', title: 'container.list'}],
              objectType: 'storageContainer',
              title: 'container.bulk_import',
              onSuccess: {state: 'container-import-jobs'}
            };
          }
        },
        parent: 'container-root'
      })
      .state('container-import-jobs', {
        url: '/containers-import-jobs',
        templateUrl: 'modules/common/import/list.html',
        controller: 'ImportJobsListCtrl',
        resolve: {
          importDetail: function() {
            return {
              breadcrumbs: [{state: 'container-list', title: 'container.list'}],
              title: 'container.bulk_import_jobs',
              objectTypes: ['storageContainer']
            }
          }
        },
        parent: 'container-root'
      })
      .state('container-replicate', {
        url: '/container-replicate/:containerId',
        templateUrl: 'modules/administrative/container/replicate.html',
        controller: 'ContainerReplCtrl',
        resolve: {
          container: function($stateParams, Container) {
            return Container.getById($stateParams.containerId);
          }
        },
        parent: 'container-root'
      })
      .state('container-detail-root', {
        url: '/containers',
        template: '<div ui-view></div>',
        resolve: {
          containerViewState: function(ContainerViewState) {
            return new ContainerViewState();
          },

          barcodingEnabled: function(CollectionProtocol) {
            return CollectionProtocol.getBarcodingEnabled();
          }
        },
        abstract: true,
        parent: 'container-root'
      })
      .state('container-detail', {
        url: '/:containerId',
        templateUrl: 'modules/administrative/container/detail.html',
        resolve: {
          rootId: function($stateParams, containerViewState, Container) {
            if (containerViewState.getRootContainerId() == -1) {
              return Container.getAncestorsHierarchy($stateParams.containerId).then(
                function(hierarchy) {
                  return containerViewState.setHierarchy(hierarchy);
                }
              );
            } else {
              return containerViewState.getRootContainerId();
            }
          },

          //
          // explicit dependency on rootId to ensure containerViewState
          // is initialized with hierarchy...
          //
          container: function($stateParams, rootId, containerViewState) {
            return containerViewState.getContainer($stateParams.containerId);
          },

          containerTree: function($stateParams, container, rootId, containerViewState) {
            if (container && container.id == rootId && !container.childContainersLoaded &&
                containerViewState.getHierarchy().length == 1) {
              return container.getChildContainers().then(
                function(childContainers) {
                  container.childContainers = childContainers;
                  containerViewState.setHierarchy(container);
                  return containerViewState.getHierarchy();
                }
              );
            } else {
              return containerViewState.getHierarchy();
            }
          }
        },
        controller: 'ContainerDetailCtrl',
        parent: 'container-detail-root'
      })
      .state('container-detail.overview', {
        url: '/overview',
        templateUrl: 'modules/administrative/container/overview.html',
        resolve: {
        },
        controller: 'ContainerOverviewCtrl',
        parent: 'container-detail'
      })
      .state('container-detail.locations', {
        url: '/locations',
        templateUrl: 'modules/administrative/container/locations.html',
        resolve: {
        },
        controller: 'ContainerLocationsCtrl',
        parent: 'container-detail'
      })
      .state('container-detail.specimens', {
        url: '/specimens?filters',
        templateUrl: 'modules/administrative/container/specimens.html',
        controller: 'ContainerSpecimensCtrl',
        parent: 'container-detail'
      })
      .state('container-detail.events', {
        url: '/events',
        templateUrl: 'modules/administrative/container/events.html',
        controller: 'ContainerEventsCtrl',
        resolve: {
          events: function(container) {
            return container.getTransferEvents();
          }
        },
        parent: 'container-detail'
      })
      .state('container-detail.maintenance', {
        url: '/maintenance',
        templateUrl: 'modules/administrative/container/maintenance.html',
        controller: 'ContainerMaintenanceCtrl',
        parent: 'container-detail'
      });
  })

  .run(function(QuickSearchSvc) {
    var opts = {caption: 'entities.container', state: 'container-detail.locations'};
    QuickSearchSvc.register('storage_container', opts);
  });
