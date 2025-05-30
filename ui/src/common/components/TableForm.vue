
<template>
  <form novalidate class="os-table-form">
    <table class="os-table">
      <thead>
        <tr>
          <th class="selection" v-if="readOnly && (selectionMode == 'radio' || selectionMode == 'checkbox')">
            <span v-if="selectionMode == 'radio'">&nbsp;</span>
            <span v-if="selectionMode == 'checkbox'">
              <os-boolean-checkbox v-model="ctx.allSelected" @change="toggleAllRowsSelection" />
            </span>
          </th>

          <th class="selection" v-else-if="removeItems == true">
            <span>&nbsp;</span>
          </th>

          <th v-if="firstColumn" :class="stickyColumn" :style="firstColumnHeaderStyle">
            <div v-show="treeLayout && !showFlat" style="width: 1.5rem; display: inline-block;"> </div>
            <slot ref="firstColumnHeader" name="first-column-header"> </slot>
          </th>

          <th v-for="(field, fieldIdx) of fields" :key="fieldIdx" @click="sort(field)"
            :class="!firstColumn && fieldIdx == 0 ? stickyColumn : []">
            <span v-if="field.displayLabel">{{field.displayLabel}}</span>
            <div v-else-if="field.icon" v-os-tooltip.bottom="field.tooltip"
              :class="{'align-icon': field.enableCopyFirstToAll && field.type == 'booleanCheckbox'}">
              <os-icon :name="field.icon" />
            </div>
            <span class="required-indicator" v-show="!readOnly && (field.required || field.requiredIf)"
              v-os-tooltip.bottom="field.requiredTooltip">
              <span>*</span>
            </span>
            <span v-if="field.tooltip && field.label">
              <os-icon name="question-circle" v-os-tooltip="field.tooltip"/>
            </span>
            <span v-show="ctx.sort.field == field.name">
              <span v-show="ctx.sort.direction == 'ASC'"> &uarr; </span>
              <span v-show="ctx.sort.direction == 'DESC'"> &darr; </span>
            </span>
            <span v-if="field.enableCopyFirstToAll && !field.menuOptions">
              <os-button class="copy-first-to-all icon-btn" size="small" left-icon="copy"
                @click="copyFirstToAll($event, field)" :hint="$t('common.copy_first_to_all')"
                v-if="field.type != 'booleanCheckbox'" />
              <div v-else>
                <os-boolean-checkbox v-model="ctx.selects[field.name]" style="margin-bottom: 0"
                  @change="copySelectToAll($event, field)" />
              </div>
            </span>
            <span class="more-options" v-if="field.menuOptions && field.menuOptions.length > 0">
              <os-menu icon="ellipsis-h" :label1="$t('common.buttons.more')" :options="field.menuOptions" />
            </span>
          </th>
          <th v-if="removeItems == true || copyItems == true || showRowActions ==  true">
            <span>&nbsp;</span>
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="!itemModels || itemModels.length == 0">
          <td :colspan="fields.length">
            <os-message type="info">
              <span>No items to display</span>
            </os-message>
          </td>
        </tr>

        <template v-for="(itemModel, itemIdx) of itemModels" :key="itemModel.key">
          <tr v-if="itemModel.show" :class="expanded && expanded.indexOf(itemModel.$context) >= 0 && 'expanded-row'">
            <td class="selection" v-if="readOnly && (selectionMode == 'radio' || selectionMode == 'checkbox')">
              <RadioButton name="rowSelection" :value="itemIdx" v-model="ctx.selectedIdx"
                @click="toggleSelection(itemIdx)" v-if="selectionMode == 'radio'" />

              <os-boolean-checkbox v-model="ctx.selectedRows[itemIdx]"
                @change="toggleSelection(itemIdx)" v-else />
            </td>

            <td class="selection" v-else-if="removeItems == true || copyItems == true">
              <os-button-group style="min-width: 5rem;">
                <os-button size="small" left-icon="copy" @click="copyItem(itemIdx)"
                  v-os-tooltip.bottom="$t('common.buttons.copy')" v-if="copyItems == true" />

                <os-button size="small" left-icon="times" @click="removeItem(itemIdx)"
                  v-os-tooltip.bottom="$t('workflows.buttons.remove')" v-if="removeItems == true"/>
              </os-button-group>
            </td>

            <td v-if="firstColumn" :class="stickyColumn" @click="rowClicked(itemModel.$context)">
              <div :style="{display: treeLayout ? 'flex' : ''}">
                <div v-show="treeLayout && !showFlat" class="node-expander"
                  :style="{'padding-left':  itemModel.depth + 'rem'}">
                  <a @click="toggleNode($event, itemModel, itemIdx)">
                    <os-icon v-show="itemModel.hasChildren && itemModel.expanded" name="chevron-down" />
                    <os-icon v-show="itemModel.hasChildren && !itemModel.expanded" name="chevron-right" />
                  </a>
                </div>
                <slot ref="firstColumn" name="first-column" v-bind:item="itemModel.$context"
                  v-bind:model="itemModel"> </slot>
              </div>
            </td>

            <td v-for="(field, fieldIdx) of fields" :key="itemIdx + '_' + fieldIdx"
              :class="!firstColumn && fieldIdx == 0 ? stickyColumn : []" @click="rowClicked(itemModel.$context)">
              <div :style="field.uiStyle">
                <div class="field-container">
                  <div v-show="treeLayout && fieldIdx == 0 && !firstColumn" class="node-expander"
                    :style="{'padding-left':  itemModel.depth + 'rem'}">
                    <a @click="toggleNode($event, itemModel, itemIdx)">
                      <os-icon v-show="itemModel.hasChildren && itemModel.expanded" name="chevron-down" />
                      <os-icon v-show="itemModel.hasChildren && !itemModel.expanded" name="chevron-right" />
                    </a>
                  </div>
                  <component class="field" :ref="'osField-' + field.name" :is="field.component" v-bind="field"
                    :md-type="true" v-model="itemModel[field.name]" v-os-tooltip.bottom="field.tooltip"
                    :tab-order="'' + (tabDirection == 'column' ? (itemIdx + numRows * fieldIdx) : (itemIdx * numCols + fieldIdx))"
                    :form="{...ctx.items[itemIdx], ...data, _formCache}"
                    :context="{...ctx.items[itemIdx], ...data, _formCache}"
                    @update:model-value="handleInput(itemIdx, field, itemModel, fieldIdx)"
                    v-if="!itemModel.hideFields[field.name]">
                  </component>
                  <os-span v-model="itemModel[field.name]" :display-type="field.displayType || field.type"
                    :ref="'osField-' + field.name" :form="itemModel.$context" v-bind="field" v-else />
                </div>
                <div v-if="!itemModel.hideFields[field.name] &&
                  v$.itemModels[itemIdx] && v$.itemModels[itemIdx][field.name] &&
                  v$.itemModels[itemIdx][field.name].$error">
                  <os-inline-message>{{errorMessages[itemIdx][field.name]}}</os-inline-message>
                </div>
              </div>
            </td>
            <td v-if="removeItems == true || copyItems == true || showRowActions == true">
              <os-button-group style="min-width: 5rem;">
                <os-button size="small" left-icon="copy" @click="copyItem(itemIdx)"
                  v-os-tooltip.bottom="$t('common.buttons.copy')" v-if="copyItems == true" />

                <os-button size="small" left-icon="times" @click="removeItem(itemIdx)"
                  v-os-tooltip.bottom="$t('workflows.buttons.remove')" v-if="removeItems == true" />

                <slot name="row-actions" :rowItem="{item: itemModel.$context}" />
              </os-button-group>
            </td>
          </tr>
          <tr class="expanded-row-child" v-if="itemModel.show && expanded && expanded.indexOf(itemModel.$context) >= 0">
            <td :colspan="columnsCount">
              <slot name="expanded-row" :rowItem="{item: itemModel.$context}" />
            </td>
          </tr>
        </template>
        <tr class="footer-row" v-if="$slots.footerRow">
          <slot name="footerRow" />
        </tr>
      </tbody>
    </table>

    <div v-if="requireSelection && ctx.showSelectRowError && !(ctx.selectedIdx >= 0)">
      <os-message type="error">
        <slot name="selectRowErrorMessage" />
      </os-message>
    </div>

    <os-divider v-if="$slots.default && $slots.default().length > 0" />

    <div class="buttons">
      <slot></slot>
    </div>
  </form>
</template>

<script>

import { reactive, watchEffect } from 'vue';
import useVuelidate from '@vuelidate/core'

import RadioButton from 'primevue/radiobutton';

import alertSvc from '@/common/services/Alerts.js';
import exprUtil from '@/common/services/ExpressionUtil.js';
import fieldFactory from '@/common/services/FieldFactory.js';
import util from '@/common/services/Util.js';

export default {
  props: [
    'schema',
    'data',
    'items',
    'tab-direction',
    'removeItems',
    'copyItems',
    'showRowActions',
    'readOnly',
    'selectionMode',
    'requireSelection',
    'treeLayout',
    'expanded'
  ],

  emits: ['input', 'form-validity', 'remove-item', 'copy-item', 'item-selected', 'selected-items', 'row-clicked'],

  components: {
    RadioButton
  },

  setup(props) {
    const ctx = reactive({
      items: [],
      sort: { field: '', direction: '' },
      selects: { },
      selectedRows: [],
      allSelected: false,
      emptyString: '-'
    });

    watchEffect(() => { ctx.items = props.items; ctx.selectedRows = new Array(props.items.length); });
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
    firstColumn: function() {
      if (this.$slots['first-column']) {
        const result = this.$slots['first-column']();
        return result && result.length > 0 && result[0];
      }

      return null;
    },

    firstColumnHeaderStyle: function() {
      if (this.$slots['first-column-header']) {
        const result = this.$slots['first-column-header']();
        return result && result.length > 0 && result[0].props && result[0].props.style;
      }

      return null;
    },

    stickyColumn: function() {
      if (this.readOnly && (this.selectionMode == 'radio' || this.selectionMode == 'checkbox')) {
        return ['sticky', 'left-offset'];
      } else if (this.removeItems == true || this.copyItems == true) {
        const result = ['sticky'];
        if (this.removeItems == true && this.copyItems == true) {
          result.push('two-btn-left-offset');
        } else {
          result.push('one-btn-left-offset');
        }

        return result;
      } else {
        return ['sticky']
      }
    },

    fields: function() {
      console.log("Compute the fields to display in the table form...");

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
          let component = fieldFactory.getComponent(this.readOnly ? 'span' : field.type);
          if (component) {
            field = Object.assign({...field, component: component});
          }

          if (this.readOnly) {
            field.displayType = field.displayType || field.type
          }
        }

        field.displayLabel = field.label;
        if (field.labelCode) {
          field.displayLabel = this.$t(field.labelCode);
        }

        const fv = field.validations;
        if (fv && (fv.required || fv.requiredIf)) {
          field.required = true;

          const req = fv.required || fv.requiredIf;
          field.requiredTooltip = req.message || 'Mandatory field'
          if (req.messageCode) {
            field.requiredTooltip = this.$t(req.messageCode);
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

    columnsCount: function() {
      let count = 0;
      if (this.readOnly && (this.selectionMode == 'radio' || this.selectionMode == 'checkbox')) {
        ++count;
      } else if (this.removeItems) {
        ++count;
      }

      if (this.firstColumn) {
        ++count;
      }

      count += this.fields.length;

      if (this.removeItems == true || this.copyItems == true || this.showRowActions == true) {
        ++count;
      }

      return count;
    },

    itemModels: function() {
      let models = [];

      const numItems = this.ctx.items.length;
      let itemIdx = -1;
      for (let item of this.ctx.items) {
        ++itemIdx;

        const key = 'items_' + numItems + '_' + itemIdx;
        let model = {key, show: true, hideFields: {}};
        if (this.treeLayout) {
          model = {
            key,
            expanded: item.expanded,
            show: item.show,
            depth: item.depth,
            hasChildren: item.hasChildren,
            hideFields: {}
          };
        }

        for (let field of this.fields) {
          if (field.showCellWhen) {
            let show = true;
            if (typeof field.showCellWhen == 'function') {
              show = field.showCellWhen(item);
            } else {
              show = exprUtil.eval(item, field.showCellWhen);
            }

            if (!show) {
              model.hideFields[field.name] = true;
              if (!field.showValue) {
                continue;
              }
            }
          }

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

    showFlat: function() {
      return this.itemModels.every(model => !model.depth);
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

          if (!validators || this.itemModels[itemIdx].hideFields[field.name]) {
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
    if (!this.itemModels || this.readOnly) {
      return {itemModels: []};
    }

    return {
      itemModels: this.itemModels.map(
        (item) => {
          const rowFields = this.fields.filter(field => !item.hideFields[field.name]);
          return fieldFactory.getValidationRules(rowFields);
        }
      )
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
      if (this.readOnly && this.selectionMode == 'radio' && this.requireSelection) {
        this.ctx.showSelectRowError = true;
        if (this.ctx.selectedIdx >= 0) {
          return true;
        }

        alertSvc.error({code: 'common.form_validation_error'});
        return false;
      }

      this.v$.$touch();

      let invalid = this.v$.$invalid;
      this.$emit('form-validity', {invalid: invalid});
      if (invalid) {
        alertSvc.error({code: 'common.form_validation_error'});
      }

      return !invalid;
    },

    fd: function(name) {
      return util.fd(this.data, name);
    },

    removeItem: function(idx) {
      this.$emit('remove-item', {item: this.ctx.items[idx], idx: idx});
    },

    copyItem: function(idx) {
      this.$emit('copy-item', {item: this.ctx.items[idx], idx: idx});
    },

    rowClicked: function(item) {
      this.$emit('row-clicked', {item});
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

      //
      // find the first row where the field is displayed or not hidden
      // the value of such field will be used to copy to the rest of the rows
      //
      const firstRow = this.itemModels.findIndex(model => !model.hideFields[field.name]);
      if (firstRow < 0) {
        return;
      }

      let value = null;
      if (typeof field.valueToCopy == 'function') {
        value = field.valueToCopy(this.ctx.items[firstRow]);
      } else if (field.type == 'storage-position') {
        value = this.itemModels[firstRow][field.name];
        if (value && value.name) {
          value = {name: value.name, mode: value.mode};
        } else {
          value = {};
        }
      } else {
        value = exprUtil.getValue(this.ctx.items[firstRow], field.name);
      }

      for (let row = 0; row < this.itemModels.length; ++row) {
        if (row == firstRow || this.itemModels[row].hideFields[field.name]) {
          continue;
        }

        let toCopy = value;
        if (value instanceof Date) {
          toCopy = new Date(value.getTime());
        } else if (typeof value == 'object' && value) {
          toCopy = JSON.parse(JSON.stringify(value));
        }

        this.itemModels[row][field.name] = toCopy;
        this.handleInput(row, field, this.itemModels[row]);
      }
    },

    copySelectToAll: function(value, field) {
      if (!this.itemModels || this.itemModels.length == 0) {
        return;
      }

      for (let idx = 0; idx < this.itemModels.length; ++idx) {
        if (this.itemModels[idx].hideFields[field.name]) {
          continue;
        }

        this.itemModels[idx][field.name] = value;
        this.handleInput(idx, field, this.itemModels[idx]);
      }

      this.ctx.selects[field.name] = value;
    },

    updateSelects: function() {
      for (let field of this.fields) {
        if (field.type != 'booleanCheckbox' || !field.enableCopyFirstToAll) {
          continue;
        }

        this.ctx.selects[field.name] = this.itemModels.every(
          item => item.hideFields[field.name] || item[field.name] == true
        );
      }
    },

    toggleSelection: function(itemIdx) {
      if (this.selectionMode == 'checkbox') {
        const {selectedRows} = this.ctx
        const selectedItems = [];

        let index = 0;
        let allSelected = true;
        for (let row of selectedRows) {
          if (row) {
            selectedItems.push(this.items[index]);
          } else {
            allSelected = false;
          }

          ++index;
        }

        this.ctx.allSelected = allSelected;
        this.$emit('selected-items', selectedItems);
        return;
      }

      this.$emit('item-selected', {index: itemIdx});
    },

    toggleAllRowsSelection: function() {
      for (let index = 0; index < this.items.length; ++index) {
        this.ctx.selectedRows[index] = this.ctx.allSelected;
      }

      this.toggleSelection(-1);
    },

    clearSelection: function() {
      this.ctx.selectedIdx = -1;
      this.ctx.selectedRows = [];
    },

    getSelection: function() {
      if (this.ctx.selectedIdx >= 0) {
        return this.items[this.ctx.selectedIdx];
      } else if (this.ctx.selectedRows && this.ctx.selectedRows.length > 0) {
        const selected = [];
        for (let idx = 0; idx < this.ctx.selectedRows.length; ++idx) {
          if (this.ctx.selectedRows[idx]) {
            selected.push(this.items[idx]);
          }
        }

        return selected;
      }

      return null;
    },

    setSelection: function(selected) {
      if (this.selectionMode == 'radio') {
        this.ctx.selectedIdx = this.items.indexOf(selected);
      } else if (this.selectionMode == 'checkbox') {
        const selectedRows = this.ctx.selectedRows = [];
        for (let idx = 0; idx < this.items.length; ++idx) {
          selectedRows[idx] = selected.indexOf(this.items[idx]) > -1;
        }

        let allSelected = true;
        for (let row of selectedRows) {
          if (!row) {
            allSelected = false;
            break;
          }
        }

        this.ctx.allSelected = allSelected;
      }
    },

    toggleNode: function(event, itemModel, start) {
      if (event) {
        event.stopPropagation();
      }

      let item = itemModel.$context;
      let show = item.expanded = !item.expanded;
      let ancestors = [item.uid];
      for (let idx = start + 1; idx < this.itemModels.length; idx++) {
        const descendant = this.itemModels[idx].$context;
        if (ancestors.indexOf(descendant.parentUid) >= 0) {
          descendant.show = show;
          if (descendant.expanded) {
            ancestors.push(descendant.uid);
          }
        }
      }

      this.$emit('toggle-node', {item: itemModel.$context});
    },

    scrollExpandedIntoView: function() {
      setTimeout(
        () => {
          this.$el && this.$el.querySelector('.expanded-row')
            .scrollIntoView({ behavior: 'smooth', block: 'center'});
          this.$el && this.$el.querySelector('.expanded-row-child')
            .scrollIntoView({ behavior: 'smooth', block: 'center'});
        },
        500
      );
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

          result.push({
            label: label,
            values: uiFields.map(uiField => uiField.getDisplayValue())
          });
        }
      }

      return result;
    }
  }
}
</script>

<style scoped>
form {
  height: 100%;
  width: 100%;
  overflow-x: auto;
  overflow-y: auto;
  position: relative;
}

table thead {
  position: sticky;
  background-color: #fff;
  top: 0;
  z-index: 2;
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

table th,
table :deep(th) {
  padding-top: 20px!important;
}

table th .align-icon {
  margin-top: -20px;
  margin-left: 3px;
}

table th .more-options :deep(button) {
  margin-left: 0.5rem;
}

table th.sticky,
table :deep(th.sticky),
table td.sticky,
table :deep(td.sticky) {
  position: sticky;
  background-color: #fff;
  left: 0px;
  z-index: 1;
}

table th.sticky.left-offset,
table :deep(th.sticky.left-offset),
table td.sticky.left-offset,
table :deep(td.sticky.left-offset) {
  left: 35px;
}

table th.sticky.one-btn-left-offset,
table :deep(th.sticky.one-btn-left-offset),
table td.sticky.one-btn-left-offset,
table :deep(td.sticky.one-btn-left-offset) {
  left: 43px;
}

table th.sticky.two-btn-left-offset,
table :deep(th.sticky.two-btn-left-offset),
table td.sticky.two-btn-left-offset,
table :deep(td.sticky.two-btn-left-offset) {
  left: 86px;
}

table td .field-container {
  display: flex;
}

table td .field-container .field {
  flex: 1;
}

table td .node-expander {
  color: #6c757d;
  padding-right: 0.5rem;
  margin-right: 0.25rem;
  padding-top: 0.125rem;
  padding-bottom: 0.125rem;
  display: inline-block
}

table td .node-expander a {
  color: #6c757d;
  display: inline-block;
  width: 0.80rem;
}

table th.selection,
table td.selection {
  padding: 10px 0px 10px 15px;
  width: 1.25rem;
  position: sticky;
  background-color: #fff;
  left: 0;
  z-index: 1;
}

table th.selection :deep(.p-field-checkbox),
table td.selection :deep(.p-field-checkbox) {
  margin-bottom: 0rem;
}

table th .copy-first-to-all {
  margin-left: 0.25rem;
}

tr.expanded-row td {
  border-top: 0px;
}

tr.expanded-row:hover, tr.expanded-row td {
  background: #edeff0;
}

tr.expanded-row, tr.expanded-row-child {
  border: 1px solid #ddd;
}

tr.expanded-row-child:hover {
  background: #fff!important;
}

</style>
