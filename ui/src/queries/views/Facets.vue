<template>
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
</template>

<script>

import queryUtil from '@/queries/services/Util.js';

import EqualityFacet from '@/queries/views/EqualityFacet.vue';
import RangeFacet    from '@/queries/views/RangeFacet.vue';

const STRING_FACETED_OPS = ['EQ', 'IN', 'EXISTS', 'ANY'];
            
const RANGE_FACETED_OPS  = ['LT', 'LE', 'GT', 'GE', 'EQ', 'BETWEEN', 'EXISTS', 'ANY'];

export default {
  props: ['query'],

  emits: ['facets-loaded', 'facets-selected'],

  components: {
    EqualityFacet,

    RangeFacet
  },

  data() {
    return {
      hasSqFacets: false,

      facets: [],

      selectedFacets: {}
    }
  },

  mounted() {
    this._loadFacets();
  },

  methods: {
    hasFacets: function() {
      return this.facets && this.facets.length > 0;
    },

    addRangeLimit: function({facet, range}) {
      const {minValue, maxValue, selected} = range || {};
      if (selected) {
        this.selectedFacets[facet.id] = {facet, values: {minValue, maxValue}};
      } else {
        delete this.selectedFacets[facet.id];
      }

      this.$emit('facets-selected', Object.values(this.selectedFacets));
    },

    addValues: function({facet, values}, newList) {
      const selectedFacet = this.selectedFacets[facet.id] = (!newList && this.selectedFacets[facet.id]) || {facet, values: []};
      Array.prototype.push.apply(selectedFacet.values, values);
      this.$emit('facets-selected', Object.values(this.selectedFacets));
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

      this.$emit('facets-selected', Object.values(this.selectedFacets));
    },

    clearFacetValues: function(event, facet) {
      event.stopPropagation();

      delete this.selectedFacets[facet.id];
      this.$emit('facets-selected', Object.values(this.selectedFacets));
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
            continue;
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
.facets {
  height: 100%;
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
