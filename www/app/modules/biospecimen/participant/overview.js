
angular.module('os.biospecimen.participant.overview', ['os.biospecimen.models'])
  .controller('ParticipantOverviewCtrl', function(
    $scope, $state, $stateParams, $injector, userRole, hasSde, hasDict, hasFieldsFn,
    storePhi, cpDict, visitsTab, cp, cpr, consents, visits,
    Visit, CollectSpecimensSvc, SpecimenLabelPrinter, ExtensionsUtil, Util, Alerts) {

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
        watchOn: ['cpr.participant']
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
      var retSt = {state: $state.current, params: $stateParams};
      CollectSpecimensSvc.collectVisit(retSt, cp, cpr.id, visit);
    }

    $scope.collectPending = function(visit) {
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

    init();
  });
