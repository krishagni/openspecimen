<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span class="os-title">
        <h3>
          <span v-if="query.title">{{query.title}}</span>
          <span v-else v-t="'queries.unsaved_query'">Unsaved Query</span>
        </h3>
      </span>
    </os-page-head>

    <os-page-body>
      <os-page-toolbar>
        <template #default v-if="query.filters.length > 0">
          <os-button left-icon="save" :label="$t('common.buttons.save')" @click="saveQuery" />
          <os-button left-icon="tachometer-alt" :label="$t('queries.get_count')" @click="viewCounts" />
          <os-button left-icon="table" :label="$t('queries.view_records')" @click="viewRecords" />
        </template>
      </os-page-toolbar>

      <os-grid>
        <os-grid-column class="cp-forms" :width="3">
          <os-panel class="cp-selector">
            <template #header>
              <span v-t="'queries.choose_cp_or_group'">Choose Collection Protocol / Group</span>
            </template>
            <template #default>
              <os-cp-n-groups v-model="ctx.cpOrGroup" :disabled="disableCpSelection" />
            </template>
          </os-panel>

          <os-accordion class="forms-selector" ref="formsList" :lazy="true" @tab-open="onFormOpen($event)">
            <os-accordion-tab v-for="form of ctx.forms" :key="form.formId">
              <template #header>
                <span>{{form.caption}}</span>
              </template>

              <template #content>
                <div class="fields">
                  <div class="search">
                    <os-input-text v-model="ctx.fieldSearchTerm" :placeholder="$t('common.buttons.search')" />
                  </div>

                  <div class="field" v-for="field of ctx.filteredFields" :key="field.id">
                    <div class="simple" v-if="!field.subFields" @click="showAddFilter($event, field)">
                      <span>{{field.caption}}</span>
                    </div>
                    <div class="subform" v-else>
                      <div class="title" @click="field.showFields = !field.showFields">
                        <span>{{field.caption}}</span>
                        <os-icon name="caret-right" v-show="!field.showFields" />
                        <os-icon name="caret-down" v-show="field.showFields" />
                      </div>
                      <div class="fields" v-if="field.showFields">
                        <div class="field" v-for="sfField of field.subFields" :key="sfField.id">
                          <div class="simple" @click="showAddFilter($event, sfField)">
                            <span>{{sfField.caption}}</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </template>
            </os-accordion-tab>
          </os-accordion>
        </os-grid-column>

        <os-grid-column :width="9">
          <os-panel class="expressions_n_filters">
            <template #header>
              <span v-t="'queries.expressions_n_filters'">Expressions and Filters</span>
            </template>
            <template #default>
              <div class="body" v-if="ctx.query.filters.length > 0">
                <ExpressionEditor :query="ctx.query" v-model="ctx.query.queryExpression" />

                <div style="flex: 1; overflow-y: auto; margin: 1rem 0rem;">
                  <FiltersList v-model="ctx.query" @update:modelValue="onFilterEdit($event)" />
                </div>
              </div>
              <div class="body" v-else>
                <os-message type="info">
                  <span v-t="'queries.add_filters_to_start'"></span>
                </os-message>
              </div>
            </template>
          </os-panel>
        </os-grid-column>
      </os-grid>
    </os-page-body>
  </os-page>

  <os-overlay class="count-po-wrapper" ref="countsOverlay" :dismissable="false" :show-close-icon="false">
    <div class="count-po">
      <div class="title">
        <span v-t="'queries.count_results'"></span>
      </div>
      <div>
        <table class="os-table" v-if="!ctx.counts">
          <tbody>
            <tr>
              <td v-t="'common.loading'"> Loading... </td>
            </tr>
          </tbody>
        </table>

        <table class="os-table" v-else>
          <tbody>
            <tr>
              <td>
                <os-icon name="users" />
                <span v-t="'queries.participants'"></span>
              </td>
              <td>{{ctx.counts.cprs}}</td>
            </tr>
            <tr>
              <td>
                <os-icon name="calendar" />
                <span v-t="'queries.visits'"></span>
              </td>
              <td>{{ctx.counts.visits}}</td>
            </tr>
            <tr>
              <td>
                <os-icon name="flask" />
                <span v-t="'queries.specimens'"></span>
              </td>
              <td>{{ctx.counts.specimens}}</td>
            </tr>
            <tr>
              <td colspan="2">
                <os-button success :label="$t('common.buttons.ok')" @click="closeCountsOverlay" />
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </os-overlay>

  <SaveQuery ref="saveQueryDialog" />

  <AddEditFilter ref="addEditFilterOverlay" />
</template>

<script>
import alertsSvc from '@/common/services/Alerts.js';
import i18n      from '@/common/services/I18n.js';
import routerSvc from '@/common/services/Router.js';

import formsCache from '@/queries/services/FormsCache.js';
import querySvc   from '@/queries/services/Query.js';

import AddEditFilter from './AddEditFilter.vue';
import ExpressionEditor from '@/queries/views/ExpressionEditor.vue';
import FiltersList from '@/queries/views/FiltersList.vue';
import SaveQuery   from '@/queries/views/SaveQuery.vue';

export default {
  props: ['query'],

  components: {
    AddEditFilter,

    ExpressionEditor,

    FiltersList,

    SaveQuery
  },

  data() {
    return {
      ctx: {
        bcrumb: [
          {url: routerSvc.getUrl('QueriesList', {}), label: i18n.msg('queries.list')}
        ],

        forms: [],

        fields: [],

        filteredFields: [],

        cpOrGroup: null
      }
    }
  },

  created() {
    const {cpId, cpGroupId} = this.ctx.query = this.query;
    if (cpId > 0) {
      this.ctx.cpOrGroup = {id: 'cp_' + cpId, cpId};
    } else if (cpGroupId > 0) {
      this.ctx.cpOrGroup = {id: 'cpg_' + cpGroupId, cpGroupId};
    }

    this.fields = {};
    this._loadForms();
  },

  watch: {
    query: function(newQuery) {
      this.ctx.query = newQuery;
    },

    'ctx.fieldSearchTerm': function(searchTerm) {
      this.filterFields(searchTerm);
    },

    'ctx.cpOrGroup': function(newVal) {
      this.fields = {};

      const {cpId, cpGroupId} = newVal || {};
      this.ctx.query.cpId = cpId;
      this.ctx.query.cpGroupId = cpGroupId;
      if (this.$refs.formsList) {
        this.$refs.formsList.activate(-1);
      }
    }
  },

  computed: {
    disableCpSelection: function() {
      const {filters, selectList} = this.ctx.query;
      for (const filter of filters) {
        if (this._isExtensionOrCustomField(filter.expr) || this._isExtensionOrCustomField(filter.field)) {
          return true;
        }
      }

      for (const field of selectList) {
        const name = typeof field == 'object' ? field.name : (typeof field == 'string' ? field : null);
        if (this._isExtensionOrCustomField(name)) {
          return true;
        }
      }

      return false;
    }
  },

  methods: {
    saveQuery: function() {
      this.$refs.saveQueryDialog.save(this.ctx.query).then(
        ({status, query}) => {
          if (status != 'saved') {
            return;
          }

          alertsSvc.success({code: 'queries.saved', args: query});
          this.$emit('query-saved', query);
        }
      );
    },

    viewCounts: async function(event) {
      this.$refs.countsOverlay.show(event);

      this.ctx.counts = null;
      this.ctx.counts = await querySvc.getCount(this.ctx.query);
    },

    closeCountsOverlay: function(event) {
      this.$refs.countsOverlay.hide(event);
    },

    viewRecords: function() {
      routerSvc.goto('QueryDetail.Results', {queryId: this.ctx.query.id});
    },

    onFormOpen: function({index}) {
      const form = this.ctx.forms[index];
      const {query: {cpId, cpGroupId}} = this.ctx;

      this.ctx.fields = [];
      this.ctx.fieldSearchTerm = null;
      if (this.fields[form.name]) {
        this.ctx.filteredFields = this.ctx.fields = this.fields[form.name];
        return;
      }

      formsCache.getFields(form, cpId, cpGroupId).then(
        fields => {
          this.fields[form.name] = this.ctx.filteredFields = this.ctx.fields = this._flattenFields(fields, 0)
        }
      );
    },

    filterFields: function() {
      const searchTerm = (this.ctx.fieldSearchTerm || '').trim().toLowerCase();
      if (!searchTerm) {
        this.ctx.filteredFields = this.ctx.fields;
        return;
      }

      const result = [];
      for (const field of this.ctx.fields) {
        if (field.subFields && field.subFields.length > 0) {
          const filteredSf = field.subFields.filter(sfField => this._matchesSearchTerm(sfField, searchTerm));
          if (filteredSf.length > 0) {
            result.push({...field, showFields: true, subFields: filteredSf});
          }
        } else if (this._matchesSearchTerm(field, searchTerm)) {
          result.push(field);
        }
      }

      this.ctx.filteredFields = result;
    },

    showAddFilter: function(event, field) {
      const {query: {filters, queryExpression}} = this.ctx;

      this.$refs.addEditFilterOverlay.open(event, {field: field.id, fieldObj: field}).then(
        filter => {
          const maxId = (filters || []).reduce((max, filter) => filter.id > max ? filter.id : max, 0);

          filter.id = maxId + 1;
          filters.push(filter);
          if (filters.length > 1) {
            queryExpression.push({nodeType: 'OPERATOR', value: 'AND'});
          }

          queryExpression.push({nodeType: 'FILTER', value: filter.id});
          this.$emit('query-saved', this.ctx.query);
        }
      );
    },

    onFilterEdit: function(query) {
      this.ctx.query = query;
      this.$emit('query-saved', query);
    },

    _loadForms: function() {
      formsCache.getForms().then(forms => this.ctx.forms = forms);
    },

    _flattenFields: function(fields, level) {
      const result = [];
      const subForms = [];
      for (const field of fields) {
        if (field.type == 'SUBFORM') {
          if (field.name == 'customFields' || field.name == 'extensions') {
            for (const sf of field.subFields || []) {
              const subFields = this._flattenFields(sf.subFields || [], level + 1);
              if (!level) {
                subForms.push({id: sf.id, caption: sf.caption, subFields});
              } else {
                Array.prototype.push.apply(result, subFields);
              }
            }
          } else {
            const subFields = this._flattenFields(field.subFields || [], level + 1);
            if (!level) {
              subForms.push({id: field.id, caption: field.caption, subFields});
            } else {
              Array.prototype.push.apply(result, subFields);
            }
          }
        } else {
          result.push({...field});
        }
      }

      subForms.sort((f1, f2) => f1.caption < f2.caption ? -1 : (f1.caption > f2.caption ? 1 : 0));
      return result.concat(subForms);
    },

    _matchesSearchTerm: function({id, caption}, searchTerm) {
      id = (id || '').toLowerCase();
      caption = (caption || '').toLowerCase();
      return id.indexOf(searchTerm) >= 0 || caption.indexOf(searchTerm) >= 0;
    },

    _isExtensionOrCustomField: function(field) {
      return !!field && (field.indexOf('extensions') >= 0 || field.indexOf('customFields') >= 0);
    }
  }
}
</script>

<style scoped>
.count-po {
  width: 300px;
  margin: -1.25rem;
}

.count-po .title {
  padding: 0.5rem 1rem;
  background: #efefef;
}

.count-po :deep(.os-icon-wrapper) {
  width: 1.25rem;
  margin-right: 0.5rem;
  display: inline-block;
}

.count-po :deep(button) {
  width: 100%;
}

.cp-forms {
  display: flex;
  flex-direction: column;
}

.cp-forms .cp-selector {
  margin-bottom: 1rem;
}

.cp-forms .cp-selector :deep(.p-panel-content) {
  height: 70px;
}

.cp-forms .forms-selector {
  flex: 1;
  overflow-y: auto;
}

.expressions_n_filters {
  height: 100%;
}

.expressions_n_filters .body {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.fields .search {
  margin-bottom: 0.5rem;
}

.fields .field .simple {
  padding: 0.25rem;
  cursor: pointer;
}

.fields .field .subform .title {
  padding: 0.25rem;
  color: #666666;
  font-weight: bold;
  cursor: pointer;
}

.fields .field .subform .title > span {
  margin-right: 0.25rem;
}

.fields .field .subform .title :deep(.svg-inline--fa) {
  vertical-align: -0.2rem;
}
</style>
