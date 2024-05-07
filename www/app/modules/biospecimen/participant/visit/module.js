
angular.module('os.biospecimen.visit', [ 
    'ui.router',
    'os.biospecimen.participant.specimen-tree',
    'os.biospecimen.extensions',
    'os.biospecimen.extensions.util',
    'os.biospecimen.visit.addedit',
    'os.biospecimen.visit.spr',
    'os.biospecimen.visit.detail'
  ])
  .config(function($stateProvider) {
    $stateProvider
      .state('visit', {
        url: '/visits/:visitId',
        controller: function($state, params) {
          $state.go('visit-detail.overview', params, {location: 'replace'});
        },
        resolve: {
          params: function($stateParams, Visit) {
            return Visit.getRouteIds($stateParams.visitId);
          }
        },
        parent: 'signed-in'
      })
      .state('visit-root', {
        url: '/visits?visitId&eventId',
        template: '<div ui-view></div>',
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

          storeSpr: function(cp, SettingUtil) {
            if (cp.storeSprEnabled == undefined || cp.storeSprEnabled == null) {
              return SettingUtil.getSetting('biospecimen', 'store_spr').then(
                function(setting) {
                  return setting.value == 'true';
                }
              );
            } else {
              return cp.storeSprEnabled;
            }
          },

          showVisitActivity: function(cp, CpConfigSvc) {
            return CpConfigSvc.getCommonCfg(cp.id, 'showVisitActivity').then(
              function(value) {
                return (value === null || value === undefined || value === '') ? true : (value == true);
              }
            );
          }
        },
        controller: function($scope, cpr, visit) {
          $scope.visit = $scope.object = visit;
          $scope.entityType = 'SpecimenCollectionGroup';
          $scope.extnState = 'visit-detail.extensions.'
        },
        abstract: true,
        parent: 'participant-root'
      })
      .state('visit-addedit', {
        url: '/addedit-visit?missedVisit&newVisit',
        templateProvider: function(PluginReg, $q) {
          return $q.when(PluginReg.getTmpls("visit-addedit", "page-body", "modules/biospecimen/participant/visit/addedit.html")).then(
            function(tmpls) {
              return '<div ng-include src="\'' + tmpls[0] + '\'"></div>';
            }
          );
        },
        resolve: {
          extensionCtxt: function(cp, Visit) {
            return Visit.getExtensionCtxt({cpId: cp.id});
          },
          latestVisit: function(cpr, visit) {
            //
            // required for lastest visit CD
            //
            return visit.id ? null : cpr.getLatestVisit();
          },
          spmnCollWfData: function(cp, CpConfigSvc) {
            return CpConfigSvc.getWorkflowData(cp.id, 'specimenCollection').then(
              function(wfData) {
                if (wfData && !angular.equals(wfData, {})) {
                  return wfData;
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
        controller: 'AddEditVisitCtrl',
        parent: 'visit-root'
      })
      .state('visit-detail', {
        url: '/detail',
        templateUrl: 'modules/biospecimen/participant/visit/detail.html',
        resolve: {
          specimens: function(cpr, visit, cpViewCtx, Specimen) {
            if (!cpViewCtx.spmnReadAllowed) {
              return [];
            }

            var criteria = { visitId: visit.id, eventId: visit.eventId };
            return Specimen.listFor(cpr.id, criteria);
          }
        },
        controller: 'VisitDetailCtrl',
        parent: 'visit-root'
      })
      .state('visit-detail.overview', {
        url: '/overview',
        templateProvider: function(PluginReg, $q) {
          var defTmpl = "modules/biospecimen/participant/visit/overview.html";
          return $q.when(PluginReg.getTmpls("visit-detail", "overview", defTmpl)).then(
            function(tmpls) {
              return '<div ng-include src="\'' + tmpls[0] + '\'"></div>';
            }
          );
        },
        controller: function(
          $scope, userRole, cpr, visit, hasFieldsFn, showVisitActivity, spmnReqs, hasDict,
          osRightDrawerSvc, ExtensionsUtil) {

          ExtensionsUtil.createExtensionFieldMap(cpr.participant, hasDict);
          ExtensionsUtil.createExtensionFieldMap(visit, hasDict);
          $scope.visitCtx = {
            obj: {cpr: cpr, visit: visit, userRole: userRole},
            spmnReqs: spmnReqs,
            inObjs: ['visit', 'calcVisit'],
            showEdit: hasFieldsFn(['visit'], []),
            showActivity: showVisitActivity
          };

          if (showVisitActivity) {
            osRightDrawerSvc.open();
          }

          $scope.toggleShowActivity = function() {
            $scope.visitCtx.showActivity = !$scope.visitCtx.showActivity;
          }

          $scope.joinCd = function(cd) {
            if (cd.value == 'Not Specified') {
              return null;
            }

            if (cd.conceptCode) {
              return cd.value + ' (' + cd.conceptCode + ')';
            } else {
              return cd.value;
            }
          }
        },
        resolve: {
          spmnReqs: function(cp, CpConfigSvc) {
            return CpConfigSvc.getCommonCfg(cp.id, 'addSpecimen', {}).then(
              function(data) {
                return (data && data.requirements) || [];
              }
            );
          }
        },
        parent: 'visit-detail'
      })
      .state('visit-detail.consents', {
        url: '/consents',
        templateUrl: 'modules/biospecimen/participant/visit/consents.html',
        controller: function() {
        }
      })
      .state('visit-detail.extensions', {
        url: '/extensions',
        template: '<div ui-view></div>',
        controller: function($scope, visit, forms, surveys, records, ExtensionsUtil) {
          $scope.extnOpts = {
            update: $scope.visitResource.updateOpts,
            entity: visit,
            isEntityActive: visit.activityStatus == 'Active'
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
                return [{type: 'SpecimenCollectionGroup', forms: wf['SpecimenCollectionGroup'] || []}];
              }
            );
          },
          fdeRules: function(cp, CpConfigSvc) {
            return CpConfigSvc.getWorkflowData(cp.id, 'formDataEntryRules', {}).then(
              function(wf) {
                return wf['visit'] || [];
              }
            );
          },
          forms: function(cp, cpr, visit, currentUser, orderSpec, fdeRules, ExtensionsUtil) {
            return visit.getForms().then(
              function(forms) {
                var ctxt = {cp: cp, cpr: cpr, visit: visit, user: currentUser};
                forms = ExtensionsUtil.getMatchingForms(forms, fdeRules, ctxt);
                return ExtensionsUtil.sortForms(forms, orderSpec);
              } 
            ) 
          },
          records: function(visit) {
            return visit.getRecords();
          },
          surveys: function(cpViewCtx) {
            return cpViewCtx.getSurveys();
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
        parent: 'visit-detail'
      })
      .state('visit-detail.extensions.list', {
        url: '/list?formId&formCtxtId&recordId',
        templateUrl: 'modules/biospecimen/extensions/list.html',
        controller: 'FormsListCtrl',
        parent: 'visit-detail.extensions'
      })
      .state('visit-detail.extensions.addedit', {
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
        parent: 'visit-detail.extensions'
      })
      .state('visit-detail.spr', {
        url: '/spr',
        templateUrl: 'modules/biospecimen/participant/visit/spr.html',
        controller: 'VisitSprCtrl',
        parent: 'visit-detail'
      })

      .state('visit-search', {
        url: '/visit-search',
        templateUrl: 'modules/biospecimen/participant/visit/search-result.html',
          resolve: {
            visits: function(VisitSearchSvc) {
              return VisitSearchSvc.getVisits();
            },
            searchKey: function(VisitSearchSvc) {
              return VisitSearchSvc.getSearchKey();
            }
          },
          controller: 'VisitResultsView',
          parent: 'visit-root'
       });
  })

  .run(function(QuickSearchSvc) {
    var opts = {
      caption: 'entities.visit',
      vueUrl: 'visit-resolver/:entityId',
      vueView: 'ParticipantsListItemVisitDetail.Overview'
    };
    QuickSearchSvc.register('visit', opts);
  });
