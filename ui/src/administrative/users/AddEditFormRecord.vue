
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

import formSvc from '@/forms/services/Form.js';

export default {
  props: ['forms', 'records', 'formId', 'formCtxtId', 'recordId'],

  components: {
    Button,
    Form,
    Panel
  },

  setup(props) {
    let ctx = reactive({
      formSchema: { rows: [] },

      record: {}
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
                    let fs = { name: field.udn, label: field.caption };
                    if (field.type == 'stringTextField') {
                      fs.type = 'text';
                    } else if (field.type == 'textArea') {
                      fs.type = 'textarea';
                    } else if (field.type == 'radiobutton') {
                      fs.type = 'radio';
                      fs.options = (field.pvs || []).map((pv) => ({caption: pv.optionName, value: pv.value}));
                    } else if (field.type == 'checkbox') {
                      fs.type = 'checkbox';
                      fs.options = (field.pvs || []).map((pv) => ({caption: pv.optionName, value: pv.value}));
                    } else if (field.type == 'booleanCheckbox') {
                      fs.type = 'booleanCheckbox';
                    } else if (field.type == 'combobox') {
                      fs.type = 'dropdown';
                      fs.listSource = {
                        options: (field.pvs || []).map((pv) => ({caption: pv.optionName, value: pv.value})),
                        displayProp: 'caption',
                        selectProp: 'value'
                      }
                    } else if (field.type == 'multiSelectListbox') {
                      fs.type = 'multiselect';
                      fs.listSource = {
                        options: (field.pvs || []).map((pv) => ({caption: pv.optionName, value: pv.value})),
                        displayProp: 'caption',
                        selectProp: 'value'
                      }
                    } else if (field.type == 'datePicker') {
                      fs.type = 'datePicker';
                      fs.showTime = field.format && field.format.indexOf('HH:mm') > 0;
                    }

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

      alert(JSON.stringify(this.ctx.record));
    },

    cancel: function() {
      alert('cancel');
    }
  }
}

</script>
