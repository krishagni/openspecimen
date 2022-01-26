angular.module('os.administrative.dp')
  .controller('DpReservedSpecimensCtrl', function($scope, $state, $window, distributionProtocol, Specimen) {

    var lctx;

    function init() {
      lctx = $scope.lctx = {
        params: {
          listName: 'reserved-specimens-list-view',
          objectId: distributionProtocol.id
        },
        emptyState: {
          loadingMessage: 'specimens.loading_list',
          emptyMessage: 'specimens.empty_list'
        },
        orderCreateOpts: {resource: 'Order', operations: ['Create']}
      };
    }

    function loadSpecimens() {
      lctx.listCtrl.loadList();
    }

    $scope.distributeAll = function() {
      $window.localStorage['os.orderDetails'] = JSON.stringify({
        allReserved: true,
        dp: distributionProtocol
      });
      $state.go('order-addedit', {orderId: -1});
    }

    $scope.distributeSpecimens = function() {
      var spmnIds = lctx.listCtrl.getSelectedItems().map(function(spmn) { return spmn.hidden.specimenId });
      $window.localStorage['os.orderDetails'] = JSON.stringify({
        specimenIds: spmnIds,
        dp: distributionProtocol
      });
      $state.go('order-addedit', {orderId: -1});
    }

    $scope.cancelReservation = function() {
      var payload = {
        dpId: distributionProtocol.id,
        specimens: lctx.listCtrl.getSelectedItems().map(function(spmn) { return {id: spmn.hidden.specimenId } }),
        cancelOp: true
      };
      distributionProtocol.reserveSpecimens(payload).then(loadSpecimens);
    }

    $scope.setListCtrl = function(listCtrl) {
      lctx.listCtrl = listCtrl;
      lctx.showSearch = listCtrl.haveFilters;
    }

    init();
  });
