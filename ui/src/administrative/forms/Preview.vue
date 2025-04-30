<template>
  <div class="os-form-preview">
    <os-form :schema="ctx.formSchema" :data="ctx.record"
      v-if="ctx.formSchema.rows && ctx.formSchema.rows.length > 0">
      <os-button primary :label="$t('common.buttons.save')"   @click="noop" />
      <os-button text    :label="$t('common.buttons.cancel')" @click="noop" />
    </os-form>
    <os-message type="info" v-else>
      <span v-t="'forms.no_fields_in_form'">No fields in the form</span>
    </os-message>
  </div>
</template>

<script>

import formSvc  from '@/forms/services/Form.js';
import formUtil from '@/common/services/FormUtil.js';

export default {
  props: ['form'],

  emits: ['form-definition'],

  data() {
    return {
      ctx: {
        formSchema: {rows: []},

        record: {}
      }
    };
  },

  created() {
    this._setupFormSchema();
  },

  watch: {
    form: function() {
      this._setupFormSchema();
    }
  },

  methods: {
    noop: function() {
      alert('Hoorayyyy... It works!');
    },

    _setupFormSchema: async function() {
      const formDef = await formSvc.getDefinition(this.form.formId);
      const { schema, defaultValues } = formUtil.fromDeToStdSchema(formDef);
      this.ctx.formSchema = schema;
      this.ctx.record = defaultValues;
      this.$emit('form-definition', formDef);
    }
  }
}
</script>

<style scoped>
.os-form-preview {
  margin-top: 1rem;
  overflow-y: auto;
}
</style>
