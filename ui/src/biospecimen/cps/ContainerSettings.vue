<template>
  <os-panel>
    <template #header>
      <span class="title" v-t="'cps.container_settings'">Container Settings</span>
    </template>

    <template #primary-actions>
      <os-button left-icon="edit" :label="$t('common.buttons.edit')" @click="switchToEditMode"
        v-show-if-allowed="cpResources.updateOpts" v-if="!editMode" />

      <os-button-link class="btn" left-icon="question-circle" :label="$t('common.buttons.help')"
        url="https://openspecimen.atlassian.net/l/cp/SYfV0Nfw" :new-tab="true" />
    </template>

    <div v-if="!editMode">
      <ul class="os-key-values os-one-col bg-col">
        <li class="item">
          <strong class="key key-sm">
            <span v-t="'cps.auto_allocation'">Auto Allocation</span>
          </strong>
          <span class="value value-md" v-if="cp.containerSelectionStrategy">
            <div>
              <span v-t="'cps.auto_allocation_methods.' + cp.containerSelectionStrategy"></span>
              <span v-if="cp.aliquotsInSameContainer">
                <span>&nbsp;</span>
                <span>(<span v-t="'cps.store_aliquots_in_same_box'"></span>)</span>
              </span>
            </div>

            <div>
              <span v-t="'cps.auto_allocation_help.' + cp.containerSelectionStrategy"></span>
              <span v-if="cp.aliquotsInSameContainer">
                <span>&nbsp;</span>
                <span v-t="'cps.store_aliquots_in_same_box_help'"></span>
              </span>
            </div>
          </span>
          <span class="value value-md" v-else>
            <span v-t="'common.not_specified'"></span>
          </span>
        </li>
      </ul>
    </div>

    <div v-else>
      <os-form ref="settingsForm" :schema="formSchema" :data="dataCtx">
        <template v-slot:[`cp.containerSelectionStrategy`]>
          <div v-if="dataCtx.cp.containerSelectionStrategy" style="margin: 1rem 0rem;">
            <i v-t="'cps.auto_allocation_help.' + dataCtx.cp.containerSelectionStrategy"></i>
          </div>
        </template>

        <template v-slot:[`cp.aliquotsInSameContainer`]>
          <div v-if="dataCtx.cp.aliquotsInSameContainer">
            <i v-t="'cps.store_aliquots_in_same_box_help'"></i>
          </div>
        </template>

        <div>
          <os-button primary :label="$t('common.buttons.update')" @click="update" />
          <os-button text    :label="$t('common.buttons.cancel')" @click="switchToViewMode" />
        </div>
      </os-form>
    </div>
  </os-panel>
</template>

<script>

import addEditLayout from '@/biospecimen/schemas/cps/container-settings-addedit.js';

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

      formSchema: this._getFormLayout()
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
      if (!this.$refs.settingsForm.validate()) {
        return;
      }

      const toSave = util.clone(this.dataCtx.cp);
      cpSvc.saveOrUpdate(toSave).then(
        savedCp => {
          this.$emit('cp-saved', savedCp);
          alertsSvc.success({code: 'cps.container_settings_saved'});
          this.switchToViewMode();
        }
      );
    },

    _getFormLayout: function() {
      return addEditLayout.layout
    }
  }
}
</script>
