<template>
  <div class="os-subform">
    <div class="table-wrapper" v-if="inputValue && inputValue.length > 0">
      <table>
        <thead>
          <tr>
            <th v-for="(field, idx) of sfFields" :key="idx">
              <span v-html="field.label"></span>
              <span class="required-indicator" v-show="field.required" v-os-tooltip.bottom="field.requiredTooltip">
                <span>*</span>
              </span>
            </th>
            <th class="actioncol">&nbsp;</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(sfRowData, sfRdIdx) of inputValue" :key="sfRdIdx">
            <td v-for="(field, cidx) of sfFields" :key="cidx">
              <component :is="field.component" v-bind="field" v-model="sfRowData[field.name]"
                :context="{...sfRowData}" @update:model-value="handleInput(sfRowData, sfRdIdx, field)">
              </component>
              <div v-if="v$.inputValue[sfRdIdx][field.name] && v$.inputValue[sfRdIdx][field.name].$error">
                <os-inline-message>{{errorMessages[sfRdIdx][field.name]}}</os-inline-message>
              </div>
            </td>
            <td class="actioncol">
              <os-button class="inline-button" left-icon="times" @click="removeSfRow(sfRdIdx)" />
            </td>
          </tr>
        </tbody>
      </table> 
    </div>

    <os-button class="inline-button" left-icon="plus"
      :label="inputValue && inputValue.length > 0 ? 'Add Another' : 'Add'"
      @click="addSfRow"
    />
  </div>
</template>

<script>

import useVuelidate from '@vuelidate/core'
import fieldFactory from '@/common/services/FieldFactory.js';

export default {
  props: ['modelValue', 'fields'],

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
        if (fv && fv.required) {
          field.required = true;
          field.requiredTooltip = (fv.required && fv.required.message) || 'Mandatory field';
        }

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
        result.push({});

        for (let field of this.fields) {
          let validators = this.v$.inputValue &&
            this.v$.inputValue[rowIdx] &&
            this.v$.inputValue[rowIdx][field.name];

          if (!validators) {
            continue;
          }

          for (let rule in field.validations) {
            if (validators[rule] && validators[rule].$invalid) {
              result[rowIdx][field.name] = field.validations[rule].message;
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
  font-weight: normal;
  text-align: left;
}

.os-subform .required-indicator {
  display: inline-block;
  padding: 0.25rem;
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
  min-width: 5rem;
  max-width: 5rem;
}

.os-subform table .actioncol :deep(button) {
  background: #fff;
}

</style>
