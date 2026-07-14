<template>
  <div class="os-tree-select-panel">
    <div class="search" v-if="searchable">
      <os-input-text v-model="ctx.searchTerm" :placeholder="$t('common.buttons.search')" />
    </div>

    <os-tree-select v-model="ctx.nodes" :disable-ordering="expandFiltered"
      @selection-changed="handleSelections" @order-changed="handleSelections" />
  </div>
</template>

<script>
export default {
  props: ['items', 'selected', 'expand-selected', 'searchable'],

  emits: ['selected-items'],

  data() {
    return {
      ctx: {
        nodes: [],

        searchTerm: null
      }
    };
  },

  created() {
    this.ctx.nodes = this._getSortedNodes(this.filteredItems, this.selected || []);
  },

  watch: {
    hierarchyKey: function() {
      this.ctx.nodes = this._getSortedNodes(this.filteredItems, this.selected || []);
    }
  },

  computed: {
    searchTerm: function() {
      return (this.ctx.searchTerm || '').trim().toLowerCase();
    },

    filteredItems: function() {
      if (!this.searchTerm) {
        return this.items;
      }

      return this._filterItems(this.items, this.searchTerm);
    },

    expandFiltered: function() {
      return !!this.searchTerm;
    },

    hierarchyKey: function() {
      return [
        this.expandSelected,
        this.expandFiltered,
        this._getHierarchyKey(this.filteredItems || [])
      ].join(':');
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

      let result = selected;
      if (this.searchTerm) {
        const visibleItems = this._getItemsMap(this.filteredItems, {});
        result = (this.selected || []).filter(item => !visibleItems[item.id]).concat(selected);
      }

      this.$emit('selected-items', result);
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
        const node = {id, label, item, expanded: this.expandFiltered, selected: false, parent};
        node.children = this._getNodes(node, item.children || [], selected);
        if (this._isSelected(node, selected)) {
          node.selected = true;
          node.expanded = node.expanded || this.expandSelected;
        }

        nodes.push(node);
      }

      return nodes;
    },

    _filterItems: function(items, searchTerm) {
      const result = [];
      for (let item of items || []) {
        const itemMatches = this._matchesSearchTerm(item, searchTerm);
        const children = itemMatches ? (item.children || []) : this._filterItems(item.children || [], searchTerm);
        if (children.length > 0 || itemMatches) {
          const copy = {...item};
          if (children.length > 0) {
            copy.children = children;
          } else {
            delete copy.children;
          }

          result.push(copy);
        }
      }

      return result;
    },

    _matchesSearchTerm: function({id, label}, searchTerm) {
      return (id || '').toLowerCase().indexOf(searchTerm) >= 0 ||
        (label || '').toLowerCase().indexOf(searchTerm) >= 0;
    },

    _getItemsMap: function(items, map) {
      for (let item of items || []) {
        map[item.id] = item;
        if (item.children && item.children.length > 0) {
          this._getItemsMap(item.children, map);
        }
      }

      return map;
    },

    _getHierarchyKey: function(items) {
      return (items || []).map(item => item.id + '[' + this._getHierarchyKey(item.children || []) + ']').join('_');
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

<style scoped>
.os-tree-select-panel .search {
  margin-bottom: 1rem;
}
</style>
