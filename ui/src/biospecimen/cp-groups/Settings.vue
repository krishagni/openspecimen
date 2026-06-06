<template>
  <os-grid>
    <os-grid-column :width="12">
      <os-panel>
        <template #primary-actions>
          <os-button left-icon="edit" :label="$t('common.buttons.edit')" @click="showEditDialog"
            v-if="permOpts.updateAllowed" />
        </template>

        <os-overview :schema="dict" :object="{cpg}" :columns="1" :bg-col="true" :show-empty-fields="true" />
      </os-panel>
    </os-grid-column>

    <os-dialog ref="editSettingsDialog">
      <template #header>
        <span v-t="'cps.settings'">Settings</span>
      </template>
      <template #content>
        <os-form ref="settingsForm" :schema="formSchema" :data="ctx" />
      </template>
      <template #footer>
        <os-button text :label="$t('common.buttons.cancel')" @click="hideEditDialog" />
        <os-button primary :label="$t('common.buttons.update')" @click="update" />
      </template>
    </os-dialog>
  </os-grid>
</template>

<script>
import alertsSvc from '@/common/services/Alerts.js';
import cpgSvc    from '@/biospecimen/services/CollectionProtocolGroup.js';
import formUtil  from '@/common/services/FormUtil.js';
import util      from '@/common/services/Util.js';

import settingsSchema from '@/biospecimen/schemas/cp-groups/settings.js';

export default {
  props: ['cpg', 'permOpts'],

  data() {
    return {
      ctx: {
        cpg: {}
      },

      dict: settingsSchema.fields,

      formSchema: formUtil.getFormSchema(settingsSchema.fields, settingsSchema.layout)
    };
  },

  methods: {
    showEditDialog: function() {
      this.ctx.cpg = util.clone(this.cpg);
      this.$refs.editSettingsDialog.open();
    },

    hideEditDialog: function() {
      this.ctx.cpg = {};
      this.$refs.editSettingsDialog.close();
    },

    update: function() {
      if (!this.$refs.settingsForm.validate()) {
        return;
      }

      cpgSvc.saveOrUpdate(this.ctx.cpg).then(
        savedCpg => {
          this.$emit('cpg-saved', savedCpg);
          alertsSvc.success({code: 'cpgs.settings_saved'});
          this.hideEditDialog();
        }
      );
    }
  }
}
</script>
