
angular.module('os.biospecimen.visit.addedit', [])
  .controller('AddEditVisitCtrl', function(
    $scope, $state, $stateParams, $injector, userRole, cp, cpr, visit, latestVisit,
    extensionCtxt, hasDict, layout, onValueChangeCb, mrnAccessRestriction,
    ParticipantSpecimensViewState, PvManager, ExtensionsUtil, CollectSpecimensSvc, Alerts) {

    function loadPvs() {
      $scope.visitStatuses = PvManager.getPvs('visit-status');
      $scope.cohorts = PvManager.getPvs('cohort');
    };

    function init() {
      var currVisit = $scope.currVisit = angular.copy(visit);
      angular.extend(currVisit, {cprId: cpr.id, cpTitle: cpr.cpTitle});

      var ctx = $scope.visitCtx = {
        obj: {visit: $scope.currVisit, cpr: cpr, cp: cp, userRole: userRole},
        opts: {viewCtx: $scope, layout: layout, onValueChange: onValueChangeCb, mdInput: false},
        inObjs: ['visit'],
        mrnAccessRestriction: mrnAccessRestriction,
        documentsCount: 0
      }

      if (!currVisit.id) {
        angular.extend(currVisit, {
          visitDate: new Date(),
          status: 'Complete',
          clinicalDiagnoses: latestVisit ? latestVisit.clinicalDiagnoses : currVisit.clinicalDiagnoses,
          site: getVisitSite(cpr, latestVisit, currVisit)
        });
        ctx.pendingToStart = true;
        delete currVisit.anticipatedVisitDate;
      } else if (currVisit.status == 'Pending') {
        currVisit.visitDate = new Date();
        ctx.pendingToStart = true;
      }

      if ($stateParams.missedVisit == 'true') {
        angular.extend(currVisit, {status: 'Missed Collection'});
      } else if ($stateParams.newVisit == 'true') {
        var presetValues = {
          id: undefined, name: undefined,
          status: 'Complete', visitDate: new Date(),
          surgicalPathologyNumber: null, sprName: null, sprLocked: false
        };

        angular.extend(currVisit, presetValues);
        $scope.visit = currVisit;
        ctx.pendingToStart = true;
      }

      $scope.deFormCtrl = {};
      extensionCtxt.sdeMode = hasDict;
      $scope.extnOpts = ExtensionsUtil.getExtnOpts(currVisit, extensionCtxt);

      if (!hasDict) {
        loadPvs();
      }

      loadDocuments();
    }

    function getVisitSite(cpr, latestVisit, currVisit) {
      var site = currVisit.site;
      if (!!site) {
        // site = site;
      } else if (latestVisit) {
        site = latestVisit.site;
      } else if (cpr.participant.pmis.length > 0) {
        site = cpr.participant.pmis[0].siteName;
      }

      return site;
    }

    function loadDocuments() {
      if (!$injector.has('ecDocument') || !!$scope.visit.id || !cp.visitLevelConsents) {
        return;
      }

      return $injector.get('ecDocument').getCount({cpId: cp.id}).then(
        function(resp) {
          $scope.visitCtx.documentsCount = resp.count;
        }
      );
    }

    $scope.saveVisit = function(gotoSpmnCollection, gotoConsents) {
      var formCtrl = $scope.deFormCtrl.ctrl;
      if (formCtrl && !formCtrl.validate()) {
        return;
      }

      if (formCtrl) {
        $scope.currVisit.extensionDetail = formCtrl.getFormData();
      }

      if ($scope.currVisit.eventLabel != visit.eventLabel) {
        $scope.currVisit.eventId = undefined;
      }

      $scope.currVisit.$saveOrUpdate().then(
        function(result) {
          Alerts.success('visits.visit_saved');
          ParticipantSpecimensViewState.specimensUpdated($scope);

          angular.extend($scope.visit, angular.extend({clinicalStatus: null, cohort: null}, result));
          if (gotoSpmnCollection) {
            var state = $state.get('visit-detail.overview');
            CollectSpecimensSvc.collectVisit({state: state, params: {cprId: cpr.id}}, cp, cpr.id, $scope.visit);
          } else if (gotoConsents) {
            $state.go('visit-detail.consents', {visitId: result.id, eventId: result.eventId});
          } else {
            $state.go('visit-detail.overview', {visitId: result.id, eventId: result.eventId});
          }
        }
      );
    };

    init();
  });
