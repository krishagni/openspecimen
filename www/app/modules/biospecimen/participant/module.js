
angular.module('os.biospecimen.participant', 
  [ 
    'ui.router',
    'os.biospecimen.common',
    'os.biospecimen.participant.root',
    'os.biospecimen.participant.list',
    'os.biospecimen.participant.detail',
    'os.biospecimen.participant.overview',
    'os.biospecimen.participant.visits',
    'os.biospecimen.participant.addedit',
    'os.biospecimen.participant.bulkregistration',
    'os.biospecimen.participant.newreg',
    'os.biospecimen.participant.collect-specimens',
    'os.biospecimen.participant.consents',
    'os.biospecimen.visit',
    'os.biospecimen.specimen',
    'os.biospecimen.extensions.list',
    'os.biospecimen.extensions.util',
    'os.biospecimen.extensions.addedit-record',
    'os.biospecimen.specimenkit'
  ])

  .config(function($stateProvider) {
    $stateProvider
      .state('cp-view', {
        url: '/cp-view/:cpId',
        template: '<div> ' +
                  '  <span ng-if="cp.draftMode && cpViewCtx.versioningEnabled" class="os-cp-draft-marker">DRAFT</span> ' +
                  '  <div ui-view></div> ' +
                  '</div>',
        controller: function($scope, cp, cpViewCtx, workflows, SettingUtil) {
          $scope.cp = cp;
          $scope.cpViewCtx = cpViewCtx;
          cpViewCtx.workflows = workflows;

          cpViewCtx.codingEnabled = $scope.global.appProps.cp_coding_enabled;

          var sites = cp.cpSites.map(function(cpSite) { return cpSite.siteName; });
          $scope.cpReadOpts = {resource: 'CollectionProtocol', operations: ['Read']};
          $scope.partRegOpts = {cp: cp.shortTitle, sites: sites, resource: 'ParticipantPhi', operations: ['Create']};
          $scope.orderCreateOpts = {cp: cp.shortTitle, sites: sites, resource: 'Order', operations: ['Create']};
          $scope.shipmentCreateOpts = {cp: cp.shortTitle, sites: sites, resource: 'ShippingAndTracking', operations: ['Create']};
          $scope.consentUpdateOpts = {cp: cp.shortTitle, sites: sites, resource: 'Consent', operations: ['Update']};
          $scope.consentLockOpts = {cp: cp.shortTitle, sites: sites, resource: 'Consent', operations: ['Lock']};
          $scope.consentUnlockOpts = {cp: cp.shortTitle, sites: sites, resource: 'Consent', operations: ['Unlock']};
          $scope.visitUpdateOpts = {cp: cp.shortTitle, sites: sites, resource: 'Visit', operations: ['Update']};
          $scope.specimenReadOpts = {
            cp: cp.shortTitle,
            sites: sites,
            resources: ['Specimen', 'PrimarySpecimen'],
            operations: ['Read']
          };

          $scope.specimenUpdateOpts = {
            cp: cp.shortTitle,
            sites: sites,
            resources: ['Specimen', 'PrimarySpecimen'],
            operations: ['Update']
          };

          $scope.allSpecimenUpdateOpts = {
            cp: cp.shortTitle,
            sites: sites,
            resource: 'Specimen',
            operations: ['Update']
          };

          $scope.specimenDeleteOpts = {
            cp: cp.shortTitle,
            sites: sites,
            resources: ['Specimen', 'PrimarySpecimen'],
            operations: ['Delete']
          };

          SettingUtil.getSetting('biospecimen', 'cp_versioning_enabled').then(
            function(setting) {
              cpViewCtx.versioningEnabled = (setting.value == 'true' || setting.value == true);
            }
          );
        },
        resolve: {
          cp: function($stateParams, CollectionProtocol) {
            return CollectionProtocol.getById($stateParams.cpId);
          },

          receiveSpecimensWfId: function($stateParams, CpConfigSvc) {
            return CpConfigSvc.getCommonCfg($stateParams.cpId, 'receiveSpecimensWorkflow');
          },

          cpViewCtx: function($q, $injector, cp, currentUser, authInit, receiveSpecimensWfId, AuthorizationService) {
            var participantUpdateAllowed = AuthorizationService.isAllowed({
              resource: 'ParticipantPhi',
              operations: ['Update'],
              cp: cp.shortTitle,
              sites: cp.cpSites.map(function(cpSite) { return cpSite.siteName; })
            });

            var participantDeleteAllowed = AuthorizationService.isAllowed({
              resource: 'ParticipantPhi',
              operations: ['Delete'],
              cp: cp.shortTitle,
              sites: cp.cpSites.map(function(cpSite) { return cpSite.siteName; })
            });

            var participantEximAllowed = AuthorizationService.isAllowed({
              resource: 'ParticipantPhi',
              operations: ['Export Import'],
              cp: cp.shortTitle
            });

            var visitReadAllowed = AuthorizationService.isAllowed({
              resource: 'Visit',
              operations: ['Read'],
              cp: cp.shortTitle,
              sites: cp.cpSites.map(function(cpSite) { return cpSite.siteName; })
            });

            var visitEximAllowed = AuthorizationService.isAllowed({
              resource: 'Visit',
              operations: ['Export Import'],
              cp: cp.shortTitle
            });

            var spmnEximAllowed = AuthorizationService.isAllowed({
              resources: ['Specimen', 'PrimarySpecimen'],
              operations: ['Export Import'],
              cp: cp.shortTitle
            });

            var allSpmnEximAllowed = AuthorizationService.isAllowed({
              resources: ['Specimen'],
              operations: ['Export Import'],
              cp: cp.shortTitle
            });

            var spmnReadAllowed = AuthorizationService.isAllowed({
              resources: ['Specimen', 'PrimarySpecimen'],
              operations: ['Read'],
              cp: cp.shortTitle,
              sites: cp.cpSites.map(function(cpSite) { return cpSite.siteName; })
            });

            var consentsReadAllowed = AuthorizationService.isAllowed({
              resources: ['Consent'],
              operations: ['Read'],
              cp: cp.shortTitle,
              sites: cp.cpSites.map(function(cpSite) { return cpSite.siteName; })
            });

            var consentsUpdateAllowed = AuthorizationService.isAllowed({
              resources: ['Consent'],
              operations: ['Update'],
              cp: cp.shortTitle,
              sites: cp.cpSites.map(function(cpSite) { return cpSite.siteName; })
            });

            var consentsEximAllowed = AuthorizationService.isAllowed({
              resource: 'Consent',
              operations: ['Export Import'],
              cp: cp.shortTitle
            });

            var queryReadAllowed = AuthorizationService.isAllowed({
              resources: ['Query'],
              operations: ['Read']
            });

            var surveys = null;

            return {
              participantImportAllowed: participantEximAllowed,
              visitImportAllowed: visitEximAllowed,
              specimenImportAllowed: spmnEximAllowed,
              participantExportAllowed: participantEximAllowed,
              visitExportAllowed: visitEximAllowed,
              specimenExportAllowed: spmnEximAllowed,
              participantUpdateAllowed: participantUpdateAllowed,
              participantDeleteAllowed: participantDeleteAllowed,
              visitReadAllowed: visitReadAllowed,
              spmnReadAllowed: spmnReadAllowed,
              allSpmnEximAllowed: allSpmnEximAllowed,
              consentsReadAllowed: consentsReadAllowed,
              consentsUpdateAllowed: consentsUpdateAllowed,
              consentsEximAllowed: consentsEximAllowed,
              visitLevelConsents: cp.visitLevelConsents == true && $injector.has('ecCpDocument'),
              queryReadAllowed: queryReadAllowed,
              recvSpmnsWfId: receiveSpecimensWfId,
              getSurveys: function() {
                if (surveys || !$injector.has('Survey')) {
                  var q = $q.defer();
                  q.resolve(surveys);
                  return q.promise;
                }

                var provider = $injector.get('Survey');
                return provider.query({cpId: cp.id}).then(
                  function(dbSurveys) {
                    surveys = dbSurveys;
                    return surveys;
                  }
                );
              }
            }
          },

          listView: function(cp, CpConfigSvc) {
            var defListViewState = !cp.specimenCentric ? 'participant-list' : 'cp-specimens';
            return CpConfigSvc.getListView(cp.id, defListViewState);
          },

          twoStepReg: function(SettingUtil) {
            return SettingUtil.getSetting('biospecimen', 'two_step_patient_reg').then(
              function(setting) {
                return setting.value == 'true';
              }
            );
          },

          mrnAccessRestriction: function(SettingUtil) {
            return SettingUtil.getSetting('biospecimen', 'mrn_restriction_enabled').then(
              function(setting) {
                return setting.value == 'true';
              }
            );
          },

          mobileDataEntryEnabled: function(cp, CpConfigSvc) {
            return CpConfigSvc.getWorkflowData(cp.id, 'mobile-app', {}).then(
              function(wf) {
                if (!wf.forms) {
                  return false;
                }

                var forms = wf.forms;
                if (forms.registration && !!forms.registration.dataEntry) {
                  return true;
                }

                if (forms.specimen && (!!forms.specimen.primaryDataEntry || !!forms.specimen.aliquotDataEntry)) {
                  return true;
                }

                return false;
              }
            );
          },

          hasSde: function($injector) {
            return $injector.has('sdeFieldsSvc');
          },

          userRole: function(authInit, cp, AuthorizationService) {
            return AuthorizationService.getRole(cp);
          },

          isCoordinator: function(userRole, cpViewCtx, SettingUtil) {
            return SettingUtil.getSetting('biospecimen', 'coordinator_role_name').then(
              function(setting) {
                cpViewCtx.isCoordinator = (setting.value == userRole);
                return cpViewCtx.isCoordinator;
              }
            );
          },

          storeAllowed: function(cp, authInit, AuthorizationService) {
            var sites = cp.cpSites.map(function(cpSite) { return cpSite.siteName; });
            return AuthorizationService.isAllowed({sites: sites, resource: 'StorageContainer', operations: ['Read']});
          },

          hasWorkflowModule: function($injector) {
            return $injector.has('WorkflowInstance');
          },

          workflows: function(cp, hasWorkflowModule, CpConfigSvc) {
            if (!hasWorkflowModule) {
              return {};
            }

            return CpConfigSvc.getWorkflowData(cp.id, 'workflows').then(
              function(workflows) {
                if (Object.keys(workflows || {}).length > 0) {
                  return workflows;
                }

                return CpConfigSvc.getWorkflowData(-1, 'workflows').then(
                  function(workflows) {
                    return workflows || {};
                  }
                );
              }
            );
          }
        },
        parent: 'signed-in',
        abstract: true
      })
      .state('cp-summary-view', {
        url: '/summary-view',
        controller: function($state, cp, summaryView) {
          $state.go(summaryView, {cpId: cp.id}, {location: 'replace'});
        },
        resolve: {
          summaryView: function(listView, CpConfigSvc) {
            var summaryView = listView;

            var summarySt = CpConfigSvc.getSummaryState();
            if (summarySt) {
              summaryView = summarySt;
            }

            return summaryView;
          }
        },
        parent: 'cp-view'
      })
      .state('cp-list-view', {
        url: '/list-view',
        controller: function($state, cp, listView) {
          $state.go(listView, {cpId: cp.id}, {location: 'replace'});
        },
        parent: 'cp-view'
      })
      .state('cp-list-view-root', {
        templateUrl: 'modules/biospecimen/participant/list-view.html',
        controller: function($scope, $state, cp, cpViewCtx, defSopDoc, defSopUrl, spmnListCfg, VueApp, Alerts) {
          var ctx = $scope.listViewCtx = {
            sopDocDownloadUrl: cp.getSopDocDownloadUrl(),
            spmnListCfg: spmnListCfg,
            showImport: (cpViewCtx.visitImportAllowed ||
                         cpViewCtx.specimenImportAllowed ||
                         (!cp.specimenCentric && cpViewCtx.participantImportAllowed)),
            showExport: (cpViewCtx.visitExportAllowed ||
                         cpViewCtx.specimenExportAllowed ||
                         (!cp.specimenCentric && cpViewCtx.participantExportAllowed))
          };

          ctx.sopDoc = cp.sopDocumentName;
          if (!ctx.sopDoc) {
            ctx.sopUrl = cp.sopDocumentUrl;
            if (!ctx.sopUrl) {
              ctx.sopDoc = defSopDoc.value;
              if (!ctx.sopDoc) {
                ctx.sopUrl = defSopUrl.value;
              }
            }
          }

          $scope.generateCpReport = function() {
            cp.generateReport().then(
              function() {
                Alerts.success('participant.report_gen_initiated', cp);
              }
            );
          }

          $scope.receiveSpecimens = function() {
            VueApp.setVueView('task-manager/workflows/' + cpViewCtx.recvSpmnsWfId + '/create-instance');
          }
        },
        resolve: {
          defSopDoc: function(cp, SettingUtil) {
            if (!!cp.sopDocumentName || !!cp.sopDocumentUrl) {
              return null;
            }

            return SettingUtil.getSetting('biospecimen', 'cp_sop_doc');
          },

          defSopUrl: function(cp, defSopDoc, SettingUtil) {
            if (!!cp.sopDocumentName || !!cp.sopDocumentUrl || !!defSopDoc.value) {
              return null;
            }

            return SettingUtil.getSetting('biospecimen', 'cp_sop_doc_url');
          },

          spmnListCfg: function(cp, cpViewCtx, CpConfigSvc) {
            if (cpViewCtx.spmnReadAllowed) {
              return CpConfigSvc.getListConfig(cp, 'specimen-list-view');
            } else {
              return null;
            }
          },

          addParticipantWorkflow: function(cp, CpConfigSvc) {
            return CpConfigSvc.getCommonCfg(cp.id || -1, 'addParticipant').then(
              function(cfg) {
                return cfg && cfg.workflow;
              }
            );
          }
        },
        parent: 'cp-view',
        abstract: true
      })
      .state('participant-list', {
        url: '/participants?filters',
        templateUrl: 'modules/biospecimen/participant/list.html',
        controller: 'ParticipantListCtrl',
        metaInfo: {
          button: {
            icon: 'fa-group',
            label: 'cp.view_participants'
          }
        },
        parent: 'cp-list-view-root'
      })
      .state('cp-specimens', {
        url: '/specimens?filters',
        templateUrl: 'modules/biospecimen/participant/specimens-list.html',
        controller: 'SpecimensListViewCtrl',
        resolve: {
          accessAllowed: function(cp, cpViewCtx, Alerts) {
            if (!cpViewCtx.spmnReadAllowed) {
              Alerts.error('specimens.no_read_access', {cp: cp});
              throw "Access to specimens of the CP: " + cp.shortTitle + " not allowed!";
            }

            return null;
          },

          sdeConfigured: function($injector, cp, CpConfigSvc) {
            if (!$injector.has('sdeFieldsSvc')) {
              return false;
            }

            return CpConfigSvc.getWorkflowData(cp.id, 'sde').then(
              function(data) {
                return !!data.singlePatientSamples;
              }
            );
          },

          spmnReqs: function(cp, CpConfigSvc) {
            if (!cp.specimenCentric) {
              return [];
            }

            return CpConfigSvc.getCommonCfg(cp.id, 'addSpecimen', {}).then(
              function(data) {
                return (data && data.requirements) || [];
              }
            );
          }
        },
        metaInfo: {
          button: {
            icon: 'fa-flask',
            label: 'cp.view_specimens'
          }
        },
        parent: 'cp-list-view-root'
      })
      .state('import-cp-objs', {
        url: '/import-cp-objs',
        templateUrl: 'modules/common/import/add.html',
        controller: 'ImportObjectCtrl',
        resolve: {
          allowedEntityTypes: function(cp, cpViewCtx) {
            var entityTypes = [];
            if (!cp.specimenCentric && cpViewCtx.participantImportAllowed) {
              entityTypes = entityTypes.concat(['CommonParticipant', 'Participant']);
            }

            if (!cp.specimenCentric && cpViewCtx.consentsEximAllowed) {
              entityTypes.push('Consent');
            }

            if (!cp.specimenCentric && cpViewCtx.visitImportAllowed) {
              entityTypes.push('SpecimenCollectionGroup');
            }

            if (cpViewCtx.specimenImportAllowed) {
              entityTypes.push('Specimen');
              entityTypes.push('SpecimenEvent');
            }

            if (cpViewCtx.allSpmnEximAllowed) {
              entityTypes.push('DerivativeAndAliquots');
            }

            return entityTypes;
          },

          forms: function(cp, allowedEntityTypes) {
            return allowedEntityTypes.length > 0 ? cp.getForms(allowedEntityTypes) : [];
          },

          importDetail: function(cp, allowedEntityTypes, forms, ImportUtil) {
            return ImportUtil.getImportDetail(cp, allowedEntityTypes, forms);
          }
        },
        parent: 'cp-view'
      })
      .state('import-cp-jobs', {
        url: '/import-cp-jobs',
        templateUrl: 'modules/common/import/list.html',
        controller: 'ImportJobsListCtrl',
        resolve: {
          importDetail: function(cp, ImportUtil) {
            var objectTypes = [
              'cpr', 'participant', 'consent', 'econsentsDocumentResponse',
              'visit', 'specimen', 'specimenDerivative', 'specimenAliquot',
              'masterSpecimen', 'specimenDisposal', 'extensions'
            ];

            return {
              breadcrumbs: [
                {state: 'cp-list-view', title:  cp.shortTitle,     params: '{cpId:' + cp.id + '}'}
              ],
              title: 'bulk_imports.jobs_list',
              objectTypes: objectTypes.concat(ImportUtil.getPluginTypes()),
              objectParams: {cpId: cp.id}
            }
          }
        },
        parent: 'cp-view'
      })
      .state('export-cp-objs', {
        url: '/export-cp-objs',
        templateUrl: 'modules/common/export/add.html',
        controller: 'AddEditExportJobCtrl',
        resolve: {
          allowedEntityTypes: function(cp, cpViewCtx) {
            var entityTypes = [];
            if (!cp.specimenCentric && cpViewCtx.participantExportAllowed) {
              entityTypes = entityTypes.concat(['CommonParticipant', 'Participant']);
            }

            if (!cp.specimenCentric && cpViewCtx.consentsEximAllowed) {
              entityTypes.push('Consent');
            }

            if (!cp.specimenCentric && cpViewCtx.visitExportAllowed) {
              entityTypes.push('SpecimenCollectionGroup');
            }

            if (cpViewCtx.specimenExportAllowed) {
              entityTypes.push('Specimen');
              entityTypes.push('SpecimenEvent');
            }

            return entityTypes;
          },

          forms: function(cp, allowedEntityTypes) {
            return allowedEntityTypes.length > 0 ? cp.getForms(allowedEntityTypes) : [];
          },

          exportDetail: function(cp, allowedEntityTypes, forms, ExportUtil) {
            return ExportUtil.getExportDetail(cp, allowedEntityTypes, forms);
          }
        },
        parent: 'cp-view'
      })
      .state('email-forms', {
        url: '/email-forms',
        templateUrl: 'modules/biospecimen/participant/email-forms.html',
        controller: 'EmailFormsCtrl',
        resolve: {
          forms: function(cp, cpViewCtx) {
            if (!cpViewCtx.participantUpdateAllowed) {
              return [];
            }

            return cp.getForms(['CommonParticipant', 'Participant']);
          },

          documents: function($injector, cp, cpViewCtx) {
            if (!$injector.has('ecDocument') || !cpViewCtx.consentsUpdateAllowed) {
              return [];
            }

            return $injector.get('ecDocument').query({cpId: cp.id, maxResults: 1000});
          }
        },
        parent: 'cp-view'
      })
      .state('participant-bulk-edit', {
        url: '/bulk-edit',
        templateUrl: "modules/biospecimen/participant/bulk-edit.html",
        controller: 'BulkEditParticipantsCtrl',
        resolve: {
          cpDict: function(cp, hasSde, CpConfigSvc) {
            if (!hasSde) {
              return {cpId: cp.id, fields: []};
            }

            return CpConfigSvc.getBulkUpdateDictionary(cp.id);
          },

          customFields: function(cp, cpDict, CpConfigSvc) {
            if (cpDict.cpId != -1 || cpDict.fields.length == 0) {
              return [];
            }

            return CpConfigSvc.getCprCustomFields(cp.id);
          }
        },
        parent: 'cp-view'
      })
      .state('participant-root', {
        url: '/participants/:cprId',
        template: '<div ui-view></div>',
        resolve: {
          cpr: function($stateParams, cp, Participant, CollectionProtocolRegistration) {
            if (!!$stateParams.cprId && $stateParams.cprId > 0) {
              return CollectionProtocolRegistration.getById($stateParams.cprId);
            } 

            var participant = new Participant({source: 'OpenSpecimen'});
            return new CollectionProtocolRegistration({
              cpId: cp.id, cpShortTitle: cp.shortTitle,
              registrationDate: new Date(), participant: participant,
              specimenLabelFmt: cp.specimenLabelFmt,
              derivativeLabelFmt: cp.derivativeLabelFmt,
              aliquotLabelFmt: cp.aliquotLabelFmtToUse
            });
          },

          sysDict: function($stateParams, hasSde, CpConfigSvc) {
            if (!hasSde) {
              return [];
            }

            return undefined || []; // CpConfigSvc.getDictionary(null);
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

          hasDict: function(hasSde, sysDict, cpDict) {
            return hasSde && (cpDict.length > 0 || sysDict.length > 0);
          },

          sysLookupFields: function(CpConfigSvc) {
            return CpConfigSvc.getWorkflowData(-1, 'participantLookup', []);
          },

          lookupFieldsCfg: function(hasSde, twoStepReg, sysLookupFields, cpDict) {
            var configured = true;
            if (!hasSde) {
              return {configured: twoStepReg, fields: []};
            }

            var cprFields = cpDict.filter(function(field) { return field.name.indexOf('cpr.') == 0; });
            var lookupFields = cprFields.filter(function(field) { return field.participantLookup == true; });
            if (lookupFields.length == 0) {
              configured = false;
              lookupFields = cprFields;
            }

            if (lookupFields.length > 0) {
              return {configured: configured, fields: lookupFields};
            }

            return {configured: sysLookupFields.length > 0, fields: sysLookupFields};
          },

          hasFieldsFn: function($injector, hasDict, sysDict, cpDict) {
            return function(inObjs, exObjs) {
              if (!hasDict) {
                return true;
              }

              return $injector.get('sdeFieldsSvc').commonFns()
                .hasFields(sysDict, cpDict, inObjs, exObjs);
            }
          },

          pendingSpmnsDispInterval: function(SettingUtil) {
            return SettingUtil.getSetting('biospecimen', 'pending_spmns_disp_interval');
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

          headers: function($q, cp, CpConfigSvc) {
            var ph = CpConfigSvc.getCommonCfg(cp.id, 'participantHeader');
            var vh = CpConfigSvc.getCommonCfg(cp.id, 'visitHeader');
            var sh = CpConfigSvc.getCommonCfg(cp.id, 'specimenHeader');

            return $q.all([ph, vh, sh]).then(
              function(headers) {
                return {participant: headers[0], visit: headers[1], specimen: headers[2]};
              }
            );
          },

          aliquotQtyReq: function(SettingUtil) {
            return SettingUtil.getSetting('biospecimen', 'mandatory_aliquot_qty').then(
              function(resp) {
                return resp.value == 'true' || resp.value == true || resp.value == '1' || resp.value == 1;
              }
            );
          },

          visitsTab: function(cp, $q, CpConfigSvc) {
            var allCfgQ = CpConfigSvc.getWorkflowData(-1,    'visitsTab', {});
            var cpCfgQ  = CpConfigSvc.getWorkflowData(cp.id, 'visitsTab', {});
            return $q.all([allCfgQ, cpCfgQ]).then(
              function(tabs) {
                var allCfg = angular.copy(tabs[0]);
                return angular.extend(allCfg, tabs[1]);
              }
            );
          },

          participantSpmnsViewState: function(cp, cpr, pendingSpmnsDispInterval, visitsTab, ParticipantSpecimensViewState) {
            var st = new ParticipantSpecimensViewState(cp, cpr, +pendingSpmnsDispInterval.value);
            st.config = {visitsTab: visitsTab};
            return st;
          },

          hasConsented: function($injector, cpr, cp) {
            if (!$injector.has('ecValidation') || cpr.id <= 0 || !cpr.id || cp.visitLevelConsents) {
              cpr.hasConsented = true;
              return true;
            }

            return $injector.get('ecValidation').getParticipantStatus(cpr.id).then(
              function(result) {
                cpr.hasConsented = (result.status == true);
                return cpr.hasConsented;
              }
            );
          }
        },
        controller: 'ParticipantRootCtrl',
        parent: 'cp-view',
        abstract: true
      })
      .state('participant-addedit', {
        url: '/addedit-participant?twoStep',
        templateProvider: function($stateParams, $q, CpConfigSvc, PluginReg) {
          return $q.when(CpConfigSvc.getRegParticipantTmpl($stateParams.cpId, $stateParams.cprId)).then(
            function(tmpl) {
              var tmpls = PluginReg.getTmpls("participant-addedit", "page-body", tmpl); 
              return '<div ng-include src="\'' + tmpls[0] + '\'"></div>';
            }
          );
        },
        controllerProvider: function($stateParams, CpConfigSvc) {
          return CpConfigSvc.getRegParticipantCtrl($stateParams.cpId, $stateParams.cprId);
        },
        resolve: {
          extensionCtxt: function(cp, Participant) {
            return Participant.getExtensionCtxt({cpId: cp.id});
          },
          addPatientOnLookupFail: function(SettingUtil) {
            return SettingUtil.getSetting('biospecimen', 'add_patient_on_lookup_fail').then(
              function(setting) {
                return setting.value == 'true';
              }
            );
          },
          lockedFields: function(cpr, CpConfigSvc) {
            var participant = cpr.participant || {};
            return CpConfigSvc.getLockedParticipantFields(participant.source || 'OpenSpecimen');
          },
          hasConsentRules: function($injector, cp) {
            if (!$injector.has('ecValidation')) {
              return false;
            }

            return $injector.get('ecValidation').getCpRules(cp.id).then(
              function(cpRules) {
                return cpRules && cpRules.rules && cpRules.rules.length > 0;
              }
            );
          },
          tmWorkflowId: function($injector, cp, cpr, hasConsentRules, workflows, CpConfigSvc) {
            if (!!cpr.id || !$injector.has('Workflow') || hasConsentRules) {
              return -1;
            }

            if (workflows.addVisit > 0) {
              return workflows.addVisit;
            }

            return CpConfigSvc.getWorkflowData(cp.id, 'specimenCollection').then(
              function(data) {
                if (data && data.workflowId > 0) {
                  return data.workflowId;
                }

                return CpConfigSvc.getWorkflowData(-1, 'specimenCollection').then(
                  function(data) {
                    return data && data.workflowId;
                  }
                );
              }
            );
          },
          cpEvents: function(cp, cpr, hasConsentRules, CollectionProtocolEvent) {
            if (!!cpr.id || hasConsentRules) {
              return null;
            }

            return CollectionProtocolEvent.listFor(cp.id).then(
              function(events) {
                return events.filter(function(event) { return event.activityStatus == 'Active'; });
              }
            );
          }
        },
        parent: 'participant-root'
      })
      .state('participant-bulkreg', {
        url: '/bulk-registration',
        templateUrl: 'modules/biospecimen/participant/bulk-registration.html',
        controller: 'BulkRegistrationCtrl',
        resolve: {
          events: function(cp, CollectionProtocolEvent) {
            return CollectionProtocolEvent.listFor(cp.id).then(
              function(events) {
                return events.filter(function(event) { return event.activityStatus != 'Closed'; });
              }
            );
          }
        },
        parent: 'participant-root'
      })
      .state('participant-detail', {
        url: '/detail',
        templateUrl: 'modules/biospecimen/participant/detail.html',
        resolve: {
          allowedEvents: function(visitsTab, cpr) {
            return cpr.getAllowedEvents(visitsTab);
          },

          visits: function($stateParams, cpViewCtx, allowedEvents, visitsTab, Visit) {
            if (!cpViewCtx.visitReadAllowed) {
              return null;
            }

            return Visit.listFor($stateParams.cprId, cpViewCtx.spmnReadAllowed, visitsTab.sortByDates).then(
              function(visits) {
                return visits.filter(
                  function(v) {
                    if (!!v.status && v.status != 'Pending') {
                      return true;
                    }

                    return !allowedEvents || !v.eventCode || allowedEvents.indexOf(v.eventCode) != -1;
                  }
                );
              }
            );
          }
        },
        controller: 'ParticipantDetailCtrl',
        parent: 'participant-root'
      })
      .state('participant-newreg', {
        url: '/newreg',
        templateUrl: 'modules/biospecimen/participant/register-new.html',
        controller: 'RegisterNewCtrl',
        parent: 'participant-detail'
      })
      .state('participant-detail.overview', {
        url: '/overview',
        templateProvider: function($q, PluginReg) {
          var defTmpl  = 'modules/biospecimen/participant/overview.html';
          return $q.when(PluginReg.getTmpls('participant-detail', 'overview', defTmpl)).then(
            function(tmpls) {
              return '<div ng-include src="\'' + tmpls[0] + '\'"></div>';
            }
          );
        },
        resolve: {
          storePhi: function(SettingUtil) {
            return SettingUtil.getSetting('biospecimen', 'store_phi').then(
              function(setting) {
                return setting.value == 'true';
              }
            );
          },
          consents: function(hasDict, hasFieldsFn, cpr, cpViewCtx) {
            return (hasDict && hasFieldsFn(['consents']) && cpViewCtx.consentsReadAllowed) ? cpr.getConsents() : null;
          },
          tmWorkflowId: function($injector, cp, cpr, CpConfigSvc) {
            if (!$injector.has('Workflow')) {
              return -1;
            }

            return CpConfigSvc.getWorkflowData(cp.id, 'specimenCollection').then(
              function(data) {
                if (data && (data.workflowId > 0 || data.visitCollectionWorkflowId > 0)) {
                  return data.workflowId || data.visitCollectionWorkflowId;
                }

                return CpConfigSvc.getWorkflowData(-1, 'specimenCollection').then(
                  function(data) {
                    return data && (data.workflowId || data.visitCollectionWorkflowId);
                  }
                );
              }
            );
          }
        },
        controller: 'ParticipantOverviewCtrl',
        parent: 'participant-detail'
      })
      .state('participant-detail.consents', {
        url: '/consents',
        templateUrl: 'modules/biospecimen/participant/consents.html',
        resolve: {
          hasEc: function($injector) {
            return $injector.has('ecDocument');
          },

          consent: function(cpr, cpViewCtx, hasEc) {
            return cpViewCtx.consentsReadAllowed && !hasEc ? cpr.getConsents() : null;
          }
        },
        controller: 'ParticipantConsentsCtrl',
        parent: 'participant-detail'
      })
      .state('participant-detail.visits', {
        url: '/visits-summary?eventId&visitId',
        templateUrl: 'modules/biospecimen/participant/visits.html',
        controller: 'ParticipantVisitsTreeCtrl',
        resolve: {
          specimens: function($stateParams, Specimen) {
            if (!$stateParams.visitId && !$stateParams.eventId) {
              return [];
            }

            var visitDetail = {
              visitId: $stateParams.visitId, 
              eventId: $stateParams.eventId
            };
            return Specimen.listFor($stateParams.cprId, visitDetail);
          }
        },
        parent: 'participant-detail'
      })
      .state('participant-detail.specimens', {
        url: '/specimens',
        templateUrl: 'modules/biospecimen/participant/reg-specimens.html',
        controller: 'RegSpecimensCtrl',
        resolve: {
          specimens: function(cpr, allowedEvents, Specimen) {
            return Specimen.listFor(cpr.id).then(
              function(specimens) {
                return specimens.filter(
                  function(spmn) {
                    if (!!spmn.visitStatus && spmn.visitStatus != 'Pending') {
                      return true;
                    }

                    return !allowedEvents || !spmn.eventCode || allowedEvents.indexOf(spmn.eventCode) != -1;
                  }
                );
              }
            );
          }
        },
        parent: 'participant-detail'
      })
      .state('participant-detail.collect-specimens', {
        url: '/collect-specimens?visitId&eventId',
        template: '<div ui-view></div>',
        controller: function() { },
        resolve: {
          visit: function($stateParams, cp, cpr, Visit) {
            if (!!$stateParams.visitId && $stateParams.visitId > 0) {
              return Visit.getById($stateParams.visitId);
            } else if (!!$stateParams.eventId) {
              return Visit.getAnticipatedVisit($stateParams.eventId, cpr.registrationDate);
            } else if (cp.specimenCentric) {
              return new Visit({cpId: cp.id});
            }

            return null;
          },
          latestVisit: function($stateParams, cpr) {
            //
            // required for lastest visit clinical diagnosis.
            //
            return ($stateParams.visitId || !cpr || !cpr.id) ? null : cpr.getLatestVisit();
          },
          spmnCollFields: function($stateParams, hasSde, cp, CpConfigSvc) {
            if (!hasSde) {
              return {};
            }

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
        abstract: true,
        parent: 'participant-root'
      })
      .state('participant-detail.collect-specimens.tree', {
        url: '/tree',
        templateUrl: 'modules/biospecimen/participant/collect-specimens.html',
        controller: 'CollectSpecimensCtrl',
        parent: 'participant-detail.collect-specimens'
      })
      .state('participant-detail.collect-specimens.nth-step', {
        url: '/nth-step',
        templateUrl: 'modules/biospecimen/participant/collect-specimens-nth-step.html',
        controller: 'CollectSpecimensNthStepCtrl',
        parent: 'participant-detail.collect-specimens'
      })
      .state('participant-detail.extensions', {
        url: '/extensions',
        template: '<div ui-view></div>',
        controller: function($scope, cpr, forms, records, surveys, ExtensionsUtil) {
          $scope.extnOpts = {
            update: $scope.participantResource.updateOpts,
            isEntityActive: cpr.activityStatus == 'Active',
            entity: cpr
          }

          angular.forEach(surveys,
            function(survey) {
              for (var j = 0; j < forms.length; ++j) {
                var form = forms[j];
                if (form.formCtxtId == survey.formCtxtId) {
                  form.survey = survey;
                  break;
                }
              }
            }
          );

          ExtensionsUtil.linkFormRecords(forms, records);
        },
        resolve: {
          orderSpec: function(cp, CpConfigSvc) {
            return CpConfigSvc.getWorkflowData(cp.id, 'forms', {}).then(
              function(wf) {
                return [
                  {type: 'CommonParticipant', forms: wf['CommonParticipant'] || []},
                  {type: 'Participant', forms: wf['Participant'] || []}
                ];
              }
            );
          },
          fdeRules: function(cp, CpConfigSvc) {
            return CpConfigSvc.getWorkflowData(cp.id, 'formDataEntryRules', {}).then(
              function(wf) {
                return wf['participant'] || [];
              }
            );
          },
          forms: function(cp, cpr, orderSpec, fdeRules, currentUser, ExtensionsUtil) {
            return cpr.getForms().then(
              function(forms) {
                var ctxt = {cp: cp, cpr: cpr, user: currentUser};
                forms = ExtensionsUtil.getMatchingForms(forms, fdeRules, ctxt);
                return ExtensionsUtil.sortForms(forms, orderSpec);
              }
            );
          },
          surveys: function(cpViewCtx) {
            return cpViewCtx.getSurveys();
          },
          records: function(cpr) {
            return cpr.getRecords();
          },
          viewOpts: function(cp) {
            return {
              goBackFn: null,
              showSaveNext: true,
              showSaveDraft: cp.draftDataEntry
            };
          }
        },
        abstract: true,
        parent: 'participant-detail'
      })
      .state('participant-detail.extensions.list', {
        url: '/list?formId&formCtxtId&recordId',
        templateUrl: 'modules/biospecimen/extensions/list.html',
        controller: 'FormsListCtrl', 
        parent: 'participant-detail.extensions'
      })
      .state('participant-detail.extensions.addedit', {
        url: '/addedit?formId&recordId&formCtxId',
        templateUrl: 'modules/biospecimen/extensions/addedit.html',
        resolve: {
          formDef: function($stateParams, Form) {
            return Form.getDefinition($stateParams.formId);
          },
          postSaveFilters: function() {
            return [];
          }
        },
        controller: 'FormRecordAddEditCtrl',
        parent: 'participant-detail.extensions'
      })
      .state('bulk-registrations', {
        url: '/bulk-registrations',
        templateProvider: function($stateParams, $q, CpConfigSvc) {
          return $q.when(CpConfigSvc.getBulkRegParticipantTmpl($stateParams.cpId, $stateParams.cprId)).then(
            function(tmpl) {
              return '<div ng-include src="\'' + tmpl + '\'"></div>';
            }
          );
        },
        controllerProvider: function($stateParams, CpConfigSvc) {
          return CpConfigSvc.getBulkRegParticipantCtrl($stateParams.cpId, $stateParams.cprId);
        },
        parent: 'participant-root'
      })
      .state('participant-search', {
        url: '/participant-search',
        templateUrl: 'modules/biospecimen/participant/search-result.html',
        resolve: {
          participants: function(ParticipantSearchSvc) {
            return ParticipantSearchSvc.getParticipants();
          },

          searchKey: function(ParticipantSearchSvc) {
            return ParticipantSearchSvc.getSearchKey();
          }
        },
        controller: 'ParticipantResultsView',
        parent: 'signed-in'
      });
  })
  .filter('mrnText', function() {
    return function(pmi) {
      if (!pmi) {
        return '';
      }

      if (pmi instanceof Array) {
        var result = [];
        angular.forEach(pmi, function(mrn) {
          result.push(mrn.siteName + (mrn.mrn ? ' (' + mrn.mrn + ')' : ''));
        });

        return result.join(', ');
      } else if (typeof pmi == 'object') {
        return pmi.siteName + (pmi.mrn ? ' (' + pmi.mrn + ')' : '');
      }

      return '';
    }
  })
  .run(function(QuickSearchSvc) {
    var opts = {caption: 'entities.participant', state: 'participant-detail.overview'};
    QuickSearchSvc.register('collection_protocol_registration', opts);
  });
