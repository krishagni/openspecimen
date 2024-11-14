<template>
  <div>
    <div class="filter" v-for="(filter, idx) of filters" :key="idx">
      <div class="id">
        <span>{{filter.id}}</span>
        <span class="parameterized" v-if="filter.parameterized" v-os-tooltip.right="$t('queries.parameterized')">
          <span>P</span>
        </span>
        <span class="sub-query" v-if="filter.subQueryId > 0" v-os-tooltip.right="$t('queries.subquery')">
          <span>Q</span>
        </span>
      </div>
      <div class="desc">
        <FilterDesc :filter="filter" />
      </div>
      <div class="ops">
        <os-button-group>
          <os-button left-icon="edit" size="small" @click="showEditFilter($event, idx)" />
          <os-button left-icon="trash" size="small" @click="deleteFilter(idx)" />
        </os-button-group>
      </div>
    </div>
  </div>

  <AddEditFilter ref="addEditFilterOverlay" align="left" />
</template>

<script>
import util      from '@/common/services/Util.js';

import AddEditFilter from './AddEditFilter.vue';
import FilterDesc from './FilterDesc.vue';

export default {
  props: ['modelValue'],

  components: {
    AddEditFilter,

    FilterDesc
  },

  computed: {
    query: {
      get() {
        return this.modelValue;
      },

      set(value) {
        this.$emit('update:modelValue', value);
      }
    },

    filters: function() {
      return this.query.filters || [];
    }
  },

  watch: {
    'ctx.query': {
      handler: function(query) {
        this.query = query;
      },

      deep: true
    }
  },

  data() {
    return {
      ctx: {
        query: {},
      }
    }
  },

  created() {
    this.ctx.query = util.clone(this.query);
  },

  methods: {
    showEditFilter: function(event, idx) {
      const filter = this.query.filters[idx];
      this.$refs.addEditFilterOverlay.open(event, filter).then(savedFilter => this.query.filters[idx] = savedFilter);
    },

    deleteFilter: function(idx) {
      const {query} = this.ctx;

      const filter = query.filters[idx];
      query.filters.splice(idx, 1);

      //
      // Remove temporal filter from select list
      //
      const selectField = '$temporal.' + filter.id;
      for (let i = 0; i < query.selectList.length; ++i) {
        let field = query.selectList[i];
        if (field && typeof field != 'string') {
          field = field.name;
        }

        if (field == selectField) {
          query.selectList.splice(i, 1);
          break;
        }
      }

      //
      // Remove from query expression nodes
      //
      var exprNodes = query.queryExpression || [];
      for (let i = 0; i < exprNodes.length; ++i) {
        const exprNode = exprNodes[i];
        if (exprNode.nodeType != 'FILTER' || filter.id != exprNode.value) {
          continue;
        }

        if (i == 0 && exprNodes.length > 1 && exprNodes[1].nodeType == 'OPERATOR') {
          // first filter -> remove filter and operator to right
          exprNodes.splice(0, 2);
        } else if (i != 0 && exprNodes[i - 1].nodeType == 'OPERATOR') {
          // operator on left -> remove filter and operator to left
          exprNodes.splice(i - 1, 2);
        } else if (i != 0 && (i + 1) < exprNodes.length && exprNodes[i + 1].nodeType == 'OPERATOR') {
          // operator on right -> remove filter and operator to right
          exprNodes.splice(i, 2);
        } else {
          // no operators before or after filter -> remove only filter
          exprNodes.splice(i, 1);
        }

        break;
      }
    }
  }
}
</script>

<style scoped>
.filter {
  display: flex;
  flex-direction: row;
  align-items: center;
  padding: 1rem;
  margin-bottom: 1rem;
  border: 1px solid #ddd;
  border-radius: 0.5rem;
}

.filter .id {
  height: 2.5rem;
  width: 2.5rem;
  border-radius: 50%;
  border: 1px solid #ccc;
  background-color: #ccc;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  margin-right: 1rem;
}

.filter .id .sub-query,
.filter .id .parameterized {
  position: absolute;
  top: -5px;
  right: -2px;
  height: 1rem;
  width: 1rem;
  border: 1px solid #428bca;
  border-radius: 50%;
  background: #428bca;
  color: #fff;
  font-size: 0.75rem;
  line-height: 1.14;
  text-align: center;
}

.filter .id .sub-query {
  background: #5cb85c;
  border: 1px solid #5cb85c;
}

.filter .id .parameterized {
  background: #428bca;
  border: 1px solid #428bca;
}

.filter .desc {
  flex: 1;
  margin-right: 1rem;
}

.filter .ops {
}

</style>

<style>
.os-query-filter-overlay:after {
  border-width: 5px;
  margin-left: -5px;
}

.os-query-filter-overlay:before {
  border-width: 7px;
  margin-left: -7px;
}
</style>
