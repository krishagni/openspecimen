
<template>
  <os-dialog ref="saveQueryDialog">
    <template #header>
      <span v-t="'queries.save_query'">Save Query</span>
    </template>

    <template #content>
      <os-form ref="saveQueryForm" :schema="saveQueryFs" :data="ctx" />
    </template>

    <template #footer>
      <os-button text      :label="$t('common.buttons.cancel')" @click="cancel" />

      <os-button secondary :label="$t('queries.save_copy')" @click="saveCopy" 
        v-show-if-allowed="queryResources.createOpts" v-if="ctx.query.id > 0" />

      <os-button primary   :label="$t('common.buttons.save')" @click="saveOrUpdate(false)"
        v-show-if-allowed="queryResources.updateOpts" />
    </template>
  </os-dialog>
</template>

<script>
import saveQuerySchema from '@/queries/schemas/save-query.js';

import savedQuerySvc from '@/queries/services/SavedQuery.js';
import util from '@/common/services/Util.js';

import queryResources from './Resources.js';

export default {
  data() {
    return {
      ctx: {
        query: {}
      },


      queryResources,

      saveQueryFs: saveQuerySchema.layout
    }
  },

  created() {
  },

  methods: {
    save: function(query) {
      this.ctx.queryId = query.id;
      this.ctx.query = util.clone(query);
      let self = this;
      return new Promise((resolve) => {
        self.resolve = resolve;
        self.$refs.saveQueryDialog.open();
      });
    },

    cancel: function() {
      this.resolve('cancel');
      this.$refs.saveQueryDialog.close();
      this.resolve = null;
      this.ctx = {};
    },

    saveCopy: function() {
      this.saveOrUpdate(true);
    },

    saveOrUpdate: function(copy) {
      if (!this.$refs.saveQueryForm.validate()) {
        return;
      }

      if (copy) {
        this.ctx.query.id = null;
      } else {
        this.ctx.query.id = this.ctx.queryId;
      }

      for (let filter of this.ctx.query.filters) {
        delete filter.fieldObj;
      }

      savedQuerySvc.saveOrUpdate(this.ctx.query).then(
        savedQuery => {
          this.resolve({status: 'saved', query: savedQuery});
          this.$refs.saveQueryDialog.close();
          this.resolve = null;
          this.ctx = {};
        }
      );
    }
  }
}
</script>
