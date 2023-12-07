
angular.module('os.biospecimen.participant.collect-specimens', ['os.biospecimen.models'])
  .factory('CollectSpecimensSvc', function(
    $state, $parse, CpConfigSvc, Specimen, Container, ParticipantSpecimensViewState, LocationChangeListener, Alerts) {

    var data = {opts: {}};

    function getReservePositionsOp(cpId, cprId, allocRules, specimens) {
      var aliquots = {}, result = [];

      angular.forEach(specimens,
        function(specimen) {
          if (specimen.storageType == 'Virtual' || (!!specimen.status && specimen.status != 'Pending')) {
            return;
          }

          angular.extend(specimen, {cpId: cpId, cprId: cprId});
          var match = specimen.getMatchingRule(allocRules);

          var selectorCrit;
          if (specimen.lineage == 'Aliquot') {
            var type = (specimen.specimenClass || 'u') + '-' + (specimen.type || 'u');
            var key = 'u-' + type + '-' + match.index;
            if (specimen.parentUid) {
              key = specimen.parentUid + '-' + type + '-' + match.index;
            }

            selectorCrit = aliquots[key];
            if (!selectorCrit) {
              aliquots[key] = selectorCrit = getSelectorCriteria(match.rule, cpId, cprId, specimen);
              result.push(selectorCrit);
            }
          } else {
            selectorCrit = getSelectorCriteria(match.rule, cpId, cprId, specimen);
            result.push(selectorCrit);
          }

          selectorCrit.minFreePositions++;
          selectorCrit.$$group.push(specimen);
        }
      );

      return {cpId: cpId, criteria: result};
    }

    function getSelectorCriteria(allocRule, cpId, cprId, specimen) {
      var selectorCrit = {
        specimen: angular.extend({}, specimen),
        minFreePositions: 0,
        ruleName:   (allocRule && allocRule.name) || undefined,
        ruleParams: (allocRule && allocRule.params) || undefined,
        '$$group': []
      };

      cleanupSpecimens([selectorCrit.specimen]);
      return selectorCrit;
    }

    function assignReservedPositions(resvOp, positions) {
      var idx = 0;
      angular.forEach(resvOp.criteria,
        function(selectorCriteria) {
          angular.forEach(selectorCriteria.$$group,
            function(specimen) {
              specimen.storageLocation = positions[idx++];
            }
          );
        }
      );
    }

    //
    // opts: {ignoreQtyWarning: [true | false], showCollVisitDetails: [true | false]}
    //
    function collect(stateDetail, visit, specimens, opts, navigateToCollPage) {
      data.stateDetail = angular.copy(stateDetail);
      data.visit = visit;
      data.specimens = specimens;
      data.opts = opts || {};

      assignUids(specimens);
      allocatePositions(visit, specimens).then(function() { gotoCollectionPage(visit, navigateToCollPage); });
    }

    function assignUids(specimens) {
      angular.forEach(specimens,
        function(specimen, index) {
          specimen.uid = index;
          if (specimen.parent) {
            specimen.parentUid = specimen.parent.uid;
          }
        }
      );
    }

    function allocatePositions(visit, specimens, reservationToCancel) {
      return CpConfigSvc.getWorkflowData(visit.cpId, 'auto-allocation').then(
        function(data) {
          if (!angular.equals(data, {})) {
            return allocatePositions0(visit, specimens, reservationToCancel, data);
          }

          return CpConfigSvc.getWorkflowData(-1, 'auto-allocation').then(
            function(sysData) {
              return allocatePositions0(visit, specimens, reservationToCancel, sysData);
            }
          );
        }
      );
    }

    function allocatePositions0(visit, specimens, reservationToCancel, data) {
      var resvOp = getReservePositionsOp(visit.cpId, visit.cprId, data.rules || [], specimens);
      resvOp.reservationToCancel = reservationToCancel;
      return Container.getReservedPositions(resvOp).then(
        function(positions) {
          if (positions.length > 0) {
            assignReservedPositions(resvOp, positions);
          }

          return true;
        }
      );
    }

    function cancelReservation(specimens) {
      var reservationId;
      for (var i = 0; i < specimens.length; ++i) {
        var loc = specimens[i].storageLocation;
        if (loc && loc.reservationId) {
          reservationId = loc.reservationId;
          break;
        }
      }

      if (reservationId) {
        Container.cancelReservation(reservationId);
      }
    }

    function gotoCollectionPage(visit, navigateToCollPage) {
      CpConfigSvc.getWorkflowData(visit.cpId, 'specimenCollection').then(
        function(wfData) {
          if (wfData && !angular.equals(wfData, {})) {
            gotoCollectionPage0(visit, wfData, navigateToCollPage);
            return;
          }

          CpConfigSvc.getWorkflowData(-1, 'specimenCollection').then(
            function(wfData) {
              gotoCollectionPage0(visit, wfData, navigateToCollPage);
            }
          );
        }
      );
    }

    function gotoCollectionPage0(visit, wfData, navigateToCollPage) {
      var params = {cpId: visit.cpId, cprId: visit.cprId, visitId: visit.id, eventId: visit.eventId};
      var state = 'tree';
      if (wfData.showCollectionTree == false || wfData.showCollectionTree == 'false') {
        state = 'nth-step';
        data.opts.hierarchical =  true;
      }

      data.opts.showVisitDetails    = wfData.showVisitDetails != 'false' && wfData.showVisitDetails != false;
      data.opts.defVisitDate        = wfData.defVisitDate;
      data.opts.showCollectionEvent = wfData.showCollectionEvent != 'false' && wfData.showCollectionEvent != false;
      data.opts.showReceivedEvent   = wfData.showReceivedEvent != 'false' && wfData.showReceivedEvent != false;
      data.opts.defReceiveQuality   = wfData.defReceiveQuality;
      data.opts.defCollectionDate   = wfData.defCollectionDate; // current_date or visit_date
      data.opts.defCollectionStatus = wfData.defCollectionStatus || {};
      data.opts.treeColumns         = wfData.treeColumns;

      var cs = data.opts.defCollectionStatus;
      if (typeof cs == 'string') {
        data.opts.defCollectionStatus = {'New': cs, 'Derived': cs, 'Aliquot': cs};
      }

      if (navigateToCollPage) {
        navigateToCollPage(state, params);
      } else {
        $state.go('participant-detail.collect-specimens.' + state, params);
      }
    }

    function isAnyChildOrPoolSpecimenPending(spmn) {
      if (!spmn.status || spmn.status == 'Pending') {
        return true;
      }

      if ((spmn.specimensPool || []).some(isAnyChildOrPoolSpecimenPending)) {
        return true;
      }

      if ((spmn.children || []).some(isAnyChildOrPoolSpecimenPending)) {
        return true;
      }

      return false;
    }

    function clear() {
      data.stateDetail = undefined;
      data.visit = undefined;
      data.specimens = [];
    }

    function cleanupSpecimens(spmns) {
      var attrsToDelete = [
        'hasChildren', 'parent', 'children', 'depth',
        'hasOnlyPendingChildren', 'isOpened', 'selected',
        'aliquotGrp', 'grpLeader', 'pLabel', 'pBarcode', 'pInitialQty',
        'isVirtual', 'existingStatus', 'showInTree',
        'expanded', 'aliquotLabels', 'aliquotBarcodes', 'removed'
      ];

      angular.forEach(spmns,
        function(spmn) {
          angular.forEach(attrsToDelete,
            function(attr) {
              delete spmn[attr];
            }
          );
        }
      );
    }


    return {
      collect: collect,

      collectPending: function(returnState, cp, cprId, visit, navigateToCollPage) {
        var visitDetail = {visitId: visit.id, eventId: visit.eventId};
        Specimen.listFor(cprId, visitDetail).then(
          function(specimens) {
            var spmnsToCollect = [];

            angular.forEach(Specimen.flatten(specimens),
              function(spmn) {
                if (isAnyChildOrPoolSpecimenPending(spmn)) {
                  spmn.isOpened = spmn.selected = true;
                  spmnsToCollect.push(spmn);
                }
              }
            );

            visit.cprId = cprId;
            collect(returnState, visit, spmnsToCollect, undefined, navigateToCollPage);
          }
        );
      },

      collectVisit: function(returnState, cp, cprId, visit, navigateToCollPage) {
        var visitDetail = {visitId: visit.id, eventId: visit.eventId};
        Specimen.listFor(cprId, visitDetail).then(
          function(specimens) {
            if (specimens.length == 0) {
              if (!visit.id || (!visit.status || visit.status == 'Pending')) {
                $state.go('visit-addedit', {cprId: cprId, visitId: visit.id, eventId: visit.eventId});
              } else {
                Alerts.info('visits.no_planned_coll');
                $state.go('visit-detail.overview', {cprId: cprId, visitId: visit.id, eventId: visit.eventId});
              }

              return;
            }

            var spmnsToCollect = Specimen.flatten(specimens);
            if (cp.visitCollectionMode == 'PRIMARY_SPMNS') {
              spmnsToCollect = spmnsToCollect.filter(function(spmn) { return spmn.lineage == 'New'; });
            }

            angular.forEach(spmnsToCollect,
              function(specimen) {
                specimen.isOpened = specimen.selected = true;
              }
            );

            visit.cprId = cprId;
            collect(returnState, visit, spmnsToCollect, undefined, navigateToCollPage);
          }
        );
      },

      allocatePositions: allocatePositions,

      cancelReservation: cancelReservation,

      clear: clear,

      setData: function(input) {
        angular.extend(data, input);
      },

      getSpecimens: function() {
        return data.specimens || []; 
      },

      getVisit: function() {
        return data.visit;
      },

      getStateDetail: function() {
        return data.stateDetail;
      },

      ignoreQtyWarning: function() {
        return data.opts.ignoreQtyWarning || false;
      },

      hierarchical: function() {
        return data.opts.hierarchical || false;
      },

      showCollVisitDetails: function() {
        return data.opts.showCollVisitDetails !== false;
      },

      defReceiveQuality: function() {
        return data.opts.defReceiveQuality;
      },

      opts: function() {
        return data.opts || {};
      },

      navigateTo: function(scope, visit, gotoVisit) {
        ParticipantSpecimensViewState.specimensUpdated(scope);

        var sd = data.stateDetail || {};
        if (sd.state && sd.state.name) {
          $state.go(sd.state.name, angular.extend(sd.params, {visitId: visit.id}));
        } else if (gotoVisit && visit && visit.id > 0) {
          $state.go('visit', {visitId: visit.id});
        } else {
          LocationChangeListener.back();
        }

        clear();
      },

      cleanupSpecimens: cleanupSpecimens
    };
  })
  .directive('osCollectSpecimens', function(
      $translate, $state, $document, $q, $parse, $injector, $modal,
      Visit, Specimen, PvManager, CollectSpecimensSvc, Container, ExtensionsUtil,
      CpConfigSvc, Alerts, Util, SpecimenUtil, AuthorizationService, ApiUrls) {

    return {
      restrict: 'E',

      templateUrl: 'modules/biospecimen/participant/collect-specimens-tree.html',

      scope: {
        cp: '=',
        cpr: '=',
        visit: '=',
        latestVisit: '=',
        cpDict: '=',
        spmnCollFields: '=',
        barcodingEnabled: '=',
        spmnBarcodesAutoGen: '=',
        mrnAccessRestriction: '=',
        onSave: '&',
        onCancel: '&'
      },

      controller: function($scope) {
        var ignoreQtyWarning = false;
        var cp = $scope.cp,
            cpr = $scope.cpr,
            visit = $scope.visit,
            latestVisit = $scope.latestVisit,
            cpDict = $scope.cpDict,
            spmnCollFields = $scope.spmnCollFields,
            barcodingEnabled = $scope.barcodingEnabled,
            spmnBarcodesAutoGen = $scope.spmnBarcodesAutoGen,
            mrnAccessRestriction = $scope.mrnAccessRestriction;

        function init() {
          var autoInit = spmnCollFields.autoInitVisit != 'false' && spmnCollFields.autoInitVisit != false;

          ignoreQtyWarning = CollectSpecimensSvc.ignoreQtyWarning();
          $scope.showCollVisitDetails = CollectSpecimensSvc.showCollVisitDetails();
          var uiOpts = $scope.uiOpts  = CollectSpecimensSvc.opts();

          var sites = cp.cpSites.map(function(cpSite) { return cpSite.siteName; });
          $scope.storeSpmnsAllowed = AuthorizationService.isAllowed(
            {sites: sites, resource: 'StorageContainer', operations: ['Read']});

          if (!uiOpts.treeColumns) {
            uiOpts.treeColumns = getDefaultTreeColumns($scope.storeSpmnsAllowed);
          } else {
            var ctx = {cp: cp, cpr: cpr, visit: visit, userRole: AuthorizationService.getRole(cp)}
            uiOpts.treeColumns = getTreeColumns(uiOpts.treeColumns, ctx, $scope.storeSpmnsAllowed);
          }

          var printSettings = {};
          angular.forEach(cp.spmnLabelPrintSettings,
            function(setting) {
              printSettings[setting.lineage] = setting.printMode;
            }
          );

          $scope.global = ui.os.global;
          $scope.specimens = CollectSpecimensSvc.getSpecimens().map(
            function(specimen) {
              specimen.hasChildren = specimen.hasChildren &&
                (specimen.children || []).some(function(s) { return s.selected; });
              specimen.existingStatus = specimen.status || 'Pending';
              specimen.isVirtual = specimen.showVirtual();
              specimen.initialQty = Util.getNumberInScientificNotation(specimen.initialQty);
              if (!specimen.status || specimen.status == 'Pending') {
                specimen.status = uiOpts.defCollectionStatus[specimen.lineage] || 'Collected';
                specimen.printLabel = printLabel(printSettings, specimen);
                if (specimen.lineage == 'New') {
                  var collEvent = specimen.collectionEvent = specimen.collectionEvent || {};
                  if (!collEvent.time) {
                    if (uiOpts.defCollectionDate == 'visit_date') {
                      collEvent.time = visit.visitDate;
                    } else if (uiOpts.defCollectionDate == 'current_date') {
                      collEvent.time = new Date().getTime();
                    }
                  }

                  collEvent.user = specimen.collector || collEvent.user;
                  if (!collEvent.user) {
                    var cu = AuthorizationService.currentUser();
                    collEvent.user = {id: cu.id, firstName: cu.firstName, lastName: cu.lastName};
                  }
                }
              }

              if (specimen.children && specimen.children.some(function(s) { return isPending(s) && s.selected; })) {
                specimen.closeAfterChildrenCreation = cp.closeParentSpecimens;
              }

              if (specimen.closeAfterChildrenCreation) {
                specimen.selected = true;
              }

              specimen.pLabel      = specimen.label;
              specimen.pBarcode    = specimen.barcode;
              specimen.pInitialQty = specimen.initialQty;
              return specimen;
            }
          );

          $scope.showCollector = $scope.specimens.some(function(specimen) { return specimen.lineage == 'New'; });
          if ($scope.showCollVisitDetails) {
            $scope.showCollVisitDetails = ($scope.specimens || []).some(
              function(specimen) {
                return specimen.lineage == 'New'  && specimen.existingStatus == 'Pending';
              }
            );
          }

          if (visit.status != 'Complete') {
            visit.visitDate = uiOpts.defVisitDate == 'none' ? null : new Date();
          } else {
            visit.visitDate = visit.visitDate || (uiOpts.defVisitDate == 'none' ? null : new Date());
          }

          visit.cprId = cpr.id;
          delete visit.anticipatedVisitDate;
          if (!!latestVisit && autoInit) {
            visit.clinicalDiagnoses = latestVisit.clinicalDiagnoses;
          }

          if (!!visit.site) {
            // visit.site = visit.site;
          } else if (latestVisit && autoInit) {
            visit.site = latestVisit.site;
          } else if (cpr.participant.pmis && cpr.participant.pmis.length > 0) {
            visit.site = cpr.participant.pmis[0].siteName;
          }

          $scope.showManifestPrint = !cp.specimenCentric &&
            (ui.os.appProps.plugins || []).indexOf('os-extras') != -1;
          $scope.visit = visit;
          $scope.autoAlloc = {
            enabled: !!$scope.cp.containerSelectionStrategy,
            manual: false,
            rerun: false
          }

          var collDate = null;
          if (uiOpts.defCollectionDate == 'visit_date') {
            collDate = visit.visitDate;
          } else if (uiOpts.defCollectionDate == 'current_date') {
            collDate = new Date();
          }

          $scope.collDetail = {
            collector: undefined,
            collectionDate: collDate,
            receiver: undefined,
            receiveDate: collDate,
            receiveQuality: CollectSpecimensSvc.defReceiveQuality()
          };

          loadPvs();
          initLabelFmts($scope.specimens);
          initAliquotGrps($scope.specimens);
          $scope.$on('$destroy', function() { CollectSpecimensSvc.cancelReservation($scope.specimens); });

          if ($scope.showCollVisitDetails && $scope.uiOpts.showCollectionEvent) {
            $scope.$watch('collDetail.collectionDate',
              function(newVal, oldValue) {
                $scope.updateCollDate(newVal);
              }
            );
          }
        }

        function getDefaultTreeColumns(storeSpmnsAllowed) {
          return {
            description: {width: storeSpmnsAllowed ? 30 : 35},
            label: {width: storeSpmnsAllowed ? 15 : 25},
            quantity: {width: storeSpmnsAllowed ? 10 : 15},
            position: {width: 25},
            status: {width: storeSpmnsAllowed ? 10 : 15},
            printLabel: {},
            closeSpecimen: {}
          }

          return result;
        }

        function getTreeColumns(treeColumns, ctx, storeSpmnsAllowed) {
          var result = {};
          var match = treeColumns.find(
            function(columns) {
              if (!columns.criteria) {
                return true;
              }

              return $parse(columns.criteria)(ctx);
            }
          );

          if (!match) {
            return getDefaultTreeColumns(storeSpmnsAllowed);
          }

          angular.forEach(match.fields,
            function(field) {
              result[field.name] = angular.copy(field);
            }
          );

          if (!result.description) {
            result.description = {width: 30};
          }

          if (result.position && !storeSpmnsAllowed) {
            var availableWidth = result.position.width;

            var count = 0;
            var columns = ['description', 'label', 'quantity', 'status'];
            for (var i = 0; i < columns.length; ++i) {
              if (result[columns[i]] && result[columns[i]].width) {
                ++count;
              }
            }

            if (count > 0) {
              var incrementBy = availableWidth / count;
              for (var j = 0; j < columns.length; ++j) {
                if (result[columns[j]] && result[columns[j]].width) {
                  result[columns[j]].width = result[columns[j]].width + incrementBy;
                }
              }
            }
          }

          return result;
        }

        function isPending(spmn) {
          return !spmn.status || spmn.status == 'Pending';
        }

        function isCollectedOrPending(spmn) {
          return spmn.status == 'Collected' || spmn.status == 'Pending';
        }

        function printLabel(cpPrintSettings, specimen) {
          return (specimen.labelAutoPrintMode == 'ON_COLLECTION') ||
            (!specimen.reqId && cpPrintSettings[specimen.lineage] == 'ON_COLLECTION');
        }

        function initAliquotGrps(specimens) {
          angular.forEach(specimens, function(specimen, $index) {
            if (specimen.parent == undefined || $index == 0) {
              //
              // Either primary specimen or parent of ad-hoc aliquots
              //
              specimen.showInTree = true;
              createAliquotGrp(specimen);
            }
          });

          // Logic of show/hide of aliquots and in the tree
          angular.forEach(specimens, function(specimen) {
            if (!!specimen.grpLeader) { 
              // A aliquot specimen's show/hide is determined by group leader
              return;
            }

            if (specimen.aliquotGrp || specimen.lineage != 'Aliquot') {
              specimen.showInTree = true;
            }

            if (!specimen.aliquotGrp) {
              return;
            }

            var expandGrp = specimen.aliquotGrp.some(
              function(sibling) {
                return sibling.children.length > 0 || specimen.initialQty != sibling.initialQty;
              }
            );

            if (expandGrp) {
              expandOrCollapseAliquotsGrp(specimen, expandGrp);
            }

            initAliquotGrpPrintLabel(specimen);
            specimen.aliquotLabels = getAliquotGrpInputs(specimen, 'label');
            specimen.aliquotBarcodes = getAliquotGrpInputs(specimen, 'barcode');
          });
        }

        function createAliquotGrp(specimen) {
          if (!specimen.children || specimen.children.length == 0) {
            return;
          }

          var aliquotGrp = [];
          var grpLeader = undefined;
          angular.forEach(specimen.children, function(child) {
            if (child.lineage == 'Aliquot' && child.selected && child.existingStatus == 'Pending') {
              aliquotGrp.push(child);

              if (!grpLeader) {
                grpLeader = child;
              } else {
                child.grpLeader = grpLeader;
              }
            }

            createAliquotGrp(child);
          });

          if (grpLeader) {
            grpLeader.$$collectedOrPending = aliquotGrp.some(isCollectedOrPending);
            grpLeader.aliquotGrp = aliquotGrp;
            listenContainerChanges(grpLeader);
          }
        }

        function expandOrCollapseAliquotsGrp(aliquot, expandOrCollapse) {
          if (!aliquot.aliquotGrp) {
            return;
          }

          setShowInTree(aliquot, expandOrCollapse)
          aliquot.expanded = expandOrCollapse;
          if (!aliquot.expanded) {
            aliquot.aliquotLabels = getAliquotGrpInputs(aliquot, 'label');
            aliquot.aliquotBarcodes = getAliquotGrpInputs(aliquot, 'barcode');
            selectUnselectAliquots(aliquot, false);
          } else {
            selectUnselectAliquots(aliquot, true);
          }
        }

        function selectUnselectAliquots(aliquot, select) {
          angular.forEach(aliquot.aliquotGrp, function(spmn) {
            var prop = 'label';

            if ($scope.barcodingEnabled) {
              prop = 'barcode';

              if ($scope.spmnBarcodesAutoGen) {
                return;
              }

            } else if (!cp.manualSpecLabelEnabled && !!spmn.labelFmt) {
              return;
            }

            if (spmn[prop]) {
              spmn.selected = true;
              spmn.removed = false;
            } else if (prop == 'label') {
              spmn.selected = select;
              spmn.removed = !select;
            }
          });
        }

        function initAliquotGrpPrintLabel(aliquot) {
          if (aliquot.expanded) {
            return;
          }

          var printLabel = aliquot.aliquotGrp.some(
            function(sibling) {
              return sibling.printLabel;
            }
          );

          if (!printLabel) {
            return;
          }

          aliquot.printLabel = printLabel;
          setAliquotGrpPrintLabel(aliquot);
        }

        function setAliquotGrpPrintLabel(aliquot) {
          if (aliquot.expanded || !aliquot.aliquotGrp) {
            return;
          }

          angular.forEach(aliquot.aliquotGrp, function(sibling) {
            sibling.printLabel = aliquot.printLabel;
          });
        }

        function getAliquotGrpInputs(specimen, prop) {
          return specimen.aliquotGrp.filter(
            function(s) {
              return !!s[prop];
            }
          ).map(
            function(s) {
              return s[prop];
            }
          ).join(",");
        }

        function setShowInTree(aliquot, showInTree) {
          angular.forEach(aliquot.aliquotGrp, function(specimen) {
            if (specimen == aliquot) {
              return;
            }

            if (showInTree) {
              specimen.showInTree = true;
              showSpecimenInTree(specimen);
            } else {
              hideSpecimenInTree(specimen);
            }
          });
        }

        function showSpecimenInTree(specimen) {
          if (specimen.grpLeader && (!specimen.children || specimen.children.length == 0)) {
            return;
          }

          specimen.showInTree = true;
          angular.forEach(specimen.children, function(child) {
            showSpecimenInTree(child);
          });
        }

        function hideSpecimenInTree(specimen) {
          specimen.showInTree = false;
          if (specimen.children.length > 0) {
            angular.forEach(specimen.children, function(child) {
              hideSpecimenInTree(child);
            });
          }
        }

        function addAliquotsToGrp(grpLeader, newSpmnsCnt) {
          var lastSpmn = grpLeader.aliquotGrp[grpLeader.aliquotGrp.length - 1];

          var newSpmns = [];
          var pos = $scope.specimens.indexOf(lastSpmn);
          for (var i = 0; i < newSpmnsCnt; ++i) {
            var newSpmn = shallowCopy(lastSpmn);
            grpLeader.aliquotGrp.push(newSpmn);
            grpLeader.parent.children.push(newSpmn);
            $scope.specimens.splice(pos + i + 1, 0, newSpmn);
          }
        }

        function shallowCopy(spmn) {
          var attrs = ['id', 'barcode', 'label', 'reqId', 'reqLabel'];
          var copy = new Specimen(spmn);
          attrs.forEach(function(attr) { delete copy[attr]; });

          copy.children = [];

          if (spmn.storageLocation) {
            copy.storageLocation = {name: spmn.storageLocation.name, mode: spmn.storageLocation.mode};
          } else {
            copy.storageLocation = {};
          }

          if (spmn.aliquotGrp) {
            copy.aliquotGrp = undefined;
            copy.grpLeader = spmn;
            copy.expanded = false;
            copy.showInTree = spmn.expanded;
          }

          return copy;
        }

        function removeAliquotsFromGrp(grpLeader, count) {
          var grp = grpLeader.aliquotGrp;
          for (var i = grp.length - 1; i >= 0 && count >= 1; --i, --count) {
            $scope.remove(grp[i]);
          }

          if (!grpLeader.expanded) {
            grpLeader.aliquotLabels = getAliquotGrpInputs(grpLeader, 'label');
            grpLeader.aliquotBarcodes = getAliquotGrpInputs(grpLeader, 'barcode');
          }
        }

        function listenContainerChanges(specimen) {
          $scope.$watch(
            function() {
              return specimen.storageLocation;
            },
            function(newVal, oldVal) {
              if (newVal == oldVal) {
                return;
              }

              if (specimen.expanded || ($scope.autoAlloc.enabled && !$scope.autoAlloc.manual)) {
                return;
              }

              angular.forEach(specimen.aliquotGrp, function(aliquot, $index) {
                if ($index == 0) {
                  return;
                }

                aliquot.storageLocation = {
                  name: specimen.storageLocation.name,
                  mode: specimen.storageLocation.mode
                };
              });
            }
          );
        }

        function loadPvs() {
          $scope.notSpecified = $translate.instant('pvs.not_specified');
          $scope.specimenStatuses = PvManager.getPvs('specimen-status');
        };

        function flatten(specimens, result) {
          angular.forEach(specimens,
            function(specimen) {
              result.push(specimen);
              flatten(specimen.specimensPool, result);
              delete specimen.specimensPool;

              flatten(specimen.children, result);
              delete specimen.children
            }
          );

          return result;
        }

        function collected(reqIds, specimens) {
          return (specimens || []).filter(
            function(specimen) {
              return specimen.status != 'Pending' && (!specimen.reqId || reqIds.indexOf(specimen.reqId) != -1);
            }
          );
        }

        function getSpmnReqIds(spmnsToSave) {
          var result = [];
          angular.forEach(spmnsToSave,
            function(spmn) {
              if (spmn.reqId) {
                result.push(spmn.reqId);
              }

              if (spmn.specimensPool) {
                result = result.concat(getSpmnReqIds(spmn.specimensPool));
              }

              if (spmn.children) {
                result = result.concat(getSpmnReqIds(spmn.children));
              }
            }
          );

          return result;
        }

        function displayCustomFieldGroups(spmnReqIds, specimens) {
          $scope.onSave({
            visit: $scope.visit,
            spmnReqIds: spmnReqIds,
            collectedSpecimens: collected(spmnReqIds, flatten(specimens, []))
          });
        }

        function initLabelFmts(specimens) {
          var noFmtSpmns = specimens.filter(function(s) { return !s.labelFmt; });
          if (noFmtSpmns.length == 0) {
            return;
          }

          CpConfigSvc.getConfig(visit.cpId, 'labelSettings', 'specimen').then(
            function(data) {
              if (!data || !data.rules || data.rules.length == 0) {
                return;
              }

              var rules = data.rules.map(
                function(r) {
                  return {criteria: r.criteria && r.criteria.replace(/#(specimen)|#(cpId)/g, '$1'), format: r.format};
                }
              );

              angular.forEach(noFmtSpmns,
                function(s) {
                  var ctxt = {cpId: visit.cpId, specimen: s};
                  var rule = rules.find(function(r) { return !r.criteria || Util.evaluate(r.criteria, ctxt) === true; });
                  if (rule) {
                    s.labelFmt = rule.format;
                  }
                }
              );
            }
          );
        }

        $scope.openSpecimenNode = function(specimen) {
          specimen.isOpened = true;
        };

        $scope.closeSpecimenNode = function(specimen) {
          specimen.isOpened = false;
        };

        $scope.remove = function(specimen) {
          var idx = $scope.specimens.indexOf(specimen);
          var descCnt = descendentCount(specimen);

          for (var i = idx + descCnt; i >= idx; --i) {
            $scope.specimens[i].selected = false;
            $scope.specimens[i].removed = true;
            $scope.specimens.splice(i, 1);
          }

          if (specimen.grpLeader) {
            var grp = specimen.grpLeader.aliquotGrp;
            var grpIdx = grp.indexOf(specimen);
            grp.splice(grpIdx, 1);
          } else if (specimen.aliquotGrp) {
            if (!specimen.expanded) {
              angular.forEach(specimen.aliquotGrp, function(aliquot) {
                aliquot.selected = false;
                aliquot.removed = true;
              });
            } else {
              // logic of changing group leader.
              adjustGrpLeader(specimen);
            }
          }

          $scope.autoAlloc.rerun = true;
        };

        function adjustGrpLeader(specimen) {
          if (!specimen.aliquotGrp) {
            return;
          }
  
          var members = specimen.aliquotGrp.splice(1);
          var newLeader = members.length > 0 ? members[0] : null;
          if (!newLeader) {
            return;
          }
  
          newLeader.aliquotGrp = members;
          newLeader.expanded = true;
          newLeader.grpLeader = null;
          newLeader.$$collectedOrPending = specimen.$$collectedOrPending;
          angular.forEach(members, function(member) {
            if (member != newLeader) {
              member.grpLeader = newLeader;
            }
          });
        }
  
        function handleSpecimensPoolStatus(specimen) {
          var pooledSpmn = specimen.pooledSpecimen;
          if (!pooledSpmn) {
            return;
          }
  
          var allSameStatus = pooledSpmn.specimensPool.every(
            function(s) {
              return s.status == specimen.status;
            }
          );
  
          if (allSameStatus|| pooledSpmn.status == 'Missed Collection') {
            pooledSpmn.status = specimen.status;
          } else if (specimen.status != 'Collected' && pooledSpmn.status == 'Collected') {
            var atLeastOneColl = pooledSpmn.specimensPool.some(
              function(s) {
                return s.status == 'Collected';
              }
            );
  
            if (!atLeastOneColl) {
              pooledSpmn.status = specimen.status;
            }
          }
        }

        function downloadManifest(visitId) {
          Util.downloadFile(ApiUrls.getBaseUrl() + 'visits/' + visitId + '/manifest');
        }
  
        $scope.statusChanged = statusChanged;
  
        $scope.updateCollDate = function(collDate) {
          $scope.collDetail.collectionDate = collDate;
          angular.forEach($scope.specimens,
            function(spmn) {
              if (spmn.lineage != 'New' || (spmn.existingStatus && spmn.existingStatus != 'Pending')) {
                return;
              }

              var collEvent = spmn.collectionEvent = spmn.collectionEvent || {};
              collEvent.time = collDate;
              angular.forEach(spmn.specimensPool,
                function(s) {
                  collEvent = s.collectionEvent = s.collectionEvent || {};
                  collEvent.time = collDate;
                }
              );
            }
          );
        }

        $scope.updateCollUser = function(user) {
          angular.forEach($scope.specimens,
            function(spmn) {
              if (spmn.lineage != 'New' || (spmn.existingStatus && spmn.existingStatus != 'Pending')) {
                return;
              }

              var collEvent = spmn.collectionEvent = spmn.collectionEvent || {};
              collEvent.user = user;
              angular.forEach(spmn.specimensPool,
                function(s) {
                  collEvent = s.collectionEvent = s.collectionEvent || {};
                  collEvent.user = user;
                }
              );
            }
          );
        }
  
        $scope.togglePrintLabels = setAliquotGrpPrintLabel;
          
        $scope.saveSpecimens = function(printManifest) {
          var prop = $scope.barcodingEnabled ? 'barcode' : 'label';
          if (areDupInputsPresent($scope.specimens, prop)) {
            Alerts.error('specimens.errors.duplicate_' + prop + 's');
            return;
          }
  
          if (!ignoreQtyWarning && !areAliquotsQtyOk($scope.specimens)) {
            return;
          }

          function saveSpecimens0() {
            var specimensToSave = getSpecimensToSave($scope.cp, $scope.specimens, []);
            var savedSpmnReqIds = getSpmnReqIds(specimensToSave);
            if (cp.specimenCentric || (!!$scope.visit.id && $scope.visit.status == 'Complete')) {
              Specimen.save(specimensToSave).then(
                function(savedSpecimens) {
                  $scope.specimens.length = 0;
                  displayCustomFieldGroups(savedSpmnReqIds, savedSpecimens);
                  if (printManifest) {
                    downloadManifest($scope.visit.id);
                  }
                }
              );
            } else {
              var visitToSave = angular.copy($scope.visit);
              visitToSave.status = 'Complete';
  
              var payload = {visit: visitToSave, specimens: specimensToSave};
              Visit.collectVisitAndSpecimens(payload).then(
                function(result) {
                  angular.extend(visit, result.data.visit);
  
                  $scope.specimens.length = 0;
                  displayCustomFieldGroups(savedSpmnReqIds, result.data.specimens);
                  if (printManifest) {
                    downloadManifest(visit.id);
                  }
                }
              );
            }
          }

          if ($scope.autoAlloc.enabled && $scope.autoAlloc.rerun) {
            Util.showConfirm({
              title: 'specimens.rerun_auto_allocation_q',
              confirmMsg: 'specimens.rerun_auto_allocation',
              okBtn: 'specimens.buttons.rerun_auto_alloc',
              cancelBtn: 'specimens.buttons.proceed_collection',
              ok: function() {
                reallocatePositions();
              },
              cancel: function() {
                saveSpecimens0();
              }
            });
          } else {
            saveSpecimens0();
          }
        };
  
        function descendentCount(specimen, onlySelected) {
          onlySelected = (onlySelected != false);
  
          var count = 0;
          angular.forEach(specimen.children, function(child) {
            if (child.removed || (!child.selected && onlySelected)) {
              return;
            }
  
            count += 1 + descendentCount(child);
          });
  
          angular.forEach(specimen.specimensPool, function(poolSpmn) {
            if (poolSpmn.removed || (!poolSpmn.selected && onlySelected)) {
              return;
            }
  
            count += 1 + descendentCount(poolSpmn);
          });
  
          return count;
        };
  
        function statusChanged(specimen) {
          if (!specimen) {
            return;
          }


          $scope.autoAlloc.rerun = true;
          angular.forEach(specimen.specimensPool, 
            function(poolSpmn) {
              poolSpmn.status = specimen.status;
            }
          );
  
          angular.forEach(specimen.children,
            function(child) {
              child.status = specimen.status;
              statusChanged(child);
            }
          );
  
          if (specimen.status == 'Collected') {
            var curr = specimen.parent;
            while (curr) {
              curr.status = specimen.status;
              curr = curr.parent;
            }
          }
  
          handleSpecimensPoolStatus(specimen);
  
          if (!specimen.expanded && specimen.aliquotGrp) {
            angular.forEach(specimen.aliquotGrp, function(sibling) {
              sibling.status = specimen.status;
            });
  
            specimen.$$collectedOrPending = isCollectedOrPending(specimen);
          } else if (specimen.aliquotGrp) {
            specimen.$$collectedOrPending = specimen.aliquotGrp.some(isCollectedOrPending);
          } else if (specimen.grpLeader && specimen.grpLeader.aliquotGrp) {
            specimen.grpLeader.$$collectedOrPending = specimen.grpLeader.aliquotGrp.some(isCollectedOrPending);
          }
        }
  
        function areDupInputsPresent(input, prop) {
          var values = [];
          for (var i = 0; i < input.length; ++i) {
            if (!!input[i][prop] && values.indexOf(input[i][prop]) != -1) {
              return true;
            }
  
            values.push(input[i][prop]);
          }
  
          return false;
        }

        function isDirty(oldValue, newValue) {
          // either of old value or new value is specified and their current values differ.
          return (!!oldValue || !!newValue) && oldValue != newValue;
        }
  
        function isDirty(oldValue, newValue) {
          // either of old value or new value is specified and their current values differ.
          return (!!oldValue || !!newValue) && oldValue != newValue;
        }

        function getSpecimensToSave(cp, uiSpecimens, visited) {
          var result = [];
          angular.forEach(uiSpecimens, 
            function(uiSpecimen) {
              if (visited.indexOf(uiSpecimen) >= 0 || // already visited
                  !uiSpecimen.selected || // not selected
                  (uiSpecimen.existingStatus == 'Collected' && !uiSpecimen.closeAfterChildrenCreation) ||
                  (
                    (!uiSpecimen.existingStatus || uiSpecimen.existingStatus == 'Pending') &&
                    (!uiSpecimen.status || uiSpecimen.status == 'Pending') &&
                    (
                      !isDirty(uiSpecimen.pLabel, uiSpecimen.label) &&
                      !isDirty(uiSpecimen.pBarcode, uiSpecimen.barcode) &&
                      !isDirty(uiSpecimen.pInitialQty, uiSpecimen.initialQty)
                    )
                  )
              ) {
                // collected and not close after children creation
                return;
              }
  
              visited.push(uiSpecimen);
              if (uiSpecimen.lineage == 'Aliquot' &&                     // specimen is a aliquot
                  uiSpecimen.status == 'Collected' &&                    // specimen is collected
                  !barcodingEnabled &&                                   // barcoding is disabled
                  (cp.manualSpecLabelEnabled || !uiSpecimen.labelFmt) && // aliquot labels are scanned
                  !uiSpecimen.label) {                                   // aliquot label is not scanned
                return;                                                  // exclude the aliquot from collection
              }
  
              var specimen = getSpecimenToSave(uiSpecimen);
              specimen.children = getSpecimensToSave(cp, uiSpecimen.children, visited);
              result.push(specimen);
              return result;
            }
          );
  
          return result;
        };
  
        function getSpecimenToSave(uiSpecimen) { // Make it object Specimen and do checks like isNew/isCollected
          var specimen = {
            id: uiSpecimen.id,
            initialQty: uiSpecimen.initialQty,
            label: uiSpecimen.label,
            barcode: uiSpecimen.barcode,
            printLabel: uiSpecimen.printLabel,
            cpId: cp.id,
            reqId: uiSpecimen.reqId,
            visitId: $scope.visit.id,
            storageLocation: uiSpecimen.storageLocation,
            parentId: angular.isDefined(uiSpecimen.parent) ? uiSpecimen.parent.id : uiSpecimen.parentId,
            lineage: uiSpecimen.lineage,
            concentration: uiSpecimen.concentration,
            status: uiSpecimen.status,
            closeAfterChildrenCreation: uiSpecimen.closeAfterChildrenCreation,
            createdOn: uiSpecimen.createdOn,
            createdBy: uiSpecimen.createdBy,
            freezeThawCycles: uiSpecimen.freezeThawCycles,
            incrParentFreezeThaw: uiSpecimen.incrParentFreezeThaw,
            comments: uiSpecimen.comments,
            extensionDetail: uiSpecimen.extensionDetail,
            externalIds: uiSpecimen.externalIds
          };
  
          if (specimen.lineage == 'New' && specimen.status == 'Collected') {
            var collEvent = specimen.collectionEvent = uiSpecimen.collectionEvent || {};
            var recvEvent = specimen.receivedEvent = uiSpecimen.receivedEvent || {};
            specimen.specimensPool = uiSpecimen.specimensPool || [];
  
            if ($scope.showCollVisitDetails) {
              var collDetail = $scope.collDetail;
              angular.extend(collEvent, {
                user: collEvent.user || collDetail.collector,
                time: collEvent.time || collDetail.collectionDate
              });
              angular.extend(recvEvent, {
                user: collDetail.receiver,
                time: collDetail.receiveDate,
                receivedQuality: collDetail.receiveQuality
              });
            }
          }
  
          if (specimen.lineage == 'Aliquot') {
            return specimen;
          }
  
          specimen.specimenClass = uiSpecimen.specimenClass;
          specimen.type = uiSpecimen.type;
          specimen.pathology = uiSpecimen.pathology;
          specimen.anatomicSite = uiSpecimen.anatomicSite;
          specimen.laterality = uiSpecimen.laterality;
          specimen.biohazards = uiSpecimen.biohazards;
          return specimen;
        };
  
        function areAliquotsQtyOk(specimens) {
          for (var i = 0; i < specimens.length; i++) {
            var specimen = specimens[i];
            if (!specimen.children || specimen.children.length == 0) {
              continue;
            }
  
            var aliquots = specimen.children.filter(
              function(child) {
                 return child.selected && child.lineage == 'Aliquot' && child.existingStatus != 'Collected';
               }
            );
  
            var aliquotsQty = aliquots.reduce(
              function(sum, aliquot) {
                return sum + (!aliquot.initialQty ? 0 : +aliquot.initialQty);
              }, 0);
  
            var parentQty = specimen.existingStatus == 'Collected' ? specimen.availableQty : specimen.initialQty;
            if (parentQty != undefined && (aliquotsQty - parentQty) > 0.000001) {
              showInsufficientQtyWarning();
              return false;
            }
  
            if (!areAliquotsQtyOk(specimen.children)) {
              return false;
            }
          };
  
          return true;
        }
  
        function showInsufficientQtyWarning() {
          SpecimenUtil.showInsufficientQtyWarning({
            ok: function () {
              ignoreQtyWarning = true;
              $scope.saveSpecimens();
            }
          });
        }
  
        function getScannedLabels(specimen, prop, title, placeholder) {
          return $modal.open({
            templateUrl: 'modules/biospecimen/participant/collect-barcodes.html',
            backdrop: 'static',
            controller: function($scope, $modalInstance) {
              $scope.inputBarcodes = specimen[prop];
              $scope.title = title;
              $scope.placeholder = placeholder;
  
              $scope.ok = function() {
                specimen[prop] = $scope.inputBarcodes;
                $modalInstance.close(true);
              }
  
              $scope.cancel = function() {
                $modalInstance.dismiss();
              }
            }
          }).result;
        }
  
        function assignInputs(aliquot, inputs, prop) {
          var inputs = Util.splitStr(inputs, /,|\t|\n/, true);
          var newSpmnsCnt = inputs.length - aliquot.aliquotGrp.length;
          if (newSpmnsCnt > 0) {
            addAliquotsToGrp(aliquot, newSpmnsCnt);
          }
  
          angular.forEach(aliquot.aliquotGrp, function(spmn, $index) {
            if ($index < inputs.length) {
              spmn[prop] = inputs[$index];
              spmn.selected = true;
              spmn.removed = false;
            } else if (prop == 'label') {
              spmn[prop] = null;
              spmn.selected = false;
              spmn.removed = true;
            }
          });

          return newSpmnsCnt;
        }
  
        $scope.assignBarcodes = function(aliquot, barcodes) {
          assignInputs(aliquot, barcodes, 'barcode');
        }
  
        $scope.getBarcodes = function(specimen, $event) {
          $event.target.blur();
          getScannedLabels(specimen, 'aliquotBarcodes', 'specimens.aliquot_barcodes', 'specimens.scan_aliquot_barcodes').then(
            function() {
              var result = assignInputs(specimen, specimen.aliquotBarcodes, 'barcode');
              $scope.autoAlloc.rerun = $scope.autoAlloc.rerun || (result != 0);
            }
          );
        }
  
        $scope.assignLabels = function(aliquot, labels) {
          assignInputs(aliquot, labels, 'label');
        }
  
        $scope.getLabels = function(specimen, $event) {
          $event.target.blur();
          getScannedLabels(specimen, 'aliquotLabels', 'specimens.aliquot_labels', 'specimens.scan_aliquot_labels').then(
            function() {
              var result = assignInputs(specimen, specimen.aliquotLabels, 'label');
              $scope.autoAlloc.rerun = $scope.autoAlloc.rerun || (result != 0);
            }
          );
        }
  
        $scope.expandAliquotsGroup = function(aliquot) {
          expandOrCollapseAliquotsGrp(aliquot, true);
        }
  
        $scope.collapseAliquotsGroup = function(aliquot) {
          expandOrCollapseAliquotsGrp(aliquot, false);
        }
  
        $scope.changeQuantity = function(specimen, qty) {
          ignoreQtyWarning = false;
          if (!specimen.expanded) {
            angular.forEach(specimen.aliquotGrp, function(sibling) {
              sibling.initialQty = qty;
            });
          }
        }
  
        $scope.updateCount = function(specimen) {
          var grp = specimen.aliquotGrp;
          var grpLen = grp.length;
  
          if (specimen.newAliquotsCnt < grpLen) {
            removeAliquotsFromGrp(specimen, grpLen - specimen.newAliquotsCnt);
            $scope.autoAlloc.rerun = true;
          } else if (specimen.newAliquotsCnt > grpLen) {
            ignoreQtyWarning = false;
            addAliquotsToGrp(specimen, specimen.newAliquotsCnt - grpLen);
            $scope.autoAlloc.rerun = true;
          }     
  
          Util.hidePopovers();
        }
  
        $scope.closePopover = function() {
          Util.hidePopovers();
        }
  
        var reallocatePositions = $scope.reallocatePositions = function() {
          var specimens = [];
          var reservationId = undefined;
          for (var i = 0; i < $scope.specimens.length; ++i) {
            var spmn = $scope.specimens[i];
            if (!reservationId && spmn.storageLocation) {
              reservationId = spmn.storageLocation.reservationId
            }
  
            if (spmn.existingStatus == 'Collected') {
              continue;
            }
  
            if (spmn.status == 'Collected' && spmn.selected) {
              spmn.status = '';
              specimens.push(spmn);
            } else {
              spmn.storageLocation = {};
            }
          }
  
          var msg = Alerts.info('specimens.running_auto_allocation', {}, false);
          function reassignSelectedStatus() {
            Alerts.remove(msg);
            angular.forEach(specimens, function(spmn) { spmn.status = 'Collected'; });
            $scope.autoAlloc.rerun = false;
            $scope.autoAlloc.manual = false;
            Alerts.success('specimens.finished_auto_allocation');
          }
  
          CollectSpecimensSvc.allocatePositions(visit, specimens, reservationId)
            .then(reassignSelectedStatus, reassignSelectedStatus);
        }
  
        //
        // action = 0 -> cancel reservation
        // action = 1 -> cancel reservation and clear positions
        // action = 2 -> cancel reservation, clear container names and positions
        //
        $scope.selectPositionsManually = function(action) {
          $q.when(CollectSpecimensSvc.cancelReservation($scope.specimens)).then(
            function() {
              $scope.autoAlloc.manual = true;
              angular.forEach($scope.specimens,
                function(spmn) {
                  var loc = spmn.storageLocation;
                  if (spmn.existingStatus == 'Collected' || !loc) {
                    return;
                  }
  
                  delete loc.reservationId;
                  if (action == 1) {
                    loc.positionX = loc.positionY = loc.position = undefined;
                  } else if (action == 2) {
                    spmn.storageLocation = {};
                  }
                }
              );
            }
          );
        }
  
        $scope.applyFirstLocationToAll = function() {
          var location = {}, firstIdx = -1;
          for (var i = 0; i < $scope.specimens.length; ++i) {
            var spmn = $scope.specimens[i];
            if (spmn.existingStatus != 'Collected' && !spmn.isVirtual) {
              location = {
                name: spmn.storageLocation.name,
                mode: spmn.storageLocation.mode,
                displayName: spmn.storageLocation.displayName,
                displayTitle: spmn.storageLocation.displayTitle
              };
              firstIdx = i;
              break;
            }
          }
  
          if (firstIdx == -1) {
            return;
          }
  
          for (var i = 1; i < $scope.specimens.length; i++) {
            var spmn = $scope.specimens[i];
            if (spmn.existingStatus != 'Collected' && !spmn.isVirtual && firstIdx != i) {
              spmn.storageLocation = angular.extend({}, location);
            }
          }
        };
  
        $scope.cancel = function() {
          if (!$scope.onCancel({visit: visit})) {
            CollectSpecimensSvc.navigateTo($scope, visit);
          }
        }
  
        init();
      },

      link: function() { }
    };
  })

  .controller('CollectSpecimensCtrl', function(
    $scope, $state, cp, cpr, visit, latestVisit, cpDict, spmnCollFields,
    barcodingEnabled, spmnBarcodesAutoGen, mrnAccessRestriction, CollectSpecimensSvc) {

    $scope.cp = cp;
    $scope.cpr = cpr;
    $scope.visit = visit;
    $scope.latestVisit = latestVisit;
    $scope.cpDict = cpDict;
    $scope.spmnCollFields = spmnCollFields;
    $scope.barcodingEnabled = barcodingEnabled;
    $scope.spmnBarcodesAutoGen = spmnBarcodesAutoGen;
    $scope.mrnAccessRestriction = mrnAccessRestriction;

    $scope.onSave = function(visit, collectedSpecimens) {
      if (!spmnCollFields || !spmnCollFields.fieldGroups || spmnCollFields.fieldGroups.length == 0) {
        CollectSpecimensSvc.navigateTo($scope, visit);
        return;
      }

      CollectSpecimensSvc.setData({visit: visit, specimens: collectedSpecimens});
      $state.go('participant-detail.collect-specimens.nth-step', {visitId: visit.id, eventId: visit.eventId});
    }
  })

  .directive('osCollectSpecimensNthStep', function(
    $state, $injector, $parse, CollectSpecimensSvc, ExtensionsUtil,
    SpecimenUtil, Util, ApiUrls, Visit, AuthorizationService) {
    return {
      restrict: 'E',

      templateUrl: 'modules/biospecimen/participant/collect-specimens-nth-step-tmpl.html',

      scope: {
        cp: '=',
        cpr: '=',
        visit: '=',
        specimens: '=',
        cpDict: '=',
        spmnCollFields: '=',
        latestVisit: '=',
        onValueChangeCb: '=',
        onSave: '&'
      },

      controller: function($scope) {
        var isVisitCompleted;
        var cp = $scope.cp,
            cpr = $scope.cpr,
            visit = $scope.visit,
            specimens = $scope.specimens,
            cpDict = $scope.cpDict,
            spmnCollFields = $scope.spmnCollFields,
            latestVisit = $scope.latestVisit,
            onValueChangeCb = $scope.onValueChangeCb;

        function init() {
          var autoInit = spmnCollFields.autoInitVisit != 'false' && spmnCollFields.autoInitVisit != false;

          var specimens = $scope.specimens;
          var uiOpts = CollectSpecimensSvc.opts();

          specimens.forEach(
            function(spmn) {
              if (!spmn.status || spmn.status == 'Pending') {
                spmn.status = uiOpts.defCollectionStatus[spmn.lineage] || 'Collected';
              }

              if (spmn.children && spmn.children.some(function(s) { return s.selected; })) {
                spmn.closeAfterChildrenCreation = cp.closeParentSpecimens;
              }

              spmn.externalIds = spmn.externalIds || [];
              ExtensionsUtil.createExtensionFieldMap(spmn, true);
            }
          );

          CollectSpecimensSvc.cleanupSpecimens(specimens);

          var opts = $scope.opts = {
            viewCtx: $scope,
            onValueChange: onValueChangeCb,
            static: false,
            allowBulkUpload: false,
            hideFooterActions: true
          };

          var userRole = AuthorizationService.getRole(cp);
          var groups = $scope.customFieldGroups = SpecimenUtil.sdeGroupSpecimens(
            cpDict,
            spmnCollFields.fieldGroups || [],
            specimens,
            {cp: cp, cpr: cpr, visit: visit, userRole: userRole},
            opts
          );

          $scope.showManifestPrint = !cp.specimenCentric &&
            (ui.os.appProps.plugins || []).indexOf('os-extras') != -1;
          $scope.visit = visit;
          if (visit) {
            isVisitCompleted = (visit.status == 'Complete');
            if (!isVisitCompleted && !!latestVisit && autoInit) {
              visit.clinicalDiagnoses = latestVisit.clinicalDiagnoses;
            }

            if (!visit.site) {
              if (!!latestVisit && autoInit) {
                visit.site = latestVisit.site;
              } else if (cpr.participant.pmis && cpr.participant.pmis.length > 0) {
                visit.site = cpr.participant.pmis[0].siteName;
              }
            }

            var visitFieldsGrp = getVisitFieldsGroup(cp, cpr, visit, spmnCollFields, userRole);
            var showVisitFields = CollectSpecimensSvc.showCollVisitDetails();
            if (visitFieldsGrp && showVisitFields) {
              groups.unshift(visitFieldsGrp);
              ExtensionsUtil.createExtensionFieldMap(visit, true)
            }
          }

          $scope.$on('$destroy', function() { CollectSpecimensSvc.cancelReservation(specimens); });
          if (groups.length == 0 || (groups.length == 1 && groups[0].noMatch)) {
            navigateTo();
          }
        }

        function navigateTo(dbVisit) {
          $scope.onSave(dbVisit || visit);
        }

        function getVisitFieldsGroup(cp, cpr, visit, spmnCollFields, userRole) {
          var group = spmnCollFields.visitFields;
          if (!group) {
            return undefined;
          }

          if (group.criteria) {
            var exprs = group.criteria.rules.map(
              function(rule) {
                if (rule.op == 'exists') {
                  return '!!' + rule.field;
                } else if (rule.op == 'not_exist') {
                  return '!' + rule.field;
                } else {
                  return rule.field + ' ' + rule.op + ' ' + rule.value;
                }
              }
            );

            var expr = $parse(exprs.join(group.criteria.op == 'AND' ? ' && ' : ' || '));
            if (!expr({cp: cp, cpr: cpr, visit: visit, userRole: userRole})) {
              return undefined;
            }
          }

          return {
            visitFields: true,
            multiple: true,
            fields: {groups: [group], table: []},
            baseFields: cpDict,
            input: [{cp: cp, cpr: cpr, visit: visit, userRole: userRole}],
            opts: {static: true}
          };
        }

        function downloadManifest(visitId) {
          Util.downloadFile(ApiUrls.getBaseUrl() + 'visits/' + visitId + '/manifest');
        }

        function updateSpecimens(printManifest) {
          var sdeSampleSvc = $injector.get('sdeSample');

          var specimens = [];
          angular.forEach($scope.customFieldGroups,
            function(group) {
              if (group.noMatch || group.visitFields) {
                return;
              }

              specimens = specimens.concat(group.input);
            }
          );

          var visitToSave = undefined;
          if ($scope.customFieldGroups[0].visitFields) {
            visitToSave = $scope.customFieldGroups[0].input[0].visit;
          }

          if (specimens.length > 0) {
            angular.forEach(specimens, function(specimen) { specimen.visit = undefined; });
            specimens[0].visit = visitToSave;
            sdeSampleSvc.updateSamples(specimens).then(
              function(savedSpecimens) {
                if (printManifest) {
                  downloadManifest(savedSpecimens[0].visitId);
                }

                navigateTo();
              }
            );
          } else if (visitToSave) {
            new Visit(visitToSave).$saveOrUpdate().then(
              function(savedVisit) {
                if (printManifest) {
                  downloadManifest(savedVisit.id);
                }

                navigateTo();
              }
            );
          } else {
            navigateTo();
          }
        }

        function collectSpecimens(printManifest) {
          var sdeSampleSvc = $injector.get('sdeSample');

          var specimens   = $scope.specimens;
          var visitToSave = angular.copy(visit);

          var events = {};
          var uids = [];
          angular.forEach($scope.customFieldGroups,
            function(group) {
              if (group.visitFields) {
                return;
              }

              if (group.noMatch) {
                angular.forEach(group.input,
                  function(sample) {
                    if (sample.specimen) {
                      uids.push(sample.specimen.uid);
                    }
                  }
                );

                return;
              }

              angular.forEach(group.input,
                function(sample) {
                  uids.push(sample.specimen.uid);

                  if (events[sample.specimen.uid]) {
                    angular.extend(events[sample.specimen.uid], sample.events);
                  } else {
                    events[sample.specimen.uid] = angular.extend({}, sample.events);
                  }
                }
              );
            }
          );

          var samples = specimens.filter(
            function(spmn) {
              return uids.indexOf(spmn.uid) > -1
            }
          ).map(
            function(spmn) {
              return {specimen: spmn, events: events[spmn.uid]};
            }
          );

          if ($scope.customFieldGroups[0].visitFields || !visitToSave.id || !isVisitCompleted) {
            if (!visitToSave.status || visitToSave.status == 'Pending') {
              visitToSave.status = 'Complete';
            }

            visitToSave.cprId = cpr.id;
            samples[0].visit = visitToSave;
          } else {
            samples[0].visit = null;
          }

          sdeSampleSvc.collectVisitSpecimens(samples).then(
            function(resp) {
              if (printManifest) {
                downloadManifest(resp[0].visitId);
              }

              navigateTo({id: resp[0].visitId})
            }
          );
        }

        $scope.setToAllChildren = function(object, prop, value, allDescendants) {
          SpecimenUtil.sdeGroupSetChildrenValue($scope.customFieldGroups, object, prop, value, allDescendants);
        }

        $scope.updateSpecimens = function(printManifest) {
          if (CollectSpecimensSvc.hierarchical()) {
            collectSpecimens(printManifest);
          } else {
            updateSpecimens(printManifest);
          }
        }

        $scope.cancel = navigateTo;

        init();
      }
    };
  })

  .controller('CollectSpecimensNthStepCtrl', function(
    $scope, $state, $injector, cp, cpr, visit, cpDict, spmnCollFields, latestVisit, onValueChangeCb,
    barcodingEnabled, spmnBarcodesAutoGen, mrnAccessRestriction, CollectSpecimensSvc) {

    $scope.cp = cp;
    $scope.cpr = cpr;
    $scope.visit = visit;
    $scope.latestVisit = latestVisit;
    $scope.cpDict = cpDict;
    $scope.spmnCollFields = spmnCollFields;
    $scope.onValueChangeCb = onValueChangeCb;
    $scope.specimens = CollectSpecimensSvc.getSpecimens();

    $scope.onSave = function(savedVisit) {
      CollectSpecimensSvc.navigateTo($scope, savedVisit || visit, true);
    }
  });
