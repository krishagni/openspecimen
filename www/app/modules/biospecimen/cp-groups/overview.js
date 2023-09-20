
angular.module('os.biospecimen.cpgroups')
  .controller('CpGroupOverviewCtrl', function($scope, $state, group, osExportSvc, CollectionProtocolGroup, Util, Alerts) {

    var octx;
    function init() {
      octx = $scope.octx = {};
      octx.workflowUri = CollectionProtocolGroup.url() + group.id + '/workflows-file';
    }

    $scope.showCpOverview = function(cp) {
      $state.go('cp-detail.overview', {cpId: cp.id});
    }

    $scope.deleteGroup = function() {
      Util.showConfirm({
        title: 'cp_groups.delete_group_q',
        confirmMsg: 'cp_groups.confirm_delete_group',
        input: group,
        ok: function() {
          group.$remove().then(
            function() {
              Alerts.success('cp_groups.group_deleted', group);
              $state.go('cp-groups-list');
            }
          );
        }
      });
    }

    $scope.exportCpRecords = function(objectType) {
      var payload = {objectType: objectType, recordIds: group.cps.map(function(cp) { return cp.id; })};
      osExportSvc.exportRecords(payload);
    }

    init();
  });
