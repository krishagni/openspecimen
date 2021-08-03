
angular.module('os.biospecimen.specimen.addaliquots', [])
  .controller('AddAliquotsCtrl', function(
    $scope, $rootScope, $state, $stateParams, userRole, specimen, cpr,
    visit, extensionCtxt, hasSde, hasDict, cpDict, onValueChangeCb, createDerived, aliquotFields, incrFreezeThawCycles,
    CollectSpecimensSvc, Specimen, SpecimenUtil, SpecimensHolder, ExtensionsUtil, Alerts) {

    function init() {
      $scope.showForm = false;
      $scope.cpr = cpr;
      $scope.visit = visit;
      var ps = $scope.parentSpecimen = specimen;
      ExtensionsUtil.createExtensionFieldMap(ps, true);

      var freezeThawIncrStep = incrFreezeThawCycles ? 1 : 0;
      $scope.aliquotSpec = new Specimen({
        lineage: 'Aliquot',
        cpId: ps.cpId,
        ppid: ps.ppid,
        specimenClass: ps.specimenClass,
        type: ps.type,
        createdOn: Date.now(),
        parentId: ps.id,
        parentLabel: ps.label,
        visitId: ps.visitId,
        anatomicSite: ps.anatomicSite,
        laterality: ps.laterality,
        pathology: ps.pathology,
        collectionContainer: ps.collectionContainer,
        freezeThawCycles: ps.freezeThawCycles + freezeThawIncrStep,
        incrParentFreezeThaw: freezeThawIncrStep,
        labelFmt: cpr.aliquotLabelFmt,
        createDerived: createDerived,
        parent: new Specimen({
          id: ps.id,
          label: ps.label,
          availableQty: ps.availableQty,
          createdOn: ps.createdOn,
          specimenClass: ps.specimenClass,
          type: ps.type,
          lineage: ps.lineage
        })
      });

      if (hasSde) {
        var groups = SpecimenUtil.sdeGroupSpecimens(
          cpDict, aliquotFields || [], [$scope.aliquotSpec], {userRole: userRole});
        if (groups.length == 1 && !groups[0].noMatch) {
          SpecimensHolder.setSpecimens([specimen]);
          $state.go('specimen-bulk-create-aliquots', {}, {location: 'replace'});
          return;
        }
      }

      $scope.showForm = true;

      //
      // On successful collection of aliquots, direct user to specimen detail view
      // TODO: where to go should be state input param
      //
      if ($rootScope.stateChangeInfo.fromState.url.indexOf("collect-specimens") == 1) {
        var params = {specimenId:  $scope.parentSpecimen.id, srId:  $scope.parentSpecimen.reqId};
        $state.go('specimen-detail.overview', params);
        return;
      }

      var exObjs = [
        'specimen.label', 'specimen.barcode', 'specimen.lineage',
        'specimen.parentLabel', 'specimen.initialQty',
        'specimen.availableQty', 'specimen.storageLocation',
        'specimen.events', 'specimen.collectionEvent', 'specimen.receivedEvent'
      ];

      if (hasDict) {
        $scope.spmnCtx = {
          aobj: {cpr: cpr, visit: visit, specimen: $scope.aliquotSpec, userRole: userRole},
          ainObjs: ['specimen'], aexObjs: exObjs,
          aopts: {onValueChange: onValueChangeCb}
        }
      } else {
        $scope.aextnOpts = ExtensionsUtil.getExtnOpts($scope.aliquotSpec, extensionCtxt);
      }

      $scope.adeFormCtrl = {};
    }

    function getState() {
      var stateInfo = $scope.stateChangeInfo;
      if (stateInfo && stateInfo.fromState) {
        return {state: stateInfo.fromState, params: stateInfo.fromParams};
      } else {
        return {state: $state.current, params: $stateParams};
      }
    }

    $scope.toggleIncrParentFreezeThaw = function() {
      if ($scope.aliquotSpec.incrParentFreezeThaw) {
        if ($scope.parentSpecimen.freezeThawCycles == $scope.aliquotSpec.freezeThawCycles) {
          $scope.aliquotSpec.freezeThawCycles = $scope.parentSpecimen.freezeThawCycles + 1;
        }
      } else {
        if (($scope.parentSpecimen.freezeThawCycles + 1) == $scope.aliquotSpec.freezeThawCycles) {
          $scope.aliquotSpec.freezeThawCycles = $scope.parentSpecimen.freezeThawCycles;
        }
      }
    }

    $scope.onChange = function(fieldName) {
      var ctx = $scope.spmnCtx;
      if (ctx && ctx.aopts && ctx.aopts.$$sdeFormFields) {
        ctx.aopts.$$sdeFormFields.valueChanged(undefined, ctx.aobj, 'specimen.' + fieldName, undefined);
      }
    }

    $scope.collectAliquots = function() {
      $scope.deFormCtrl = $scope.adeFormCtrl;
      var specimens = SpecimenUtil.collectAliquots($scope);
      if (specimens) {
        var opts = {ignoreQtyWarning: true, showCollVisitDetails: false};
        CollectSpecimensSvc.collect(getState(), $scope.visit, specimens, opts);
      }
    }

    init();
  });
