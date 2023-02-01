
angular.module('os.biospecimen.participant.addedit', ['os.biospecimen.models', 'os.administrative.models'])
  .controller('ParticipantAddEditCtrl', function(
    $scope, $state, $stateParams, $translate, $modal, $q, $timeout, $injector,
    userRole, cp, cpr, extensionCtxt, hasDict, cpDict, twoStepReg, layout,
    onValueChangeCb, mrnAccessRestriction, addPatientOnLookupFail,
    lookupFieldsCfg, lockedFields, cpEvents, visitsTab,
    CpConfigSvc, CollectionProtocolRegistration, Participant,
    Visit, CollectSpecimensSvc, Site, PvManager, ExtensionsUtil, Alerts) {

    var availableSites = [];
    var inputParticipant = null;

    function init() {
      $scope.cpId = $stateParams.cpId;

      $scope.cp = cp;
      $scope.cpr = angular.copy(cpr);
      setBirthDate($scope.cpr);

      $scope.partCtx = {
        cpSites: cp.cpSites.map(function(cpSite) { return cpSite.siteName; }),
        cpEvents: cpEvents,
        documentsCount: 0,
        fieldOpts: {
          viewCtx: $scope,
          lockedFields: lockedFields,
          layout: layout,
          onValueChange: onValueChangeCb,
          mdInput: false
        },
        twoStep: lookupFieldsCfg.configured,
        edit: !!cpr.id,
        obj: {cpr: $scope.cpr, cp: cp, userRole: userRole},
        inObjs: ['cpr'],
        mrnAccessRestriction: mrnAccessRestriction,
        step: !cpr.id ? 'lookupParticipant' : 'registerParticipant',
        matchingTabOpts: {
          table: getMatchingTabFields(),
          multiple: true,
          static: true,
          allowBulkUpload: false,
          rowSelect: 'single',
          onRowSelect: onMatchSelect
        },
        allowSkip: !!addPatientOnLookupFail
      }

      $scope.disableFieldOpts = {}
      if (!!cpr.id && cpr.participant.source != 'OpenSpecimen') {
        initDisableFieldOpts(lockedFields);
      }

      $scope.op         = !!$scope.cpr.id ? 'Update' : 'Create';
      $scope.deFormCtrl = {};

      extensionCtxt.sdeMode = hasDict;
      $scope.extnOpts = ExtensionsUtil.getExtnOpts(
        $scope.cpr.participant, extensionCtxt, $scope.disableFieldOpts.customFields);

      if (!$scope.cpr.id || !$scope.cpr.participant.pmis || $scope.cpr.participant.pmis.length == 0) {
        $scope.cpr.participant.addPmi($scope.cpr.participant.newPmi());
      }

      if (!hasDict) {
        loadPvs();
      }

      if (!cpr.id && visitsTab && visitsTab.anticipatedEvents) {
        $scope.$watch('cpr',
          function(newVal, oldVal) {
            var allowedEvents = $scope.cpr.getAllowedEvents(visitsTab);
            $scope.partCtx.cpEvents = (cpEvents || []).filter(
              function(e) {
                return !allowedEvents || !e.code || allowedEvents.indexOf(e.code) != -1;
              }
            );
          },
          true
        );
      }

      loadDocuments();
    };

    function loadDocuments() {
      if (!$injector.has('ecCpDocument') || !!cpr.id || cp.visitLevelConsents) {
        return;
      }

      return $injector.get('ecCpDocument').getCount(cp.id, {includeOnlyActive: true}).then(
        function(resp) {
          $scope.partCtx.documentsCount = resp.count;
        }
      );
    }

    function onMatchSelect(match) {
      $scope.partCtx.selectedMatch = match;
    }

    function initDisableFieldOpts(lockedFields) {
      $scope.disableFieldOpts = {
        fields: getStaticFields(lockedFields),
        customFields: getCustomFields(lockedFields)
      }
      angular.extend($scope.partCtx.fieldOpts, {lockedFields: lockedFields});
    }

    function getStaticFields(fields) {
      var result = {};
      angular.forEach(fields,
        function(f) {
          if (f.indexOf('cpr.participant.extensionDetail.attrsMap') != 0) {
            result[f] = true;
          }
        }
      );

      return result;
    }

    function getCustomFields(fields) {
      return fields.filter(
        function(f) {
          return f.indexOf('cpr.participant.extensionDetail.attrsMap') == 0
        }
      ).map(
        function(f) {
          return f.substring('cpr.participant.extensionDetail.attrsMap.'.length);
        }
      );
    }

    function getMatchingTabFields() {
      if (!hasDict) {
        return [];
      }

      var fields = cpDict.filter(
        function(field) {
          return field.name.indexOf('cpr.participant.') == 0 &&
            field.name.indexOf('cpr.participant.extensionDetail.') == -1;
        }
      ).map(
        function(field) {
          var result = angular.copy(field);
          result.displayFormat = result.formatType;
          result.formatType = result.type;
          result.type = 'span'
          return result;
        }
      );

      fields.push({
        name: 'cps',
        caption: 'Registered Protocols',
        type: 'span',
        multiple: true
      });

      return fields;
    }

    function loadPvs() {
      $scope.genders       = PvManager.getPvs('gender');
      PvManager.loadPvs('vital-status', null, function(pv) { return pv; }, true, false, {includeProps: true}).then(
        function(pvs) {
          var vitalStatuses = [];
          var deadStatuses = [];
          angular.forEach(pvs,
            function(pv) {
              vitalStatuses.push(pv.value);
              if (pv.props && (pv.props.dead == 'true' || pv.props.dead == true)) {
                deadStatuses.push(pv.value);
              }
            }
          );

          $scope.vitalStatuses = vitalStatuses;
          $scope.deadStatuses = deadStatuses;
        }
      );
    };

    function registerParticipant(event, gotoConsents) {
      var formCtrl = $scope.deFormCtrl.ctrl;
      if (formCtrl && !formCtrl.validate()) {
        return;
      }

      var cprToSave = angular.copy($scope.cpr);
      cprToSave.cpId = $scope.cpId;
      cprToSave.site = cprToSave.site || '';

      if (formCtrl) {
        cprToSave.participant.extensionDetail = formCtrl.getFormData();
      }

      cprToSave.$saveOrUpdate().then(
        function(savedCpr) {
          if (savedCpr.activityStatus == 'Active') {
            var registeredCps = cpr.participant.registeredCps;
            angular.extend(cpr, savedCpr);
            cpr.site = savedCpr.site;

            cpr.participant.registeredCps = registeredCps;

            if (event) {
              if (cp.spmnLabelPrePrintMode == 'ON_REGISTRATION') {
                Visit.listFor(savedCpr.id, false).then(
                  function(visits) {
                    var pendingVisit = visits.find(function(v) { return v.status == 'Pending' && v.eventId == event.id });
                    gotoSpmnCollection(cpr, event, pendingVisit);
                  }
                );
              } else {
                gotoSpmnCollection(savedCpr, event);
              }
            } else if (gotoConsents) {
              if ($injector.has('Survey')) {
                $injector.get('Survey').getStarterConsent(savedCpr.id).then(
                  function(survey) {
                    if (survey) {
                      $injector.get('SurveyInstance').switchToSurveyMode(savedCpr, survey, true);
                    } else {
                      $state.go('participant-detail.consents', {cprId: savedCpr.id});
                    }
                  }
                );
              } else {
                $state.go('participant-detail.consents', {cprId: savedCpr.id});
              }
            } else {
              $state.go('participant-detail.overview', {cprId: savedCpr.id});
            }
          } else {
            $state.go('participant-list', {cpId: $scope.cp.id});
          }
        }
      );
    };

    function gotoSpmnCollection(cpr, event, pendingVisit) {
      var state = $state.get('participant-detail.overview');
      var visit = pendingVisit;
      if (!visit) {
        visit = new Visit({cpId: cp.id, cprId: cpr.id, eventId: event.id});
      }

      CollectSpecimensSvc.collectVisit({state: state, params: {cprId: cpr.id}}, cp, cpr.id, visit);
    }

    function updateUsingSelectedParticipant(match) {
      var selectedPart = match.participant;

      var promise;
      if (inputParticipant.participant.source != selectedPart.source) {
        promise = CpConfigSvc.getLockedParticipantFields(selectedPart.source);
      } else {
        var q = $q.defer();
        q.resolve(lockedFields);
        promise = q.promise;
      }

      promise.then(
        function(lockedFields) {
          clearFields($scope, lockedFields);
          angular.extend($scope.cpr.participant, selectedPart);
          registerParticipant();
        }
      );
    };

    function confirmMergeWithSelectedParticipant(match) {
      var modalInstance = $modal.open({
        templateUrl: "modules/biospecimen/participant/confirm-merge.html",
        controller: function($scope, $state, $modalInstance, cpr) {
          $scope.cpr = cpr;
          $scope.params = {
            cpr: cpr,
            url: $state.href('participant-detail.overview', {cpId: cpr.cpId, cprId: cpr.id})
          };

          $scope.ok = function() {
            $modalInstance.close(true);
          }

          $scope.cancel = function() {
            $modalInstance.dismiss('cancel');
          }
        },
        resolve: {
          cpr: function() {
            return cpr;
          }
        }
      });

      modalInstance.result.then(
        function() {
          updateUsingSelectedParticipant(match);
        }
      );
    }

    function clearFields(inputObj, fieldsToClear) {
      angular.forEach(fieldsToClear,
        function(field) {
          var attrs = field.split('\.');
          var obj = inputObj;
          for (var i = 0; i < attrs.length - 1; ++i) {
            obj = obj[attrs[i]];
            if (!obj) {
              return;
            }
          }

          delete obj[attrs[attrs.length - 1]];
        }
      );
    }

    function handleParticipantMatches(matches, event, gotoConsents) {
      $scope.partCtx.matches = matches;
      $scope.partCtx.matchAutoSelected = false;
      $scope.partCtx.allowIgnoreMatches = matches.every(
        function(match) {
          return match.matchedAttrs.length == 1 && match.matchedAttrs[0] == 'lnameAndDob';
        }
      );

      angular.forEach(matches,
        function(match) {
          if (!match.participant.id && match.participant.source != 'OpenSpecimen') {
            //
            // Ask API to not use existing participant ID
            //
            match.participant.id = -1;
          }

          match.cpr = {participant: match.participant};
          match.cps = (match.participant.registeredCps || [])
            .map(function(reg) { return reg.cpShortTitle; })
            .sort();
        }
      );

      if (!matches || matches.length == 0) {
        if ($scope.partCtx.edit) {
          registerParticipant();
        } else if (!addPatientOnLookupFail && lookupFieldsCfg.configured) {
          Alerts.error('participant.no_matching_participant');
        } else {
          if (!lookupFieldsCfg.configured) {
            registerParticipant(event, gotoConsents);
          } else {
            $scope.partCtx.step = 'registerParticipant';
          }
        }
      } else if (isAutoSelect(matches)) {
        $scope.partCtx.matchAutoSelected = true;

        var match = matches[0];
        $scope.partCtx.registeredCps = (match.cps || []).join(', ');
        if ($scope.partCtx.twoStep) {
          var participant = match.participant;
          for (var i = 0; i < (participant.registeredCps || []).length; ++i) {
            if (participant.registeredCps[i].cpId == cp.id) {
              $state.go('participant-detail.overview', {cpId: cp.id, cprId: participant.registeredCps[i].cprId});
              return;
            }
          }
        }

        useSelectedMatch(match);
      } else {
        $scope.partCtx.step = 'chooseMatch';
        $scope.partCtx.selectedMatch = null;
      }
    }

    function isAutoSelect(matches) {
      if ($scope.partCtx.edit || matches.length > 1) {
        return false;
      }

      if (matches.length == 1 && matches[0].matchedAttrs.length == 1 && matches[0].matchedAttrs[0] == 'lnameAndDob') {
        return false;
      }

      return true;
    }

    function useSelectedMatch(match) {
      var matchedCprId = undefined;
      var participant = match.participant;
      for (var i = 0; i < (participant.registeredCps || []).length; ++i) {
        if (participant.registeredCps[i].cpId == cp.id) {
          matchedCprId = participant.registeredCps[i].cprId;
          break;
        }
      }

      if ($scope.partCtx.edit) {
        if (match.participant.id != -1) {
          confirmMergeWithSelectedParticipant(match);
        } else {
          updateUsingSelectedParticipant(match);
        }

        return;
      }

      if (!matchedCprId) {
        prepareRegParticipantStep(undefined, match.participant);
      } else {
        CollectionProtocolRegistration.getById(matchedCprId).then(prepareRegParticipantStep);
      }
    }

    function prepareRegParticipantStep(matchedCpr, matchedParticipant) {
      if (!matchedCpr && !matchedParticipant) {
        return;
      }

      if (matchedCpr) {
        matchedParticipant = matchedCpr.participant;
        ExtensionsUtil.createExtensionFieldMap(matchedParticipant);
      }

      return setLockingOpts(matchedParticipant.source).then(
        function(lockedFields) {
          clearFields($scope, lockedFields);
          angular.extend($scope.cpr.participant, matchedParticipant);
          if (lockedFields.indexOf('cpr.participant.pmis') == -1) {
            $scope.cpr.participant.addPmi($scope.cpr.participant.newPmi());
          }

          if (matchedCpr) {
            delete matchedCpr.participant;
            angular.extend($scope.cpr, matchedCpr);
            $scope.extnOpts = ExtensionsUtil.getExtnOpts(
              $scope.cpr.participant, extensionCtxt, $scope.disableFieldOpts.customFields);
          }

          var birthDate = $scope.cpr.participant.birthDate;
          if (!!birthDate && typeof birthDate == 'string') {
            $scope.cpr.participant.birthDateStr = birthDate;
            setBirthDate($scope.cpr);
          }
          $scope.partCtx.step = 'registerParticipant';
        }
      );
    }

    function setLockingOpts(source) {
      if (!source || source == 'OpenSpecimen') {
        initDisableFieldOpts([]);

        var q = $q.defer();
        q.resolve([]);
        return q.promise;
      } else {
        return CpConfigSvc.getLockedParticipantFields(source).then(
          function(lockedFields) {
            initDisableFieldOpts(lockedFields);
            return lockedFields;
          }
        );
      }
    }

    function setBirthDate(reg) {
      if (!reg.participant.birthDateStr) {
        return;
      }

      var parts = reg.participant.birthDateStr.split('-');
      reg.participant.birthDate = new Date(parts[0], parseInt(parts[1]) - 1, parts[2]);
    }

    function cacheInputParticipant() {
      var formCtrl = $scope.deFormCtrl.ctrl;
      if (formCtrl && !formCtrl.validate()) {
        return;
      }

      inputParticipant = angular.copy($scope.cpr);
      if (formCtrl) {
        inputParticipant.participant.extensionDetail = formCtrl.getFormData();
        // ExtensionsUtil.createExtensionFieldMap(inputParticipant.participant)
      }
    }

    function revertToInputParticipant() {
      var formCtrl = $scope.deFormCtrl.ctrl;
      $scope.partCtx.obj.cpr = $scope.cpr = angular.copy(inputParticipant);

      if (formCtrl) {
        $scope.extnOpts = ExtensionsUtil.getExtnOpts(
          $scope.cpr.participant, extensionCtxt, $scope.disableFieldOpts.customFields);
      }

      var birthDate = $scope.cpr.participant.birthDate;
      if (!!birthDate && typeof birthDate == 'string') {
        $scope.cpr.participant.birthDateStr = birthDate;
        setBirthDate($scope.cpr);
      }
    }

    $scope.vitalStatusChanged = function() {
      var p = $scope.cpr.participant;
      if ($scope.deadStatuses.indexOf(p.vitalStatus) == -1 && !!p.deathDate) {
        p.deathDate = null;
      }
    }

    $scope.pmiText = function(pmi) {
      return pmi.siteName + (pmi.mrn ? " (" + pmi.mrn + ")" : "");
    }

    $scope.addPmiIfLast = function(idx) {
      if (lockedFields.indexOf('cpr.participant.pmis') != -1) {
        return;
      }

      var participant = $scope.cpr.participant;
      if (idx == participant.pmis.length - 1) {
        participant.addPmi(participant.newPmi());
      }
    };

    $scope.removePmi = function(pmi) {
      var participant = $scope.cpr.participant;
      participant.removePmi(pmi);

      if (participant.pmis.length == 0) {
        participant.addPmi(participant.newPmi());
      }
    };

    $scope.register = function(event, gotoConsents) {
      if ($scope.partCtx.edit) {
        cacheInputParticipant();
        $scope.cpr.participant.getMatchingParticipants().then(
          function(matches) {
            handleParticipantMatches(matches, event, gotoConsents)
          }
        );
      } else {
        registerParticipant(event, gotoConsents);
      }
    };

    $scope.lookup = function(event, gotoConsents) {
      cacheInputParticipant();
      $scope.partCtx.skipped = false;
      $scope.cpr.participant.getMatchingParticipants().then(
        function(matches) {
          handleParticipantMatches(matches, event, gotoConsents)
        }
      );
    }

    $scope.skip = function() {
      $scope.partCtx.skipped = true;
      handleParticipantMatches([]);
    }

    $scope.useSelectedMatch = function() {
      var matches = $scope.partCtx.matches;
      for (var i = 0; i < matches.length; ++i) {
        if (matches[i].$$selected == 'true' || matches[i].$$selected == true) {
          useSelectedMatch(matches[i]);
          return;
        }
      }
    }

    $scope.selectMatch = function(selectedMatch) {
      angular.forEach($scope.partCtx.matches,
        function(match) {
          if (match != selectedMatch) {
            match.$$selected = false;
          }
        }
      );

      onMatchSelect(selectedMatch);
    }

    $scope.ignoreMatches = function() {
      angular.forEach($scope.partCtx.matches,
        function(match) {
          match.$$selected = false;
        }
      );

      if (!lookupFieldsCfg.configured || $scope.partCtx.edit) {
        registerParticipant();
      } else {
        var cpr = $scope.partCtx.obj.cpr = $scope.cpr = angular.copy(inputParticipant);
        if (cpr.participant && typeof cpr.participant.birthDate == 'string') {
          var bd = cpr.participant.birthDate.split('-');
          cpr.participant.birthDate = new Date(+bd[0], +bd[1] - 1, bd[2]);
        }

        $scope.partCtx.step = 'registerParticipant';
      }
    }

    $scope.previous = function() {
      if ($scope.partCtx.step == 'registerParticipant') {
        if ($scope.partCtx.edit) {
          $scope.back();
        } else if (isAutoSelect($scope.partCtx.matches)) {
          revertToInputParticipant();
          $scope.partCtx.step = 'lookupParticipant';
        } else {
          $scope.partCtx.step = 'chooseMatch';
        }
      } else if ($scope.partCtx.step == 'chooseMatch') {
        revertToInputParticipant();
        $scope.partCtx.step = $scope.partCtx.edit ? 'registerParticipant' : 'lookupParticipant';
      } else {
        $scope.back();
      }
    }

    $scope.toggleEventsMenu = function(open) {
      if (!open) {
        return;
      }

      $timeout(function() {
        var el = $scope.partCtx.eventsDd[0];
        el.scrollIntoView(true);
      });
    }

    init();
  });
