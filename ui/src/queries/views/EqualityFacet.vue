<template>
  <ul class="equality-facet">
    <li class="input">
      <os-input-text :md-type="true" :placeholder="$t('common.buttons.search')"
        v-model="searchTerm" :debounce="500" @change="_loadFacetValues" />
    </li>
        
    <li v-for="(item, itemIdx) of items" :key="itemIdx">
      <os-boolean-checkbox v-model="item.selected" @change="toggleSelection(item)"/>
      <span>{{item.value}}</span>
    </li>
  </ul>
</template>

<script>
import querySvc from '@/queries/services/Query.js';

export default {
  props: ['query', 'facet', 'selected'],

  emits: ['value-selected', 'value-unselected'],

  data() {
    return {
      searchTerm: null,

      list: []
    }
  },

  computed: {
    items: function() {
      const {values} = this.selected || {values: []};
      return (this.list || []).map(value => ({value, selected: values.indexOf(value) >= 0}));
    }
  },

  watch: {
    'facet.loadValues': function(loadValues) {
      if (loadValues) {
        this._loadFacetValues();
      }
    }
  },

  mounted() {
    this._loadFacetValues();
  },

  methods: {
    toggleSelection: function(item) {
      if (item.selected) {
        this.$emit('value-selected', {facet: this.facet, value: item.value});
      } else {
        this.$emit('value-unselected', {facet: this.facet, value: item.value});
      }
    },

    _loadFacetValues: async function() {
      const facet = this.facet;
      if (!facet.loadValues) {
        return;
      }

      if (facet.values) {
        this.list = facet.values;
        if (this.searchTerm) {
          const searchTerm = this.searchTerm.toLowerCase();
          this.list = facet.values.filter(value => value.toLowerCase().indexOf(searchTerm) >= 0);
        }
      } else {
        if (!this.searchTerm) {
          if (!this.baseList) {
            this.baseList = await this._getFacetValues();
          }

          this.list = this.baseList;
        } else {
          this.list = await this._getFacetValues(this.searchTerm);
        }
      }
    },

    _getFacetValues: async function(searchTerm) {
      const {values} = await querySvc.getFacetValues(this.query, this.facet.expr, searchTerm);
      return values;
    }

  }
}
</script>

<style scoped>
.equality-facet {
  list-style: none;
  margin: 0;
  padding: 0;
}       
        
.equality-facet li {
  display: flex;
  flex-direction: row;
}       
      
.equality-facet li.input {
  margin-bottom: 1.25rem;
}
            
.equality-facet li.input :deep(.os-input-text) {
  width: 100%;
}
  
.equality-facet li :deep(.os-boolean-checkbox) {
  margin-right: 1rem;
}
</style>
