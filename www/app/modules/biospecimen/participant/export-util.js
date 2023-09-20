angular.module('os.biospecimen.participant')
  .factory('ExportUtil', function($translate, $injector) {

    function addForms(exportTypes, group, entityType, input, forms) {
      angular.forEach(forms,
        function(form) {
          exportTypes.push({
            group: group,
            type: 'extensions',
            title: form.caption,
            '$$input': input,
            params: { entityType: entityType, formName: form.name }
          });
        });
      return exportTypes;
    }

    function msg(key) {
      return $translate.instant(key);
    }

    function getCpTypes() {
      var group = $translate.instant('cp.self');
      return [
        {group: group, type: 'cp', title: msg('cp.list'), '$$input': null },
        {group: group, type: 'cpe', title: msg('cp.cpe_list'), '$$input': null },
        {group: group, type: 'sr', title: msg('srs.list'), '$$input': null }
      ];
    }

    function getParticipantTypes(entityForms, cp, addConsent) {
      var group = $translate.instant('participant.title');
      var input = {'var': 'ppids', varName: 'participant.ppids', varDesc: 'participant.ppids_csv'};

      var exportTypes = [ { group: group, type: 'cpr', title: msg('participant.list'), '$$input': input } ];
      if (addConsent) {
        exportTypes = exportTypes.concat(getConsentTypes(cp, input, true));
      }

      addForms(exportTypes, group, 'CommonParticipant', input, entityForms['CommonParticipant']);
      return addForms(exportTypes, group, 'Participant', input, entityForms['Participant']);
    }

    function getConsentTypes(cp, input, participantLevel) {
      if ($injector.has('ecDocument')) {
        if (cp.id != -1) {
          if (cp.visitLevelConsents && participantLevel) {
            return [];
          } else if (!cp.visitLevelConsents && !participantLevel) {
            return [];
          }
        }
      } else if (!participantLevel) {
        return [];
      }

      var type = 'consent';
      if ($injector.has('ecDocument')) {
        type = 'econsentsDocumentResponse';
      }

      var group = $translate.instant('participant.title');
      var params = {};
      if (!participantLevel) {
        group = $translate.instant('visits.title');
        params.visitConsents = true;
      }

      input = input || {'var': 'ppids', varName: 'participant.ppids', varDesc: 'participant.ppids_csv'};
      return [{ group: group, type: type, title: msg('participant.consents'), '$$input': input, params: params }];
    }

    function getVisitTypes(entityForms, cp, addConsent) {
      var group = $translate.instant('visits.title');
      var input = {'var': 'visitNames', varName: 'visits.names', varDesc: 'visits.names_csv'};

      var exportTypes = [
        { group: group, type: 'visit', title: msg('visits.visits_text_rpts'), '$$input': input },
        { group: group, type: 'visit', title: msg('visits.visits_pdf_rpts'), '$$input': input, params: { sprFileType: 'pdf' } }
      ];

      if (addConsent) {
        exportTypes = exportTypes.concat(getConsentTypes(cp, input, false));
      }

      return addForms(exportTypes, group, 'SpecimenCollectionGroup', input, entityForms['SpecimenCollectionGroup']);
    }

    function getSpecimenTypes(cp, entityForms) {
      var group = $translate.instant('specimens.title');
      var input = {'var': 'specimenLabels', varName: 'specimens.labels', varDesc: 'specimens.labels_csv'};

      var exportTypes = [{ group: group, type: 'specimen', title: msg('specimens.list'), '$$input': input }]
      addForms(exportTypes, group, 'Specimen', input, entityForms['Specimen']);
      return addForms(exportTypes, group, 'SpecimenEvent', input, entityForms['SpecimenEvent']);
    }

    function getExportDetail(cp, allowedEntityTypes, forms) {
      var breadcrumbs, onSuccess;
      if (cp.id == -1) {
        breadcrumbs = [{state: 'cp-list', title: 'cp.list'}];
        onSuccess = {state: 'cp-list', params: {}};
      } else {
        breadcrumbs = [{state: 'cp-list-view', title: cp.shortTitle, params: '{cpId:' + cp.id + '}'}];
        onSuccess = {state: 'cp-list-view', params: {cpId: cp.id}};
      }

      var entityForms = {};
      angular.forEach(forms,
        function(form) {
          if (!entityForms[form.entityType]) {
            entityForms[form.entityType] = [];
          }

          entityForms[form.entityType].push(form);
        }
      );

      var exportTypes = [];
      if ((!cp || cp.id == -1) && allowedEntityTypes.indexOf('CollectionProtocol') >= 0) {
        exportTypes = exportTypes.concat(getCpTypes());
      }

      if (!cp.specimenCentric && allowedEntityTypes.indexOf('Participant') >= 0) {
        exportTypes = exportTypes.concat(getParticipantTypes(entityForms, cp, allowedEntityTypes.indexOf('Consent') >= 0));
      } else if (!cp.specimenCentric && allowedEntityTypes.indexOf('Consent') >= 0) {
        exportTypes = exportTypes.concat(getConsentTypes(cp.id));
      }

      if (!cp.specimenCentric && allowedEntityTypes.indexOf('SpecimenCollectionGroup') >= 0) {
        exportTypes = exportTypes.concat(getVisitTypes(entityForms, cp, allowedEntityTypes.indexOf('Consent') >= 0));
      }

      if (allowedEntityTypes.indexOf('Specimen') >= 0) {
        exportTypes = exportTypes.concat(getSpecimenTypes(cp, entityForms));
      }

      angular.forEach(exportTypes,
        function(exportType) {
          if (!exportType.params) {
            exportType.params = {};
          }

          exportType.params.cpId = cp.id;
        }
      );

      return {
        breadcrumbs: breadcrumbs,
        title: 'export.title',
        type: undefined,
        onSuccess: onSuccess,
        types: exportTypes,
        params: {
          cpId: cp.id
        }
      };
    }

    return {
      getExportDetail: function(cp, allowedEntityTypes, forms) {
        return $translate('common.none').then(
          function() {
            return getExportDetail(cp, allowedEntityTypes, forms);
          }
        );
      }
    }
  });
