
<template>
  <form novalidate>
    <table class="os-table">
      <thead>
        <tr>
          <th v-for="(field, fieldIdx) of fields" :key="fieldIdx" @click="sort(field)">
            <span v-if="field.displayLabel">{{field.displayLabel}}</span>
            <div v-else-if="field.icon" v-os-tooltip="field.tooltip"
              :class="{'align-icon': field.enableCopyFirstToAll && field.type == 'booleanCheckbox'}">
              <os-icon :name="field.icon" />
            </div>
            <span class="required-indicator" v-show="field.required" v-os-tooltip.bottom="field.requiredTooltip">
              <span>*</span>
            </span>
            <span v-if="field.tooltip && field.label">
              <os-icon name="question-circle" v-os-tooltip="field.tooltip"/>
            </span>
            <span v-show="ctx.sort.field == field.name">
              <span v-show="ctx.sort.direction == 'ASC'"> &uarr; </span>
              <span v-show="ctx.sort.direction == 'DESC'"> &darr; </span>
            </span>
            <span v-if="field.enableCopyFirstToAll && !field.options">
              <a v-if="field.type != 'booleanCheckbox'" @click="copyFirstToAll($event, field)">
                <span v-t="'common.copy_first_to_all'"> (Copy first to all) </span>
              </a>
              <div v-else>
                <os-boolean-checkbox v-model="ctx.selects[field.name]" style="margin-bottom: 0"
                  @change="copySelectToAll($event, field)" />
              </div>
            </span>
            <span class="more-options" v-if="field.options && field.options.length > 0">
              <os-menu :label="$t('common.buttons.more')" :options="field.options" />
            </span>
          </th>
          <th v-if="removeItems == true || copyItems == true">
            <span>&nbsp;</span>
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(itemModel, itemIdx) of itemModels" :key="itemIdx">
          <td v-for="(field, fieldIdx) of fields" :key="itemIdx + '_' + fieldIdx">
            <div :style="field.uiStyle">
              <component :ref="'osField-' + field.name" :is="field.component" v-bind="field"
                :md-type="true" v-model="itemModel[field.name]" v-os-tooltip.bottom="field.tooltip"
                :tab-order="'' + (tabDirection == 'column' ? (itemIdx + numRows * fieldIdx) : (itemIdx * numCols + fieldIdx))"
                :form="{...ctx.items[itemIdx], ...data, _formCache}"
                :context="{...ctx.items[itemIdx], ...data, _formCache}"
                @update:model-value="handleInput(itemIdx, field, itemModel, fieldIdx)">
              </component>
              <div v-if="v$.itemModels[itemIdx] && v$.itemModels[itemIdx][field.name] &&
                v$.itemModels[itemIdx][field.name].$error">
                <os-inline-message>{{errorMessages[itemIdx][field.name]}}</os-inline-message>
              </div>
            </div>
          </td>
          <td v-if="removeItems == true || copyItems == true">
            <os-button-group style="min-width: 5rem;">
              <os-button size="small" left-icon="copy" @click="copyItem(itemIdx)"
                v-os-tooltip.bottom="$t('common.buttons.copy')" v-if="copyItems == true" />

              <os-button size="small" left-icon="times" @click="removeItem(itemIdx)"
                v-os-tooltip.bottom="$t('workflows.buttons.remove')" v-if="removeItems == true" />
            </os-button-group>
          </td>
        </tr>
        <tr class="footer-row" v-if="$slots.footerRow">
          <slot name="footerRow" />
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

import { reactive, watchEffect } from 'vue';
import useVuelidate from '@vuelidate/core'

import alertSvc from '@/common/services/Alerts.js';
import exprUtil from '@/common/services/ExpressionUtil.js';
import fieldFactory from '@/common/services/FieldFactory.js';

export default {
  props: ['schema', 'data', 'items', 'tab-direction', 'removeItems', 'copyItems'],

  emits: ['input', 'form-validity', 'remove-item', 'copy-item'],

  setup(props) {
    const ctx = reactive({
      items: [],
      sort: { field: '', direction: '' },
      selects: { }
    });

    watchEffect(() => { ctx.items = props.items; });
    return {
      v$: useVuelidate(),
      ctx
    };
  },

  beforeCreate: function() {
    this._formCache = {};
  },

  created: function() {
    this.updateSelects();
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

        field.displayLabel = field.label;
        if (field.labelCode) {
          field.displayLabel = this.$t(field.labelCode);
        }

        const fv = field.validations;
        if (fv && fv.required) {
          field.required = true;
          field.requiredTooltip = fv.required.message || 'Mandatory field'
          if (fv.required.messageCode) {
            field.requiredTooltip = this.$t(fv.required.messageCode);
          }
        }

        field.uiStyle = field.uiStyle || {'min-width': '150px'};
        if (field.sortable) {
          field.uiStyle['cursor'] = 'pointer';
        }

        result.push(field);
      }

      return result;
    },

    itemModels: function() {
      let models = [];

      for (let item of this.ctx.items) {
        let model = {};
        for (let field of this.fields) {
          if (typeof field.value == 'function') {
            model[field.name] = field.value(item);
          } else {
            model[field.name] = exprUtil.getValue(item, field.name);
          }
        }

        model.$context = item;
        models.push(model);
      }

      return models;
    },

    numRows: function() {
      return this.itemModels.length;
    },

    numCols: function() {
      return this.fields.length;
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
              let message = field.validations[rule].message;
              if (field.validations[rule].messageCode) {
                message = this.$t(field.validations[rule].messageCode);
              }

              result[itemIdx][field.name] = message;
              break;
            }
          }
        }
      }

      return result;
    }
  },

  watch: {
    'items.length'() {
      this.updateSelects();
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
    handleInput: function(itemIdx, field, itemModel, fieldIdx) {
      const item  = this.ctx.items[itemIdx];
      const value = itemModel[field.name];

      exprUtil.setValue(item, field.name, value);
      this.$emit('input', {field, value, item, itemIdx, fieldIdx})

      if (this.v$.itemModels[itemIdx][field.name]) {
        this.v$.itemModels[itemIdx][field.name].$touch();
      }

      if (field.type == 'booleanCheckbox' && field.enableCopyFirstToAll) {
        if (value == true) {
          this.ctx.selects[field.name] = this.itemModels.every(item => item[field.name] == true);
        } else {
          this.ctx.selects[field.name] = false;
        }
      }

      this.$emit('form-validity', {invalid: this.v$.$invalid});
    },

    validate: function() {
      this.v$.$touch();

      let invalid = this.v$.$invalid;
      this.$emit('form-validity', {invalid: invalid});
      if (invalid) {
        alertSvc.error({code: 'common.form_validation_error'});
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
      this.$emit('remove-item', {item: this.ctx.items[idx], idx: idx});
    },

    copyItem: function(idx) {
      this.$emit('copy-item', {item: this.ctx.items[idx], idx: idx});
    },

    sort: function(field) {
      if (!field.sortable) {
        return;
      }

      const sort = this.ctx.sort;
      let factor = 1;
      if (sort.field == field.name) {
        if (sort.direction == 'ASC') {
          sort.direction = 'DESC';
          factor = -1;
        } else {
          sort.direction = 'ASC';
        }
      } else {
        sort.field = field.name;
        sort.direction = 'ASC';
      }

      let valueFn;
      if (typeof field.value == 'function') {
        valueFn = (item) => field.value(item);
      } else {
        valueFn = (item) => exprUtil.getValue(item, field.name);
      }

      this.ctx.items.sort(
        (i1, i2) => {
          const v1 = valueFn(i1);
          const v2 = valueFn(i2);
          return factor * (v1 < v2 ? -1 : (v1 > v2 ? 1 : 0));
        }
      );
    },

    copyFirstToAll: function(event, field) {
      event.stopPropagation();
      if (!this.itemModels || this.itemModels.length == 0) {
        return;
      }

      let value = null;
      if (typeof field.valueToCopy == 'function') {
        value = field.valueToCopy(this.ctx.items[0]);
      } else if (field.type == 'storage-position') {
        value = this.itemModels[0][field.name];
        if (value && value.name) {
          value = {name: value.name, mode: value.mode};
        } else {
          value = {};
        }
      } else {
        value = exprUtil.getValue(this.ctx.items[0], field.name);
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
    },

    copySelectToAll: function(value, field) {
      if (!this.itemModels || this.itemModels.length == 0) {
        return;
      }

      for (let idx = 0; idx < this.itemModels.length; ++idx) {
        this.itemModels[idx][field.name] = value;
        this.handleInput(idx, field, this.itemModels[idx]);
      }
    },

    updateSelects: function() {
      for (let field of this.fields) {
        if (field.type != 'booleanCheckbox' || !field.enableCopyFirstToAll) {
          continue;
        }

        this.ctx.selects[field.name] = this.itemModels.every(item => item[field.name] == true);
      }
    },

    getViewState() {
      let result = [];
      for (let field of this.fields) {
        const uiFields = this.$refs['osField-' + field.name];
        if (uiFields) {
          let label = field.displayLabel;
          if (!label && field.icon && field.tooltip) {
            label = field.tooltip;
          }

          result.push({label: label, values: uiFields.map(uiField => uiField.getDisplayValue())});
        }
      }

      return result;
    }
  }
}
</script>

<style scoped>
form {
  width: 100%;
  overflow-x: auto;
}

table th a {
  font-weight: normal;
}

table th .required-indicator {
  display: inline-block;
  padding: 0.25rem;
  color: red;
  cursor: help;
}

.buttons :deep(button) {
  margin-right: 0.5rem;
}

table th {
  padding-top: 20px!important;
}

table th .align-icon {
  margin-top: -20px;
  margin-left: 3px;
}

table th .more-options :deep(button) {
  padding: 2px 4px;
}
</style>
