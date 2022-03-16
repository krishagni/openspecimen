
angular.module('os.biospecimen.specimen.addedit', [])
  .controller('AddEditSpecimenCtrl', function(
    $scope, $state, $parse, userRole, cp, cpr, visit, specimen, extensionCtxt,
    aliquotQtyReq, barcodingEnabled, spmnBarcodesAutoGen, hasDict, sysDict,
    cpDict, layout, onValueChangeCb, spmnReq, defSpmns, spmnCollFields,
    createDerived, imagingEnabled, requirements,
    Alerts, CpConfigSvc, PluginReg, Util, ParticipantSpecimensViewState,
    Specimen, CollectSpecimensSvc) {

    var inputCtxts;

    function init() {
      $scope.opts = {
        cp: cp, cpr: cpr, visit: visit, spmnReq: spmnReq,
        extensionCtxt: extensionCtxt, hasDict: hasDict,
        sysDict: sysDict, cpDict: cpDict, userRole: userRole,
        barcodingEnabled: barcodingEnabled, spmnBarcodesAutoGen: spmnBarcodesAutoGen,
        editMode: !!specimen.id, reqId: specimen.reqId, aliquotQtyReq: aliquotQtyReq,
        layout: layout, onValueChange: onValueChangeCb, mdInput: false,
        hasInfo: PluginReg.getTmpls('specimen-addedit', 'info').length > 0,
        createDerived: createDerived, imagingEnabled: imagingEnabled,
        allSpmnUpdate: $scope.specimenAllowedOps.allUpdate,
        storeSpmn: $scope.specimenAllowedOps.store,
        spmnCollFields: spmnCollFields,
        requirements: requirements,
        inputCtxtsFn: getInputCtxts
      }

      CpConfigSvc.getCommonCfg(cp.id, 'addSpecimen').then(
        function(cfg) {
          cfg = angular.copy(cfg || {});
          if (cfg.requirements) {
            delete cfg.requirements;
          }

          angular.extend($scope.opts, cfg);
        }
      );

      if (!specimen.id && !specimen.reqId && defSpmns.length > 0) {
        inputCtxts = $scope.inputCtxts = getInputCtxts(defSpmns);
      } else {
        inputCtxts = $scope.inputCtxts = [{specimen: angular.copy(specimen), form: {}, open: true}]
      }
    }

    function getInputCtxts(defSpmns) {
      return (defSpmns || []).map(
        function(defSpmn, idx) {
          var spmn = new Specimen({lineage: 'New', visitId: visit.id, labelFmt: cpr.specimenLabelFmt});
          angular.forEach(defSpmn.fields,
            function(field) {
              try {
                $parse(field.name).assign({specimen: spmn}, field.value);
              } catch (e) {
                // meant for devs / support staff to debug
                Alerts.errorText("Invalid field definition: " + (field && JSON.stringify(field)));
              }
            }
          );

          var aliquots = (defSpmn.aliquots || []).map(
            function(spec) {
              return new Specimen({
                lineage: 'Aliquot', type: spec.type,
                noOfAliquots: spec.count, qtyPerAliquot: spec.quantity,
                pathology: spec.pathology
              });
            }
          );

          return {specimen: spmn, form: {}, open: idx == 0, aliquots: aliquots.length > 0 ? aliquots : undefined};
        }
      );
    }

    function getState() {
      if (cp.specimenCentric) {
        return {state: $state.get('cp-specimens'), params: {cpId: cp.id}};
      } else {
        return {state: $state.get('visit-detail.overview'), params: {visitId: visit.id}};
      }
    };

    $scope.addAnother = function() {
      inputCtxts.push({
        specimen: new Specimen({lineage: 'New', visitId: visit.id, labelFmt: cpr.specimenLabelFmt}),
        form: {},
        open: true
      });
    }

    $scope.remove = function(event, index) {
      event.stopPropagation();

      Util.showConfirm({
        title: 'specimens.delete_q',
        confirmMsg: 'specimens.confirm_q',
        isWarning: true,
        ok: function() {
          inputCtxts.splice(index, 1);
          if (inputCtxts.length == 0) {
            $scope.addAnother();
          }
        }
      });
    }

    $scope.addCopyOfLast = function(reqAliquots) {
      var lastCtxt = inputCtxts[inputCtxts.length - 1];

      var lastCtrl = lastCtxt.form.ctrl;

      var lastSpmn = lastCtrl.getSpecimens(false, true)[0];

      if (!!lastSpmn) {
        var spmn = angular.copy(lastSpmn);
        var location = spmn.storageLocation;
        if (location) {
          spmn.storageLocation = { name: location.name, mode: location.mode };
        }

        delete spmn.label;
        delete spmn.barcode;
        delete spmn.$$count;
        delete spmn.$$labels;

        var aliquots = reqAliquots ? angular.copy(lastCtrl.getAliquotSpec()) : undefined;
        inputCtxts.push({
          specimen: spmn,
          aliquots: aliquots,
          form: {},
          open: true,
          createChildren: lastCtrl.isCreateChildrenEnabled()
        });
      }
    }

    $scope.next = function() {
      var error = false, specimensToCollect = [];
      angular.forEach(inputCtxts,
        function(ctxt) {
          var spmns = ctxt.form.ctrl.getSpecimens(true);

          if (spmns) {
            spmns[0].selected = true;
            spmns[0].status = 'Pending';

            var labels = Util.splitStr(spmns[0].$$labels || '', /,|\t|\n/);
            var count = labels.length || spmns[0].$$count || 1;
            for (var i = 0; i < +count; ++i) {
              if (i != 0) {
                spmns = angular.copy(spmns);
                var location = spmns[0].storageLocation;
                if (location) {
                  spmns[0].storageLocation = { name: location.name, mode: location.mode };
                }

                delete spmns[0].label;
                delete spmns[0].barcode;
                delete spmns[0].$$count;
                delete spmns[0].$$labels;
              }

              if (labels.length > 0) {
                spmns[0].label = labels[i];
              }

              Array.prototype.push.apply(specimensToCollect, spmns);
            }
          } else {
            error = true;
          }
        }
      );

      if (error) {
        return;
      }

      var opts = {showCollVisitDetails: false};
      CollectSpecimensSvc.collect(getState(), visit, specimensToCollect, opts);
    }

    $scope.update = function() {
      var input = inputCtxts[0].form.ctrl.getSpecimens();
      if (!input || input.length == 0) {
        return;
      }

      input[0].$saveOrUpdate().then(
        function(result) {
          angular.extend(specimen, result);
          ParticipantSpecimensViewState.specimensUpdated($scope);

          var params = {specimenId: result.id, cprId: result.cprId, visitId: result.visitId, srId: result.reqId};
          $state.go('specimen-detail.overview', params);
        }
      );
    }

    init();
  })
  .directive('osSpecimenAddeditForm', function($rootScope, Specimen, SpecimenUtil, Util, PvManager, ExtensionsUtil) {

    function loadPvs(scope) {
      scope.biohazards       = PvManager.getPvs('specimen-biohazard');
      scope.specimenStatuses = PvManager.getPvs('specimen-status');
    };

    function updateParent(parent, children) {
      angular.forEach(children, function(child) { child.parent = parent; });
      parent.children = parent.children || [];
      parent.children = parent.children.concat(children);
    }

    function getAliquots(opts, primarySpmn, types, typeSpecs) {
      var result = [], children = [], derived = undefined;

      angular.forEach(types,
        function(type) {
          derived = undefined;
          angular.forEach(typeSpecs[type],
            function(aliquotSpec) {
              var detail = angular.copy({aliquotSpec: aliquotSpec});
              angular.extend(detail, {parentSpecimen: primarySpmn, deFormCtrl: {}, cpr: opts.cpr});
              detail.aliquotSpec.createDerived = opts.createDerived;

              var tree = SpecimenUtil.collectAliquots(detail);
              tree.splice(0, 1); // remove parent as it is already in our final list
              if (type != primarySpmn.type) {
                if (derived) {
                  if (!isNaN(derived.initialQty) && !isNaN(tree[0].initialQty)) {
                    derived.initialQty = derived.initialQty + tree[0].initialQty;
                  } else {
                    derived.initialQty = undefined;
                  }

                  tree.splice(0, 1);         // remove derived
                  primarySpmn.children = []; // remove derived as child of primary specimen;
                  updateParent(derived, tree);
                } else {
                  derived = tree[0];
                }
              }

              Array.prototype.push.apply(result, tree);
              Array.prototype.push.apply(children, primarySpmn.children); // accumulate direct children of primary spmns
            }
          );
        }
      );

      primarySpmn.children = children;
      return result;
    }

    function getDefValues(opts) {
      var currentDate = new Date().getTime();
      var defValues = {};
      angular.forEach(opts.cpDict || opts.sysDict,
        function(field) {
          if (!field.name || !field.name.startsWith('specimen.')) {
            return;
          }

          defValues[field.name] = field.defaultValue;
          if (field.defaultValue == 'current_user') {
            defValues[field.name] = $rootScope.currentUser;
          } else if (field.defaultValue == 'current_date') {
            defValues[field.name] = currentDate;
          }
        }
      );

      return defValues;
    }


    return {
      restrict: 'E',

      templateUrl: 'modules/biospecimen/participant/specimen/addedit-form.html',

      scope: {
        opts    : '=',
        specimen: '=',
        aliquots: '=',
        createChildren: '=',
        ctrl    : '='
      },

      controller: function($scope) {
        this.getSpecimens = function(reqAliquots, includeChildren) {
          var formCtrl = $scope.deFormCtrl.ctrl;
          if (formCtrl && !formCtrl.validate()) {
            return null;
          }

          var primarySpmn = angular.copy($scope.inputSpmn);
          if (formCtrl) {
            primarySpmn.extensionDetail = formCtrl.getFormData();
          }

          var spmnCtx = $scope.spmnCtx, result = [primarySpmn];
          if (reqAliquots && spmnCtx.createAliquots) {
            var types = [], typeSpecs = {};
            angular.forEach(spmnCtx.aliquots,
              function(spec) {
                if (!spec.type) {
                  return;
                }

                if (types.indexOf(spec.type) == -1) {
                  types.push(spec.type);
                }

                typeSpecs[spec.type] = typeSpecs[spec.type] || [];
                typeSpecs[spec.type].push(spec);
              }
            );

            var tree = getAliquots($scope.opts, primarySpmn, types, typeSpecs);
            Array.prototype.push.apply(result, tree);
          } else if (spmnCtx.createChildren) {
            var spmns = [primarySpmn];
            while(spmns.length > 0) {
              var spmn = spmns[0];
              spmn.selected = true;
              spmn.hasChildren = spmn.isOpened = spmn.children && spmn.children.length > 0;

              spmns.splice(0, 1);
              Array.prototype.push.apply(spmns, spmn.children || []);
            }

            result = Specimen.flatten([primarySpmn]);
          } else if (!includeChildren) {
            primarySpmn.children = [];
          }

          return result;
        }

        this.getAliquotSpec = function() {
          return ($scope.spmnCtx.createAliquots && $scope.spmnCtx.aliquots) || [];
        }

        this.isCreateChildrenEnabled = function() {
          return !!$scope.spmnCtx.createChildren;
        }
      },


      link: function(scope, element, attrs, ctrl) {
        var opts = scope.opts, specimen = scope.specimen;
        scope.ctrl.ctrl = ctrl;

        var inputSpmn = scope.inputSpmn = specimen; //angular.copy(specimen);
        inputSpmn.cpId = inputSpmn.cpId || opts.cp.id;
        inputSpmn.visitId = opts.visit && opts.visit.id;
        inputSpmn.createdOn = inputSpmn.createdOn || new Date();

        if (inputSpmn.lineage == 'Aliquot') {
          inputSpmn.anatomicSite = inputSpmn.laterality = undefined;
        }

        var defValues = opts.defValues = opts.defValues || getDefValues(opts) || {};
        if (inputSpmn.status != 'Collected') {
          if (!inputSpmn.id) {
            inputSpmn.status = defValues['specimen.status'] || 'Collected';
          }

          inputSpmn.availableQty = inputSpmn.initialQty;
        }

        if (!inputSpmn.labelFmt) {
          if (specimen.lineage == 'New') {
            inputSpmn.labelFmt = opts.cpr.specimenLabelFmt;
          } else if (specimen.lineage == 'Aliquot') {
            inputSpmn.labelFmt = opts.cpr.aliquotLabelFmt;
          } else if (specimen.lineage == 'Derived') {
            inputSpmn.labelFmt = opts.cpr.derivativeLabelFmt;
          }
        }

        var exObjs = ['specimen.lineage', 'specimen.parentLabel', 'specimen.events'];

        if (!inputSpmn.id && !inputSpmn.reqId) {
          var ce = inputSpmn.collectionEvent = inputSpmn.collectionEvent || {};
          ce.user = (!ce.user || !ce.user.id) ? $rootScope.currentUser : ce.user;

          ce.container = ce.container || defValues['specimen.collectionEvent.container'] || 'Not Specified';
          ce.procedure = ce.procedure || defValues['specimen.collectionEvent.procedure'] || 'Not Specified';

          var re = inputSpmn.receivedEvent = inputSpmn.receivedEvent || {};
          re.user = (!re.user || !re.user.id) ? $rootScope.currentUser : re.user;
          re.receivedQuality = re.receivedQuality ||
            (opts.spmnCollFields && opts.spmnCollFields.defReceiveQuality) ||
            defValues['specimen.receivedEvent.receivedQuality'] ||
            'Acceptable';
        }

        if (inputSpmn.lineage != 'New') {
          exObjs.push('specimen.collectionEvent', 'specimen.receivedEvent');
        }

        inputSpmn.initialQty    = Util.getNumberInScientificNotation(inputSpmn.initialQty);
        inputSpmn.availableQty  = Util.getNumberInScientificNotation(inputSpmn.availableQty);
        inputSpmn.concentration = Util.getNumberInScientificNotation(inputSpmn.concentration);

        var spmnCtx = scope.spmnCtx = {
          currentDate: new Date(),
          obj: {specimen: inputSpmn, cpr: opts.cpr, visit: opts.visit, cp: opts.cp, userRole: opts.userRole},
          opts: angular.extend({viewCtx: scope}, opts),
          inObjs: ['specimen'], exObjs: exObjs,
          isVirtual: inputSpmn.showVirtual(),
          manualSpecLabelReq: !!inputSpmn.label || !inputSpmn.labelFmt || opts.cp.manualSpecLabelEnabled,
          aliquots: scope.aliquots || [new Specimen({lineage: 'Aliquot'})],
          createAliquots: (scope.aliquots || []).length > 0,
          createChildren: !!scope.createChildren
        };

        scope.deFormCtrl = {};
        opts.extensionCtxt.sdeMode = opts.hasDict;
        scope.extnOpts = ExtensionsUtil.getExtnOpts(inputSpmn, opts.extensionCtxt);

        if (!opts.hasDict) {
          loadPvs(scope);
        }

        scope.toggleAliquots = function() {
          spmnCtx.aliquots = [new Specimen({lineage: 'Aliquot'})];
        }

        scope.addAnotherAliquot = function() {
          var aliquot = new Specimen({lineage: 'Aliquot'});
          spmnCtx.aliquots.push(aliquot);

          if (spmnCtx.opts.$$sdeFormFields) {
            spmnCtx.opts.$$sdeFormFields.valueChanged(spmnCtx.obj, aliquot, 'aliquots.$onAdd', 'aliquots');
          }
        }

        scope.removeAliquot = function(idx) {
          var aliquots = spmnCtx.aliquots.splice(idx, 1);
          if (spmnCtx.opts.$$sdeFormFields && aliquots.length > 0) {
            spmnCtx.opts.$$sdeFormFields.valueChanged(spmnCtx.obj, aliquots[0], 'aliquots.$onRemove', 'aliquots');
          }

          if (spmnCtx.aliquots.length == 0) {
            scope.addAnotherAliquot();
          }
        }

        scope.addIfLastAliquot = function(idx) {
          if (idx == (spmnCtx.aliquots.length - 1)) {
            scope.addAnotherAliquot();
          }
        }

        scope.onReqSelect = function(req) {
          if (!req.id) {
            return;
          }

          var spmn = angular.extend(scope.inputSpmn, Specimen.toSpecimenAttrs(req));
          spmn.availableQty = spmn.availableQty || spmn.initialQty;
          if (spmn.collectionEvent) {
            spmn.collectionEvent.user = spmn.collectionEvent.user || $rootScope.currentUser;
            spmn.collectionEvent.time = spmn.collectionEvent.time || new Date();
            spmn.collectionContainer = spmn.collectionEvent.container;
          }

          if (spmn.receivedEvent) {
            spmn.receivedEvent.user = spmn.receivedEvent.user || $rootScope.currentUser;
            spmn.receivedEvent.time = spmn.receivedEvent.time || new Date();
          }
        }
      }
    }
  });
