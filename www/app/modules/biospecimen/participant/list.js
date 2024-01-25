
angular.module('os.biospecimen.participant.list', ['os.biospecimen.models'])
  .controller('ParticipantListCtrl', function(
    $scope, $state, $injector, cp, twoStepReg, mobileDataEntryEnabled,
    addParticipantWorkflow, hasWorkflowModule, workflows, enableBetaFeatures,
    ParticipantsHolder, PluginReg, DeleteUtil, CollectionProtocolRegistration, VueApp) {

    var ctrl = this;

    function init() {
      if (enableBetaFeatures) {
        VueApp.setVueView('cp-view/' + cp.id + '/participants/-1');
        return;
      }

      $scope.cpId = cp.id;
      $scope.ctx = {
        params: {
          listName: 'participant-list-view',
          objectId: cp.id
        },
        emptyState: {
          loadingMessage: 'participant.loading_list',
          emptyMessage: 'participant.empty_list'
        }
      };

      angular.extend($scope.listViewCtx, {
        twoStepReg: twoStepReg,
        listName: 'participant.list',
        ctrl: ctrl,
        headerButtonsTmpl: 'modules/biospecimen/participant/register-button.html',
        headerActionsTmpl: 'modules/biospecimen/participant/list-pager.html',
        showPrimaryBtnDd: !!cp.bulkPartRegEnabled || (PluginReg.getTmpls('participant-list', 'primary-button').length > 0),
        showRapidRegistration: addParticipantWorkflow == 'rapid',
        mobileDataEntryEnabled: mobileDataEntryEnabled,
        showAddParticipantWf: hasWorkflowModule && workflows.addParticipant > 0
      });
    }

    $scope.showParticipant = function(row) {
      $state.go('participant-detail.overview', {cprId: row.hidden.cprId});
    };

    $scope.setListCtrl = function(listCtrl) {
      ctrl.listCtrl = $scope.ctx.listCtrl = listCtrl;
      $scope.listViewCtx.showSearch = listCtrl.haveFilters;
      $scope.listViewCtx.pagerOpts  =  listCtrl.pagerOpts;
    }

    ctrl.navToAddParticipantWf = function() {
      var WorkflowInstance = $injector.get('WorkflowInstance');
      new WorkflowInstance({workflow: {id: workflows.addParticipant}, params: {cpId: cp.id}}).$saveOrUpdate().then(
        function(instance) {
          VueApp.setVueView('task-manager/instances/' + instance.id, {});
        }
      );
    }

    ctrl.bulkEdit = function() {
      var selectedRows = ctrl.listCtrl.getSelectedItems();
      var cprs = selectedRows.map(function(row) { return {id: +row.hidden.cprId}; });
      ParticipantsHolder.setParticipants(cprs);
      $state.go('participant-bulk-edit');
    }

    ctrl.deleteParticipants = function() {
      var selectedRows = ctrl.listCtrl.getSelectedItems();
      var cprIds = selectedRows.map(function(row) { return +row.hidden.cprId; });

      var opts = {
        confirmDelete:  'participant.delete_participants',
        successMessage: 'participant.participants_deleted',
        pendingMessage: 'participant.participants_delete_pending',
        onBulkDeletion: function() { ctrl.listCtrl.loadList(); },
        askReason:      true
      }

      DeleteUtil.bulkDelete({bulkDelete: CollectionProtocolRegistration.bulkDelete}, cprIds, opts);
    }

    init();
  })

  .directive('osShowIfMultipleOptionsPresent', function($timeout) {
    function isElementDisplayed(item) {
      return !(
        item.style.display == 'none' ||
        item.style.visibility == 'hidden' ||
        parseFloat(item.style.opacity) <= 0 ||
        (item.classList && typeof item.classList.contains == 'function' && item.classList.contains('ng-hide'))
      );
    }

    return {
      restrict: 'A',
      link: function(scope, element, attrs) {
        scope.$watch(
          function() {
            return element.find("ul.dropdown-menu > li:not(.divider)").
              filter(function() {
                return isElementDisplayed(this);
              }).length;
          }, function(val) {
            if (val <= 1) {
              element.find('.dropdown-toggle').hide();
              element.find('.single-option').show();
            } else {
              element.find('.dropdown-toggle').show();
              element.find('.single-option').hide();
            }
          }
        );
      }
    }
  });
