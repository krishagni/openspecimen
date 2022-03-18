
angular.module('os.query', 
  [
    'os.query.models',
    'os.query.globaldata',
    'os.query.addeditfolder',
    'os.query.delete',
    'os.query.importquery',
    'os.query.list',
    'os.query.save',
    'os.query.addedit',
    'os.query.addeditfilter',
    'os.query.expr',
    'os.query.util',
    'os.query.datepicker',
    'os.query.executor',
    'os.query.results',
    'os.query.defineview',
    'os.query.parameterized',
    'os.query.lookup'
  ]
).config(function($stateProvider) {
   $stateProvider
     .state('query-root', {
       url: '/queries',
       template: '<div ui-view></div>',
       controller: function($scope, queryGlobal, QueryUtil, AuthorizationService) {
         $scope.queryGlobal = queryGlobal;

         var permOpts = $scope.permOpts = {
           create: {resource: 'Query', operations: ['Create']},
           update: {resource: 'Query', operations: ['Update']},
           delete: {resource: 'Query', operations: ['Delete']},
           exim:   {resource: 'Query', operations: ['Export Import']}
         };

         permOpts.createAllowed = AuthorizationService.isAllowed(permOpts.create);
         permOpts.updateAllowed = AuthorizationService.isAllowed(permOpts.update);
         permOpts.deleteAllowed = AuthorizationService.isAllowed(permOpts.delete);
         permOpts.eximAllowed   = AuthorizationService.isAllowed(permOpts.exim);
         permOpts.createJobAllowed = AuthorizationService.isAllowed({resource: 'ScheduledJob', operations: ['Create']});

         QueryUtil.initOpsDesc();
       },
       resolve: {
         queryGlobal: function(QueryGlobalData) {
           return new QueryGlobalData();
         }
       },
       abstract: true,
       parent: 'signed-in'
     })
     .state('query-list', {
       url: '/list?folderId&filters',
       templateUrl: 'modules/query/list.html',
       controller: 'QueryListCtrl',
       resolve: {
         folders: function(currentUser, queryGlobal) {
           return queryGlobal.loadFolders(currentUser).then(
             function(resp) {
               return resp.allFolders;
             }
           );
         },

         folder: function($stateParams, folders) {
           if (!!$stateParams.folderId) {
             return folders.find(function(folder) { return folder.id == $stateParams.folderId; });
           }

           return undefined;
         }
       },
       parent: 'query-root'
     })
     .state('query-addedit', {
       url: '/addedit?queryId',
       templateUrl: 'modules/query/addedit.html',
       controller: 'QueryAddEditCtrl',
       resolve: {
         cps: function(queryGlobal) {
           return queryGlobal.getCps();
         },
         queryCtx: function($stateParams, queryGlobal) {
           return queryGlobal.getQueryCtx($stateParams.queryId);        
         }
       },
       parent: 'query-root'
     })  
     .state('query-results', {
       url: '/results?queryId&editMode&cpId',
       templateUrl: 'modules/query/results.html',
       controller: 'QueryResultsCtrl',
       resolve: {
         queryCtx: function($rootScope, $stateParams, queryGlobal, QueryCtxHolder) {
           var ctx = QueryCtxHolder.getCtx();
           if ($rootScope.stateChangeInfo && ctx && $rootScope.stateChangeInfo.fromState.name == ctx.fromState) {
             queryGlobal.setQueryCtx(ctx);
             $stateParams.editMode = true;
           }

           QueryCtxHolder.clearCtx();
           return queryGlobal.getQueryCtx($stateParams.queryId, $stateParams.cpId);
         },

         cps: function(queryGlobal) {
           return queryGlobal.getCps();
         }
       },
       parent: 'query-root'
     })
     .state('query-audit-logs', {
       url: '/query-audit-logs',
       templateUrl: 'modules/query/audit-logs.html',
       controller: 'QueryAuditLogsCtrl',
       parent: 'signed-in'
     })
     .state('export-query-audit-logs', {
       url: '/export-query-audit-logs',
       templateUrl: 'modules/query/export-audit-logs.html',
       controller: 'QueryAuditLogsExportCtrl',
       parent: 'signed-in'
     });
  }).run(function(UrlResolver) {
    UrlResolver.regUrlState('folder-queries', 'query-list', 'folderId');
  });

