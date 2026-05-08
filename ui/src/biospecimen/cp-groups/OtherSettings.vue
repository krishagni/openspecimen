<template>
  <os-page-toolbar v-if="updateAllowed">
    <template #default>
      <os-button left-icon="edit" :label="$t('common.buttons.edit')" @click="showEditDialog" />
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="12">
      <os-overview :schema="settingsDict" :object="{cpg}" :show-empty-fields="true" />
    </os-grid-column>
  </os-grid>

  <os-dialog ref="settingsDialog">
    <template #header>
      <span v-t="'cpgs.other_settings'">Other settings</span>
    </template>
    <template #content>
      <os-form ref="settingsForm" :schema="formSchema" :data="dataCtx" />
    </template>
    <template #footer>
      <os-button text    :label="$t('common.buttons.cancel')" @click="hideEditDialog" />
      <os-button primary :label="$t('common.buttons.update')" @click="save" />
    </template>
  </os-dialog>
</template>

<script>
import addEditLayout  from '@/biospecimen/schemas/cp-groups/settings-addedit.js';
import settingsSchema from '@/biospecimen/schemas/cp-groups/settings.js';

import alertsSvc from '@/common/services/Alerts.js';
import cpgSvc    from '@/biospecimen/services/CollectionProtocolGroup.js';
import formUtil  from '@/common/services/FormUtil.js';
import util      from '@/common/services/Util.js';

export default {
  props: ['cpg', 'permOpts'],

  emits: ['cpg-saved'],

  data() {
    return {
      dataCtx: {},

      formSchema: formUtil.getFormSchema(settingsSchema.fields, addEditLayout.layout),

      settingsDict: settingsSchema.fields
    };
  },

  computed: {
    updateAllowed: function() {
      return this.permOpts && this.permOpts.updateAllowed;
    }
  },

  methods: {
    showEditDialog: function() {
      this.dataCtx = {cpg: util.clone(this.cpg)};
      this.$refs.settingsDialog.open();
    },

    hideEditDialog: function() {
      this.dataCtx = {};
      this.$refs.settingsDialog.close();
    },

    save: function() {
      if (!this.$refs.settingsForm.validate()) {
        return;
      }

      cpgSvc.saveOrUpdate(this.dataCtx.cpg).then(
        savedCpg => cpgSvc.getGroupById(savedCpg.id)
      ).then(
        cpg => {
          this.$emit('cpg-saved', cpg);
          alertsSvc.success({code: 'cpgs.settings_saved'});
          this.hideEditDialog();
        }
      );
    }
  }
}
</script>
