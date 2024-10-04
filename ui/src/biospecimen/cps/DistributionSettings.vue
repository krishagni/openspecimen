<template>
  <os-panel>
    <template #header>
      <span class="title" v-t="'cps.distribution'">Distribution</span>
    </template>

    <template #primary-actions>
      <os-button left-icon="plus" :label="$t('common.buttons.add')" @click="switchToEditMode"
        v-show-if-allowed="cpResources.updateOpts"
        v-if="!editMode && (!cp.distributionProtocols || cp.distributionProtocols.length == 0)" />

      <os-button left-icon="edit" :label="$t('common.buttons.edit')" @click="switchToEditMode"
        v-show-if-allowed="cpResources.updateOpts"
        v-else-if="!editMode && (cp.distributionProtocols && cp.distributionProtocols.length > 0)" />

      <os-button-link class="btn" left-icon="question-circle" :label="$t('common.buttons.help')"
        url="https://openspecimen.atlassian.net/l/cp/GKDSRkHA" :new-tab="true" />
    </template>

    <div v-if="!editMode">
      <os-overview :schema="dict" :object="{cp}" :columns="1" :bg-col="true" :show-empty-fields="true" />
    </div>

    <div v-else>
      <os-form ref="dpForm" :schema="formSchema" :data="dataCtx">
        <div>
          <os-button primary :label="$t('common.buttons.save')" @click="update" />
          <os-button text    :label="$t('common.buttons.cancel')" @click="switchToViewMode" />
        </div>
      </os-form>
    </div>
  </os-panel>
</template>

<script>

import addEditLayout  from '@/biospecimen/schemas/cps/distribution-settings-addedit.js';
import settingsSchema from '@/biospecimen/schemas/cps/distribution-settings.js';

import alertsSvc from '@/common/services/Alerts.js';
import cpSvc     from '@/biospecimen/services/CollectionProtocol.js';
import formUtil  from '@/common/services/FormUtil.js';
import util      from '@/common/services/Util.js';

import cpResources from './Resources.js';

export default {
  props: ['cp'],

  data() {
    return {
      editMode: false,

      cpResources,

      dataCtx: {},

      dict: settingsSchema.fields,

      formSchema: this._getFormLayout()
    }
  },

  computed: {
    distributionProtocols: function() {
      if (this.cp.distributionProtocols && this.cp.distributionProtocols.length > 0) {
        return this.cp.distributionProtocols.map(dp => dp.shortTitle).join(', ');
      }

      return null;
    }
  },

  methods: {
    switchToEditMode: function() {
      const cp = this.dataCtx.cp = util.clone(this.cp);
      formUtil.createCustomFieldsMap(cp);
      this.editMode = true;
    },

    switchToViewMode: function() {
      this.editMode = false;
    },

    update: function() {
      if (!this.$refs.dpForm.validate()) {
        return;
      }

      const toSave = util.clone(this.dataCtx.cp);
      cpSvc.saveOrUpdate(toSave).then(
        savedCp => {
          this.$emit('cp-saved', savedCp);
          alertsSvc.success({code: 'cps.dp_settings_saved'});
          this.switchToViewMode();
        }
      );
    },

    _getFormLayout: function() {
      return formUtil.getFormSchema(settingsSchema.fields, addEditLayout.layout);
    }
  }
}
</script>
