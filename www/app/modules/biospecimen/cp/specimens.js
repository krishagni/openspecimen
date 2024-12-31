
angular.module('os.biospecimen.cp.specimens', ['os.biospecimen.models'])
  .controller('CpSpecimensCtrl', function(
    $scope, $state, $stateParams, $timeout, $modal, $injector,
    cp, events, specimenRequirements, aliquotQtyReq, barcodingEnabled,
    Specimen, SpecimenRequirement, PvManager, Alerts, Util) {

    if (!$stateParams.eventId && !!events && events.length > 0) {
      $state.go('cp-detail.specimen-requirements', {eventId: events[0].id});
      return;
    }

    function init() {
      $scope.cp = cp;
      $scope.events = events;
      $scope.eventId = $stateParams.eventId;
      $scope.selectEvent({id: $stateParams.eventId});
      $scope.selectedEvent = events.find(function(evt) { return evt.id == $stateParams.eventId; });
      $scope.barcodingEnabled = barcodingEnabled;

      $scope.specimenRequirements = Specimen.flatten(specimenRequirements);
      $scope.aliquotQtyReq = aliquotQtyReq;

      $scope.view = 'list_sr';
      $scope.sr = {};
      $scope.childReq = {};
      $scope.poolReq = {};
      $scope.errorCode = '';
    }

    function sortReqs(reqs) {
      reqs.sort(
        function(r1, r2) {
          if (r1.sortOrder != null && r2.sortOrder != null) {
            return r1.sortOrder - r2.sortOrder;
          } else if (r1.sortOrder != null) {
            return -1;
          } else if (r2.sortOrder != null) {
            return 1;
          } else {
            return r1.id - r2.id;
          }
        }
      );

      angular.forEach(reqs, function(req) { req.children = sortReqs(req.children); });
      return reqs;
    }

    function addToSrList(sr) {
      specimenRequirements.push(sr);
      $scope.specimenRequirements = Specimen.flatten(sortReqs(specimenRequirements));
    }

    function updateSrList(sr) {
      var result = findSr(specimenRequirements, sr.id);
      angular.extend(result.sr, sr);
      $scope.specimenRequirements = Specimen.flatten(sortReqs(specimenRequirements));
    }

    function deleteFromSrList(sr) {
      var result = findSr(specimenRequirements, sr.id);
      result.list.splice(result.idx, 1);
      $scope.specimenRequirements = Specimen.flatten(sortReqs(specimenRequirements));
    }

    function findSr(srList, srId) {
      if (!srList) {
        return undefined;
      }

      for (var i = 0; i < srList.length; ++i) {
        if (srList[i].id == srId) {
          return {list: srList, sr: srList[i], idx: i};
        }
        var result = findSr(srList[i].children, srId);
        if (!!result) {
          return result;
        }

        result = findSr(srList[i].specimensPool, srId);
        if (!!result) {
          return result;
        }
      }

      return undefined;
    }

    function addChildren(parent, children) {
      if (!parent.children) {
        parent.children = [];
      }

      angular.forEach(children, function(child) {
        parent.children.push(child);
      });

      $scope.specimenRequirements = Specimen.flatten(sortReqs(specimenRequirements));
    };

    var pvsLoaded = false;
    function loadPvs() {
      if (pvsLoaded) {
        return;
      }

      $scope.storageTypes = PvManager.getPvs('storage-type');

      loadLabelAutoPrintModes();
      pvsLoaded = true;
    };
    
    function loadLabelAutoPrintModes() {
      $scope.primaryLabelAutoPrintModes = [];
      $scope.childLabelAutoPrintModes   = [];

      PvManager.loadPvs('specimen-label-auto-print-modes').then(
        function(pvs) {
          if ($scope.cp.spmnLabelPrePrintMode == 'NONE' && !$injector.has('Supply')) {
            pvs = pvs.filter(function(pv) { return pv.name != 'PRE_PRINT'; });
          }

          $scope.primaryLabelAutoPrintModes = pvs;
          $scope.childLabelAutoPrintModes = pvs.filter(function(pv) { return pv.name != 'ON_RECEIVE'; });
        }
      );
    }

    function removeUiProps(sr) {
      delete sr.labelFmtSpecified;
      angular.forEach(sr.specimensPool,
        function(poolSpmn) {
          delete poolSpmn.labelFmtSpecified;
        }
      );

      return sr;
    }

    function cloneSr(sr) {
      var delAttrs = [
        'depth', 'hasChildren', 'children', 'isOpened', 'parent',
        'pooledSpecimen', 'specimensPool'
      ];

      var result = angular.copy(sr);
      angular.forEach(delAttrs, function(attr) { delete result[attr]; });
      return result;
    }

    $scope.openSpecimenNode = function(sr) {
      sr.isOpened = true;
    };

    $scope.closeSpecimenNode = function(sr) {
      sr.isOpened = false;
    };

    $scope.showAddSr = function() {
      $scope.view = 'addedit_sr';
      $scope.sr = new SpecimenRequirement({eventId: $scope.eventId, cpShortTitle: cp.shortTitle, cpId: cp.id});
      loadPvs();
    };

    $scope.showEditSr = function(sr) {
      $scope.specimensCount = 0;
      $scope.sr = cloneSr(sr);
      $scope.sr.$$storedInRepo = (sr.storageType != 'Virtual');

      if (sr.isAliquot()) {
        $scope.view = 'addedit_aliquot';
        $scope.parentSr = sr.parent;
        $scope.childReq = $scope.sr;
      } else if (sr.isDerivative()) {
        $scope.view = 'addedit_derived';
        $scope.parentSr = sr.parent;
        $scope.childReq = $scope.sr;
      } else if (!!sr.pooledSpecimen) {
        $scope.view = 'addedit_pool';
        $scope.parentSr = sr.pooledSpecimen;
        $scope.poolReq = $scope.sr;
      } else {
        $scope.view = 'addedit_sr';
      }

      $scope.sr.getSpecimensCount().then(
        function(count) {
          $scope.specimensCount = count;
        }
      );
      $scope.sr.initialQty = Util.getNumberInScientificNotation($scope.sr.initialQty);
      loadPvs();
    };

    $scope.viewSr = function(sr) {
      $scope.view = 'view_sr';
      $scope.parentSr = sr.parent;
      $scope.childReq = sr;
    };

    $scope.revertEdit = function() {
      $scope.view = 'list_sr';
      $scope.parentSr = null;
      $scope.childReq = {};
      $scope.poolReq= {};
      $scope.sr = {};
    };

    $scope.createSr = function() {
      $scope.sr.storageType = ($scope.sr.$$storedInRepo ? 'Manual' : 'Virtual');
      removeUiProps($scope.sr).$saveOrUpdate().then(
        function(result) {
          addToSrList(result);
          $scope.view = 'list_sr';
          cp.draftMode = true;
        }
      );
    };

    $scope.updateSr = function() {
      $scope.sr.storageType = ($scope.sr.$$storedInRepo ? 'Manual' : 'Virtual');
      removeUiProps($scope.sr).$saveOrUpdate().then(
        function(result) {
          updateSrList(result);
          $scope.revertEdit();
          cp.draftMode = true;
        }
      );
    };

    $scope.ensureLabelFmtSpecified = function(sr, lineage) {
      sr.labelFmtSpecified = true;

      if (sr.labelAutoPrintMode != 'PRE_PRINT' || !!sr.labelFmt) {
        return;
      }

      if (lineage == 'Aliquot') {
        sr.labelFmtSpecified = !!$scope.cp.aliquotLabelFmt;
      } else if (lineage == 'Derivative') {
        sr.labelFmtSpecified = !!$scope.cp.derivativeLabelFmt;
      } else {
        sr.labelFmtSpecified = !!$scope.cp.specimenLabelFmt;
      }
    }

    ////////////////////////////////////////////////
    //
    //  Aliquot logic
    //
    ////////////////////////////////////////////////
    $scope.showCreateAliquots = function(sr) {
      if (sr.availableQty() == 0) {
        Alerts.error('srs.errors.insufficient_qty');
        return;
      }

      $scope.parentSr = sr;
      $scope.childReq = {
        cpShortTitle: cp.shortTitle,
        specimenClass: sr.specimenClass,
        type: sr.type,
        $$storedInRepo: true,
        storageType: 'Manual'
      };
      $scope.view = 'addedit_aliquot';
      loadPvs();
    };

    $scope.createAliquots = function() {
      var spec = $scope.childReq;
      var availableQty = $scope.parentSr.availableQty();

      if (!!spec.qtyPerAliquot && !!spec.noOfAliquots) {
        var requiredQty = spec.qtyPerAliquot * spec.noOfAliquots;
        if ((requiredQty - availableQty) > 0.000001) {
          Alerts.error("srs.errors.insufficient_qty");
          return;
        }
      } else if (!!spec.qtyPerAliquot) {
        spec.noOfAliquots = Math.floor(availableQty / spec.qtyPerAliquot);
      } else if (!!spec.noOfAliquots && availableQty != null && availableQty != undefined) {
        spec.qtyPerAliquot = Math.round(availableQty / spec.noOfAliquots * 10000) / 10000;
      }

      $scope.parentSr.createAliquots(removeUiProps(spec)).then(
        function(aliquots) {
          addChildren($scope.parentSr, aliquots);
          $scope.parentSr.isOpened = true;

          $scope.childReq = {};
          $scope.parentSr = undefined;
          $scope.view = 'list_sr';
          cp.draftMode = true;
        }
      );
    };

    ////////////////////////////////////////////////
    //
    //  Derivative logic
    //
    ////////////////////////////////////////////////
    $scope.showCreateDerived = function(sr) {
      $scope.parentSr = sr;
      $scope.view = 'addedit_derived';
      $scope.childReq = {
        pathology: sr.pathology,
        anatomicSite: sr.anatomicSite,
        laterality: sr.laterality,
        storageType: 'Virtual'
      };
      loadPvs();
    };

    $scope.createDerivative = function() {
      $scope.parentSr.createDerived(removeUiProps($scope.childReq)).then(
        function(derived) {
          addChildren($scope.parentSr, [derived]);
          $scope.parentSr.isOpened = true;

          $scope.childReq = {};
          $scope.parentSr = undefined;
          $scope.view = 'list_sr';
          cp.draftMode = true;
        }
      );
    };

    $scope.showCreatePoolSpecimen = function(sr) {
      $scope.parentSr = sr;
      $scope.view = 'addedit_pool';
      $scope.poolReq = new SpecimenRequirement({cpShortTitle: cp.shortTitle, eventId: $scope.eventId});
    };

    $scope.showEditPooledSpmn = function(sr) {
      $scope.parentSr = sr.parent;
      $scope.view = 'addedit_pool';
      $scope.pooledReq.spmn = sr;
    };

    $scope.addToSpmnPool = function() {
      $scope.parentSr.addPoolSpecimens([removeUiProps($scope.poolReq)]).then(
        function(poolSpmns) {
          $scope.parentSr.specimensPool = $scope.parentSr.specimensPool.concat(poolSpmns);
          $scope.parentSr.isOpened = true;

          $scope.poolReq = {};
          $scope.parentSr = undefined;
          $scope.view = 'list_sr';
          $scope.specimenRequirements = Specimen.flatten(specimenRequirements);
          cp.draftMode = true;
        }
      );
    };
        
    $scope.copySr = function(sr) {
      var aliquotReq = {noOfAliquots: 1, qtyPerAliquot: sr.initialQty};
      if (sr.isAliquot() && !sr.parent.hasSufficientQty(aliquotReq)) {
        Alerts.error('srs.errors.insufficient_qty');
        return;
      }
      
      sr.copy().then(
        function(result) {
          if (sr.pooledSpecimen) {
            sr.pooledSpecimen.specimensPool.push(result);
            $scope.specimenRequirements = Specimen.flatten(specimenRequirements);
          } else if (sr.parent) {
            addChildren(sr.parent, [result]);
          } else {
            addToSrList(result);
          }
          cp.draftMode = true;
        }
      );
    };

    $scope.deleteSr = function(sr) {
      Util.showConfirm({
        templateUrl: 'modules/biospecimen/cp/delete_sr.html',
        ok: function() {
          sr.delete().then(
            function() {
              deleteFromSrList(sr);
              cp.draftMode = true;
            }
          );
        }
      });
    }

    $scope.closeSr = function(sr) {
      Util.showConfirm({
        templateUrl: 'modules/biospecimen/cp/close_sr.html',
        ok: function() {
          var toClose = cloneSr(sr);
          toClose.activityStatus = 'Closed';

          removeUiProps(toClose).$saveOrUpdate().then(
            function(result) {
              updateSrList(result);
              cp.draftMode = true;
            }
          );
        }
      });
    }

    $scope.reopenSr = function(sr) {
      var toOpen = cloneSr(sr);
      toOpen.activityStatus = 'Active';

      removeUiProps(toOpen).$saveOrUpdate().then(
        function(result) {
          updateSrList(result);
          cp.draftMode = true;
        }
      );
    }

    init();
  });
