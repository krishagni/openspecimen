<template>
  <div class="os-tree">
    <draggable v-model="nodes" tag="ul" handle=".node-grip" @update:model-value="orderChanged">
      <li class="node-grip" v-for="node of nodes" :key="node.id">
        <div class="node">
          <span v-if="node.children && node.children.length > 0">
            <os-icon class="expand"   name="plus"  @click="expandNode(node)"   v-if="!node.expanded" />
            <os-icon class="collapse" name="minus" @click="collapseNode(node)" v-else />
          </span>
          <os-icon class="filler" name="ban" v-else />
          <input type="checkbox" v-model="node.selected" @click="toggleSelection(node)" />
          <span> {{node.label}} </span>
        </div>

        <os-tree-select v-model="node.children" @selection-changed="selectionChanged" @order-changed="orderChanged"
          v-show="node.expanded && node.children.length > 0" />
      </li>
    </draggable>
  </div>
</template>

<script>
import { VueDraggableNext } from "vue-draggable-next";

export default {
  props: ['modelValue'],

  emits: ['selection-changed', 'order-changed'],

  components: {
    draggable: VueDraggableNext
  },

  data() {
    return {
      ctx: {
      }
    };
  },

  computed: {
    nodes: {
      get() {
        return this.modelValue;
      },

      set(value) {
        this.$emit('update:modelValue', value);
      }
    }
  },

  methods: {
    expandNode: function(node) {
      node.expanded = true;
    },

    collapseNode: function(node) {
      node.expanded = false;
    },

    toggleSelection: function(node) {
      setTimeout(() => node.selected ? this._selectNode(node) : this._unselectNode(node));
    },

    selectionChanged: function() {
      this.$emit('selection-changed');
    },

    orderChanged: function() {
      this.$emit('order-changed');
    },

    _selectNode: function(node) {
      node.expanded = true;
      this._selectAncestors(node);
      this._selectChildren(node);
      this.selectionChanged();
    },

    _selectAncestors: function(node) {
      while (node.parent && !node.parent.selected) {
        node.parent.selected = true;
        node = node.parent;
      }
    },

    _selectChildren: function(node) {
      for (const child of node.children || []) {
        child.selected = true;
        child.expanded = true;
        this._selectChildren(child);
      }
    },

    _unselectNode: function(node) {
      this._unselectChildren(node);
      this._unselectAncestors(node);
      this.selectionChanged();
    },

    _unselectAncestors: function({parent}) {
      if (parent && parent.children && parent.children.every(child => !child.selected)) {
        parent.selected = false;
        this._unselectAncestors(parent);
      }
    },

    _unselectChildren: function(node) {
      for (const child of node.children || []) {
        child.selected = false;
        this._unselectChildren(child);
      }
    }
  }
}
</script>

<style scoped>
.os-tree ul {
  list-style: none;
  padding: 0;
  padding-left: 1rem;
  margin: 0;
}

.os-tree ul li {
  margin-bottom: 0.25rem;
}

.os-tree ul li.node-grip {
  cursor: grab;
}

.os-tree .node {
  display: flex;
  flex-direction: row;
  align-items: center;
}

.os-tree .node .expand,
.os-tree .node .collapse,
.os-tree .node .filler {
  margin-right: 0.25rem;
}

.os-tree .node .expand :deep(svg),
.os-tree .node .collapse :deep(svg),
.os-tree .node .filler :deep(svg) {
  border: 1px solid #777;
  border-radius: 50%;
  height: 14px;
  width: 14px;
}

.os-tree .node .filler :deep(svg) {
  border: 0;
}

.os-tree .node .expand :deep(svg path),
.os-tree .node .collapse :deep(svg path),
.os-tree .node .filler :deep(svg path) {
  stroke-width: 10% !important;
  stroke: #fff !important;
}

.os-tree .node .filler :deep(svg path) {
  fill: #fff;
}

.os-tree .node input {
  margin-right: 0.5rem;
}
</style>
