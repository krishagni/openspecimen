angular.module('os.biospecimen.specimen')
  .controller('BulkCreateDerivativesCtrl', function(
    $scope, $state, $injector, $translate, $q, userRole, parentSpmns, cp, cpr,
    cpDict, derivedFields, spmnHeaders, incrFreezeThawCycles, containerAllocRules,
    Specimen, Alerts, Util, SpecimenUtil, Container, ExtensionsUtil, SpecimensHolder) {

    var ctx, reservationId;

    function init() {
      var createdOn = new Date().getTime();
      var freezeThawIncrStep = incrFreezeThawCycles ? 1 : 0;

      var derivedSpmns = parentSpmns.map(
        function(ps) {
          return new Specimen({
            lineage: 'Derived',
            cpId: ps.cpId,
            ppid: ps.ppid,
            parentId: ps.id,
            parentLabel: ps.label,
            parentType: ps.type,
            parentCreatedOn: ps.createdOn,
            createdOn: createdOn,
            status: 'Collected',
            freezeThawCycles: ps.freezeThawCycles + freezeThawIncrStep,
            incrParentFreezeThaw: freezeThawIncrStep,
            parent: new Specimen(ps)
          });
        }
      );

      $scope.cp = cp;
      $scope.cpr = cpr;
      var inputLabels = $scope.inputLabels = (!!cp.id && (!cp.derivativeLabelFmt || cp.manualSpecLabelEnabled));
      ctx = $scope.ctx = {
        showCustomFields: true,
        autoPosAllocate: false,
        manualPosSelection: false,
        derivedSpmns: derivedSpmns,
        inputLabels: inputLabels,
        spmnHeaders: spmnHeaders,
        warnPerfIssue: derivedSpmns.length > 50
      };

      var opts = $scope.opts = {
        viewCtx: $scope, static: false,
        showRowCopy: true, hideFooterActions: true, allowBulkUpload: false
      };

      var groups = ctx.customFieldGroups = SpecimenUtil.sdeGroupSpecimens(
        cpDict, derivedFields || [], derivedSpmns, {userRole: userRole}, opts);
      ctx.warnNoMatch = groups.length > 1 && groups[groups.length - 1].noMatch;
      ctx.showCustomFields = (groups.length > 1) || (groups.length == 1 && !groups[0].noMatch);
      if (ctx.showCustomFields) {
        $scope.$watch(
          function() {
            return groups.map(
              function(group) {
                var types = [];
                angular.forEach(group.input, function(input) { types.push(input.specimen.type); });
                return types;
              }
            );
          },
          function() {
            countRows(groups);
          },
          true
        );

        if (!!cp.containerSelectionStrategy) {
          ctx.autoPosAllocate    = true;
          ctx.manualPosSelection = false;
          //
          // exclude the location field from the UI form
          //
          angular.forEach(groups,
            function(group) {
              group.fields.table = group.fields.table.filter(
                function(f) {
                  return f.name != 'specimen.storageLocation';
                }
              );
            }
          )

          $scope.$on('$destroy', vacateReservedPositions);
        }
      }
    }

    function countRows(groups) {
      angular.forEach(groups,
        function(group) {
          var typeCounts = {};
          angular.forEach(group.input,
            function(input) {
              var count = typeCounts[input.specimen.type || 'Not Specified'] || 0;
              typeCounts[input.specimen.type || 'Not Specified'] = count + 1;
            }
          );

          var totalRows = 0;
          var result = '';
          angular.forEach(Object.keys(typeCounts).sort(),
            function(t) {
              if (result) {
                result += ' | ';
              }

              result += t + ': ' + typeCounts[t];
              totalRows += typeCounts[t];
            }
          );

          result = $translate.instant('common.total_rows') + ': ' + totalRows + ' | ' + result;
          group.$$counts = result;
        }
      );
    }

    function initCloseParent(parentSamples) {
      angular.forEach(parentSamples,
        function(samples) {
          var closeParent = false;
          angular.forEach(samples,
            function(sample) {
              closeParent = closeParent || sample.specimen.closeParent;
              sample.specimen.closeParent = false;
            }
          );

          samples[samples.length - 1].specimen.closeParent = closeParent;
        }
      );
    }

    function submitSamples() {
      var parentSamples = {};

      var samples = [];
      for (var i = 0; i < ctx.customFieldGroups.length; ++i) {
        var group = ctx.customFieldGroups[i];
        for (var j = 0; j < group.input.length; ++j) {
          var spec = group.input[j];
          var spmn = spec.specimen;
          if (!isValidCreatedOn(spmn)) {
            return;
          }

          var sample = {specimen: spmn, events: spec.events};
          samples.push(sample);

          parentSamples[spmn.parent.id] = parentSamples[spmn.parent.id] || [];
          parentSamples[spmn.parent.id].push(sample);
        }
      }

      initCloseParent(parentSamples);
      $injector.get('sdeSample').saveSamples(samples).then(
        function(resp) {
          Alerts.success('specimens.derivatives_created');

          var createAliquotsOf = [];
          for (var i = 0; i < samples.length; ++i) {
            if (samples[i].specimen.createAliquots) {
              ExtensionsUtil.createExtensionFieldMap(resp.samples[i], true);
              createAliquotsOf.push(resp.samples[i]);
            }
          }

          if (createAliquotsOf.length > 0) {
            SpecimensHolder.setSpecimens(createAliquotsOf);
            $state.go('specimen-bulk-create-aliquots', {}, {location: 'replace'});
          } else {
            $scope.back();
          }
        }
      );
    }

    function isValidCreatedOn(spmn) {
      if (spmn.createdOn > new Date().getTime()) {
        Alerts.error("specimens.errors.children_created_on_gt_curr_time", {parentLabel: spmn.parentLabel});
        return false;
      } else {
        return true;
      }
    }

    function setLocations(spmns, locations) {
      var locationIdx = 0, derivatives = [];
      for (var i = 0; i < spmns.length; ++i) {
        var derivative = spmns[i];
        if (locations.length > 0) {
          derivative.storageLocation = locations[locationIdx++];
        }

        derivatives.push(derivative);
      }

      ctx.derivatives = derivatives;
    }

    function vacateReservedPositions() {
      if (!!reservationId) {
        return Container.cancelReservation(reservationId);
      }

      return null;
    }

    $scope.copyFirstToAll = function() {
      var spmnToCopy = $scope.ctx.derivedSpmns[0];
      var attrsToCopy = ['specimenClass', 'type', 'initialQty', 'createdOn', 'printLabel', 'closeParent'];
      Util.copyAttrs(spmnToCopy, attrsToCopy, $scope.ctx.derivedSpmns);
      SpecimenUtil.copyContainerName(spmnToCopy, $scope.ctx.derivedSpmns);
    }

    $scope.removeSpecimen = function(index) {
      $scope.ctx.derivedSpmns.splice(index, 1);
      if ($scope.ctx.derivedSpmns.length == 0) {
        $scope.back();
      }
    }

    $scope.addAnother = function(group) {
      group.input.push(angular.copy(group.lastRow));
    }

    $scope.validateSpecs = function() {
      var criteria = [], specs = [];
      angular.forEach(ctx.customFieldGroups,
        function(group) {
          angular.forEach(group.input,
            function(spec) {
              specs.push(spec.specimen);

              var spmn = new Specimen(spec.specimen);
              var parent = spmn.parent;

              spmn.lineage = 'Derived';
              spmn.quantity = spmn.initialQty;

              var match = spmn.getMatchingRule(containerAllocRules);
              criteria.push({
                specimen: spmn,
                minFreePositions: 1,
                ruleName: match.index != -1 ? match.rule.name : undefined,
                ruleParams: match.index != -1 ? match.rule.params : undefined
              });
            }
          );
        }
      );

      for (var i = 0; i < specs.length; ++i) {
        if (!isValidCreatedOn(specs[i])) {
          return false;
        }
      }

      return Container.getReservedPositions({
        cpId: cp.id,
        reservationToCancel: reservationId,
        criteria: criteria,
      }).then(
        function(locations) {
          var location = locations.find(function(l) { return !!l && !!l.reservationId; });
          if (location) {
            reservationId = location.reservationId;
          }

          setLocations(specs, locations);
          return true;
        }
      );
    }

    $scope.showSpecs = function() {
      vacateReservedPositions();
      reservationId = undefined;
      ctx.derivatives.length = 0;
      ctx.autoPosAllocate = !!cp.containerSelectionStrategy
      ctx.manualPosSelection = !ctx.autoPosAllocate;
      return true;
    }

    $scope.manuallySelectContainers = function() {
      $q.when(vacateReservedPositions()).then(
        function() {
          reservationId = undefined;
          angular.forEach(ctx.derivatives,
            function(derivative) {
              derivative.storageLocation = {};
            }
          );

          ctx.manualPosSelection = true;
        }
      );
    }

    $scope.applyFirstLocationToAll = function() {
      var loc = {};
      if (ctx.derivatives.length > 0 && !!ctx.derivatives[0].storageLocation) {
        loc = ctx.derivatives[0].storageLocation;
      }

      angular.forEach(ctx.derivatives,
        function(derivative, idx) {
          if (idx != 0) {
            derivative.storageLocation = {name: loc.name, mode: loc.mode, displayTitle: loc.displayTitle};
          }
        }
      );
    }

    $scope.createDerivatives = function() {
      if (ctx.showCustomFields) {
        submitSamples();
        return;
      }

      var result = [];
      for (var i = 0; i < ctx.derivedSpmns.length; ++i) {
        var spmn = angular.copy(ctx.derivedSpmns[i]);
        if (!isValidCreatedOn(spmn)) {
          return;
        }

        delete spmn.ppid;
        delete spmn.parentCreatedOn;
        delete spmn.parentType;

        if (spmn.closeParent) {
          result.push(new Specimen({
            id: spmn.parentId,
            status: 'Collected',
            children: [spmn],
            closeAfterChildrenCreation: true
          }));
        } else {
          result.push(spmn);
        }
      }

      Specimen.save(result).then(
        function(resp) {
          Alerts.success('specimens.derivatives_created');
          $scope.back();
        }
      );
    }

    init();
  });
