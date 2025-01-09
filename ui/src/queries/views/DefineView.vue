<template>
  <os-dialog ref="columnsDialog" size="lg" :closable="false">
    <template #header>
      <span v-t="'queries.columns'">Columns</span>
    </template>

    <template #content>
      <os-steps ref="columnsWizard">
        <os-step :title="$t('queries.select_fields')">
          <div class="fields-selection">
            <os-tree-select-panel :items="ctx.fieldsTree" :selected="ctx.selectedFields"
              :expand-selected="false" @selected-items="onFieldsSelection" />

            <div class="settings">
              <os-boolean-checkbox v-model="ctx.query.wideRowEnabled" :inlineLabel="$t('queries.enable_wide_rows')" />
              <os-boolean-checkbox v-model="ctx.query.outputColumnExprs" :inlineLabel="$t('queries.display_field_names')" />
              <os-boolean-checkbox v-model="ctx.query.caseSensitive" :inlineLabel="$t('queries.case_sensitive_search')" />
            </div>
          </div>
        </os-step>
        <os-step :title="$t('queries.field_labels')">
          <table class="os-table">
            <thead>
              <tr>
                <th v-t="'queries.field'">Field</th>
                <th v-t="'queries.label'">Label</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="selectedField of ctx.selectedFields" :key="selectedField.id">
                <td v-if="selectedField.field">
                  <span v-show="!!selectedField.field.formCaption">{{selectedField.field.formCaption}}:&nbsp;</span>
                  <span>{{selectedField.field.label}}</span>
                </td>
                <td>
                  <os-input-text v-model="selectedField.displayLabel" :md-type="true" />
                </td>
              </tr>
            </tbody>
          </table>
        </os-step>
        <os-step :title="$t('queries.aggregates')">
          <div style="display: flex; flex-direction: row;">
            <os-list-group :list="ctx.selectedFields" :selected="ctx.aggregateField"
              @on-item-select="onAggFieldSelection($event)" style="flex: 1; margin: 0.5rem;">
              <template #header>
                <span v-t="'queries.selected_fields'">Selected Fields</span>
              </template>
              <template #default="{item}">
                <div style="display: flex;">
                  <div style="flex: 1;">
                    <span v-if="item.displayLabel">
                      {{item.displayLabel}}
                    </span>
                    <span v-else-if="item.field">
                      <span v-show="!!item.field.formCaption">{{item.field.formCaption}}:&nbsp;</span>
                      <span>{{item.field.label}}</span>
                    </span>
                  </div>
                  <div>
                    <os-badge>{{(item.aggFns || []).length}}</os-badge>
                  </div>
                </div>
              </template>
            </os-list-group>
            <os-panel style="flex: 1; margin: 0.5rem;">
              <template #header>
                <span v-t="'queries.aggregate_functions'"></span>
              </template>
              <div>
                <AggregateFunctionSelector v-model="ctx.aggregateField.aggFns" :field="ctx.aggregateField.field"
                  :label="ctx.aggregateField.displayLabel || ctx.aggregateField.field.label"
                  v-if="ctx.aggregateField && ctx.aggregateField.id" />
                <span v-else>Select field on the left side panel</span>
              </div>
            </os-panel>
          </div>
        </os-step>
        <os-step :title="$t('queries.reporting_options')">
          <os-form :schema="reportingSchema.layout" :data="ctx.query" v-if="ctx.query" />
        </os-step>
      </os-steps>
    </template>

    <template #footer>
      <os-button text :label="$t('common.buttons.cancel')" @click="closeColumnsDialog"
        :disabled="!ctx.selectedFields || ctx.selectedFields.length == 0" />
      <os-button secondary :label="$t('common.buttons.previous')" @click="previous" v-if="!isFirstStep()" />
      <os-button primary :label="$t('common.buttons.next')" @click="next" v-if="!isLastStep()"
        :disabled="!ctx.selectedFields || ctx.selectedFields.length == 0" />
      <os-button primary :label="$t('common.buttons.done')" @click="done"
        :disabled="!ctx.selectedFields || ctx.selectedFields.length == 0" />
    </template>
  </os-dialog> 
</template>

<script>
import reportingSchema from '@/queries/schemas/reporting.js';

import util from '@/common/services/Util.js';
import formsCache from '@/queries/services/FormsCache.js';

import AggregateFunctionSelector from '@/queries/views/AggregateFunctionSelector.vue';

export default {
  components: {
    AggregateFunctionSelector
  },

  data() {
    return {
      ctx: {
        query: {
          reporting: {type: 'none', params: {}},

          getPivotTableFields: (type) => this._getPivotTableFields(type),

          getValueFields: () => this._getValueFields(),

          getColumnSummaryTableFields: (type) => this._getColumnSummaryTableFields(type)
        },

        fieldsTree: []
      },

      reportingSchema
    }
  },

  methods: {
    open: async function(query) {
      const self = this;
      query = this.ctx.query = util.clone(query);
      query.wideRowEnabled = query.wideRowMode === 'DEEP';

      const reporting = query.reporting = query.reporting || {type: 'none'};
      const params = reporting.params = reporting.params || {};

      query.getPivotTableFields = (type) => this._getPivotTableFields(type);
      query.getValueFields    = () => this._getValueFields();
      query.getColumnSummaryTableFields = (type) => this._getColumnSummaryTableFields(type);

      this.ctx.fieldsTree     = await self._getFieldsTree(query);
      this.ctx.fieldsTreeMap  = this._getFieldsTreeMap({}, this.ctx.fieldsTree);
      this.ctx.selectedFields = (query.selectList || []).map(
        selectedField => {
          let result = {};
          if (typeof selectedField == 'string') {
            result = {id: selectedField, name: selectedField};
          } else {
            result = {...selectedField, id: selectedField.name};
          }

          const field = this.ctx.fieldsTreeMap[result.id];
          result.field = field;
          result.aggFns = result.aggFns || [];
          return result;
        }
      );
      this.ctx.aggregateField = {};

      if (params && params.groupRowsBy) {
        params.groupRowsBy = params.groupRowsBy.map(
          field => {
            let result = {...field};
            result.field = this.ctx.fieldsTreeMap[result.name || result.id];
            return result;
          }
        );
      }

      if (params && params.groupColBy) {
        params.groupColBy.field = this.ctx.fieldsTreeMap[params.groupColBy.name || params.groupColBy.id];
      }

      if (params && params.summaryFields) {
        params.summaryFields = params.summaryFields.map(
          field => {
            let result = {...field};
            result.field = this.ctx.fieldsTreeMap[result.name || result.id];
            return result;
          }
        );
      }

      return new Promise((resolve) => {
        self.resolve = resolve;
        self.$refs.columnsDialog.open();
      });
    },

    isFirstStep: function() {
      if (!this.$refs.columnsWizard) {
        return true;
      }

      return this.$refs.columnsWizard.isFirstStep();
    },

    isLastStep: function() {
      if (!this.$refs.columnsWizard) {
        return false;
      }

      return this.$refs.columnsWizard.isLastStep();
    },

    next: function() {
      this.$refs.columnsWizard.next();
    },

    previous: function() {
      this.$refs.columnsWizard.previous();
    },

    closeColumnsDialog: function() {
      this.$refs.columnsDialog.close();
      this.resolve('cancel');
    },

    done: function() {
      const {selectedFields:selectList, query} = this.ctx;
      const {havingClause, reporting, wideRowEnabled, outputColumnExprs, caseSensitive} = query;
      const wideRowMode = wideRowEnabled ? 'DEEP' : 'SHALLOW';
      this.resolve({selectList, havingClause, reporting, wideRowMode, outputColumnExprs, caseSensitive});
      this.$refs.columnsDialog.close();
    },

    onFieldsSelection: function(selectedFields) {
      const fieldsMap = this.ctx.selectedFields.reduce(
        (map, field) => {
          map[field.id] = field;
          return map;
        },
        {}
      );

      const result = [];
      for (let selectedField of selectedFields) {
        if (fieldsMap[selectedField.id]) {
          result.push(fieldsMap[selectedField.id]);
        } else {
          let field = this.ctx.fieldsTreeMap[selectedField.id];
          result.push({name: selectedField.id, displayLabel: null, aggFns: [], id: selectedField.id, field});
        }
      }

      this.ctx.selectedFields = result;
    },

    onAggFieldSelection: function({item}) {
      this.ctx.aggregateField = item;
    },

    _getFieldsTree: async function(query) {
      const forms = await formsCache.getForms();
      const tree = [];
      for (const form of forms) {
        const fields = await formsCache.getFields(form, query.cpId, query.cpGroupId);

        const item = {id: form.name, label: form.caption, children: []};
        item.children = this._getFieldsTree0(form, null, form.name, fields);
        tree.push(item);
      }

      for (const filter of query.filters) {
        if (filter.expr) {
          tree.push({id: '$temporal.' + filter.id, label: filter.desc});
        }
      }

      return tree;
    },

    _getFieldsTree0: function(rootForm, form, prefix, fields) {
      const subTree = [];
      for (let field of fields) {
        if (field.type == 'SUBFORM') {
          if (field.name == 'extensions' || field.name == 'customFields') {
            for (let extnForm of field.subFields) {
              const extnFormPrefix = prefix + '.' + field.name + '.' + extnForm.name;

              const children     = this._getFieldsTree0(rootForm, extnForm, extnFormPrefix, extnForm.subFields || []);
              if (children.length > 1) {
                const extnFormTree = {id: extnFormPrefix, label: extnForm.caption, children: children};
                subTree.push(extnFormTree);
              } else if (children.length == 1) {
                subTree.push(children[0]);
              }
            }
          } else {
            const children = this._getFieldsTree0(rootForm, form, prefix + '.' + field.name, field.subFields);
            if (children.length > 1) {
              subTree.push({id: prefix + '.' + field.name, label: field.caption, children});
            } else if (children.length == 1) {
              subTree.push(children[0]);
            }
          }
        } else {
          const formCaption = rootForm.caption + (form ? ': ' + form.caption : '');
          subTree.push({id: prefix + '.' + field.name, label: field.caption, formCaption, type: field.type});
        }
      }

      subTree.sort((n1, n2) => n1.label < n2.label ? -1 : (n1.label > n2.label ? 1 : 0));
      return subTree;
    },

    _getFieldsTreeMap: function(map, tree) {
      for (let node of tree) {
        map[node.id] = node;
        if (node.children && node.children.length > 0) {
          this._getFieldsTreeMap(map, node.children);
        }
      }

      return map;
    },

    _getPivotTableFields: async function(type) {
      const {reporting: {params: {groupRowsBy, groupColBy, summaryFields}}} = this.ctx.query;

      let result = [];
      for (let field of this.ctx.selectedFields) {
        if (field.aggFns && field.aggFns.length > 0) {
          for (let aggFn of field.aggFns) {
            result.push({id: field.id + '$' + aggFn.name, displayLabel: aggFn.desc});
          }
        } else {
          result.push(field);
        }
      }

      if (type == 'column_fields' || type == 'value_fields') {
        if (groupRowsBy && groupRowsBy.length > 0) {
          result = result.filter(field => groupRowsBy.every(c => c.id != field.id));
        }
      }

      if (type == 'row_fields' || type == 'value_fields') {
        if (groupColBy) {
          result = result.filter(field => groupColBy.id != field.id);
        }
      }

      if (type == 'row_fields' || type == 'column_fields') {
        if (summaryFields && summaryFields.length > 0) {
          result = result.filter(field => summaryFields.every(c => c.id != field.id));
        }
      }

      return result;
    },

    _getValueFields: async function() {
      const {reporting: {params: {summaryFields}}} = this.ctx.query;
      return summaryFields || [];
    },

    _getColumnSummaryTableFields: async function(type) {
      const {reporting: {params: {sum, avg}}} = this.ctx.query;

      let result = [];
      for (let field of this.ctx.selectedFields) {
        if (field.aggFns && field.aggFns.length > 0) {
          for (let aggFn of field.aggFns) {
            result.push({id: field.id + '$' + aggFn.name, displayLabel: aggFn.desc});
          }
        } else if (field.field && (field.field.type == 'INTEGER' || field.field.type == 'FLOAT')) {
          result.push(field);
        }
      }

      if (type == 'sum') {
        if (avg && avg.length > 0) {
          result = result.filter(field => avg.every(c => c.id != field.id));
        }
      }

      if (type == 'avg') {
        if (sum && sum.length > 0) {
          result = result.filter(field => sum.every(c => c.id != field.id));
        }
      }

      return result;
    }
  }
}
</script>

<style scoped>
.fields-selection .settings {
  margin-top: 1.25rem;
  padding-left: 1rem;
}
</style>
