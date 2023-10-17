
angular.module('os.biospecimen.participant.overview', ['os.biospecimen.models'])
  .controller('ParticipantOverviewCtrl', function(
    $scope, $state, $stateParams, $injector, userRole, hasSde, hasDict, hasFieldsFn,
    storePhi, cpDict, visitsTab, cp, cpr, consents, visits, tmWorkflowId, workflows,
    Visit, CollectSpecimensSvc, SpecimenLabelPrinter, ExtensionsUtil, Util, Alerts, VueApp) {

    function init() {
      $scope.occurredVisits    = Visit.completedVisits(visits);
      $scope.anticipatedVisits = Visit.anticipatedVisits(visits);
      $scope.missedVisits      = Visit.missedVisits(visits);

      ExtensionsUtil.createExtensionFieldMap($scope.cpr.participant, hasDict);
      $scope.partCtx = {
        obj: {cpr: $scope.cpr, consents: createCodedResps(consents), userRole: userRole},
        inObjs: ['cpr', 'consents', 'calcCpr'],
        showEdit: hasFieldsFn(['cpr'], []),
        auditObjs: [
          {objectId: cpr.id, objectName: 'collection_protocol_registration'},
          {objectId: cpr.participant.id, objectName: 'participant'}
        ],
        showAnonymize: storePhi,
        watchOn: ['cpr.participant'],
        workflowId: tmWorkflowId
      }

      $scope.occurredVisitsCols = initVisitTab(visitsTab.occurred, $scope.occurredVisits);
    }

    function initVisitTab(fieldsCfg, inputVisits) {
      if (!hasSde || !fieldsCfg || fieldsCfg.length == 0) {
        return [];
      }

      var field = fieldsCfg.find(
        function(cfg) {
          if (cfg.field.indexOf('visit.extensionDetail.attrsMap') == 0) {
            return true;
          }

          return !!cfg.displayExpr && cfg.displayExpr.indexOf('visit.extensionDetail.attrsMap') != -1;
        }
      );
      if (!!field) {
        angular.forEach(inputVisits,
          function(iv) {
            ExtensionsUtil.createExtensionFieldMap(iv, hasDict);
          }
        );
      }

      var objFn = function(visit) { return {cpr: cpr, visit: visit}; };
      var result = $injector.get('sdeFieldsSvc').getFieldValues(cpDict, fieldsCfg, inputVisits, objFn);
      if (result.fields.length > 0) {
        angular.forEach(inputVisits,
          function(visit, idx) {
            visit.$$columns = result.values[idx];
          }
        );
      }

      return result.fields;
    }

    function createCodedResps(consents) {
      angular.forEach(consents && consents.responses,
        function(resp) {
          if (resp.code) {
            consents[resp.code] = resp.response;
          }
        }
      );

      return consents;
    }

    $scope.isOtherProtocol = function(other) {
      return other.cpShortTitle != $scope.cpr.cpShortTitle;
    }

    $scope.anonymize = function() {
      Util.showConfirm({
        title: "participant.anonymize",
        confirmMsg: "participant.confirm_anonymize",
        isWarning: true,
        ok: function() {
          $scope.cpr.anonymize().then(
            function(savedCpr) {
              $scope.cpr.participant = null;
              angular.extend($scope.cpr, savedCpr);
              ExtensionsUtil.createExtensionFieldMap($scope.cpr.participant, hasDict);
              Alerts.success("participant.anonymized_successfully");
            }
          )
        }
      });
    }

    $scope.collect = function(visit) {
      if ($scope.partCtx.workflowId > 0) {
        var tmWfInstance = $injector.get('WorkflowInstance');

        var selected = [];
        if (visit) {
          selected = [visit];
        } else {
          selected = ($scope.anticipatedVisits || []).filter(function(v) { return v.selected; });
        }

        var model = new tmWfInstance({
          workflow: {id: $scope.partCtx.workflowId},
          inputItems: selected.map(
            function(visit) {
              return {
                cpr: {id: cpr.id},
                cpe: visit.eventId && {id: visit.eventId},
                visit: visit.id && {id: visit.id}
              }
            }
          )
        });

        model.$saveOrUpdate().then(
          function(instance) {
            VueApp.setVueView('task-manager/instances/' + instance.id, {});
          }
        );

        return;
      }

      var retSt = {state: $state.current, params: $stateParams};
      CollectSpecimensSvc.collectVisit(retSt, cp, cpr.id, visit);
    }

    $scope.newVisit = function(visit) {
      $state.go('visit-addedit', {visitId: visit.id, eventId: visit.eventId, newVisit: true});
    }

    $scope.collectPending = function(visit) {
      if (workflows.collectPendingSpecimens > 0) {
        var wfInstance = $injector.get('WorkflowInstance');
        var workflow = {id: workflows.collectPendingSpecimens};
        var inputItems = [{cpr: cpr, visit: visit}];
        new wfInstance({workflow: workflow, inputItems: inputItems}).$saveOrUpdate().then(
          function(instance) {
            VueApp.setVueView('task-manager/instances/' + instance.id);
          }
        );

        return;
      }

      var retSt = {state: $state.current, params: $stateParams};
      CollectSpecimensSvc.collectPending(retSt, cp, cpr.id, visit);
    }

    $scope.printSpecimenLabels = function(args) {
      args = args || {};
      args = angular.extend(args, {cprId: cpr.id});

      var ts = Util.formatDate(Date.now(), 'yyyyMMdd_HHmmss');
      var outputFilename = [cpr.cpShortTitle, cpr.ppid, ts].join('_') + '.csv';
      SpecimenLabelPrinter.printLabels(args, outputFilename);
    }

    $scope.toggleAvSelect = function(visit) {
      $scope.avSelected = ($scope.anticipatedVisits || []).some(function(v) { return !!v.selected; });
    }

    init();
  });
