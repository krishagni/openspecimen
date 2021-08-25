
<template>
  <Panel>
    <template #header>
      <span v-if="ctx.formDef">{{ctx.formDef.caption}}</span>
    </template>

    <Form ref="deForm" :schema="ctx.formSchema" :data="ctx.record" @input="handleChange($event)">
      <Button label="Save" @click="saveRecord" />
      <Button label="Cancel" @click="cancel" />
    </Form>

    <pre> {{ ctx.formDef }} </pre>
  </Panel>
</template>

<script>

import { reactive, watchEffect } from 'vue';

import Button from '@/common/components/Button.vue';
import Form from '@/common/components/Form.vue';
import Panel from '@/common/components/Panel.vue';

// import http from '@/common/services/HttpClient.js';
import fieldFactory from '@/common/services/FieldFactory.js';
import formSvc from '@/forms/services/Form.js';

export default {
  props: ['objectId', 'forms', 'records', 'formId', 'formCtxtId', 'recordId'],

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
            formDef.rows.forEach(
              (row) => {
                let rs = {fields: []};
                row.forEach(
                  (field) => {
                    let fs = fieldFactory.getFieldSchema(field);
                    if (fs.type) {
                      rs.fields.push(fs);
                    }
                  }
                );
                schema.rows.push(rs);
              }
            );

            ctx.formSchema = schema;
            ctx.formDef = formDef;
          }
        );

        formSvc.getRecord({formId: props.formId, recordId: props.recordId}).then(
          (record) => {
            ctx.record = record;
          }
        );
      }
    );

    return { ctx };
  },

  methods: {
    handleChange: function(event) {
      console.log(event);
    },

    saveRecord: function() {
      if (!this.$refs.deForm.validate()) {
        return;
      }

      let formData = this.ctx.record;
      formData.appData = {objectId: +this.objectId, formCtxtId: +this.formCtxtId, formId: +this.formId};
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
      alert('cancel');
    }
  }
}

</script>
