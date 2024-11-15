<template>
  <os-tree-select v-model="ctx.nodes"
    @selection-changed="handleSelections" @order-changed="handleSelections" />
</template>

<script>
export default {
  props: ['items', 'selected', 'expand-selected'],

  emits: ['selected-items'],

  data() {
    return {
      ctx: {
        nodes: []
      }
    };
  },

  created() {
    this.ctx.nodes = this._getSortedNodes(this.items, this.selected || []);
  },

  watch: {
    hierarchyKey: function() {
      this.ctx.nodes = this._getSortedNodes(this.items, this.selected || []);
    }
  },

  computed: {
    hierarchyKey: function() {
      return (this.items || []).map(({id}) => id).join('_');
    }
  },

  methods: {
    handleSelections: function() {
      const selected = [];
      const working = [...this.ctx.nodes];
      while (working.length > 0) {
        const node = working.shift();
        if (!node.selected) {
          continue;
        }

        if (!node.children || node.children.length == 0) {
          selected.push(node.item);
        } else {
          Array.prototype.unshift.apply(working, node.children || []);
        }
      }

      this.$emit('selected-items', selected);
    },

    _getSortedNodes: function(items, selected) {
      const nodes = this._getNodes(null, items, selected);
      if (selected && selected.length > 0) {
        this._sortNodes(nodes, (selected || []).map(({id}) => id));
      }

      return nodes;
    },

    _getNodes: function(parent, items, selected) {
      const nodes = [];
      for (const item of items) {
        const {id, label} = item;
        const node = {id, label, item, expanded: false, selected: false, parent};
        node.children = this._getNodes(node, item.children || [], selected);
        if (this._isSelected(node, selected)) {
          node.selected = true;
          node.expanded = this.expandSelected;
        }

        nodes.push(node);
      }

      return nodes;
    },

    _isSelected: function(node, selected) {
      if (!selected || selected.length == 0) {
        return false;
      }

      return selected.some(({id}) => id == node.id) || node.children.some(child => this._isSelected(child, selected));
    },

    _sortNodes: function(nodes, selected) {
      for (const node of nodes || []) {
        if (node.children && node.children.length > 0) {
          node.sortSeq = this._sortNodes(node.children, selected);
        } else {
          node.sortSeq = selected.indexOf(node.id);
          if (node.sortSeq == -1) {
            node.sortSeq = 999999;
          }
        }
      }

      nodes.sort((n1, n2) => n1.sortSeq - n2.sortSeq);
      return nodes.length == 0 ? 999999 : nodes[0].sortSeq;
    }
  }
}
</script>
