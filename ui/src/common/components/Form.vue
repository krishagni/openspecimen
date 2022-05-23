
<template>
  <form novalidate>
    <div class="row" v-for="(formRow, rowIdx) of formRows" :key="rowIdx">
      <template v-for="(field, fieldIdx) of formRow" :key="rowIdx + '_' + fieldIdx">
        <div class="field">
          <os-label v-show="field.label">
            <span>{{field.label}}</span>
            <span class="required-indicator" v-show="field.required" v-os-tooltip.bottom="field.requiredTooltip">
              <span>*</span>
            </span>
            <span class="help" v-if="field.tooltip">
              <os-icon name="question-circle" v-os-tooltip.bottom="field.tooltip"/>
            </span>
          </os-label>
          <component :ref="'osField-' + field.name" :is="field.component" v-bind="field"
            v-model="formModel[field.name]" :form="ctx" :context="ctx" @update:model-value="handleInput(field)">
          </component>
          <div v-if="v$.formModel[field.name] && v$.formModel[field.name].$error">
            <os-inline-message>{{errorMessages[field.name]}}</os-inline-message>
          </div>
        </div>
      </template>
    </div>

    <os-divider v-if="$slots.default && $slots.default().length > 0"></os-divider>

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

import alertSvc     from '@/common/services/Alerts.js';
import fieldFactory from '@/common/services/FieldFactory.js';
import http         from '@/common/services/HttpClient.js';
import exprUtil     from '@/common/services/ExpressionUtil.js';
import util         from '@/common/services/Util.js';

export default {
   props: ['schema', 'data'],

   emits: ['input', 'form-validity'],

   setup(props) {
     let ctx = reactive({
       formData: props.data || {},
       fd: function(name) {
         let object = ctx.formData;
         if (!name) {
           return object;
         }

         let props = name.split('.');
         for (let i = 0; i < props.length; ++i) {
           if (!object) {
             return undefined;
           }
           object = object[props[i]];
         }

         return object;
       }
     });

     return {
       ctx,
       v$: useVuelidate(),
     };
   },

   beforeCreate: function() {
     this.ctx._formCache = {};
   },

   mounted() {
     http.addListener(this);
   },

   unmounted() {
     http.removeListener(this);
     if (this.masked) {
       util.disableMask();
     }
   },

   methods: {
     handleInput: function(field) {
       exprUtil.setValue(this.ctx.formData, field.name, this.formModel[field.name]);
       this.$emit('input', {field: field, value: this.formModel[field.name], data: this.ctx.formData})
       if (this.v$.formModel[field.name]) {
         this.v$.formModel[field.name].$touch();
       }

       this.$emit('form-validity', {invalid: this.v$.$invalid});
     },

     validate: function() {
       this.v$.$touch();

       let invalid = this.v$.$invalid;
       for (let formRow of this.formRows) {
         for (let field of formRow) {
           if (field.type == 'subform') {
             let sf = this.$refs['osField-' + field.name];
             if (sf instanceof Array && sf.length > 0) {
               sf = sf[0];
             }

             if (!sf.validate()) {
               invalid = true;
             }
           }
         }
       }

       this.$emit('form-validity', {invalid: invalid});
       if (invalid) {
         alertSvc.error('There are validation errors as highlighted below. Please correct them.');
       }

       return !invalid;
     },

     fd: function(name) {
       let object = this.ctx.formData;
       if (!name) {
         return object;
       }

       let props = name.split('.');
       for (let i = 0; i < props.length; ++i) {
         if (!object) {
           return undefined;
         }
         object = object[props[i]];
       }

       return object;
     },

     uploadFile: async function(fieldName) {
       let fieldRef = this.$refs['osField-' + fieldName];
       if (fieldRef instanceof Array && fieldRef.length > 0) {
         fieldRef = fieldRef[0];
       }

       if (fieldRef) {
         return fieldRef.upload();
       }

       return null;
     },

     callStarted: function({method}) {
       if (method == 'post' || method == 'put') {
         this.masked = true;
         util.enableMask();
       }
     },

     callCompleted: function({method}) {
       if (method == 'post' || method == 'put') {
         this.masked = false;
         util.disableMask();
       }
     },

     callFailed: function({method}) {
       if (method == 'post' || method == 'put') {
         this.masked = false;
         util.disableMask();
       }
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
       formModel: fieldFactory.getValidationRules(fields)
     }
   },

   computed: {
     formRows: function() {
       let result = [];

       for (let row of this.schema.rows) {
         let formRow = [];

         for (let field of row.fields) {
           if (field.showWhen && !exprUtil.eval(this, field.showWhen)) {
             continue;
           }

           if (!field.component) {
             let component = fieldFactory.getComponent(field.type);
             if (component) {
               field = Object.assign({...field, component: component});
             }
           }

           if (field.disableWhen) {
             field.disabled = exprUtil.eval(this, field.disableWhen);
           }

           const fv = field.validations;
           if (fv && (fv.requiredIf || fv.required)) {
             field.required = !!fv.required || exprUtil.eval(this, fv.requiredIf.expr);
             if (field.required) {
               field.requiredTooltip = (fv.required || fv.requiredIf).message || 'Mandatory field'
             }
           }

           formRow.push(field);
         }

         if (formRow.length > 0) {
           result.push(formRow);
         }
       }

       return result;
     },

     //
     // TODO: Is this efficient?
     //
     formModel: function() {
       let model = {};
       for (let row of this.schema.rows) {
         for (let field of row.fields) {
           model[field.name] = exprUtil.getValue(this.ctx.formData, field.name);
         }
       }

       return model;
     },

     errorMessages: function() {
       let result = {};

       for (let row of this.schema.rows) {
         for (let field of row.fields) {
           let validators = this.v$.formModel && this.v$.formModel[field.name];
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
     data: function(newVal) {
       this.ctx.formData = newVal;
     },

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
  max-width: 80%;
  margin: auto;
}

.p-dialog form {
  max-width: 100%;
}

.row {
  display: flex;
}

.row .field {
  flex: 1 1 0;
  padding: 0.5rem 1rem;
  overflow-x: auto;
}

.row .field :deep(.btn) {
  margin-right: 0.5rem;
}

.row .field .required-indicator {
  display: inline-block;
  padding: 0.25rem;
  color: red;
  cursor: help;
}

.row .field .help {
  display: inline-block;
  padding: 0.25rem;
}
</style>
