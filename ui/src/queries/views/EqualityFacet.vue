<template>
  <div class="equality-facet">
    <div class="search-items" v-if="facet.hideOptions">
      <os-textarea :rows="2" :placeholder="$t('queries.search_filter_value')" v-model="searchTerm" />

      <os-button success size="small" :label="$t('common.buttons.go')" @click="addConditions" />
    </div>

    <div class="search-items" v-else>
      <os-input-text :md-type="true" :placeholder="$t('common.buttons.search')"
        v-model="searchTerm" :debounce="500" @change="_loadFacetValues" />
    </div>

    <ul class="items" v-if="items.length > 0">
      <li v-for="(item, itemIdx) of items" :key="itemIdx">
        <os-boolean-checkbox v-model="item.selected" @change="toggleSelection(item)"/>
        <span>{{item.value}}</span>
      </li>
    </ul>
  </div>
</template>

<script>
import querySvc from '@/queries/services/Query.js';
import util from '@/common/services/Util.js';

export default {
  props: ['query', 'facet', 'selected'],

  emits: ['values-selected', 'values-unselected'],

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
        this.$emit('values-selected', {facet: this.facet, values: [item.value]});
      } else {
        this.$emit('values-unselected', {facet: this.facet, values: [item.value]});
      }
    },

    addConditions: function() {
      this.list = util.splitStr(this.searchTerm || '', /,|\t|\n/, false);
      this.searchTerm = null;
      this.$emit('values-added', {facet: this.facet, values: this.list});
    },

    _loadFacetValues: async function() {
      const facet = this.facet;
      if (!facet.loadValues || facet.hideOptions) {
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
.equality-facet .search-items {
  margin-bottom: 1.25rem;
  display: flex;
  flex-direction: column;
}

.equality-facet ul {
  list-style: none;
  margin: 0;
  padding: 0;
}       
        
.equality-facet ul li {
  display: flex;
  flex-direction: row;
}       
      
.equality-facet ul li :deep(.os-boolean-checkbox) {
  margin-right: 1rem;
}
</style>
