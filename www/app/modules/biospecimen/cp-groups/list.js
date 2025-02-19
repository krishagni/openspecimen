
angular.module('os.biospecimen.cpgroups')
  .controller('CpGroupsListCtrl', function($scope, $state, CollectionProtocol, CollectionProtocolGroup, ListPagerOpts, Util) {

    var ctx, pagerOpts, filterOpts;

    function init() {
      $scope.ctx = ctx = { };

      ctx.updateOpts   = {resource: 'CollectionProtocol', operations: ['Update']};
      ctx.pagerOpts    = new ListPagerOpts({listSizeGetter: getGroupsCount});
      ctx.filterOpts   = Util.filterOpts({includeStats: true, maxResults: ctx.pagerOpts.recordsPerPage + 1});
      ctx.emptyState   = {
        empty: true,
        loading: true,
        emptyMessage: 'cp_groups.empty_list',
        loadingMessage: 'cp_groups.loading_list'
      }

      loadGroupsList(ctx.filterOpts);
      Util.filter($scope, 'ctx.filterOpts', loadGroupsList);
    }

    function setList(list) {
      ctx.emptyState.loading = false;
      ctx.emptyState.empty = list.length <= 0;
      ctx.pagerOpts.refreshOpts(list);
      ctx.groupsList = list;
    }

    function getGroupsCount() {
      alert("TODO");
    }

    function loadGroupsList(filterOpts) {
      ctx.emptyState.loading = true;
      CollectionProtocolGroup.query(filterOpts).then(
        function(groupsList) {
          setList(groupsList);
        }
      );
    };

    $scope.showGroupSummary = function(group) {
      $state.go('cp-group-detail.overview', {groupId: group.id});
    };

    $scope.pageSizeChanged = function() {
      ctx.filterOpts.maxResults = ctx.pagerOpts.recordsPerPage + 1;
    }

    $scope.searchCps = function(searchTerm) {
      CollectionProtocol.query({query: searchTerm}).then(
        function(cps) {
          ctx.cps = cps;
        }
      );
    }

    init();
  });
