<template>
  <os-dialog ref="formDialog">
    <template #header>
      <span v-if="ctx.rateList.id > 0" v-t="'lab_services.edit_rate_list'">Edit Rate List</span>
      <span v-else v-t="'lab_services.create_rate_list'">Create Rate List</span>
    </template>

    <template #content>
      <os-form ref="rateListForm" :schema="rateListFs" :data="ctx" />
    </template>

    <template #footer>
      <os-button text :label="$t('common.buttons.cancel')" @click="hideFormDialog" />
      <os-button primary :label="$t(ctx.rateList.id > 0 ? 'common.buttons.update' : 'common.buttons.create')"
        @click="saveOrUpdate" />
    </template>
  </os-dialog>
</template>

<script>
import rateListSvc from '@/biospecimen/services/RateList.js';
import util from '@/common/services/Util.js';

export default {
  data() {
    return {
      ctx: {
        rateList: {}
      },

      rateListFs: rateListSvc.getAddEditSchema()
    }
  },

  methods: {
    open: function(rateList) {
      this.ctx.rateList = util.clone(rateList || {});

      const that = this;
      return new Promise(
        (resolve) => {
          that.ctx.resolve = resolve
          that.$refs.formDialog.open();
        }
      );
    },

    hideFormDialog: function() {
      this.$refs.formDialog.close();
    },

    saveOrUpdate: function() {
      if (!this.$refs.rateListForm.validate()) {
        return;
      }

      rateListSvc.saveOrUpdate(this.ctx.rateList).then(
        saved => {
          this.$refs.formDialog.close();
          this.ctx.resolve(saved);
        }
      );
    }
  }
}
</script>
