
angular.module('os.biospecimen.specimen', 
  [
    'ui.router',
    'os.biospecimen.specimen.addedit',
    'os.biospecimen.specimen.detail',
    'os.biospecimen.specimen.overview',
    'os.biospecimen.specimen.close',
    'os.biospecimen.specimen.addaliquots',
    'os.biospecimen.specimen.addderivative',
    'os.biospecimen.specimen.bulkaddevent',
    'os.biospecimen.extensions'
  ])
  .config(function($stateProvider) {

    function createDerived(cp, CpConfigSvc) {
      if (!cp) {
        return false;
      }

      return CpConfigSvc.getCommonCfg(cp.id || -1, 'addSpecimen').then(
        function(cfg) {
          return cfg && (cfg.aliquotDerivativesOnly == 'true' || cfg.aliquotDerivativesOnly == true)
        }
      );
    }

    function defBoolTrue(value) {
      return (value === null || value === undefined || value === '' || value == true);
    }

    $stateProvider
      .state('specimen', {
        url: '/specimens/:specimenId',
        controller: function(params, VueApp) {
          VueApp.setVueView(
            'cp-view/' + params.cpId +
            '/participants/' + params.cprId +
            '/visit/' + params.visitId +
            '/specimen/' + params.specimenId +
            '/overview'
          );
        },
        resolve: {
          params: function($stateParams, Specimen) {
            return Specimen.getRouteIds($stateParams.specimenId);
          }
        },
        parent: 'signed-in'
      })
      .state('specimen-root', {
        url: '/specimens?specimenId&srId',
        template: '<div ui-view></div>',
        resolve: {
          specimen: function($stateParams, cpr, Specimen) {
            if ($stateParams.specimenId) {
              return Specimen.getById($stateParams.specimenId);
            } else if ($stateParams.srId) {
              return Specimen.getAnticipatedSpecimen($stateParams.srId);
            }
 
            return new Specimen({
              lineage: 'New', 
              visitId: $stateParams.visitId, 
              labelFmt: cpr.specimenLabelFmt
            });
          },

          showSpmnActivity: function(cp, CpConfigSvc) {
            return CpConfigSvc.getCommonCfg(cp.id, 'showSpmnActivity').then(defBoolTrue);
          },

          incrFreezeThawCycles: function(cp, CpConfigSvc) {
            return CpConfigSvc.getCommonCfg(cp.id, 'incrementFreezeThawCycles').then(defBoolTrue);
          },

          imagingEnabled: function(SettingUtil) {
            return SettingUtil.getSetting('biospecimen', 'imaging').then(
              function(setting) {
                return setting.value == 'true' || setting.value == true;
              }
            );
          },

          notCoordinatOrStoreAllowed: function(cp, specimen, isCoordinator, storeAllowed) {
            return !isCoordinator || (cp.storageSiteBasedAccess && storeAllowed && specimen.storageLocation && specimen.storageLocation.id > 0);
          }
        },
        controller: function($scope, participantSpmnsViewState, specimen, imagingEnabled, notCoordinatOrStoreAllowed) {
          $scope.specimen = $scope.object = specimen;
          $scope.entityType = 'Specimen';
          $scope.extnState = 'specimen-detail.extensions.';
          $scope.imagingEnabled = imagingEnabled;
          $scope.notCoordinatOrStoreAllowed = notCoordinatOrStoreAllowed;

          participantSpmnsViewState.selectSpecimen(specimen);
          $scope.$on('$destroy', function() { participantSpmnsViewState.unselectSpecimen(); });
        },
        abstract: true,
        parent: 'visit-root'
      })
      .state('specimen-addedit', {
        url: '/addedit-specimen?reqName',
        templateProvider: function(PluginReg, $q) {
          var defaultTmpl = "modules/biospecimen/participant/specimen/addedit.html";
          return $q.when(PluginReg.getTmpls("specimen-addedit", "page-body", defaultTmpl)).then(
            function(tmpls) {
              return '<div ng-include src="\'' + tmpls[0] + '\'"></div>';
            }
          );
        },
        resolve: {
          extensionCtxt: function(cp, specimen) {
            return specimen.getExtensionCtxt({cpId: cp.id});
          },

          spmnReq: function($stateParams, cp, CpConfigSvc) {
            if (!$stateParams.reqName) {
              return undefined;
            }

            return CpConfigSvc.getCommonCfg(cp.id, 'addSpecimen').then(
              function(addSpmnCfg) {
                var reqs = (addSpmnCfg && addSpmnCfg.requirements) || [];
                return reqs.find(function(req) { return req.name == $stateParams.reqName; });
              }
            );
          },

          defSpmns: function(spmnReq) {
            return (spmnReq && (spmnReq.specimens || [])) || [];
          },

          requirements: function($stateParams, $translate, cp, SpecimenRequirement) {
            if (!cp.specimenCentric || !!$stateParams.reqName) {
              return [];
            }

            var processReqs = function processReqs(requirements) {
              var result = [];
              for (var i = 0; i < requirements.length; ++i) {
                var req = requirements[i];
                if (req.activityStatus != 'Active') {
                  continue;
                }

                req.children = processReqs(req.children || []);
                req.title = req.name || (req.type + (req.code ? ' (' + req.code + ')' : ''));
                if (!req.labelFmt) {
                  if (req.lineage == 'New') {
                    req.labelFmt = cp.specimenLabelFmt;
                  } else if (req.lineage == 'Derived') {
                    req.labelFmt = cp.derivativeLabelFmt;
                  } else if (req.lineage == 'Aliquot') {
                    req.labelFmt = cp.aliquotLabelFmt;
                  }
                }

                req.hasChildren = (req.children || []).length > 0;
                result.push(req);
              }

              return result;
            }

            return SpecimenRequirement.getByCpId(cp.id).then(
              function(reqs) {
                reqs = processReqs(reqs);
                if (reqs && reqs.length > 0) {
                  reqs.push({title: $translate.instant('specimens.other'), activityStatus: 'Active'});
                }

                return reqs;
              }
            );
          },

          spmnCollFields: function(cp, CpConfigSvc) {
            return CpConfigSvc.getWorkflowData(cp.id, 'specimenCollection').then(
              function(data) {
                if (data && !angular.equals(data, {})) {
                  return data;
                }

                return CpConfigSvc.getWorkflowData(-1, 'specimenCollection').then(
                  function(data) {
                    return data || {};
                  }
                );
              }
            );
          },

          createDerived: createDerived
        },
        controller: 'AddEditSpecimenCtrl',
        parent: 'specimen-root'
      })
      .state('specimen-detail', {
        url: '/detail',
        templateUrl: 'modules/biospecimen/participant/specimen/detail.html',
        controller: 'SpecimenDetailCtrl',
        parent: 'specimen-root'
      })
      .state('specimen-detail.overview', {
        url: '/overview',
        templateProvider: function(PluginReg, $q) {
          var defaultTmpl = "modules/biospecimen/participant/specimen/overview.html";
          return $q.when(PluginReg.getTmpls("specimen-detail", "overview", defaultTmpl)).then(
            function(tmpls) {
              return '<div ng-include src="\'' + tmpls[0] + '\'"></div>';
            }
          );
        },
        controller: 'SpecimenOverviewCtrl',
        parent: 'specimen-detail'
      })
      .state('specimen-detail.extensions', {
        url: '/extensions',
        template: '<div ui-view></div>',
        controller: function($scope, specimen, forms, records, ExtensionsUtil) {
          $scope.extnOpts = {
            update: $scope.specimenResource.updateOpts,
            entity: specimen,
            isEntityActive: (specimen.activityStatus != 'Disabled')
          };

          ExtensionsUtil.linkFormRecords(forms, records);
        },
        resolve: {
          orderSpec: function(cp, CpConfigSvc) {
            return CpConfigSvc.getWorkflowData(cp.id, 'forms', {}).then(
              function(wf) {
                return [{type: 'Specimen', forms: wf['Specimen'] || []}];
              } 
            );
          },
          fdeRules: function(cp, CpConfigSvc) {
            return CpConfigSvc.getWorkflowData(cp.id, 'formDataEntryRules', {}).then(
              function(wf) {
                return wf['specimen'] || [];
              }
            );
          },
          forms: function(cp, cpr, visit, specimen, currentUser, orderSpec, fdeRules, ExtensionsUtil) {
            return specimen.getForms().then(
              function(forms) {
                var ctxt = {cp: cp, cpr: cpr, visit: visit, specimen: specimen, user: currentUser};
                forms = ExtensionsUtil.getMatchingForms(forms, fdeRules, ctxt);
                return ExtensionsUtil.sortForms(forms, orderSpec);
              }
            )
          },
          records: function(specimen) {
            return specimen.getRecords();
          } 
        },
        abstract: true,
        parent: 'specimen-detail'
      })
      .state('specimen-detail.extensions.list', {
        url: '/list?formCtxtId&formId&recordId',
        templateUrl: 'modules/biospecimen/extensions/list.html',
        controller: 'FormsListCtrl', 
        parent: 'specimen-detail.extensions'
      })
      .state('specimen-detail.extensions.addedit', {
        url: '/addedit?formId&recordId&formCtxId&spe',
        templateUrl: 'modules/biospecimen/extensions/addedit.html',
        resolve: {
          formDef: function($stateParams, Form) {
            return Form.getDefinition($stateParams.formId);
          },
          postSaveFilters: function() {
            return [
              function(specimen, formName, formData) {
                if (formName == 'SpecimenCollectionEvent') {
                  specimen.setCollectionEvent(formData);
                } else if (formName == 'SpecimenReceivedEvent') {
                  specimen.setReceivedEvent(formData);
                } else if (formName == "SpecimenFrozenEvent" && formData.appData.newFreezeThawEvent &&
                  formData.incrementFreezeThaw == 1) {
                  ++specimen.freezeThawCycles;
                } else if (formName == "SpecimenThawEvent" && formData.appData.newFreezeThawEvent &&
                  formData.incrementFreezeThaw == 1) {
                  ++specimen.freezeThawCycles;
                }

                return formData
              }
            ];
          },
          viewOpts: function($state, $stateParams, formDef, cp, SpecimenEvent, LocationChangeListener) {
            var goBackFn = null;
            if ($stateParams.spe == 'true') {
              if ($stateParams.recordId) {
                goBackFn = LocationChangeListener.back;
              } else {
                goBackFn = function(eventData) {
                  if (!eventData) {
                    LocationChangeListener.back();
                  } else {
                    LocationChangeListener.allowChange();
                    $state.go('specimen-detail.event-overview',
                      {formId: eventData.containerId, recordId: eventData.id},
                      {location: 'replace'}
                    );
                  }
                }
              }
            }

            return {
              goBackFn: goBackFn,
              showSaveNext: $stateParams.spe != 'true',
              showActionBtns: !SpecimenEvent.isSysEvent(formDef.name) || SpecimenEvent.isEditable(formDef.name),
              showSaveDraft: cp.draftDataEntry,
              disabledFields: SpecimenEvent.getDisabledFields(formDef.name)
            };
          }
        },
        controller: 'FormRecordAddEditCtrl',
        parent: 'specimen-detail.extensions'
      })
      .state('specimen-detail.events', {
        url: '/events',
        templateUrl: 'modules/biospecimen/participant/specimen/events.html',
        controller: function($scope, $state, cp, cpr, visit, specimen, fdeRules, currentUser, ExtensionsUtil) {
          $scope.entityType = 'SpecimenEvent';
          $scope.extnState = 'specimen-detail.events';
          $scope.events = specimen.getEvents();
          $scope.eventForms = [];
          specimen.getForms({entityType: 'SpecimenEvent'}).then(
            function(eventForms) {
              var ctxt = {cp: cp, cpr: cpr, visit: visit, specimen: specimen, user: currentUser};
              $scope.eventForms = ExtensionsUtil.getMatchingForms(eventForms, fdeRules, ctxt);
            }
          );

          $scope.showOverview = function(event) {
            $state.go('specimen-detail.event-overview', {formId: event.formId, recordId: event.id});
          }

          $scope.deleteEvent = function(event) {
            var record = {recordId: event.id, formId: event.formId, formCaption: event.name};
            ExtensionsUtil.deleteRecord(
              record, 
              function() {
                var idx = $scope.events.indexOf(event);
                $scope.events.splice(idx, 1);
              }
            );
          }
        },
        resolve: {
          fdeRules: function(cp, CpConfigSvc) {
            return CpConfigSvc.getWorkflowData(cp.id, 'formDataEntryRules', {}).then(
              function(wf) {
                return wf['specimenEvent'] || [];
              }
            );
          }
        },
        parent: 'specimen-detail'
      })
      .state('specimen-detail.event-overview', {
        url: '/event-overview?formId&recordId',
        templateUrl: 'modules/biospecimen/participant/specimen/event-overview.html',
        controller: function($scope, isCoordinator, event, specimen, ExtensionsUtil, SpecimenEvent) {
          $scope.event = event;
          event.osEntity = specimen;
          event.isDeletable = !isCoordinator && (event.appData.sysForm != 'true' && event.appData.sysForm != true);
          event.isEditable = !isCoordinator && (event.isDeletable || SpecimenEvent.isEditable(event.name));

          $scope.deleteEvent = function(event) {
            var record = {recordId: event.id, formId: event.containerId, formCaption: event.name};
            ExtensionsUtil.deleteRecord(
              record,
              function() {
                $scope.back();
              }
            );
          }
        },
        resolve: {
          event: function($stateParams, Form) {
            return new Form({formId: $stateParams.formId}).getRecord($stateParams.recordId, {includeMetadata: true});
          }
        },
        parent: 'specimen-detail'
      })
      .state('specimen-create-derivative', {
        url: '/derivative',
        templateProvider: function(PluginReg, $q) {
          var defaultTmpl = "modules/biospecimen/participant/specimen/add-derivative.html";
          return $q.when(PluginReg.getTmpls("specimen-create-derivative", "page-body", defaultTmpl)).then(
            function(tmpls) {
              return '<div ng-include src="\'' + tmpls[0] + '\'"></div>';
            }
          );
        },
        resolve: {
          extensionCtxt: function(cp, Specimen) {
            return Specimen.getExtensionCtxt({cpId: cp.id});
          },

          derivedFields: function(cp, hasSde, CpConfigSvc) {
            if (!hasSde) {
              return {};
            }

            return CpConfigSvc.getCommonCfg(cp.id || -1, 'derivedSpecimens');
          }
        },
        controller: 'AddDerivativeCtrl',
        parent: 'specimen-root'
      })
      .state('specimen-create-aliquots', {
        url: '/aliquots',
        templateProvider: function(PluginReg, $q) {
          var defaultTmpl = "modules/biospecimen/participant/specimen/add-aliquots.html";
          return $q.when(PluginReg.getTmpls("specimen-create-aliquots", "page-body", defaultTmpl)).then(
            function(tmpls) {
              return '<div ng-include src="\'' + tmpls[0] + '\'"></div>';
            }
          );
        },
        resolve: {
          extensionCtxt: function(cp, Specimen) {
            return Specimen.getExtensionCtxt({cpId: cp.id});
          },

          createDerived: createDerived,

          aliquotFields: function(cp, hasSde, CpConfigSvc) {
            if (!hasSde) {
              return {};
            }

            return CpConfigSvc.getCommonCfg(cp.id || -1, 'aliquotsCollection');
          }
        },
        controller: 'AddAliquotsCtrl',
        parent: 'specimen-root'
      })
      .state('specimen-bulk-create-aliquots', {
        url: '/bulk-create-aliquots',
        templateUrl: 'modules/biospecimen/participant/specimen/bulk-create-aliquots.html',
        controller: 'BulkCreateAliquotsCtrl',
        resolve: {
          parentSpmns: function(SpecimensHolder) {
            var specimens = SpecimensHolder.getSpecimens();
            SpecimensHolder.setSpecimens([]);
            return specimens || [];
          },

          cp: function(parentSpmns, CollectionProtocol) {
            if (parentSpmns.length == 0) {
              return {};
            }

            var cpId = parentSpmns[0].cpId;
            if (parentSpmns.every(function(spmn) { return spmn.cpId == cpId })) {
              return CollectionProtocol.getById(cpId);
            } else {
              return {};
            }
          },

          cpr: function(parentSpmns, CollectionProtocolRegistration) {
            if (parentSpmns.length == 0) {
              return {};
            }

            var cprId = parentSpmns[0].cprId;
            if (parentSpmns.every(function(spmn) { return spmn.cprId == cprId })) {
              return CollectionProtocolRegistration.getById(cprId);
            } else {
              return {};
            }
          },

          containerAllocRules: function(cp, CpConfigSvc) {
            if (!cp.containerSelectionStrategy) {
              return [];
            }

            return CpConfigSvc.getWorkflowData(cp.id, 'auto-allocation').then(
              function(data) {
                return (data && data.rules && data.rules.length > 0) ? data.rules : [];
              }
            );
          },

          aliquotQtyReq: function(SettingUtil) {
            return SettingUtil.getSetting('biospecimen', 'mandatory_aliquot_qty').then(
              function(resp) {
                return resp.value == 'true' || resp.value == true || resp.value == 1 || resp.value == '1';
              }
            );
          },

          sysAliquotFmt: function(SettingUtil) {
            return SettingUtil.getSetting('biospecimen', 'aliquot_label_format').then(
              function(resp) {
                return resp.value;
              }
            );
          },

          createDerived: createDerived,

          hasSde: function($injector) {
            return $injector.has('sdeFieldsSvc');
          },

          cpDict: function(cp, hasSde, CpConfigSvc) {
            return !hasSde ? [] : CpConfigSvc.getDictionary(cp.id || -1, []);
          },

          aliquotFields: function(cp, hasSde, CpConfigSvc) {
            if (!hasSde) {
              return {};
            }

            return CpConfigSvc.getCommonCfg(cp.id || -1, 'aliquotsCollection');
          },

          spmnHeaders: function(cp, CpConfigSvc) {
            if (!cp.id) {
              return {};
            }

            return CpConfigSvc.getCommonCfg(cp.id, 'specimenHeader');
          },

          incrFreezeThawCycles: function(cp, CpConfigSvc) {
            return CpConfigSvc.getCommonCfg(cp.id, 'incrementFreezeThawCycles').then(defBoolTrue);
          },

          userRole: function(cp, AuthorizationService) {
            if (cp && cp.id > 0) {
              return AuthorizationService.getRole(cp);
            }

            return null;
          }
        },
        parent: 'signed-in'
      })
      .state('specimen-bulk-create-derivatives', {
        url: '/bulk-create-derivatives',
        templateUrl: 'modules/biospecimen/participant/specimen/bulk-create-derivatives.html',
        controller: 'BulkCreateDerivativesCtrl',
        resolve: {
          parentSpmns: function(SpecimensHolder) {
            var specimens = SpecimensHolder.getSpecimens();
            SpecimensHolder.setSpecimens([]);
            return specimens || [];
          },

          cp: function(parentSpmns, CollectionProtocol) {
            if (parentSpmns.length == 0) {
              return {};
            }

            var cpId = parentSpmns[0].cpId;
            if (parentSpmns.every(function(spmn) { return spmn.cpId == cpId })) {
              return CollectionProtocol.getById(cpId);
            } else {
              return {};
            }
          },

          cpr: function(parentSpmns, CollectionProtocolRegistration) {
            if (parentSpmns.length == 0) {
              return {};
            }

            var cprId = parentSpmns[0].cprId;
            if (parentSpmns.every(function(spmn) { return spmn.cprId == cprId })) {
              return CollectionProtocolRegistration.getById(cprId);
            } else {
              return {};
            }
          },

          hasSde: function($injector) {
            return $injector.has('sdeFieldsSvc');
          },

          cpDict: function(cp, hasSde, CpConfigSvc) {
            return !hasSde ? [] : CpConfigSvc.getDictionary(cp.id || -1, []);
          },

          derivedFields: function(cp, hasSde, CpConfigSvc) {
            if (!hasSde) {
              return {};
            }

            return CpConfigSvc.getCommonCfg(cp.id || -1, 'derivedSpecimens');
          },

          containerAllocRules: function(cp, CpConfigSvc) {
            if (!cp.containerSelectionStrategy) {
              return [];
            }

            return CpConfigSvc.getWorkflowData(cp.id, 'auto-allocation').then(
              function(data) {
                return (data && data.rules && data.rules.length > 0) ? data.rules : [];
              }
            );
          },

          spmnHeaders: function(cp, CpConfigSvc) {
            if (!cp.id) {
              return {};
            }

            return CpConfigSvc.getCommonCfg(cp.id, 'specimenHeader');
          },

          incrFreezeThawCycles: function(cp, CpConfigSvc) {
            return CpConfigSvc.getCommonCfg(cp.id, 'incrementFreezeThawCycles').then(defBoolTrue);
          },

          userRole: function(cp, AuthorizationService) {
            if (cp && cp.id > 0) {
              return AuthorizationService.getRole(cp);
            }

            return null;
          }
        },
        parent: 'signed-in'
      })
      .state('specimen-create-pooled', {
        url: '/add-pooled-specimen?visitId&cprId',
        templateUrl: 'modules/biospecimen/participant/specimen/add-pooled.html',
        controller: 'AddPooledSpecimenCtrl',
        resolve: {
          cpr: function($stateParams, cp, Participant, CollectionProtocolRegistration) {
            if (!!$stateParams.cprId && $stateParams.cprId > 0) {
              return CollectionProtocolRegistration.getById($stateParams.cprId);
            } else if (cp.specimenCentric) {
              var participant = new Participant({source: 'OpenSpecimen'});
              return new CollectionProtocolRegistration({
                cpId: cp.id, cpShortTitle: cp.shortTitle,
                registrationDate: new Date(),
                specimenLabelFmt: cp.specimenLabelFmt,
                derivativeLabelFmt: cp.derivativeLabelFmt,
                aliquotLabelFmt: cp.aliquotLabelFmtToUsecpId,
                participant: participant
              });
            }

            return null;
          },

          visit: function($stateParams, cp, Visit) {
            if (!!$stateParams.visitId && $stateParams.visitId > 0) {
              return Visit.getById($stateParams.visitId);
            } else if (cp.specimenCentric) {
              return new Visit({cpId: cp.id});
            }

            return null;
          },

          extensionCtxt: function(cp, Specimen) {
            return Specimen.getExtensionCtxt({cpId: cp.id});
          },

          cpDict: function($stateParams, hasSde, CpConfigSvc) {
            if (!hasSde) {
              return [];
            }

            return CpConfigSvc.getDictionary($stateParams.cpId, []);
          },

          layout: function($stateParams, hasSde, CpConfigSvc) {
            if (!hasSde) {
              return [];
            }

            return CpConfigSvc.getLayout($stateParams.cpId, []);
          },

          onValueChangeCb: function($stateParams, hasSde, CpConfigSvc) {
            if (!hasSde) {
              return undefined;
            }

            return CpConfigSvc.getOnValueChangeCallbacks($stateParams.cpId, ['dictionary']);
          },

          hasDict: function(hasSde, cpDict) {
            return hasSde && cpDict.length > 0;
          },

          additionalLabelAutoGen: function(cp, SettingUtil) {
            if (!!cp.additionalLabelFmt) {
              return true;
            }

            return SettingUtil.getSetting('biospecimen', 'specimen_addl_label_format').then(
              function(setting) {
                return !!setting.value;
              }
            );
          },

          barcodingEnabled: function(cp, SettingUtil) {
            if (cp.barcodingEnabled) {
              return true;
            }

            return SettingUtil.getSetting('biospecimen', 'enable_spmn_barcoding').then(
              function(setting) {
                return setting.value.toLowerCase() == 'true';
              }
            );
          },

          spmnBarcodesAutoGen: function(cp, SettingUtil) {
            if (!!cp.specimenBarcodeFmt) {
              return true;
            }

            return SettingUtil.getSetting('biospecimen', 'specimen_barcode_format').then(
              function(setting) {
                return !!setting.value;
              }
            );
          },

          imagingEnabled: function(SettingUtil) {
            return SettingUtil.getSetting('biospecimen', 'imaging').then(
              function(setting) {
                return setting.value == 'true' || setting.value == true;
              }
            );
          },

          spmnCollFields: function(cp, CpConfigSvc) {
            return CpConfigSvc.getWorkflowData(cp.id, 'specimenCollection').then(
              function(data) {
                if (data && !angular.equals(data, {})) {
                  return data;
                }

                return CpConfigSvc.getWorkflowData(-1, 'specimenCollection').then(
                  function(data) {
                    return data || {};
                  }
                );
              }
            );
          }
        },
        parent: 'cp-view'
      })
      .state('specimen-bulk-edit', {
        url: '/bulk-edit-specimens',
        templateUrl: "modules/biospecimen/participant/specimen/bulk-edit.html",
        controller: 'BulkEditSpecimensCtrl',
        resolve: {
          hasSde: function($injector) {
            return $injector.has('sdeFieldsSvc');
          },

          cpId: function(SpecimensHolder) {
            var spmns = (SpecimensHolder.getSpecimens() || []);
            var cpId  = spmns.length > 0 ? spmns[0].cpId : -1;
            for (var i = 1; i < spmns.length; ++i) {
              if (spmns[i].cpId != cpId) {
                return -1;
              }
            }

            return cpId;
          },

          cpDict: function(cpId, hasSde, CpConfigSvc) {
            if (!hasSde) {
              return {cpId: cpId, fields: []};
            }

            return CpConfigSvc.getBulkUpdateDictionary(cpId);
          },

          customFields: function(cpId, cpDict, CpConfigSvc) {
            if (cpDict.cpId != -1 || cpDict.fields.length == 0) {
              return [];
            }

            return CpConfigSvc.getSpecimenCustomFields(cpId);
          }
        },
        parent: 'signed-in'
      })
      .state('bulk-add-event', {
        url: '/bulk-add-event',
        templateUrl: 'modules/biospecimen/participant/specimen/bulk-add-event.html',
        controller: 'BulkAddEventCtrl',
        resolve: {
          events: function(CollectionProtocol) {
            return new CollectionProtocol({id: -1}).getForms(['SpecimenEvent']);
          },

          event: function() {
            return null;
          },

          acceptablePv: function() {
            return null;
          }
        },
        parent: 'signed-in'
      })
      .state('specimen-search', {
        url: '/specimen-search',
        templateUrl: 'modules/biospecimen/participant/specimen/search-result.html',
        resolve: {
          specimens: function(SpecimenSearchSvc) {
            return SpecimenSearchSvc.getSpecimens();
          },

          searchKey: function(SpecimenSearchSvc) {
            return SpecimenSearchSvc.getSearchKey();
          }
        },
        controller: 'SpecimenResultsView',
        parent: 'signed-in'
      })
      .state('bulk-transfer-specimens', {
        url: '/bulk-transfer-specimens',
        templateUrl: 'modules/biospecimen/participant/specimen/bulk-transfer-specimens.html',
        controller: 'BulkTransferSpecimensCtrl',
        parent: 'signed-in'
      });
  })

  .run(function(QuickSearchSvc, ExtensionsUtil) {
    var opts = {
      caption: 'entities.specimen',
      vueUrl: 'specimen-resolver/:entityId',
      vueView: 'ParticipantsListItemSpecimenDetail.Overview'
    };
    QuickSearchSvc.register('specimen', opts);


    ExtensionsUtil.registerView(
      'ContainerTransferEvent', 'modules/biospecimen/participant/specimen/transfer-event.html');
    ExtensionsUtil.registerView(
      'SpecimenDistributedEvent', 'modules/biospecimen/participant/specimen/distributed-event.html');
    ExtensionsUtil.registerView(
      'SpecimenChildrenEvent', 'modules/biospecimen/participant/specimen/processed-event.html');
    ExtensionsUtil.registerView(
      'SpecimenReservedEvent', 'modules/biospecimen/participant/specimen/reserved-event.html');
    ExtensionsUtil.registerView(
      'SpecimenReservationCancelledEvent', 'modules/biospecimen/participant/specimen/reserved-event.html');
    ExtensionsUtil.registerView(
      'SpecimenReturnEvent', 'modules/biospecimen/participant/specimen/return-event.html');
    ExtensionsUtil.registerView(
      'SpecimenShipmentShippedEvent', 'modules/biospecimen/participant/specimen/shipment-event.html');
    ExtensionsUtil.registerView(
      'SpecimenShipmentReceivedEvent', 'modules/biospecimen/participant/specimen/shipment-event.html');
    ExtensionsUtil.registerView(
      'SpecimenTransferEvent', 'modules/biospecimen/participant/specimen/transfer-event.html');
  });
