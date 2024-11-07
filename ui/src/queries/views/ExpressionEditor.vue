<template>
  <div>
    <div class="ops">
      <os-button-group>
        <os-button label="AND"           @click="addOp('AND')" />
        <os-button label="OR"            @click="addOp('OR')" />
        <os-button label="NOT"           @click="addOp('NOT')" />
        <os-button left-icon="share-alt" @click="addOp('NTHCHILD')" />
      </os-button-group>

      <os-button label="(...)" @click="addParenthesis" />
    </div>

    <draggable :class="classList" v-model="ctx.queryExpression" tag="div" handle=".node">
      <div class="node" v-for="(node, idx) of nodes" :key="idx">
        <div class="filter" v-if="node.nodeType == 'FILTER'" v-os-tooltip.bottom="filterDesc(node.value)">
          <span>{{node.value}}</span>
          <span class="parameterized" v-if="isParameterized(node.value)"
            v-os-tooltip.right="$t('queries.parameterized')">P</span>
          <span class="sub-query" v-if="isSubqueryFilter(node.value)"
            v-os-tooltip.right="$t('queries.subquery')">Q</span>
        </div>

        <div class="operator" v-if="node.nodeType == 'OPERATOR'" @click="toggleOperator(node)">
          <span class="value-icon">
            <span v-if="node.value != 'NTHCHILD'">{{node.value.toLowerCase()}}</span>
            <os-icon v-else name="share-alt" />
          </span>
          <span class="remove-icon" @click="removeNode(idx)">x</span>
        </div>

        <div class="parenthesis" v-if="node.nodeType == 'PARENTHESIS'">
          <span class="value-icon">
            <span v-if="node.value == 'LEFT'">(</span>
            <span v-else>)</span>
          </span>
          <span class="remove-icon" @click="removeNode(idx)">x</span>
        </div>
      </div>
    </draggable>
  </div>
</template>

<script>
import { VueDraggableNext } from "vue-draggable-next";

import queryUtil from '@/queries/services/Util.js';
import util      from '@/common/services/Util.js';

export default {
  props: ['query', 'modelValue'],

  components: {
    draggable: VueDraggableNext
  },

  computed: {
    nodes: {
      get() {
        return this.modelValue;
      },

      set(value) {
        this.$emit('update:modelValue', value);
      }
    },

    filtersMap: function() {
      const map = {};
      for (const filter of this.query.filters || []) {
        map[filter.id] = filter;
      }
 
      return map;
    },

    isValid: function() {
      return queryUtil.isValidExpr(this.nodes);
    },

    classList: function() {
      const result = ['expression'];
      if (!this.isValid) {
        result.push('error');
      }

      return result;
    }
  },

  watch: {
    'ctx.queryExpression': {
      handler: function(exprNodes) {
        this.nodes = exprNodes;
      },

      deep: true
    }
  },

  data() {
    return {
      ctx: {
        queryExpression: []
      }
    }
  },

  created() {
    this.ctx.queryExpression = util.clone(this.nodes);
  },

  methods: {
    toggleOperator: function(node) {
      node.value = node.value == 'AND' ? 'OR' : (node.value == 'OR' ? 'AND' : node.value);
    },

    addOp: function(op) {
      this.ctx.queryExpression.push({nodeType: 'OPERATOR', value: op});
    },

    addParenthesis: function() {
      this.ctx.queryExpression.unshift({nodeType: 'PARENTHESIS', value: 'LEFT'});
      this.ctx.queryExpression.push({nodeType: 'PARENTHESIS', value: 'RIGHT'});
    },

    removeNode: function(nodeIdx) {
      this.ctx.queryExpression.splice(nodeIdx, 1);
    },

    filterDesc: function(filterId) {
      const filter = this.filtersMap[filterId] || {};
      return queryUtil.getFilterDesc(filter);
    },

    isParameterized: function(filterId) {
      const filter = this.filtersMap[filterId] || {};
      return filter.parameterized;
    },

    isSubqueryFilter: function(filterId) {
      const filter = this.filtersMap[filterId] || {};
      return filter.subQueryId > 0;
    }
  }
}
</script>

<style scoped>
.ops {
  display: flex;
  flex-direction: row;
  margin-bottom: 1rem;
}

.ops :deep(.os-btn-group) {
  margin-right: 0.5rem;
}

.ops :deep(button) {
  height: auto;
  width: auto;
  padding: 0.25rem 0.5rem;
  font-size: 0.75rem;
  background: transparent;
  border-radius: 0.5rem;
}

.expression {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 0.5rem;
}

.expression.error {
  border-color: red;
}

.expression .node {
  cursor: grab;
  margin: 0.5rem;
}

.expression .node .filter {
  height: 2.5rem;
  width: 2.5rem;
  border-radius: 50%;
  border: 1px solid #ccc;
  background-color: #ccc;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
}

.expression .node .filter .sub-query,
.expression .node .filter .parameterized {
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

.expression .node .filter .sub-query {
  background: #5cb85c;
  border: 1px solid #5cb85c;
}

.expression .node .filter .parameterized {
  background: #428bca;
  border: 1px solid #428bca;
}

.expression .node .operator {
  height: 2.5rem;
  width: 2.5rem;
  border-radius: 0.25rem;
  border: 1px solid #ccc;
  background-color: #f5f5f5;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  position: relative;
}

.expression .node .parenthesis {
  height: 2.5rem;
  width: 1.25rem;
  border-radius: 0.25rem;
  border: 1px solid #ccc;
  background-color: #fff;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
}

.expression .node .operator .remove-icon,
.expression .node .parenthesis .remove-icon {
  position: absolute;
  top: -5px;
  right: -2px;
  font-size: 0.60rem;
  border: 1px solid #ddd;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #f5f5f5;
  text-align: center;
  line-height: 0.7;
  cursor: pointer;
  color: #777;
}
</style>
