
angular.module('os.biospecimen.cpgroups', [])
  .config(function($stateProvider) {
    $stateProvider
      .state('cp-groups', {
        url: '/cp-groups',
        abstract: true,
        template: '<div ui-view></div>',
        controller: function($scope) { },
        parent: 'signed-in'
      })
      .state('cp-groups-list', {
        url: '?filters', 
        templateUrl: 'modules/biospecimen/cp-groups/list.html',
        controller: 'CpGroupsListCtrl',
        parent: 'cp-groups'
      })
      .state('cp-group-addedit', {
        url: '/:groupId/addedit',
        templateUrl: 'modules/biospecimen/cp-groups/addedit.html',
        controller: 'CpGroupAddEditCtrl',
        resolve: {
          group: function($stateParams, CollectionProtocolGroup) {
            if (!$stateParams.groupId || $stateParams.groupId < 0) {
              return new CollectionProtocolGroup({cps: []});
            }

            return CollectionProtocolGroup.getById($stateParams.groupId);
          }
        },
        parent: 'cp-groups'
      })
      .state('cp-group-detail', {
        url: '/detail/:groupId',
        abstract: true,
        templateUrl: 'modules/biospecimen/cp-groups/detail.html',
        controller: function ($scope, group, AuthorizationService) {
          var ctx = $scope.ctx = {group: group, editAllowed: true, exportAllowed: true};
          var opts = {resource: 'CollectionProtocol', operations: ['Update']};
          for (var i = 0; i < group.cps.length; ++i) {
            opts.cp = group.cps[i].shortTitle;
            if (!AuthorizationService.isAllowed(opts)) {
              ctx.editAllowed = false;
              break;
            }
          }

          var eopts = {resource: 'CollectionProtocol', operations: ['Export Import']};
          for (var i = 0; i < group.cps.length; ++i) {
            eopts.cp = group.cps[i].shortTitle;
            if (!AuthorizationService.isAllowed(eopts)) {
              ctx.exportAllowed = false;
              break;
            }
          }
        },
        resolve: {
          group: function($stateParams, CollectionProtocolGroup) {
            if (!$stateParams.groupId || $stateParams.groupId < 0) {
              return new CollectionProtocolGroup();
            }

            return CollectionProtocolGroup.getById($stateParams.groupId).then(
              function(group) {
                group.cps.sort(
                  function(c1, c2) {
                    return c1.shortTitle < c2.shortTitle? -1 : (c1.shortTitle > c2.shortTitle ? 1 : 0);
                  }
                );

                return group;
              }
            );
          }
        },
        parent: 'cp-groups'
      })
      .state('cp-group-detail.overview', {
        url: '/overview',
        templateUrl: 'modules/biospecimen/cp-groups/overview.html',
        controller: 'CpGroupOverviewCtrl',
        parent: 'cp-group-detail'
      })
      .state('cp-group-detail.forms', {
        url: '/forms',
        templateUrl: 'modules/biospecimen/cp-groups/forms.html',
        controller: 'CpGroupFormsCtrl',
        parent: 'cp-group-detail'
      })
      .state('cp-group-detail.import-workflows', {
        url: '/import-workflows',
        templateUrl: 'modules/biospecimen/cp-groups/import-workflows.html',
        controller: 'CpGroupImportWorkflowsCtrl',
        parent: 'cp-group-detail'
      });
    });
