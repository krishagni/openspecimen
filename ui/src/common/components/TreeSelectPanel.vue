<template>
  <os-tree-select :nodes="ctx.nodes" @selection-changed="handleSelections" />
</template>

<script>
export default {
  props: ['items', 'selected'],

  emits: ['selected-items'],

  data() {
    return {
      ctx: {
        nodes: []
      }
    };
  },

  created() {
    this.ctx.nodes = this._getNodes(null, this.items, this.selected || []);
  },

  watch: {
    hierarchyKey: function() {
      this.ctx.nodes = this._getNodes(null, this.items, this.selected || []);
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
          Array.prototype.push.apply(working, node.children || []);
        }
      }

      this.$emit('selected-items', selected);
    },

    _getNodes: function(parent, items, selected) {
      const nodes = [];
      for (const item of items) {
        const {id, label} = item;
        const node = {id, label, item, expanded: false, selected: false, parent};
        node.children = this._getNodes(node, item.children || [], selected);
        if (this._isSelected(node, selected)) {
          node.selected = true;
          node.expanded = true;
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
    }
  }
}
</script>
