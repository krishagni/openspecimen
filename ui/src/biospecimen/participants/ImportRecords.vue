<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="bcrumb" />
      </template>

      <h3 v-t="'participants.import_records'">Import Records</h3>
    </os-page-head>

    <os-page-body>
      <os-grid>
        <os-grid-column :width="12">
          <os-import-records ref="impRecsForm" :object-type="objectType" :object-params="objectParams"
            :hide-ops="hideOps" :show-upsert="showUpsert" :csv-type="csvType" :record-types="getRecordTypes"
            :field-separator="fieldSeparator" :repeat-job-id="repeatJobId"
            @record-type-selected="onRecordTypeSelection">
            <os-button primary :label="$t('import.validate_n_import')" @click="submitJob" />

            <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />
          </os-import-records>
        </os-grid-column>
      </os-grid>
    </os-page-body>
  </os-page>
</template>

<script>
import authSvc     from '@/common/services/Authorization.js';
import cpSvc       from '@/biospecimen/services/CollectionProtocol.js';
import pluginViews from '@/common/services/PluginViewsRegistry.js';
import routerSvc   from '@/common/services/Router.js';
import settingSvc  from '@/common/services/Setting.js';
import util        from '@/common/services/Util.js';

import CpViewContext from './CpViewContext.js';

export default {
  props: ['cpId', 'repeatJobId'],

  inject: ['cpViewCtx'],

  data() {
    return {
      recordType: {},

      getRecordTypes: this._getRecordTypes
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

    objectType: function() {
      return null;
    },

    objectParams: function() {
      return {cpId: this.cpId};
    },

    hideOps: function() {
      const {hideOps} = this.recordType || {};
      return hideOps;
    },

    csvType: function() {
      const {csvType} = this.recordType || {};
      return csvType;
    },

    showUpsert: function() {
      const {showUpsert} = this.recordType || {};
      return showUpsert;
    },

    fieldSeparator: function() {
      const {fieldSeparator} = this.recordType || {};
      return fieldSeparator || null;
    }
  },

  methods: {
    submitJob: async function() {
      const job = await this.$refs.impRecsForm.importRecords();
      if (job == null) {
        return null;
      }

      if (typeof this.onSubmit == 'function') {
        this.onSubmit(job);
      } else {
        routerSvc.back();
      }
    },

    cancel: function() {
      routerSvc.back();
    },

    onRecordTypeSelection: function(recordType) {
      this.recordType = recordType || {};
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
    
      if (cpViewCtx.isImportAllSpecimensAllowed()) {
        entityTypes.push('DerivativeAndAliquots');
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

      const importTypes = [];
      if (!(+this.cpId > 0) && entityTypes.indexOf('CollectionProtocol') >= 0) {
        Array.prototype.push.apply(importTypes, this._getCpTypes());
      }

      const cp = this.cpViewCtx ? this.cpViewCtx.getCp() : {};
      if (!cp.specimenCentric && entityTypes.indexOf('Participant') >= 0) {
        Array.prototype.push.apply(importTypes, this._getParticipantTypes(entityFormsMap, cp, entityTypes.indexOf('Consent') >= 0));
      } else if (!cp.specimenCentric && entityTypes.indexOf('Consent') >= 0) {
        Array.prototype.push.apply(importTypes, this._getConsentTypes(cp, true));
      }

      if (!cp.specimenCentric && entityTypes.indexOf('SpecimenCollectionGroup') >= 0) {
        Array.prototype.push.apply(importTypes, this._getVisitTypes(entityFormsMap, cp, entityTypes.indexOf('Consent') >= 0));
      }
      
      if (entityTypes.indexOf('Specimen') >= 0) {
        Array.prototype.push.apply(importTypes, this._getSpecimenTypes(entityFormsMap, cp, entityTypes));
      }

      for (const importType of importTypes) {
        const params = importType.params = importType.params || {};
        params.cpId = cp.id || -1;
      }
       
      return importTypes;
    },

    _getCpTypes() {
      const group = this.$t('cps.cp');
      return [
        {group: group, id: 'cp',  type: 'cp',  title: this.$t('cps.list'),   hideOps: false, showUpsert: false },
        {group: group, id: 'cpe', type: 'cpe', title: this.$t('cps.events'), hideOps: false, showUpsert: false },
        {group: group, id: 'sr',  type: 'sr',  title: this.$t('cps.srs'),    hideOps: false, showUpsert: false }
      ];
    },

    _getParticipantTypes: function(entityFormsMap, cp, addConsent) {
      const group = this.$t('participants.list');
      
      let importTypes = [];
      if (!cp.id || cp.id == -1) {
        importTypes = [
          {
            group, id: 'cprMultiple', type: 'cprMultiple', title: this.$t('participants.registrations'),
            hideOps: false, showUpsert: true
          }
        ]
      } else {
        importTypes = [
          {
            group, id: 'cpr', type: 'cpr', title: this.$t('participants.list'), hideOps: false, showUpsert: true
          }
        ]
      }

      if (addConsent) {
        Array.prototype.push.apply(importTypes, this._getConsentTypes(cp, true));
      }   

      this._addPluginTypes(importTypes, group, 'Participant');
      this._addForms(importTypes, group, 'CommonParticipant', entityFormsMap['CommonParticipant'] || []);
      return this._addForms(importTypes, group, 'Participant', entityFormsMap['Participant'] || []);
    },

    _getVisitTypes: function(entityFormsMap, cp, addConsent) {
      const group = this.$t('participants.visits');
      const importTypes = [{ group, id: 'visit', type: 'visit', title: group, showUpsert: true }];

      if (addConsent) {
        Array.prototype.push.apply(importTypes, this._getConsentTypes(cp, false));
      }

      this._addPluginTypes(importTypes, group, 'SpecimenCollectionGroup');
      return this._addForms(importTypes, group, 'SpecimenCollectionGroup', entityFormsMap['SpecimenCollectionGroup'] || []);
    },

    _getConsentTypes: function(cp, participantLevel) {
      const group = this.$t(participantLevel ? 'participants.list' : 'participants.visit');
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
          title: this.$t('participant_consents.list'), hideOps: true
        }];
      } else if (participantLevel) {
        return [{
          id: 'consent', group, type: 'consent', title: this.$t('participant_consents.list'),
          hideOps: true, csvType: 'MULTIPLE_ROWS_PER_OBJ'
        }];
      } else {
        return [];
      }
    },

    _getSpecimenTypes: function(entityFormsMap, cp, entityTypes) {
      const group = this.$t('specimens.list');

      const importTypes = [];
      importTypes.push({ group: group, id: 'specimen', type: 'specimen', title: group, showUpsert: true });

      if (entityTypes.indexOf('DerivativeAndAliquots') != -1) {
        importTypes.push({
          group: group, id: 'specimenAliquot', type: 'specimenAliquot', title: this.$t('specimens.aliquots'), hideOps: true
        });
        importTypes.push({
          group: group, id: 'specimenDerivative', type: 'specimenDerivative', title: this.$t('specimens.derivatives'), hideOps: true
        });
      }

      if (!cp.specimenCentric) {
        importTypes.push({
          group: group, id: 'masterSpecimen', type: 'masterSpecimen', title: this.$t('participants.master_specimens'),
          hideOps: true
        });
      }

      if (!cp.id || cp.id == -1) {
        importTypes.push({
          group: group, id: 'containerSpecimen', type: 'containerSpecimen',
          title: this.$t('specimens.container_specimens'), hideOps: true
        });
      }

      importTypes.push({
        group: group, id: 'specimenDisposal', type: 'specimenDisposal', title: this.$t('specimens.dispose_specimens'),
        hideOps: true
      });

      this._addPluginTypes(importTypes, group, 'Specimen');
      this._addForms(importTypes, group, 'Specimen', entityFormsMap['Specimen']);
      this._addForms(importTypes, group, 'SpecimenEvent', entityFormsMap['SpecimenEvent']);
      return this._addEventTypes(cp, importTypes, group);
    },

    _addEventTypes: function(cp, importTypes, group) {
      if (cp.id != -1) {
        return importTypes;
      }

      const events = [
        'containerTransferEvent', 'specimenDisposalEvent',
        'specimenReservedEvent', 'specimenReservationCancelEvent',
        'specimenReturnEvent', 'specimenTransferEvent'
      ];
      for (const event of events) {
        importTypes.push({
          group,
          id: event,
          type: event,
          title: this.$t('imports.object_types.' + event),
          hideOps: true,
          params: {}
        });
      }

      return importTypes;
    },

    _addForms: function(importTypes, group, entityType, forms) {
      for (const form of forms) {
        if (form.sysForm) { 
          continue;
        }
          
        importTypes.push({
          id: entityType + '_' + form.formId,
          group,
          type: 'extensions',
          title: form.caption,
          params: { entityType: entityType, formName: form.name },
          showUpsert: true
        });
      }
      
      return importTypes;
    },

    _addPluginTypes: function(importTypes, group, entityType) {
      const types = pluginViews.getImportTypes(entityType);
      if (!types || types.length == 0) {
        return;
      }

      for (const type of types) {
        const importType = util.clone(type);
        importType.group = group;
        importTypes.push(importType);
      }

      return importTypes;
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
  },
}
</script>
