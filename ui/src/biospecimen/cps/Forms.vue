<template>
  <os-grid>
    <os-grid-column class="os-cp-forms" :width="3" style="overflow-y: auto;">

      <os-list-group :list="participantForms" :selected="participantForm"
        @on-item-select="onParticipantFormSelect($event)">
        <template #header>
          <span v-t="'cps.participant_forms'">Participant</span>
        </template>

        <template #actions v-if="participantForms.length > 1">
          <os-button left-icon="sort" size="small"
            @click="showSortFormsDialog('CommonParticipant', participantForms)"
            v-show-if-allowed="cpResources.updateOpts" />
        </template>

        <template #empty-list>
          <os-message class="os-cp-no-forms" type="info">
            <span v-t="'cps.no_forms'"></span>
          </os-message>
        </template>

        <template #default="{item}">
          <span>{{item.caption}}</span>
        </template>
      </os-list-group>

      <os-list-group :list="registrationForms" :selected="registrationForm"
        @on-item-select="onRegFormSelect($event)">
        <template #header>
          <span v-t="'cps.registration_forms'">Participant</span>
        </template>

        <template #actions v-if="registrationForms.length > 1">
          <os-button left-icon="sort" size="small"
            @click="showSortFormsDialog('Participant', registrationForms)"
            v-show-if-allowed="cpResources.updateOpts"  />
        </template>

        <template #empty-list>
          <os-message type="info">
            <span v-t="'cps.no_forms'"></span>
          </os-message>
        </template>

        <template #default="{item}">
          <span>{{item.caption}}</span>
        </template>
      </os-list-group>

      <os-list-group :list="visitForms" :selected="visitForm"
        @on-item-select="onVisitFormSelect($event)">
        <template #header>
          <span v-t="'cps.visit_forms'">Visit</span>
        </template>

        <template #actions v-if="visitForms.length > 1">
          <os-button left-icon="sort" size="small"
            @click="showSortFormsDialog('SpecimenCollectionGroup', visitForms)" 
            v-show-if-allowed="cpResources.updateOpts" />
        </template>

        <template #empty-list>
          <os-message type="info">
            <span v-t="'cps.no_forms'"></span>
          </os-message>
        </template>

        <template #default="{item}">
          <span>{{item.caption}}</span>
        </template>
      </os-list-group>

      <os-list-group :list="specimenForms" :selected="specimenForm"
        @on-item-select="onSpecimenFormSelect($event)">
        <template #header>
          <span v-t="'cps.specimen_forms'">Specimen</span>
        </template>

        <template #actions v-if="specimenForms.length > 1">
          <os-button left-icon="sort" size="small"
            @click="showSortFormsDialog('Specimen', specimenForms)" 
            v-show-if-allowed="cpResources.updateOpts" />
        </template>

        <template #empty-list>
          <os-message type="info">
            <span v-t="'cps.no_forms'"></span>
          </os-message>
        </template>

        <template #default="{item}">
          <span>{{item.caption}}</span>
        </template>
      </os-list-group>
    </os-grid-column>

    <os-grid-column :width="9" style="overflow-y: auto;">
      <os-panel>
        <template #header>
          <span v-t="{path: 'cps.form_preview', args: selectedForm}">Form Preview</span>
        </template>
        <template #default>
          <div v-if="!selectedForm">
            <os-message type="info">
              <span v-t="'cps.select_form_on_left'">Select a form on the left side</span>
            </os-message>
          </div>
          <div v-else-if="!selectedFormSchema">
            <os-message type="info">
              <span v-t="'cps.loading_form'">Loading...</span>
            </os-message>
          </div>
          <div v-else>
            <os-form :schema="selectedFormSchema" :object="{}" />
          </div>
        </template>
      </os-panel>
    </os-grid-column>
  </os-grid>

  <os-dialog ref="sortFormsDialog">
    <template #header>
      <span v-t="sortFormsTitle"></span>
    </template>
    <template #content>
      <os-sort-list v-model="sortCtx.forms" item-key="id">
        <template #item="{item}">
          <div>{{item.caption}}</div>
        </template>
      </os-sort-list>
    </template>
    <template #footer>
      <os-button text    :label="$t('common.buttons.cancel')" @click="hideSortFormsDialog" />
      <os-button primary :label="$t('common.buttons.save')"   @click="saveFormsOrder" />
    </template>
  </os-dialog>
</template>

<script>

import alertsSvc from '@/common/services/Alerts.js';
import cpSvc     from '@/biospecimen/services/CollectionProtocol.js';
import formSvc   from '@/forms/services/Form.js';
import formUtil  from '@/common/services/FormUtil.js';
import util      from '@/common/services/Util.js';

import cpResources from './Resources.js';

export default {
  props: ['cp'],

  data() {
    return {
      selectedForm: null,

      selectedFormSchema: null,

      participantForm: null,

      participantForms: [],

      registrationForm: null,

      registrationForms: [],

      visitForm: null,

      visitForms: [],

      specimenForm: null,

      specimenForms: [],

      sortCtx: {},

      cpResources
    }
  },

  created() {
    this._loadForms();
  },

  computed: {
    sortFormsTitle: function() {
      const {entityType} = this.sortCtx || {};
      if (entityType == 'CommonParticipant') {
        return 'cps.sort_participant_forms';
      } else if (entityType == 'Participant') {
        return 'cps.sort_registration_forms';
      } else if (entityType == 'SpecimenCollectionGroup') {
        return 'cps.sort_visit_forms';
      } else if (entityType == 'Specimen') {
        return 'cps.sort_specimen_forms';
      }

      return '';
    }
  },

  methods: {
    onParticipantFormSelect: function({item}) {
      this._selectForm('participantForm', item);
    },

    onRegFormSelect: function({item}) {
      this._selectForm('registrationForm', item);
    },

    onVisitFormSelect: function({item}) {
      this._selectForm('visitForm', item);
    },

    onSpecimenFormSelect: function({item}) {
      this._selectForm('specimenForm', item);
    },

    showSortFormsDialog: function(entityType, forms) {
      this.sortCtx.entityType = entityType;
      this.sortCtx.forms = forms.map(form => ({id: form.formId, name: form.name, caption: form.caption}));
      this.$refs.sortFormsDialog.open();
    },

    hideSortFormsDialog: function() {
      this.$refs.sortFormsDialog.close();
    },

    saveFormsOrder: function() {
      const formsOrder = util.clone(this.formsOrder || {});
      formsOrder[this.sortCtx.entityType] = this.sortCtx.forms.map(({id, name}) => ({id, name})); 
      cpSvc.saveWorkflow(this.cp.id, 'forms', formsOrder).then(
        () => {
          this._sortForms(this.forms);
          alertsSvc.success({code: 'cps.forms_sort_order_saved'});
          this.hideSortFormsDialog();
        }
      );
    },

    _selectForm: function(entity, form) {
      this.selectedFormSchema = null;
      this.participantForm = this.registrationForm = this.visitForm = this.specimenForm = null;

      if (this.selectedForm == form) {
        this[entity] = this.selectedForm = null;
        return;
      }
        
      this[entity] = this.selectedForm = form;

      const formsCache = this.formsCache = this.formsCache || {};
      if (!formsCache[form.formId]) {
        formsCache[form.formId] = formSvc.getDefinition(form.formId).then(
          formDef => {
            const {schema} = formUtil.fromDeToStdSchema(formDef);
            return schema;
          }
        );
      }

      formsCache[form.formId].then(formSchema => this.selectedFormSchema = formSchema);
    },

    _loadForms: function() {
      const entityTypes = ['CommonParticipant', 'Participant', 'SpecimenCollectionGroup', 'Specimen'];
      cpSvc.getForms(this.cp.id, entityTypes).then(
        (forms) => {
          this.forms = forms;
          this._sortForms(forms);
        }
      );
    },

    _sortForms: async function(forms) {
      const formsOrder = this.formsOrder = await cpSvc.getWorkflow(this.cp.id, 'forms') || {};
      const map = forms.reduce(
        (acc, f) => {
          const entityForms = acc[f.entityType] = acc[f.entityType] || [];
          entityForms.push(f);
          return acc;
        },
        {}
      );

      this.participantForms  = this._sortEntityForms(map['CommonParticipant'] || [], formsOrder.CommonParticipant);
      this.registrationForms = this._sortEntityForms(map['Participant'] || [], formsOrder.Participant);
      this.visitForms        = this._sortEntityForms(map['SpecimenCollectionGroup'] || [], formsOrder.SpecimenCollectionGroup);
      this.specimenForms     = this._sortEntityForms(map['Specimen'] || [], formsOrder.Specimen);
    },

    _sortEntityForms: function(forms, order) {
      if (!order || order.length == 0) {
        return forms;
      }

      forms.sort((f1, f2) => this._getIndex(order, f1) - this._getIndex(order, f2));
      return forms;
    },

    _getIndex: function(forms, f) {
      return forms.findIndex(form => form.id == f.formId);
    }
  }
}
</script>

<style scoped>
.os-cp-forms :deep(.os-panel) {
  margin-bottom: 1.25rem;
}

.os-cp-no-forms {
  margin: 1rem;
}

</style>
