<template>
  <div class="facets-panel">
    <div class="facet-actions" v-if="hasPendingChanges">
      <os-button-group>
        <os-button primary :label="$t('common.buttons.apply')" @click="applyFacets" />

        <os-button :label="$t('queries.discard_facet_changes')" @click="discardChanges" />
      </os-button-group>
    </div>

    <os-accordion class="facets" @tab-opened="facets[$event.index].loadValues = true">
      <os-accordion-tab class="facet" v-for="facet of facets" :key="facet.id">
        <template #header>
          <span class="facet-header">
            <span class="label">{{facet.caption}}</span>
            <os-button left-icon="times" size="small" @click="clearFacetValues($event, facet)"
              v-if="selectedFacets[facet.id] && selectedFacets[facet.id].values" />
          </span>
        </template>

        <template #content>
          <RangeFacet :facet="facet" :selected="selectedFacets[facet.id]"
            @range-selected="addRangeLimit($event)" v-if="facet.isRange" />

          <EqualityFacet :query="this.query" :facet="facet" :selected="selectedFacets[facet.id]"
            @values-added="addValues($event, true)" @values-selected="addValues($event)"
            @values-unselected="removeValues($event)" v-else />
        </template>
      </os-accordion-tab>
    </os-accordion>
  </div>
</template>

<script>

import queryUtil from '@/queries/services/Util.js';

import EqualityFacet from '@/queries/views/EqualityFacet.vue';
import RangeFacet    from '@/queries/views/RangeFacet.vue';

const STRING_FACETED_OPS = ['EQ', 'IN', 'EXISTS', 'ANY'];
            
const RANGE_FACETED_OPS  = ['LT', 'LE', 'GT', 'GE', 'EQ', 'BETWEEN', 'EXISTS', 'ANY'];

export default {
  props: ['query'],

  emits: ['facets-loaded', 'facets-applied', 'facets-changed'],

  components: {
    EqualityFacet,

    RangeFacet
  },

  data() {
    return {
      hasSqFacets: false,

      facets: [],

      selectedFacets: {},

      appliedFacets: {},

      selectionChanged: false
    }
  },

  mounted() {
    this._loadFacets();
  },

  computed: {
    hasPendingChanges: function() {
      return this.selectionChanged;
    }
  },

  methods: {
    hasFacets: function() {
      return this.facets && this.facets.length > 0;
    },

    discardChanges: function() {
      this.selectedFacets = this._copyFacets(this.appliedFacets);
      this._setSelectionChanged(false);
    },

    applyFacets: function() {
      this.appliedFacets = this._copyFacets(this.selectedFacets);
      this._setSelectionChanged(false);
      this.$emit('facets-applied', Object.values(this.selectedFacets));
    },

    addRangeLimit: function({facet, range}) {
      const {minValue, maxValue, selected} = range || {};
      if (selected) {
        this.selectedFacets[facet.id] = {facet, values: {minValue, maxValue}};
      } else {
        delete this.selectedFacets[facet.id];
      }

      this._setSelectionChanged(true);
    },

    addValues: function({facet, values}, newList) {
      const selectedFacet = this.selectedFacets[facet.id] = (!newList && this.selectedFacets[facet.id]) || {facet, values: []};
      Array.prototype.push.apply(selectedFacet.values, values);
      this._setSelectionChanged(true);
    },

    removeValues: function({facet, values}) {
      const selectedFacet = this.selectedFacets[facet.id] = this.selectedFacets[facet.id] || {facet, values: []};
      for (const value of values) {
        const idx = selectedFacet.values.indexOf(value);
        if (idx >= 0) {
          selectedFacet.values.splice(idx, 1);
        }
      }

      if (selectedFacet.values.length == 0) {
        delete this.selectedFacets[facet.id];
      }

      this._setSelectionChanged(true);
    },

    clearFacetValues: function(event, facet) {
      event.stopPropagation();

      delete this.selectedFacets[facet.id];
      this._setSelectionChanged(true);
    },

    _copyFacets: function(facets) {
      const result = {};
      for (const [id, {facet, values}] of Object.entries(facets)) {
        result[id] = {
          facet,
          values: Array.isArray(values) ? [...values] : (values && typeof values == 'object' ? {...values} : values)
        };
      }

      return result;
    },

    _setSelectionChanged: function(changed) {
      this.selectionChanged = changed;
      this.$emit('facets-changed', changed);
    },

    _loadFacets: function() {
      this.facets = this._getFacets('', this.query);
      this.$emit('facets-loaded', this.facets);
    },

    _getFacets: function(prefix, query) {
      const facets = [];

      for (const filter of query.filters || []) {
        if (!filter.parameterized && !filter.subQuery) {
          continue;
        }

        if (filter.subQuery) {
          //
          // TODO: if the sub-query has parameterised filters then all the facet
          // values are loaded instead of loading only those that satisfy
          // the query criteria
          //
          const sqFacets = this._getFacets(prefix + filter.id + '.', filter.subQuery);
          if (sqFacets.length > 0) {
            this.hasSqFacets = true;
            Array.prototype.push.apply(facets, sqFacets);
          }
          continue;
        }

        let expr = null, op = null, type = null, values;
        if (filter.expr) {
          const tObj = queryUtil.parseTemporalExpression(filter.expr);
          op = this._getOpEnum(tObj.op);

          const rhs = tObj.rhs && tObj.rhs.trim();
          if (rhs) {
            if (op == 'BETWEEN') {
              values = JSON.parse('[' + rhs.substring(1, rhs.length - 1) + ']');
            } else {
              values = [tObj.rhs];
            }
          }

          expr = tObj.lhs;
          type = 'INTEGER';
        } else {
          expr = filter.field;
          type = filter.fieldObj.type;
          op = filter.op;
          values = filter.values;
        }

        if (!op) {
          continue;
        }

        switch (type) {
          case 'STRING':
            if (STRING_FACETED_OPS.indexOf(op) == -1) {
              continue;
            }
            break;

          case 'INTEGER':
          case 'FLOAT':
          case 'DATE':
            if (RANGE_FACETED_OPS.indexOf(op) == -1) {
              continue;
            }
            break;

          default:
            if (type != 'BOOLEAN') {
              continue;
            }
        }

        facets.push(this._getFacet(prefix, filter, expr, type, op, values));
      }

      return facets;
    },

    _getFacet: function(prefix, filter, expr, type, op, values) {
      let facetValues = undefined;
      if (type == 'INTEGER' || type == 'FLOAT' || type == 'DATE') {
        let minValue = null, maxValue = null;
        if (values && values.length > 0) {
          switch (op) {
            case 'EQ':
              minValue = maxValue = values[0];
              break;

            case 'LT':
            case 'LE':
              maxValue = values[0];
              break;

            case 'GT':
            case 'GE':
              minValue = values[0];
              break;

            case 'BETWEEN':
              minValue = values[0];
              maxValue = values[1];
              break;
          }
        }

        if (minValue != null || maxValue != null) {
          facetValues = {minValue, maxValue, selected: true};
        } else {
          facetValues = {};
        }
      } else if (type == 'STRING' && values.length > 0 && values.some(value => value != null && value != undefined)) {
        facetValues = values;
      }

      return {
        id: prefix + filter.id,
        caption: filter.desc || filter.fieldObj.caption,
        type: type,
        isRange: type == 'INTEGER' || type == 'FLOAT' || type == 'DATE',
        op: op,
        expr,
        hideOptions: filter.hideOptions,
        values: facetValues
      }
    },

    _getOpEnum: function(op) {
      switch (op) {
        case '<':  return 'LT';
        case '<=': return 'LE';
        case '>':  return 'GT';
        case '>=': return 'GE';
        case '=':  return 'EQ';
        case '!=': return 'NE';
        case 'in': return 'IN';
        case 'not in': return 'NOT_IN';
        case 'exists': return 'EXISTS';
        case 'not exists': return 'NOT_EXISTS';
        case 'any': return 'ANY';
        case 'between': return 'BETWEEN';
        case 'contains': return 'CONTAINS';
        case 'starts with': return 'STARTS_WITH';
        case 'ends with': return 'ENDS_WITH';
      }

      return op;
    },
  }
}
</script>

<style scoped>
.facets-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.facet-actions {
  padding-bottom: 0.75rem;
  display: flex;
  gap: 0.5rem;
}

.facet-actions :deep(button),
.facet-actions :deep(.os-btn-group) {
  flex: 1;
}

.facets {
  flex: 1;
  overflow-y: auto;
}

.facet-header {
  width: 100%;
  display: flex;
  flex-direction: row;
  align-items: center;
}

.facet-header .label {
  flex: 1;
}
</style>
