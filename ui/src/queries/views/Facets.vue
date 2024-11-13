<template>
  <os-accordion class="facets" @tab-opened="facets[$event.index].loadValues = true">
    <os-accordion-tab class="facet" v-for="facet of facets" :key="facet.id">
      <template #header>
        <span>
          <span>{{facet.caption}}</span>
          <os-button left-icon="times" @click="clearFacetValues($event, facet)"
            v-if="selectedFacets[facet.id] && selectedFacets[facet.id].values" />
        </span>
      </template>

      <template #content>
        <RangeFacet :facet="facet" :selected="selectedFacets[facet.id]"
          @range-selected="addRangeLimit($event)" v-if="facet.isRange" />

        <EqualityFacet :query="this.query" :facet="facet" :selected="selectedFacets[facet.id]"
          @value-selected="addValue($event)" @value-unselected="removeValue($event)" v-else />
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
    addRangeLimit: function({facet, range}) {
      const {minValue, maxValue, selected} = range || {};
      if (selected) {
        this.selectedFacets[facet.id] = {facet, values: {minValue, maxValue}};
      } else {
        delete this.selectedFacets[facet.id];
      }

      this.$emit('facets-selected', Object.values(this.selectedFacets));
    },

    addValue: function({facet, value}) {
      const selectedFacet = this.selectedFacets[facet.id] = this.selectedFacets[facet.id] || {facet, values: []};
      selectedFacet.values.push(value);
      this.$emit('facets-selected', Object.values(this.selectedFacets));
    },

    removeValue: function({facet, value}) {
      const selectedFacet = this.selectedFacets[facet.id] = this.selectedFacets[facet.id] || {facet, values: []};
      const idx = selectedFacet.values.indexOf(value);
      if (idx >= 0) {
        selectedFacet.values.splice(idx, 1);
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
      this.facets = this._getFacets(this.query);
    },

    _getFacets: function(query) {
      const facets = [];

      for (const filter of query.filters || []) {
        if (!filter.parameterized && !filter.subQuery) {
          continue;
        }

        if (filter.subQuery) {
          //
          // if the sub-query has parameterised filters then all the facet
          // values are loaded instead of loading only those that satisfy
          // the query criteria
          //
          const sqFacets = this._getFacets(filter.subQuery);
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

        facets.push(this._getFacet(filter, expr, type, op, values));
      }

      return facets;
    },

    _getFacet: function(filter, expr, type, op, values) {
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
        id: filter.id,
        caption: filter.desc || filter.fieldObj.caption,
        type: type,
        isRange: type == 'INTEGER' || type == 'FLOAT' || type == 'DATE',
        op: op,
        expr,
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
  width: 30vw;
}
</style>
