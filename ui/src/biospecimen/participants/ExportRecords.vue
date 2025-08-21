<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="bcrumb" />
      </template>

      <h3 v-t="'export.export_records'">Export Records</h3>
    </os-page-head>

    <os-page-body>
      <os-grid>
        <os-grid-column :width="12">
          <os-form ref="exportForm" :schema="formSchema" :data="ctx">
            <os-button primary :label="$t('common.buttons.submit')" @click="exportRecords" />
            <os-button text    :label="$t('common.buttons.cancel')" @click="cancel" />
          </os-form>
        </os-grid-column>
      </os-grid>

      <os-confirm ref="confirmExportAll">
        <template #title>
          <span v-t="'export.export_all_q'">Export All</span>
        </template>
        <template #message>
          <span v-t="{path: 'export.export_all_records_q', args: ctx.recordType}">Are you sure you want to export all records of the entity: {recordType}</span>
        </template>
      </os-confirm>
    </os-page-body>
  </os-page>
</template>

<script>
import authSvc     from '@/common/services/Authorization.js';
import cpSvc       from '@/biospecimen/services/CollectionProtocol.js';
import exportSvc   from '@/common/services/ExportService.js';
import routerSvc   from '@/common/services/Router.js';
import settingSvc  from '@/common/services/Setting.js';
import util        from '@/common/services/Util.js';

import CpViewContext from './CpViewContext.js';

export default {
  props: ['cpId'],

  inject: ['cpViewCtx'],

  data() {
    return {
      ctx: {
        recordType: null,

        getRecordTypes: this._getRecordTypes
      }
    };
  },

  computed: {
    bcrumb: function() {
      if (+this.cpId > 0) {
        const cp = this.cpViewCtx.getCp();
        return [ {url: routerSvc.getUrl('ParticipantsList', {cpId: this.cpId}), label: cp.shortTitle } ];
      } else {
        return [ {url: routerSvc.getUrl('CpsList'), label: this.$t('cps.list') } ];
      }
    },

    formSchema: function() {
      return {
        rows: [
          {
            fields: [
              {
                type: 'group-single-select',
                name: 'recordType',
                labelCode: 'export.record_type',
                listSource: {
                  displayProp: 'title',
                  groupNameProp: 'group',
                  groupItemsProp: 'types',
                  loadFn: ({context}) => context.formData.getRecordTypes()
                },
                validations: {
                  required: {
                    messageCode: 'export.record_type_req'
                  }
                }
              }
            ]
          },
          {
            fields: [
              {
                type: 'textarea',
                name: 'ppids',
                labelCode: 'participants.ppids',
                placeholder: this.$t('participants.scan_ppids'),
                showWhen: 'recordType && recordType.group == "participant"'
              }
            ]
          },
          {
            fields: [
              {
                type: 'textarea',
                name: 'visitNames',
                labelCode: 'visits.visit_names',
                placeholder: this.$t('visits.scan_visit_names'),
                showWhen: 'recordType && recordType.group == "visit"'
              }
            ]
          },
          {
            fields: [
              {
                type: 'textarea',
                name: 'specimenLabels',
                labelCode: 'specimens.specimen_labels',
                placeholder: this.$t('specimens.scan_specimen_labels'),
                showWhen: 'recordType && recordType.group == "specimen"'
              }
            ]
          }
        ]
      }
    }
  },

  methods: {
    exportRecords: async function() {
      if (!this.$refs.exportForm.validate()) {
        return;
      }

      const {ppids, visitNames, specimenLabels} = this.ctx;
      if (this._isBlank(ppids) && this._isBlank(visitNames) && this._isBlank(specimenLabels)) {
        this.$refs.confirmExportAll.open().then(
          resp => {
            if (resp == 'proceed') {
              this._exportRecords();
            }
          }
        );
      } else {
        this._exportRecords();
      }
    },


    cancel: function() {
      routerSvc.back();
    },

    _isBlank: function(str) {
      return !str || str.trim().length == 0;
    },

    _exportRecords: function() {
      const {recordType: {type, params}, ppids, visitNames, specimenLabels} = this.ctx;
      const jobParams = {ppids, visitNames, specimenLabels, ...(params || {})};

      const payload = { objectType: type, params: jobParams };
      exportSvc.exportRecords(payload).then(
        () => {
          if (+this.cpId > 0) {
            routerSvc.goto('ParticipantsList', {cpId: this.cpId});
          } else {
            routerSvc.goto('CpsList');
          }
        }
      );
    },

    _allowedEntityTypes: async function() {
      const cpViewCtx = await this._getAccessCtx();
      const cp = cpViewCtx.getCp();
      
      const entityTypes = [];
      if (!cp.id && authSvc.isAllowed(['CollectionProtocol'], ['Export Import'])) {
        entityTypes.push('CollectionProtocol');
      }

      if (!cp.specimenCentric && cpViewCtx.isImportParticipantsAllowed()) {
        Array.prototype.push.apply(entityTypes, ['CommonParticipant', 'Participant']);
      }

      if (!cp.specimenCentric && cpViewCtx.isImportConsentsAllowed()) {
        entityTypes.push('Consent');
      }

      if (!cp.specimenCentric && cpViewCtx.isImportVisitsAllowed()) {
        entityTypes.push('SpecimenCollectionGroup');
      }
    
      if (cpViewCtx.isImportSpecimensAllowed()) {
        entityTypes.push('Specimen');
        entityTypes.push('SpecimenEvent');
      }
    
      return entityTypes;
    },

    _getForms: async function(entityTypes) {
      const cpId = +this.cpId > 0 ? +this.cpId : -1;
      return cpSvc.getForms(cpId, entityTypes);
    },

    _getRecordTypes: async function() {
      const entityTypes = await this._allowedEntityTypes();
      const forms       = await this._getForms(entityTypes);

      const entityFormsMap = {};
      for (const form of forms) {
        const entityForms = entityFormsMap[form.entityType] = entityFormsMap[form.entityType]  || [];
        entityForms.push(form);
      }

      const exportTypes = [];
      if (!(+this.cpId > 0) && entityTypes.indexOf('CollectionProtocol') >= 0) {
        exportTypes.push({group: this.$t('cps.cp'), types: this._getCpTypes()});
      }

      const cp = this.cpViewCtx ? this.cpViewCtx.getCp() : {};
      if (!cp.specimenCentric && entityTypes.indexOf('Participant') >= 0) {
        exportTypes.push({
          group: this.$t('participants.participant'),
          types: this._getParticipantTypes(entityFormsMap, cp, entityTypes.indexOf('Consent') >= 0)
        });
      } else if (!cp.specimenCentric && entityTypes.indexOf('Consent') >= 0) {
        exportTypes.push({
          group: this.$t('participants.participant'),
          types: this._getConsentTypes(cp, true)
        });
      }

      if (!cp.specimenCentric && entityTypes.indexOf('SpecimenCollectionGroup') >= 0) {
        exportTypes.push({
          group: this.$t('visits.visit'),
          types: this._getVisitTypes(entityFormsMap, cp, entityTypes.indexOf('Consent') >= 0)
        });
      }
      
      if (entityTypes.indexOf('Specimen') >= 0) {
        exportTypes.push({
          group: this.$t('specimens.specimen'),
          types: this._getSpecimenTypes(entityFormsMap)
        });
      }

      for (const {types} of exportTypes) {
        for (const type of types) {
          const params = type.params = type.params || {};
          params.cpId = params.cpId || cp.id || -1;
        }
      }
       
      return exportTypes;
    },

    _getCpTypes() {
      const group = 'collection_protocol';
      return [
        {group, id: 'cp',  type: 'cp',  title: this.$t('cps.list')   },
        {group, id: 'cpe', type: 'cpe', title: this.$t('cps.events') },
        {group, id: 'sr',  type: 'sr',  title: this.$t('cps.srs')    }
      ];
    },

    _getParticipantTypes: function(entityFormsMap, cp, addConsent) {
      const group = 'participant';
      
      let exportTypes = [{ group, id: 'cpr', type: 'cpr', title: this.$t('participants.list') }];
      if (addConsent) {
        Array.prototype.push.apply(exportTypes, this._getConsentTypes(cp, true));
      }   

      this._addForms(exportTypes, group, 'CommonParticipant', entityFormsMap['CommonParticipant'] || []);
      return this._addForms(exportTypes, group, 'Participant', entityFormsMap['Participant'] || []);
    },

    _getVisitTypes: function(entityFormsMap, cp, addConsent) {
      const group = 'visit';
      const exportTypes = [
        { group, id: 'visit', type: 'visit', title: this.$t('participants.visits_text_rpts') },
        { group, id: 'visit', type: 'visit', title: this.$t('participants.visits_pdf_rpts'), params: {sprFileType: true } }
      ];

      if (addConsent) {
        Array.prototype.push.apply(exportTypes, this._getConsentTypes(cp, false));
      }

      return this._addForms(exportTypes, group, 'SpecimenCollectionGroup', entityFormsMap['SpecimenCollectionGroup'] || []);
    },

    _getConsentTypes: function(cp, participantLevel) {
      const group = participantLevel ? 'participant' : 'visit';
      if (this.$osSvc.ecDocSvc) {
        if (cp.id > 0) {
          if (cp.visitLevelConsents && participantLevel) {
            return [];
          } else if (!cp.visitLevelConsents && !participantLevel) {
            return [];
          }
        }

        return [{
          id: 'econsentsDocumentResponse', group, type: 'econsentsDocumentResponse',
          title: this.$t('participant_consents.list')
        }];
      } else if (participantLevel) {
        return [{ id: 'consent', group, type: 'consent', title: this.$t('participant_consents.list') }];
      } else {
        return [];
      }
    },

    _getSpecimenTypes: function(entityFormsMap) {
      const group = 'specimen';

      const exportTypes = [];
      exportTypes.push({ group: group, id: 'specimen', type: 'specimen', title: this.$t('specimens.list') });

      this._addForms(exportTypes, group, 'Specimen', entityFormsMap['Specimen']);
      this._addForms(exportTypes, group, 'SpecimenEvent', entityFormsMap['SpecimenEvent']);
      return exportTypes;
    },

    _addForms: function(exportTypes, group, entityType, forms) {
      forms = forms || [];
      for (const form of forms) {
        exportTypes.push({
          id: entityType + '_' + form.formId,
          group,
          type: 'extensions',
          title: form.caption,
          params: { entityType: entityType, formName: form.name }
        });
      }
      
      return exportTypes;
    },

    _getAccessCtx: async function() {
      let result = this.cpViewCtx;
      if (!result) {
        const mrnSetting = await settingSvc.getSetting('biospecimen', 'mrn_restriction_enabled')
        const accessBasedOnMrn = util.isTrue(mrnSetting[0].value);

        const roleSetting = await settingSvc.getSetting('biospecimen', 'coordinator_role_name');
        const coordinatorRole = roleSetting[0].value;

        result = new CpViewContext({cpSites: []}, {accessBasedOnMrn, coordinatorRole});
      }

      return result;
    }
  }
}
</script>
