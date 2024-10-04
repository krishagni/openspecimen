<template>
  <os-panel class="os-full-height-panel">
    <template #header>
      <span class="title" v-t="'cps.label_fmt_n_print'">Label Format and Print</span>
    </template>

    <template #primary-actions>
      <os-button left-icon="edit" :label="$t('common.buttons.edit')" @click="switchToEditMode"
        v-show-if-allowed="cpResources.updateOpts" v-if="!editMode" />

      <os-button-link class="btn" left-icon="question-circle" :label="$t('common.buttons.help')"
        url="https://openspecimen.atlassian.net/wiki/x/TgBvG" :new-tab="true" />
    </template>

    <div v-if="!editMode">
      <os-overview :schema="labelsDict" :object="ctx" :columns="1" :bg-col="true" :show-empty-fields="true" />

      <os-section>
        <template #title>
          <span v-t="'cps.misc_settings'">Miscellaneous Settings</span>
        </template>

        <template #content>
          <os-overview :schema="miscDict" :object="ctx" :columns="1" :bg-col="true" :show-empty-fields="true" />
        </template>
      </os-section>
    </div>

    <div v-else>
      <os-form ref="labelsForm" :schema="addEditFs" :data="dataCtx">
        <div>
          <os-button primary :label="$t('common.buttons.update')" @click="update" />
          <os-button text :label="$t('common.buttons.cancel')" @click="switchToViewMode" />
        </div>
      </os-form>
    </div>
  </os-panel>
</template>

<script>

import addEditLayout       from '@/biospecimen/schemas/cps/label-settings-addedit.js';
import labelSettingsSchema from '@/biospecimen/schemas/cps/label-settings.js';
import miscSettingsSchema  from '@/biospecimen/schemas/cps/misc-settings.js';

import alertsSvc from '@/common/services/Alerts.js';
import cpSvc     from '@/biospecimen/services/CollectionProtocol.js';
import formUtil  from '@/common/services/FormUtil.js';
import util      from '@/common/services/Util.js';

import cpResources from './Resources.js';

export default {
  props: ['cp'],

  emits: ['cp-saved'],

  data() {
    const fields = [...labelSettingsSchema.fields];
    Array.prototype.push.apply(fields, miscSettingsSchema.fields);

    const addEditFs = formUtil.getFormSchema(fields, addEditLayout.layout);
    return {
      editMode: false,

      addEditFs,

      labelsDict: labelSettingsSchema.fields,

      miscDict: miscSettingsSchema.fields,

      ctx: {},

      dataCtx: {},

      cpResources
    }
  },

  created() {
    this._setupCp();
  },

  watch: {
    cp: function() {
      this._setupCp();
    }
  },

  methods: {
    switchToEditMode: function() {
      const cp = this.dataCtx.cp = util.clone(this.cp);
      formUtil.createCustomFieldsMap(cp);

      cp.spmnLabelPrintSettings = this._getLabelPrintSettings(cp)
      if (cp.setQtyToZero != true && cp.setQtyToZero != false) {
        cp.setQtyToZero = 'use_system_setting';
      }

      this.editMode = true;
    },

    switchToViewMode: function() {
      this.editMode = false;
    },

    update: function() {
      if (!this.$refs.labelsForm.validate()) {
        return;
      }

      const toSave = util.clone(this.dataCtx.cp);
      if (toSave.setQtyToZero == 'use_system_setting') {
        toSave.setQtyToZero = null;
      }

      cpSvc.saveOrUpdate(toSave).then(
        savedCp => {
          this.$emit('cp-saved', savedCp);
          alertsSvc.success({code: 'cps.label_fmt_n_print_saved'});
          this.switchToViewMode();
        }
      );
    },

    _setupCp: function() {
      const cp = util.clone(this.cp);
      cp.spmnLabelPrintSettings = this._getLabelPrintSettings(cp);
      this.ctx.cp = cp;
    },

    _getLabelPrintSettings: function(cp) {
      const lineages = ['New', 'Derived', 'Aliquot'];
      let spmnLabelPrintSettings = cp.spmnLabelPrintSettings || [];
      return lineages.map(
        lineage => {
          const setting = spmnLabelPrintSettings.find(s => s.lineage == lineage);
          return setting ? setting : {lineage};
        }
      );
    }
  }
}
</script>
