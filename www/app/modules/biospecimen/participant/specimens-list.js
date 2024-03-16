angular.module('os.biospecimen.participant')
  .controller('SpecimensListViewCtrl', function(
    $scope, $state, currentUser, cp, sdeConfigured, mobileDataEntryEnabled, spmnReqs, enableBetaFeatures,
    PluginReg, Specimen, SpecimensHolder, Alerts, VueApp) {

    var ctrl = this;

    function init() {
      if (enableBetaFeatures) {
        VueApp.setVueView('cp-view/' + cp.id + '/participants/-1', {view: 'specimens_list'});
        return;
      }

      ctrl.showAddSpmn = !sdeConfigured,
      ctrl.resourceOpts = {
        containerReadOpts: {resource: 'StorageContainer', operations: ['Read']},
        orderCreateOpts:    $scope.orderCreateOpts,
        shipmentCreateOpts: $scope.shipmentCreateOpts,
        allSpecimenUpdateOpts: $scope.allSpecimenUpdateOpts,
        specimenUpdateOpts: $scope.specimenUpdateOpts,
        specimenDeleteOpts: $scope.specimenDeleteOpts
      }

      $scope.ctx = {
        params: {
          listName: 'specimen-list-view',
          objectId: cp.id
        },
        emptyState: {
          loadingMessage: 'specimens.loading_list',
          emptyMessage: 'specimens.empty_list'
        },
        resourceOpts: ctrl.resourceOpts
      };

      angular.extend($scope.listViewCtx, {
        listName: 'specimens.list',
        ctrl: ctrl,
        headerActionsTmpl: 'modules/biospecimen/participant/specimens-list-pager.html',
        headerButtonsTmpl: 'modules/biospecimen/participant/specimens-list-ops.html',
        showPrimaryBtnDd: !!cp.bulkPartRegEnabled || (PluginReg.getTmpls('participant-list', 'primary-button').length > 0),
        mobileDataEntryEnabled: mobileDataEntryEnabled,
        spmnReqs: spmnReqs
      });
    }

    function gotoView(state, params, msgCode) {
      var selectedSpmns = $scope.ctx.listCtrl.getSelectedItems();
      if (!selectedSpmns || selectedSpmns.length == 0) {
        Alerts.error('specimen_list.' + msgCode);
        return;
      }

      var ids = selectedSpmns.map(function(spmn) { return spmn.hidden.specimenId; });
      Specimen.getByIds(ids).then(
        function(spmns) {
          SpecimensHolder.setSpecimens(spmns);
          $state.go(state, params);
        }
      );
    }

    function createNewList(spmns) {
      SpecimensHolder.setSpecimens(spmns);
      $state.go('specimen-list-addedit', {listId: ''});
    }

    $scope.showSpecimen = function(row) {
      $state.go('specimen', {specimenId: row.hidden.specimenId});
    }

    $scope.setListCtrl = function(listCtrl) {
      ctrl.listCtrl = $scope.ctx.listCtrl = listCtrl;
      $scope.listViewCtx.showSearch = listCtrl.haveFilters;
      $scope.listViewCtx.pagerOpts  = listCtrl.pagerOpts;
    }

    this.getSelectedSpecimens = function() {
      var selectedSpmns = $scope.ctx.listCtrl.getSelectedItems();
      if (!selectedSpmns || selectedSpmns.length == 0) {
        return [];
      }

      return selectedSpmns.map(function(spmn) { return {id: spmn.hidden.specimenId, cpId: cp.id}; });
    }

    this.loadSpecimens = function() {
      $scope.ctx.listCtrl.loadList();
    }

    this.addSpecimensToList = function(list) {
      var items = $scope.ctx.listCtrl.getSelectedItems();
      if (!items || items.length == 0) {
        Alerts.error('specimens.no_specimens_for_specimen_list');
        return;
      }

      var spmns = items.map(function(item) { return {id: item.hidden.specimenId}; });
      if (!list) {
        createNewList(spmns);
      } else {
        list.addSpecimens(spmns).then(
          function() {
            var type = list.getListType(currentUser);
            Alerts.success('specimen_list.specimens_added_to_' + type, list);
          }
        );
      }
    }

    init();
  });
