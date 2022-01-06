
<template>
  <os-panel>
    <template #header>
      <span>{{ctx.formDef && ctx.formDef.caption}}</span>
    </template>

    <os-form ref="deForm" :schema="ctx.formSchema" :data="ctx.record" @input="handleChange($event)">
      <os-button primary label="Save"   @click="saveRecord" />
      <os-button label="Cancel" @click="cancel" />
    </os-form>
  </os-panel>
</template>

<script>

import { reactive, watchEffect } from 'vue';

import formUtil from '@/common/services/FormUtil.js';
import routerSvc from '@/common/services/Router.js';
import formSvc from '@/forms/services/Form.js';

export default {
  props: ['entity', 'formId', 'formCtxtId', 'recordId', 'listView'],

  setup(props) {
    let ctx = reactive({
      formSchema: { rows: [] },

      record: { },
    });

    watchEffect(
      () => {
        formSvc.getDefinition(props.formId).then(
          (formDef) => {
            const { schema, defaultValues } = formUtil.fromDeToStdSchema(formDef);
            ctx.formSchema = schema;
            ctx.formDef    = formDef;
            if (!props.recordId) {
              ctx.record = defaultValues;
            }
          }
        );

        if (props.recordId) {
          formSvc.getRecord({formId: props.formId, recordId: props.recordId}).then(
            (record) => {
              ctx.record = record;
            }
          );
        }
      }
    );

    return { ctx };
  },

  methods: {
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
