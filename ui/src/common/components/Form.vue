
<template>
  <form novalidate ref="dataEntryForm">
    <div class="row" v-if="$slots.message && $slots.message().length > 0">
      <div class="field">
        <slot name="message"></slot>
      </div>
    </div>

    <div class="row" v-for="(formRow, rowIdx) of formRows" :key="rowIdx">
      <div class="title" v-if="formRow.label || formRow.labelCode">
        <os-label>
          <span>{{label(formRow)}}</span>
        </os-label>
      </div>

      <template v-for="(field, fieldIdx) of formRow.fields" :key="rowIdx + '_' + fieldIdx">
        <div class="field">
          <os-label v-show="field.label || field.labelCode">
            <span>{{label(field)}}</span>
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
          <slot :name="field.name"></slot>
        </div>
      </template>
    </div>

    <slot name="static-fields"></slot>

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
import exprUtil     from '@/common/services/ExpressionUtil.js';
import fieldFactory from '@/common/services/FieldFactory.js';
import http         from '@/common/services/HttpClient.js';
import i18n         from '@/common/services/I18n.js';
import util         from '@/common/services/Util.js';

export default {
   props: ['schema', 'data', 'disabled-fields'],

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
       const oldValue = exprUtil.getValue(this.ctx.formData, field.name);
       exprUtil.setValue(this.ctx.formData, field.name, this.formModel[field.name]);
       this.$emit('input', {field: field, value: this.formModel[field.name], data: this.ctx.formData, oldValue})
       if (this.v$.formModel[field.name]) {
         this.v$.formModel[field.name].$touch();
       }

       this.$emit('form-validity', {invalid: this.v$.$invalid});
     },

     label: function(field) {
       if (field.labelCode) {
         return this.$t(field.labelCode);
       } else if (field.label) {
         return field.label;
       } else {
         return 'Unknown';
       }
     },

     validate: function() {
       this.v$.$touch();

       let invalid = this.v$.$invalid;
       for (let formRow of this.formRows) {
         for (let field of formRow.fields) {
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
         alertSvc.error({code: 'common.form_validation_error'});
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
     },

     getFieldRef(fieldName, instance = 0) {
       let fieldRef = this.$refs['osField-' + fieldName];
       if (fieldRef instanceof Array && fieldRef.length > 0) {
         fieldRef = fieldRef[instance];
       }

       return fieldRef;
     },

     getViewState() {
       let state = [];
       for (let formRow of this.formRows) {
         const row = [];
         for (let field of formRow.fields) {
           let label = '';
           if (field.label || field.labelCode) {
             label = this.label(field);
           } else if (field.icon && field.tooltip) {
             label = field.tooltip;
           }

           row.push({
             label: label,
             value: this.$refs['osField-' + field.name][0].getDisplayValue()
           });
         }
         state.push(row);
       }

       return state;
     },

     scrollToTop() {
       const that = this;
       setTimeout(
         () => {
           let el = that.$refs.dataEntryForm;
           if (el instanceof Array) {
             el = el[0];
           }

           if (el) {
             el.scrollIntoView({block: 'start', behaviour: 'smooth'});
           }
         }, 500);
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

           if (!field.disabled && this.disabledFields instanceof Array) {
             field.disabled = this.disabledFields.indexOf(field.name) >= 0;
           }

           const fv = field.validations;
           if (fv && (fv.requiredIf || fv.required)) {
             field.required = !!fv.required || exprUtil.eval(this, fv.requiredIf.expr);
             if (field.required) {
               const validator = fv.required || fv.requiredIf;
               if (validator.messageCode) {
                 field.requiredTooltip = this.$t(validator.messageCode);
               } else {
                 field.requiredTooltip = validator.message || 'Mandatory field'
               }
             }
           }

           if (field.placeholderCode) {
             field.placeholder = this.$t(field.placeholderCode);
           }

           formRow.push(field);
         }

         if (formRow.length > 0) {
           result.push({label: row.label, labelCode: row.labelCode, fields: formRow});
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
           if (typeof field.value == 'function') {
             model[field.name] = field.value(this.ctx.formData);
           } else {
             model[field.name] = exprUtil.getValue(this.ctx.formData, field.name);
           }
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
               if (field.validations[rule].messageCode) {
                 result[field.name] = i18n.msg(field.validations[rule].messageCode);
               } else if (typeof field.validations[rule].message == 'function') {
                 result[field.name] = field.validations[rule].message();
               } else {
                 result[field.name] = field.validations[rule].message;
               }
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

.row, :slotted(.row) {
  display: flex;
  flex-wrap: wrap;
}

.row .title,
:slotted(.row .title) {
  flex: 1 1 100%;
  padding: 0.5rem 1rem 0rem 1rem;
}

.row .field,
:slotted(.row .field) {
  flex: 1 1 0;
  padding: 0.5rem 1rem;
  overflow-x: auto;
}

.row .title ~ .field,
:slotted(.row .title ~ .field) {
  padding-top: 0rem;
}

.row .title ~ .field label,
:slotted(.row .title ~ .field label) {
  font-size: 85%;
  font-weight: normal;
}

.row .field :deep(.btn),
:slotted(.row .field :deep(.btn)) {
  margin-right: 0.5rem;
}

.row .field .required-indicator,
:slotted(.row .field :deep(.required-indicator)) {
  display: inline-block;
  padding: 0rem 0.25rem;
  color: red;
  cursor: help;
}

.row .field .help,
:slotted(.row .field .help) {
  display: inline-block;
  padding: 0.25rem;
}
</style>
