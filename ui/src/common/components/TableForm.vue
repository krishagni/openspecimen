
<template>
  <form novalidate>
    <table class="os-table">
      <thead>
        <tr>
          <th v-for="(field, fieldIdx) of fields" :key="fieldIdx" :style="field.uiStyle || {'min-width': '150px'}">
            <span>{{field.label}}</span>
            <a v-if="field.enableCopyFirstToAll" @click="copyFirstToAll(field)">
              <span> (Copy first to all) </span>
            </a>
          </th>
          <th v-if="removeItems == true">
            <span>&nbsp;</span>
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(itemModel, itemIdx) of itemModels" :key="itemIdx">
          <td v-for="(field, fieldIdx) of fields" :key="itemIdx + '_' + fieldIdx">
            <component :is="field.component" v-bind="field" :md-type="true"
              v-model="itemModel[field.name]" v-os-tooltip.bottom="field.tooltip"
              :form="{...items[itemIdx], ...data, _formCache}" :context="{...items[itemIdx], ...data, _formCache}"
              @update:model-value="handleInput(itemIdx, field, itemModel)">
            </component>
            <div v-if="v$.itemModels[itemIdx] && v$.itemModels[itemIdx][field.name] &&
              v$.itemModels[itemIdx][field.name].$error">
              <os-inline-message>{{errorMessages[itemIdx][field.name]}}</os-inline-message>
            </div>
          </td>
          <td v-if="removeItems == true">
            <os-button size="small" left-icon="times" @click="removeItem(itemIdx)" />
          </td>
        </tr>
      </tbody>
    </table>

    <os-divider v-if="$slots.default && $slots.default().length > 0" />

    <div class="buttons">
      <slot></slot>
    </div>
  </form>
</template>

<script>

import useVuelidate from '@vuelidate/core'

import alertSvc from '@/common/services/Alerts.js';
import exprUtil from '@/common/services/ExpressionUtil.js';
import fieldFactory from '@/common/services/FieldFactory.js';

export default {
  props: ['schema', 'data', 'items', 'removeItems'],

  emits: ['input', 'form-validity'],

  setup() {
    return { v$: useVuelidate() };
  },

  beforeCreate: function() {
    this._formCache = {};
  },

  computed: {
    fields: function() {
      let result = [];

      for (let field of this.schema.columns) {
        if (field.showWhen) {
          if (typeof field.showWhen == 'function') {
            if (!field.showWhen(this.data)) {
              continue;
            }
          } else if (!exprUtil.eval(this.data, field.showWhen)) {
            continue;
          }
        }

        if (!field.component) {
          let component = fieldFactory.getComponent(field.type);
          if (component) {
            field = Object.assign({...field, component: component});
          }
        }

        result.push(field);
      }

      return result;
    },

    itemModels: function() {
      let models = [];
      for (let item of this.items) {
        let model = {};
        for (let field of this.fields) {
          if (typeof field.value == 'function') {
            model[field.name] = field.value(item);
          } else {
            model[field.name] = exprUtil.getValue(item, field.name);
          }
        }

        models.push(model);
      }

      return models;
    },

    errorMessages: function() {
      let result = [];
      if (!this.itemModels) {
        return result;
      }

      for (let itemIdx = 0; itemIdx < this.itemModels.length; ++itemIdx) {
        result.push({});

        for (let field of this.fields) {
          let validators = this.v$.itemModels &&
            this.v$.itemModels[itemIdx] &&
            this.v$.itemModels[itemIdx][field.name];

          if (!validators) {
            continue;
          }

          for (let rule in field.validations) {
            if (validators[rule] && validators[rule].$invalid) {
              result[itemIdx][field.name] = field.validations[rule].message;
              break;
            }
          }
        }
      }

      return result;
    }
  },

  validations() {
    if (!this.itemModels) {
      return {itemModels: []};
    }

    return {
      itemModels: this.itemModels.map(() => fieldFactory.getValidationRules(this.fields))
    }
  },

  methods: {
    handleInput: function(itemIdx, field, itemModel) {
      exprUtil.setValue(this.items[itemIdx], field.name, itemModel[field.name]);
      this.$emit('input', {field: field, value: itemModel[field.name], item: this.items[itemIdx], itemIdx: itemIdx})

      if (this.v$.itemModels[itemIdx][field.name]) {
        this.v$.itemModels[itemIdx][field.name].$touch();
      }

      this.$emit('form-validity', {invalid: this.v$.$invalid});
    },

    validate: function() {
      this.v$.$touch();

      let invalid = this.v$.$invalid;
      this.$emit('form-validity', {invalid: invalid});
      if (invalid) {
        alertSvc.error('There are validation errors as highlighted below. Please correct them.');
      }

      return !invalid;
    },

    fd: function(name) {
      let object = this.data;
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

    removeItem: function(idx) {
      this.$emit('removeItem', {item: this.items[idx], idx: idx});
    },

    copyFirstToAll: function(field) {
      if (!this.itemModels || this.itemModels.length == 0) {
        return;
      }

      let value = null;
      if (typeof field.valueToCopy == 'function') {
        value = field.valueToCopy(this.items[0]);
      } else if (field.type == 'storage-position') {
        value = this.itemModels[0][field.name];
        if (value && value.name) {
          value = {name: value.name, mode: value.mode};
        } else {
          value = {};
        }
      } else {
        value = exprUtil.getValue(this.items[0], field.name);
      }

      for (let idx = 1; idx < this.itemModels.length; ++idx) {
        let toCopy = value;

        if (value instanceof Date) {
          toCopy = new Date(value.getTime());
        } else if (typeof value == 'object' && value) {
          toCopy = JSON.parse(JSON.stringify(value));
        }

        this.itemModels[idx][field.name] = toCopy;
        this.handleInput(idx, field, this.itemModels[idx]);
      }
    }
  }
}
</script>

<style scoped>

table th a {
  font-weight: normal;
}

.buttons :deep(button) {
  margin-right: 0.5rem;
}

</style>
