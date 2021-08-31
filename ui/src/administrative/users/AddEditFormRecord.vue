
<template>
  <Panel>
    <template #header>
      <span>{{ctx.formDef && ctx.formDef.caption}}</span>
    </template>

    <Form ref="deForm" :schema="ctx.formSchema" :data="ctx.record" @input="handleChange($event)">
      <Button label="Save"   @click="saveRecord" />
      <Button label="Cancel" @click="cancel" />
    </Form>
  </Panel>
</template>

<script>

import { reactive, watchEffect } from 'vue';

import Button from '@/common/components/Button.vue';
import Form from '@/common/components/Form.vue';
import Panel from '@/common/components/Panel.vue';

import fieldFactory from '@/common/services/FieldFactory.js';
import routerSvc from '@/common/services/Router.js';
import formSvc from '@/forms/services/Form.js';

export default {
  props: ['entity', 'formId', 'formCtxtId', 'recordId'],

  components: {
    Button,
    Form,
    Panel
  },

  setup(props) {
    let ctx = reactive({
      formSchema: { rows: [] },

      record: { },
    });

    watchEffect(
      () => {
        formSvc.getDefinition(props.formId).then(
          (formDef) => {
            let schema = { rows: [] };
            let dvRec = {}; // default values record

            formDef.rows.forEach(
              (row) => {
                let rowSchema = {fields: []};
                row.forEach(
                  (field) => {
                    let fieldSchema = fieldFactory.getFieldSchema(field);
                    if (fieldSchema.type) {
                      rowSchema.fields.push(fieldSchema);
                      if (fieldSchema.defaultValue) {
                        dvRec[field.name] = fieldSchema.defaultValue;
                      }
                    }
                  }
                );
                schema.rows.push(rowSchema);
              }
            );

            ctx.formSchema = schema;
            ctx.formDef = formDef;
            if (!props.recordId) {
              ctx.record = dvRec;
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
            name: 'UserFormsList',
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
