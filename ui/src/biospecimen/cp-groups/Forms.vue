<template>
  <os-grid>
    <os-grid-column width="12">
      <os-accordion>
        <os-accordion-tab v-for="level of levels" :key="level.entityType">
          <template #header>
            <span class="title">{{level.title}}</span>
          </template>
          <template #content>
            <div>
              <ul class="os-key-values">
                <li class="item">
                  <strong class="key" v-t="'cpgs.custom_fields'">Custom Fields</strong>
                  <span class="value" v-if="ctx.forms[level.customFields] && ctx.forms[level.customFields].length == 1">
                    <router-link :to="{name: 'FormPreview', params: {formId: ctx.forms[level.customFields][0].formId}}"
                      target="_blank">
                      <span>{{ctx.forms[level.customFields][0].caption}}</span>
                    </router-link>
                    <os-button left-icon="times" size="small" @click="removeCustomFieldsForm(level.customFields)"
                      v-if="updateAllowed" />
                  </span>
                  <span class="value" v-else>
                    <span v-t="'common.none'">-</span>
                    <os-button left-icon="plus" size="small" @click="showAddFormDialog(level.customFields, true)"
                      v-if="updateAllowed" />
                  </span>
                </li>
              </ul>
              <os-section>
                <template #title>
                  <span v-t="'cpgs.forms'">Forms</span>
                </template>
                <template #content>
                  <div>
                    <div class="toolbar" v-if="updateAllowed">
                      <os-button left-icon="plus"  :label="$t('common.buttons.add')"
                        @click="showAddFormDialog(level.entityType, false)"
                        v-if="!ctx.selectedForms[level.entityType] || ctx.selectedForms[level.entityType].length == 0" />
                      <os-button left-icon="times" :label="$t('common.buttons.remove')" v-else
                        @click="removeForms(level.entityType)" />
                    </div>

                    <os-list-view ref="formsList" :data="ctx.forms[level.entityType]" :schema="formsListSchema"
                      :allow-selection="updateAllowed"
                      @selected-rows="onFormsSelection(level.entityType, $event)" />
                  </div>
                </template>
              </os-section>
            </div>
          </template>
        </os-accordion-tab>
      </os-accordion>
    </os-grid-column>

    <os-dialog ref="addFormDialog">
      <template #header>
        <span v-t="'cpgs.add_form'">Add Form</span>
      </template>
      <template #content>
        <os-form ref="addForm" :schema="addFormFs" :data="ctx" />
      </template>
      <template #footer>
        <os-button text :label="$t('common.buttons.cancel')" @click="hideAddFormDialog" />
        <os-button primary :label="$t('common.buttons.add')" @click="addForm" />
      </template>
    </os-dialog>

    <os-confirm ref="rmFormsConfirm">
      <template #title>
        <span v-if="ctx.toRemove.formIds.length > 1" v-t="'cpgs.remove_forms'">Remove Forms</span>
        <span v-else v-t="'cpgs.remove_form'">Remove Form</span>
      </template>
      <template #message>
        <span v-if="ctx.toRemove.formIds.length > 1" v-t="'cpgs.confirm_remove_forms'">Remove Forms</span>
        <span v-else v-t="'cpgs.confirm_remove_form'">Remove Form</span>
      </template>
    </os-confirm>
  </os-grid>
</template>

<script>
import cpgSvc from '@/biospecimen/services/CollectionProtocolGroup.js';
import formSvc from '@/forms/services/Form.js';
import routerSvc from '@/common/services/Router.js';

import addFormSchema from '@/biospecimen/schemas/cp-groups/add-form.js';

export default {
  props: ['cpg', 'permOpts'],

  data() {
    return {
      ctx: {
        level: ['Participant', 'SpecimenCollectionGroup', 'Specimen'],

        forms: {},

        selectedForms: {},

        formCtx: {
        },

        getForms: this._getForms
      },

      addFormFs: addFormSchema.layout
    }
  },

  mounted: function() {
    this._loadForms();
  },

  computed: {
    eximAllowed: function() {
      return this.permOpts && this.permOpts.eximAllowed;
    },

    updateAllowed: function() {
      return this.permOpts && this.permOpts.updateAllowed;
    },

    levels: function() {
      return [
        {
          customFields: 'ParticipantExtension',
          entityType: 'Participant',
          title: this.$t('cpgs.participant')
        },
        {
          customFields: 'VisitExtension',
          entityType: 'SpecimenCollectionGroup',
          title: this.$t('cpgs.visit')
        },
        {
          customFields: 'SpecimenExtension',
          entityType: 'Specimen',
          title: this.$t('cpgs.specimen')
        }
      ];
    },

    formsListSchema: function() {
      return {
        columns: [
          {
            "name": "caption",
            "labelCode": "cpgs.name",
            "href": ({rowObject}) => routerSvc.getUrl('FormPreview', {formId: rowObject.formId}),
            "hrefTarget": "_blank"
          },
          {
            "name": "multipleRecords",
            "labelCode": "cpgs.multiple_records",
            "value": ({multipleRecords}) => this.$t(multipleRecords ? 'common.yes' : 'common.no')
          },
          {
            "name": "notifEnabled",
            "labelCode": "cpgs.form_notifs_enabled",
            "value": ({notifEnabled}) => this.$t(notifEnabled ? 'common.yes' : 'common.no')
          },
          {
            "name": "dataInNotif",
            "labelCode": "cpgs.form_data_in_notif",
            "value": ({dataInNotif}) => this.$t(dataInNotif ? 'common.yes' : 'common.no')
          },
          {
            "name": "notifUserGroups",
            "labelCode": "cpgs.notif_rcpts",
            "value": ({notifUserGroups}) => (notifUserGroups || []).map(({name}) => name).join(', ')
          }
        ]
      }
    }
  },

  methods: {
    onFormsSelection: function(entityType, selectedForms) {
      this.ctx.selectedForms[entityType] = selectedForms.map(({rowObject}) => rowObject);
    },

    showAddFormDialog: function(level, customFields) {
      this.ctx.formCtx = {level, customFields};
      this.$refs.addFormDialog.open();
    },

    hideAddFormDialog: function() {
      this.ctx.formCtx = {};
      this.$refs.addFormDialog.close();
    },

    addForm: function() {
      if (!this.$refs.addForm.validate()) {
        return;
      }

      const {level, form, multipleRecords, notifEnabled, dataInNotif, notifUserGroups} = this.ctx.formCtx;
      const toAdd = {...form, multipleRecords, notifEnabled, dataInNotif, notifUserGroups}
      cpgSvc.addForms(this.cpg.id, level, [toAdd]).then(
        () => {
          this.hideAddFormDialog();
          this._reloadForms();
        }
      );
    },

    removeCustomFieldsForm: function(level) {
      this.ctx.toRemove = { level, formIds: [this.ctx.forms[level][0].formId] };
      this._removeForms(this.ctx.toRemove);
    },

    removeForms: function(level) {
      this.ctx.toRemove = { level, formIds: this.ctx.selectedForms[level].map(({formId}) => formId) };
      this._removeForms(this.ctx.toRemove);
    },

    _loadForms: async function() {
      const forms = await cpgSvc.getForms(this.cpg.id);
      this.ctx.forms = forms.reduce(
        (map, groupForms) => {
          map[groupForms.level] = groupForms.forms;
          return map;
        },
        {}
      );
      this.ctx.selectedForms = {};
    },

    _getForms: function(query) {
      return formSvc.getForms({name: query}).then(
        dbForms => {
          dbForms.forEach(form => form.id = form.formId)

          const {formCtx, forms} = this.ctx;
          const entityFormIds = (forms[formCtx.level] || []).map(({formId}) => formId);
          return dbForms.filter(form => entityFormIds.indexOf(form.id) == -1);
        }
      );
    },

    _removeForms: function(toRemove) {
      this.$refs.rmFormsConfirm.open().then(
        resp => {
          if (resp == 'proceed') {
            const {level, formIds} = toRemove;
            cpgSvc.removeForms(this.cpg.id, level, formIds).then(() => this._reloadForms());
          }
        }
      );
    },

    _reloadForms: function() {
      this.ctx.forms = {};
      for (const formsList of this.$refs.formsList) {
        formsList.reload();
      }

      setTimeout(() => this._loadForms());
    }
  }
}
</script>

<style scoped>
.value {
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
}

.value :deep(.btn) {
  margin-left: 1rem;
}

.toolbar {
  margin-bottom: 1rem;
}
</style>
