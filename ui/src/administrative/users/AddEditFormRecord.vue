
<template>
  <os-panel>
    <template #header>
      <span>{{ctx.formDef && ctx.formDef.caption}}</span>
    </template>

    <os-form ref="deForm" :schema="ctx.formSchema" :data="ctx.record" @input="handleChange($event)">
      <os-button primary label="Save"   @click="saveRecord" />
      <os-button text label="Cancel" @click="cancel" />
    </os-form>
  </os-panel>
</template>

<script>

import formUtil from '@/common/services/FormUtil.js';
import routerSvc from '@/common/services/Router.js';
import formSvc from '@/forms/services/Form.js';

export default {
  props: ['entity', 'formId', 'formCtxtId', 'recordId', 'listView', 'routeQuery'],

  data() {
    return {
      ctx: {
        formSchema: { rows: [] },

        record: { },
      }
    };
  },

  created() {
    this.loadForm();
    this.loadRecord();
  },

  watch: {
    formId: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this.loadForm();
      }
    },

    recordId: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this.loadRecord();
      }
    }
  }, 

  methods: {
    loadForm: async function() {
      const formDef = await formSvc.getDefinition(this.formId);
      const { schema, defaultValues } = formUtil.fromDeToStdSchema(formDef);
      this.ctx.formSchema = schema;
      this.defaultValues  = defaultValues;
      this.ctx.formDef    = formDef;
      if (!this.recordId) {
        this.ctx.record = defaultValues;
      }
    },

    loadRecord: async function() {
      if (this.recordId) {
        this.ctx.record = await formSvc.getRecord({formId: this.formId, recordId: this.recordId});
      } else {
        this.ctx.record = this.defaultValues;
      }
    },
      
    handleChange: function(/* event */) {
      // console.log(event);
    },

    saveRecord: function() {
      if (!this.$refs.deForm.validate()) {
        return;
      }

      let formData = this.ctx.record;
      formData.appData = {objectId: +this.entity.id, formCtxtId: +this.formCtxtId, formId: +this.formId};
      formSvc.saveOrUpdateRecord(formData).then(
        (savedData) => {
          this.$emit('reload-records');
          this.$router.push({
            name: this.listView,
            query: {
              ...this.routeQuery,
              formId: this.formId,
              formCtxtId: this.formCtxtId,
              recordId: savedData.id
            }
          });
        }
      );
    },

    cancel: function() {
      routerSvc.back();
    }
  }
}

</script>
