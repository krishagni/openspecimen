
<template>
  <os-panel :class="hidePanel && 'hide-panel'">
    <template #header>
      <span>{{ctx.formDef && ctx.formDef.caption}}</span>
    </template>

    <os-form ref="deForm" :schema="ctx.formSchema" :data="ctx.record" @input="handleChange($event)">
      <os-button primary :label="$t('common.buttons.save')"   @click="saveRecord(false)" />
      <os-button primary :label="$t('common.buttons.save_draft')" @click="saveRecord(false, true)" v-if="showDraft" />
      <os-button primary :label="$t('common.buttons.save_n_next')"  @click="saveRecord(true)" v-if="showNext" />
      <os-button text    :label="$t('common.buttons.cancel')" @click="cancel" />
    </os-form>
  </os-panel>
</template>

<script>

import formUtil  from '@/common/services/FormUtil.js';
import formSvc   from '@/forms/services/Form.js';

export default {
  props: [
    'entity',
    'formId',
    'formCtxtId',
    'formDef',
    'recordId',
    'hidePanel',
    'showNext',
    'showDraft'
  ],

  emits: ['saved', 'cancelled'],

  data() {
    return {
      ctx: {
        formSchema: { rows: [] },

        record: { },
      }
    };
  },

  created() {
    this._loadForm();
    this._loadRecord();
  },

  watch: {
    formId: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this._loadForm();
      }
    },

    recordId: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this._loadRecord();
      }
    },

    formDef: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this._loadForm();
      }
    }
  }, 

  methods: {
    _loadForm: async function() {
      let formDef = this.formDef;
      if (!formDef) {
        formDef = await formSvc.getDefinition(this.formId);
      }

      const { schema, defaultValues } = formUtil.fromDeToStdSchema(formDef);
      this.ctx.formSchema = schema;
      this.defaultValues  = defaultValues;
      this.ctx.formDef    = formDef;
      if (!this.recordId) {
        this.ctx.record = defaultValues;
      }
    },

    _loadRecord: async function() {
      if (this.recordId) {
        this.ctx.record = await formSvc.getRecord({formId: this.formId, recordId: this.recordId});
      } else {
        this.ctx.record = this.defaultValues;
      }
    },
      
    handleChange: function(/* event */) {
      // console.log(event);
    },

    saveRecord: function(next, saveAsDraft) {
      if (!saveAsDraft && !this.$refs.deForm.validate()) {
        return;
      }

      let formStatus = saveAsDraft ? 'DRAFT' : 'COMPLETE';
      let formData = this.ctx.record;
      formData.appData = {objectId: +this.entity.id, formCtxtId: +this.formCtxtId, formId: +this.formId, formStatus};
      formSvc.saveOrUpdateRecord(formData).then(
        (savedData) => {
          savedData.nextForm = next;
          this.$emit('saved', savedData);
        }
      );
    },

    cancel: function() {
      this.$emit('cancelled');
    }
  }
}
</script>

<style scoped>
.hide-panel :deep(.p-panel-header) {
  display: none;
}

.hide-panel :deep(.p-panel-content) {
  border: 0;
}
</style>
