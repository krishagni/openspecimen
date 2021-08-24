
<template>
  <form novalidate>
    <div class="row" v-for="(formRow, rowIdx) of formRows" :key="rowIdx">
      <template v-for="(field, fieldIdx) of formRow" :key="rowIdx + '_' + fieldIdx">
        <div class="field">
          <os-label>{{field.label}}</os-label>
          <component :is="field.component" v-bind="field" v-model="ctx.formData[field.name]"
            :form="ctx" @update:model-value="handleInput(field)">
          </component>
          <div v-if="v$.ctx.formData[field.name] && v$.ctx.formData[field.name].$error">
            <os-inline-message>{{errorMessages[field.name]}}</os-inline-message>
          </div>
        </div>
      </template>
    </div>

    <os-divider></os-divider>

    <div class="row">
      <div class="field">
        <slot></slot>
      </div>
    </div>
  </form>
</template>

<script>

import { reactive } from 'vue';
import useVuelidate from '@vuelidate/core'

import alertSvc from '@/common/services/Alerts.js';
import fieldFactory from '@/common/services/FieldFactory.js';

import Dropdown from '@/common/components/Dropdown.vue';
import MultiSelectDropdown from '@/common/components/MultiSelectDropdown.vue';
import DatePicker from '@/common/components/DatePicker.vue';
import InputText from '@/common/components/InputText.vue';
import Password from '@/common/components/Password.vue';
import RadioButton from '@/common/components/RadioButton.vue';
import BooleanCheckbox from '@/common/components/BooleanCheckbox.vue';
import Checkbox from '@/common/components/Checkbox.vue';
import Textarea from '@/common/components/Textarea.vue';
import FileUpload from '@/common/components/FileUpload.vue';
import SignaturePad from '@/common/components/SignaturePad.vue';
import UserDropdown from '@/common/components/UserDropdown.vue';
import PvDropdown from '@/common/components/PvDropdown.vue';
import SiteDropdown from '@/common/components/SiteDropdown.vue';
import StorageContainerDropdown from '@/common/components/StorageContainerDropdown.vue';
import Label from '@/common/components/Label.vue';
import InlineMessage from '@/common/components/InlineMessage.vue';
import Divider from '@/common/components/Divider.vue';

export default {
   props: ['schema', 'data'],

   components: {
     Dropdown,
     MultiSelectDropdown,
     DatePicker,
     InputText,
     Password,
     RadioButton,
     BooleanCheckbox,
     Checkbox,
     Textarea,
     FileUpload,
     SignaturePad,
     UserDropdown,
     PvDropdown,
     SiteDropdown,
     StorageContainerDropdown,
     'os-label': Label,
     'os-inline-message': InlineMessage,
     'os-divider': Divider
   },

   emits: ['input', 'form-validity'],

   setup(props) {
     let ctx = reactive({
       formData: props.data,
       fd: function(name) {
         return ctx.formData[name];
       }
     });

     return {
       ctx,
       v$: useVuelidate(),
     };
   },

   methods: {
     handleInput: function(field) {
       this.$emit('input', {field: field, data: this.ctx.formData})
       if (this.v$.ctx.formData[field.name]) {
         this.v$.ctx.formData[field.name].$touch();
       }

       this.$emit('form-validity', {invalid: this.v$.$invalid});
     },

     validate: function() {
       this.v$.$touch();
       this.$emit('form-validity', {invalid: this.v$.$invalid});
       if (this.v$.$invalid) {
         alertSvc.error('There are validation errors as highlighted below. Please correct them.');
       }

       return !this.v$.$invalid;
     },

     fd: function(name) {
       return this.ctx.formData[name]
     }
   },

   validations() {
     let fields = [];
     for (let row of this.schema.rows) {
       for (let field of row.fields) {
         fields.push(field);
       }
     }

     return {
       ctx: {
         formData: fieldFactory.getValidationRules(fields)
       }
     }
   },

   computed: {
     formRows: function() {
       let result = [];

       for (let row of this.schema.rows) {
         let formRow = [];

         for (let field of row.fields) {
           if (field.showWhen) {
             var showFn = new Function('return ' + field.showWhen);
             if (!showFn.call(this)) {
               continue;
             }
           }

           let component = fieldFactory.getComponent(field.type);
           formRow.push({...field, component: component});
         }

         if (formRow.length > 0) {
           result.push(formRow);
         }
       }

       return result;
     },

     errorMessages: function() {
       let result = {};

       for (let row of this.schema.rows) {
         for (let field of row.fields) {
           let validators = this.v$.ctx.formData && this.v$.ctx.formData[field.name];
           if (!validators) {
             continue;
           }

           for (let rule in field.validations) {
             if (validators[rule] && validators[rule].$invalid) {
               result[field.name] = field.validations[rule].message;
               break;
             }
           }
         }
       }

       return result;
     }
   },

   watch: {
     'ctx.formData': {
       deep: true,

       handler: function(newVal) {
         console.log(newVal);
       }
     }
   }
}

</script>

<style scoped>

form {
  max-width: 720px;
  margin: auto;
}

.row {
  display: flex;
}

.row .field {
  flex: 1 1 0;
  padding: 0.5rem 1rem;
}

.row .field :deep(.btn) {
  margin-right: 0.5rem;
}
</style>
