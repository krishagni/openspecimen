<template>
  <div class="os-subform">
    <div class="table-wrapper" v-if="inputValue && inputValue.length > 0">
      <table>
        <thead>
          <tr>
            <th v-for="(field, idx) of sfFields" :key="idx">
              <os-html :content="label(field)" />
              <span class="required-indicator" v-show="field.required" v-os-tooltip.bottom="field.requiredTooltip">
                <span>*</span>
              </span>
            </th>
            <th class="actioncol" v-if="readOnlyCollection != true">&nbsp;</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(sfRowData, sfRdIdx) of inputValue" :key="sfRdIdx">
            <td v-for="(field, cidx) of sfFields" :key="cidx">
              <component :ref="'osField-' + cidx" :is="field.component" v-bind="field" v-model="sfRowData[field.name]"
                :form="{...context, ...sfRowData}" :context="{...context, ...sfRowData}"
                @update:model-value="handleInput(sfRowData, sfRdIdx, field)">
              </component>
              <div v-if="v$.inputValue[sfRdIdx][field.name] && v$.inputValue[sfRdIdx][field.name].$error">
                <os-inline-message>{{errorMessages[sfRdIdx][field.name]}}</os-inline-message>
              </div>
            </td>
            <td class="actioncol" v-if="readOnlyCollection != true">
              <os-button left-icon="times" @click="removeSfRow(sfRdIdx)" />
            </td>
          </tr>
        </tbody>
      </table> 
    </div>
    <div v-else-if="readOnlyCollection == true">
      <span>-</span>
    </div>

    <os-button class="inline-button" left-icon="plus"
      :label="inputValue && inputValue.length > 0 ? $t('common.buttons.add_another') : $t('common.buttons.add')"
      @click="addSfRow" v-if="readOnlyCollection != true" />
  </div>
</template>

<script>

import useVuelidate from '@vuelidate/core'

import exprUtil from '@/common/services/ExpressionUtil.js';
import fieldFactory from '@/common/services/FieldFactory.js';
import i18n         from '@/common/services/I18n.js';

export default {
  props: ['modelValue', 'fields', 'disabled', 'context', 'read-only-collection'],

  setup() {
    return {
      v$: useVuelidate(),
    };
  },

  data() {
    return {
    }
  },

  validations() {
    if (!this.inputValue) {
      return {inputValue: []};
    }

    return {
      inputValue: this.inputValue.map(() => fieldFactory.getValidationRules(this.fields))
    }
  },

  computed: {
    inputValue: {
      get() {
        return this.modelValue ? this.modelValue : [];
      },

      set(value) {
        this.$emit('update:modelValue', value);
      }
    },

    sfFields: function() {
      const result = [];
      for (let field of this.fields) {
        if (!field.component) {
          const component = fieldFactory.getComponent(field.type);
          if (component) {
            field = {...field, component: component};
          }
        }

        const fv = field.validations;
        if (fv && (fv.required || fv.requiredIf)) {
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

        field.disabled = (this.disabled == 'true' || this.disabled == true);
        result.push(field);
      }

      return result;
    },

    errorMessages: function() {
      let result = [];
      if (!this.inputValue) {
        return result;
      }

      for (let rowIdx = 0; rowIdx < this.inputValue.length; ++rowIdx) {
        const error = {};
        result.push(error);

        for (let field of this.fields) {
          let validators = this.v$.inputValue &&
            this.v$.inputValue[rowIdx] &&
            this.v$.inputValue[rowIdx][field.name];

          if (!validators) {
            continue;
          }

          for (let rule in field.validations) {
            if (validators[rule] && validators[rule].$invalid) {
              if (field.validations[rule].messageCode) {
                error[field.name] = i18n.msg(field.validations[rule].messageCode);
              } else if (typeof field.validations[rule].message == 'function') {
                error[field.name] = field.validations[rule].message();
              } else {
                error[field.name] = field.validations[rule].message;
              }
              break;
            }
          }
        }
      }

      return result;
    }
  },

  methods: {
    handleInput: function(sfRowData, sfRdIdx, field) {
      this.$emit('input', {field: field, data: sfRowData})
      if (this.v$.inputValue[sfRdIdx][field.name]) {
        this.v$.inputValue[sfRdIdx][field.name].$touch();
      }
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
      return !this.v$.$invalid;
    },

    addSfRow: function() {
      this.inputValue = this.inputValue || [];
      this.inputValue.push(
        this.fields.reduce(
          (acc, field) => {
            if (field.defaultValue) {
              acc[field.name] = field.defaultValue;
            }

            return acc;
          },
          {}
        )
      );
    },

    removeSfRow: function(idx) {
      this.inputValue.splice(idx, 1);
    },

    getDisplayValue: function() {
      let result = [];
      for (let idx = 0; idx < this.sfFields.length; ++idx) {
        const refs = this.$refs['osField-' + idx];
        if (!refs) {
          break;
        }

        const values = refs.map(ref => ref.getDisplayValue());
        result.push({label: this.label(this.sfFields[idx]), values: values});
      }

      return result;
    }
  }
}
</script>

<style scoped>

.os-subform table {
  border-collapse: collapse;
  width: 100%;
}

.os-subform table th {
  padding: 0.5rem 0.75rem;
  min-width: 175px;
  color: #707070;
  background: #efefef;
  border-bottom: 1px solid #dee2e6;
  font-weight: bold;
  text-align: left;
}

.os-subform .required-indicator {
  display: inline-block;
  padding: 0rem 0.25rem;
  color: red;
  cursor: help;
}

.os-subform table td {
  padding: 0.5rem 0.75rem;
}

.os-subform .table-wrapper {
  width: 100%;
  overflow-y: auto;
  margin-bottom: 1.25rem;
}

.os-subform table .actioncol {
  /*min-width: 5rem;
  max-width: 5rem;*/
}

.os-subform table .actioncol :deep(button) {
  /*background: #fff;*/
  padding: 2px 6px;
}

.os-subform[disabled="true"] button {
  opacity: 0.65;
  pointer-events: none;
}
</style>
