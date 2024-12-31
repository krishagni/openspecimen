
angular.module('os.biospecimen.cp', 
  [
    'ui.router',
    'os.biospecimen.cp.list',
    'os.biospecimen.cp.addedit',
    'os.biospecimen.cp.import',
    'os.biospecimen.cp.detail',
    'os.biospecimen.cp.consents',
    'os.biospecimen.cp.events',
    'os.biospecimen.cp.specimens',
    'os.biospecimen.cp.dp'
  ])

  .config(function($stateProvider) {
    $stateProvider
      .state('cps', {
        url: '/cps',
        abstract: true,
        template: '<div ui-view></div>',
        controller: function($scope, cpsCtx) {
          $scope.cpsCtx = cpsCtx;

          // Collection Protocol Authorization Options
          $scope.cpResource = {
            createOpts: {resource: 'CollectionProtocol', operations: ['Create']},
            updateOpts: {resource: 'CollectionProtocol', operations: ['Update']},
            deleteOpts: {resource: 'CollectionProtocol', operations: ['Delete']},
            importOpts: {resource: 'CollectionProtocol', operations: ['Export Import']}
          }
          
          $scope.participantResource = {
            createOpts: {resource: 'ParticipantPhi', operations: ['Create']},
            updateOpts: {resource: 'ParticipantPhi', operations: ['Update']}
          }
          
          $scope.specimenResource = {
            updateOpts: {resource: 'Specimen', operations: ['Create', 'Update']}
          }
          
          $scope.codingEnabled = $scope.global.appProps.cp_coding_enabled;
        },
        resolve: {
          cpsCtx: function(currentUser, authInit, AuthorizationService) {
            var cpCreateAllowed = AuthorizationService.isAllowed({
              resource: 'CollectionProtocol',
              operations: ['Create']
            });

            var cpUpdateAllowed = AuthorizationService.isAllowed({
              resource: 'CollectionProtocol',
              operations: ['Update']
            });

            var cpEximAllowed = AuthorizationService.isAllowed({
              resource: 'CollectionProtocol',
              operations: ['Export Import']
            });

            var participantEximAllowed = AuthorizationService.isAllowed({
              resource: 'ParticipantPhi',
              operations: ['Export Import']
            });

            var visitEximAllowed = AuthorizationService.isAllowed({
              resource: 'Visit',
              operations: ['Export Import']
            });

            var spmnEximAllowed = AuthorizationService.isAllowed({
              resources: ['Specimen', 'PrimarySpecimen'],
              operations: ['Export Import']
            });

            var allSpmnEximAllowed = AuthorizationService.isAllowed({
              resources: ['Specimen'],
              operations: ['Export Import']
            });

            var consentsEximAllowed = AuthorizationService.isAllowed({
              resources: ['Consent'],
              operations: ['Export Import']
            });

            var queryReadAllowed = AuthorizationService.isAllowed({
              resources: ['Query'],
              operations: ['Read']
            });

            return {
              cpCreateAllowed: cpCreateAllowed,
              cpUpdateAllowed: cpCreateAllowed || cpUpdateAllowed,
              cpImportAllowed: cpEximAllowed,
              participantImportAllowed: participantEximAllowed,
              visitImportAllowed: visitEximAllowed,
              specimenImportAllowed: spmnEximAllowed,
              cpExportAllowed: cpEximAllowed,
              participantExportAllowed: participantEximAllowed,
              visitExportAllowed: visitEximAllowed,
              specimenExportAllowed: spmnEximAllowed,
              allSpmnEximAllowed: allSpmnEximAllowed,
              consentsEximAllowed: consentsEximAllowed,
              queryReadAllowed: queryReadAllowed
            }
          }
        },
        parent: 'signed-in'
      })
      .state('cp-list', {
        url: '?filters', 
        templateUrl: 'modules/biospecimen/cp/list.html',
        controller: 'CpListCtrl',
        parent: 'cps'
      })
      .state('cp-addedit', {
        url: '/addedit/:cpId?mode',
        templateUrl: 'modules/biospecimen/cp/addedit.html',
        resolve: {
          cp: function($stateParams, CollectionProtocol) {
            if ($stateParams.cpId) {
              return CollectionProtocol.getById($stateParams.cpId);
            }
            return new CollectionProtocol();
          },
          extensionCtxt: function(CollectionProtocol) {
            return CollectionProtocol.getExtensionCtxt();
          }
        },
        controller: 'CpAddEditCtrl',
        parent: 'cps'
      })
      .state('cp-import', {
        url: '/import',
        templateUrl: 'modules/biospecimen/cp/import.html',
        controller: 'CpImportCtrl',
        parent: 'cps'
      })
      .state('import-multi-cp-objs', {
        url: '/import-multi-cp-objs',
        templateUrl: 'modules/common/import/add.html',
        controller: 'ImportObjectCtrl',
        resolve: {
          cp: function(CollectionProtocol) {
            return new CollectionProtocol({id: -1});
          },

          allowedEntityTypes: function(cpsCtx) {
            var entityTypes = [];
            if (cpsCtx.cpImportAllowed) {
              entityTypes.push('CollectionProtocol');
            }

            if (cpsCtx.participantImportAllowed) {
              entityTypes = entityTypes.concat(['CommonParticipant', 'Participant']);
            }

            if (cpsCtx.consentsEximAllowed) {
              entityTypes.push('Consent');
            }

            if (cpsCtx.visitImportAllowed) {
              entityTypes.push('SpecimenCollectionGroup');
            }

            if (cpsCtx.specimenImportAllowed) {
              entityTypes = entityTypes.concat(['Specimen', 'SpecimenEvent']);

              if (cpsCtx.allSpmnEximAllowed) {
                entityTypes.push('DerivativeAndAliquots');
              }
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
        parent: 'cps'
      })
      .state('import-multi-cp-jobs', {
        url: '/import-multi-cp-jobs',
        templateUrl: 'modules/common/import/list.html',
        controller: 'ImportJobsListCtrl',
        resolve: {
          importDetail: function(ImportUtil) {
            var objectTypes = [
              'cp', 'cpe', 'sr',
              'cprMultiple', 'otherCpr', 'cpr', 'participant', 'consent', 'econsentsDocumentResponse',
              'visit', 'specimen', 'specimenDerivative', 'specimenAliquot',
              'masterSpecimen', 'containerSpecimen', 'specimenDisposal', 'extensions',
              'containerTransferEvent', 'specimenDisposalEvent', 'specimenReservedEvent',
              'specimenReservationCancelEvent', 'specimenReturnEvent', 'specimenTransferEvent'
            ];

            return {
              breadcrumbs: [
                {state: 'cp-list', title: "cp.list"}
              ],
              objectTypes: objectTypes.concat(ImportUtil.getPluginTypes()),
              title: 'bulk_imports.jobs_list',
              objectParams: {cpId: -1}
            }
          }
        },
        parent: 'cps'
      })
      .state('export-multi-cp-objs', {
        url: '/export-multi-cp-objs',
        templateUrl: 'modules/common/export/add.html',
        controller: 'AddEditExportJobCtrl',
        resolve: {
          cp: function(CollectionProtocol) {
            return new CollectionProtocol({id: -1});
          },

          allowedEntityTypes: function(cp, cpsCtx) {
            var entityTypes = [];

            if (cpsCtx.cpExportAllowed) {
              entityTypes.push('CollectionProtocol');
            }

            if (cpsCtx.participantExportAllowed) {
              entityTypes.push('CommonParticipant');
              entityTypes.push('Participant');
            }

            if (cpsCtx.consentsEximAllowed) {
              entityTypes.push('Consent');
            }

            if (cpsCtx.visitExportAllowed) {
              entityTypes.push('SpecimenCollectionGroup');
            }

            if (cpsCtx.specimenExportAllowed) {
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
        parent: 'cps'
      })
      .state('cp-detail', {
        url: '/:cpId',
        templateUrl: 'modules/biospecimen/cp/detail.html',
        parent: 'cps',
        resolve: {
          cp: function($stateParams, CollectionProtocol) {
            return CollectionProtocol.getById($stateParams.cpId);
          }
        },
        controller: 'CpDetailCtrl',
        breadcrumb: {
          title: '{{cp.title}}',
          state: 'cp-detail.overview'
        }
      })
      .state('cp-detail.overview', {
        url: '/overview',
        templateUrl: 'modules/biospecimen/cp/overview.html',
        parent: 'cp-detail'
      })
      .state('cp-detail.settings.import-events', {
        url: '/import-events',
        templateUrl: 'modules/common/import/add.html',
        controller: 'ImportObjectCtrl',
        resolve: {
          importDetail: function(cp) {
            return {
              objectType: 'cpe',
              title: 'cp.import_events',
              objectParams: {'cpId': cp.id},
              onSuccess: {state: 'cp-detail.settings.import-jobs'}
            };
          }
        },
        parent: 'cp-detail.settings'
      })
      .state('cp-detail.settings.import-srs', {
        url: '/import-reqs',
        templateUrl: 'modules/common/import/add.html',
        controller: 'ImportObjectCtrl',
        resolve: {
          importDetail: function(cp) {
            return {
              objectType: 'sr',
              title: 'cp.import_reqs',
              objectParams: {'cpId': cp.id},
              onSuccess: {state: 'cp-detail.settings.import-jobs'}
            };
          }
        },
        parent: 'cp-detail.settings'
      })
      .state('cp-detail.settings.import-jobs', {
        url: '/import-jobs',
        templateUrl: 'modules/common/import/list.html',
        controller: 'ImportJobsListCtrl',
        resolve: {
          importDetail: function(cp) {
            return {
              title: 'bulk_imports.jobs_list',
              objectTypes: ['cpe', 'sr'],
              objectParams: {'cpId': cp.id}
            };
          }
        },
        parent: 'cp-detail.settings'
      })
      .state('cp-detail.consents', {
        url: '/consents',
        templateUrl: 'modules/biospecimen/cp/consents.html',
        parent: 'cp-detail',
        resolve: {
          hasEc: function($injector) {
            return $injector.has('ecDocument');
          },

          consentTiers: function(cp, hasEc) {
            return hasEc ? null : cp.getConsentTiers();
          }
        },
        controller: 'CpConsentsCtrl'
      })
      .state('cp-detail.events', {
        templateUrl: 'modules/biospecimen/cp/events.html',
        parent: 'cp-detail',
        resolve: {
          events: function($stateParams, cp, CollectionProtocolEvent) {
            return CollectionProtocolEvent.listFor(cp.id).then(
              function(events) {
                if (events.length > 0 || !cp.specimenCentric) {
                  return events;
                } else {
                  return [new CollectionProtocolEvent({id: -1, cpId: cp.id})];
                }
              }
            );
          },

          mrnAccessRestriction: function(SettingUtil) {
            return SettingUtil.getSetting('biospecimen', 'mrn_restriction_enabled').then(
              function(setting) {
                return setting.value == 'true';
              }
            );
          }
        },
        controller: 'CpEventsCtrl'
      })
      .state('cp-detail.specimen-requirements', {
        url: '/specimen-requirements?eventId',
        templateUrl: 'modules/biospecimen/cp/specimens.html',
        parent: 'cp-detail.events',
        resolve: {
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

          specimenRequirements: function($stateParams, SpecimenRequirement) {
            var eventId = $stateParams.eventId;
            if (!eventId || eventId == -1) {
              return [];
            }

            return SpecimenRequirement.listFor(eventId);
          },

          aliquotQtyReq: function(SettingUtil) {
            return SettingUtil.getSetting('biospecimen', 'mandatory_aliquot_qty').then(
              function(resp) {
                return resp.value == 'true' || resp.value == true || resp.value == 1 || resp.value == '1';
              }
            );
          }
        },
        controller: 'CpSpecimensCtrl'
      })
      .state('cp-detail.versions', {
        url: '/published-versions',
        templateUrl: 'modules/biospecimen/cp/published-versions.html',
        parent: 'cp-detail',
        controller: 'CpPublishedVersionsCtrl'
      })
      .state('cp-detail.settings', {
        url: '/settings',
        abstract: true,
        template: '<div class="clearfix">' +
                  '  <div class="col-xs-12">' +
                  '    <div ui-view></div>' +
                  '  </div>' +
                  '</div>',
        parent: 'cp-detail'
      })
      .state('cp-detail.settings.labels', {
        url: '/label',
        templateUrl: 'modules/biospecimen/cp/label-settings.html',
        parent: 'cp-detail.settings',
        controller: 'CpLabelSettingsCtrl',
        resolve: {
          hasSupplies: function($injector) {
            return $injector.has('Supply');
          }
        }
      })
      .state('cp-detail.settings.forms', {
        url: '/forms',
        templateUrl: 'modules/biospecimen/cp/form-settings.html',
        parent: 'cp-detail.settings',
        resolve: {
          forms: function(cp) {
            return cp.getForms(['CommonParticipant', 'Participant', 'SpecimenCollectionGroup', 'Specimen']);
          },

          formsWf: function(cp, CpConfigSvc) {
            return CpConfigSvc.getWorkflow(cp.id, 'forms');
          }
        },
        controller: 'CpFormSettingsCtrl'
      })
      .state('cp-detail.settings.container', {
        url: '/container',
        templateUrl: 'modules/biospecimen/cp/container-settings.html',
        parent: 'cp-detail.settings',
        controller: 'CpContainerSettingsCtrl'
      })
      .state('cp-detail.settings.reporting', {
        url: '/reporting',
        templateUrl: 'modules/biospecimen/cp/report-settings.html',
        parent: 'cp-detail.settings',
        resolve: {
          reportSettings: function(cp) {
            if (cp.reportSettings) {
              return cp.reportSettings;
            }

            return cp.getReportSettings().then(
              function(settings) {
                cp.reportSettings = settings || {enabled: true};
              }
            );
          }
        },
        controller: 'CpReportSettingsCtrl'
      })
      .state('cp-detail.settings.dp', {
        url: '/dp',
        templateUrl: 'modules/biospecimen/cp/dp-settings.html',
        parent: 'cp-detail.settings',
        controller: 'CpDpSettingsCtrl'
      })
      .state('cp-detail.settings.import-workflows', {
        url: '/import-workflows',
        templateUrl: 'modules/biospecimen/cp/import-workflows.html',
        parent: 'cp-detail.settings',
        controller: 'CpImportWorkflowsCtrl'
      })
    })

    .run(function(UrlResolver, QuickSearchSvc) {
      UrlResolver.regUrlState('cp-overview', 'cp-detail.overview', 'cpId');

      var opts = {caption: 'entities.collection_protocol', state: 'cp-detail.overview'};
      QuickSearchSvc.register('collection_protocol', opts);
    });
  
