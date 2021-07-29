
angular.module('os.biospecimen.specimen.addderivative', [])
  .controller('AddDerivativeCtrl', function(
    $scope, $state, userRole, cp, specimen, cpr, visit, extensionCtxt, cpDict, derivedFields,
    incrFreezeThawCycles, hasSde, hasDict, onValueChangeCb,
    Specimen, SpecimensHolder, SpecimenUtil, Container, ExtensionsUtil, Alerts) {

    function init() {
      $scope.showForm = false;
      $scope.cpr = cpr;
      $scope.visit = visit;

      var ps = $scope.parentSpecimen = specimen;
      ExtensionsUtil.createExtensionFieldMap(ps, true);

      var opts = {incrFreezeThawCycles: incrFreezeThawCycles};
      var derivative = $scope.derivative = SpecimenUtil.getNewDerivative($scope, opts);
      derivative.labelFmt = cpr.derivativeLabelFmt;
      derivative.parent = new Specimen(ps);

      if (hasSde) {
        var groups = SpecimenUtil.sdeGroupSpecimens(cpDict, derivedFields || [], [derivative], {userRole: userRole});
        if (groups.length == 1 && !groups[0].noMatch) {
          delete specimen.children;
          SpecimensHolder.setSpecimens([specimen]);
          $state.go('specimen-bulk-create-derivatives', {}, {location: 'replace'});
          return;
        }
      }

      var exObjs = [
        'specimen.lineage', 'specimen.parentLabel', 'specimen.events',
        'specimen.collectionEvent', 'specimen.receivedEvent'
      ];

      if (hasDict) {
        $scope.spmnCtx = {
          obj: {specimen: $scope.derivative, userRole: userRole},
          inObjs: ['specimen'], exObjs: exObjs,
          opts: {onValueChange: onValueChangeCb}
        }
      } else {
        $scope.extnOpts = ExtensionsUtil.getExtnOpts($scope.derivative, extensionCtxt);
      }

      $scope.deFormCtrl = {};
      $scope.showForm = true;
    }

    $scope.toggleIncrParentFreezeThaw = function() {
      if ($scope.derivative.incrParentFreezeThaw) {
        if ($scope.parentSpecimen.freezeThawCycles == $scope.derivative.freezeThawCycles) {
          $scope.derivative.freezeThawCycles = $scope.parentSpecimen.freezeThawCycles + 1;
        }
      } else {
        if (($scope.parentSpecimen.freezeThawCycles + 1) == $scope.derivative.freezeThawCycles) {
          $scope.derivative.freezeThawCycles = $scope.parentSpecimen.freezeThawCycles;
        }
      }
    };

    $scope.createDerivative = function() {
      SpecimenUtil.createDerivatives($scope);
    };

    $scope.revertEdit = function () {
      $scope.back();
    }

    init();
  });
