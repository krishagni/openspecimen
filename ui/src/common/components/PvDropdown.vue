
<template>
  <Dropdown v-model="inputValue" :list-source="listSource" />
</template>

<script>
import Dropdown from '@/common/components/Dropdown.vue';

import http from '@/common/services/HttpClient.js';
import util from '@/common/services/Util.js';

export default {
  props: ['modelValue', 'selectProp', 'attribute', 'leafValue'],

  components: {
    Dropdown
  },

  data() {
    return {
      listSource: {
        loadFn: async (opts) => {
          let queryParams = Object.assign(
            {
              searchString: opts.query || '',
              attribute: this.attribute,
              includeOnlyLeafValue: this.leafValue == true
            },
            opts || {maxResults: 100}
          );

          if (opts.value || opts.value == 0) {
            try {
              let id = parseInt(opts.value);
              if (!isNaN(id)) {
                return http.get('permissible-values/v/' + id).then(pv => [pv]);
              }
            } catch {
              console.log('PvDropdown: Error getting value: ' + opts.value);
            }
          }

          let key = util.queryString(queryParams);
          this.cachedQueries = this.cachedQueries || {};
          let options = this.cachedQueries[key];
          if (!options) {
            options = this.cachedQueries[key] = await http.get('permissible-values/v', queryParams);
          }

          return options;
        },
        selectProp: this.selectProp,
        displayProp: 'value'
      }
    }
  },

  computed: {
    inputValue: {
      get() {
        if (this.modelValue != null && !isNaN(this.modelValue)) {
          return +this.modelValue;
        }

        return this.modelValue;
      },

      set(value) {
        this.$emit('update:modelValue', value);
      }
    }
  },

  methods: {
  }
}
</script>
