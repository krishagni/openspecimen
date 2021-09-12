
<template>
  <Dropdown v-model="inputValue" :list-source="listSource" />
</template>

<script>
import Dropdown from '@/common/components/Dropdown.vue';

import http from '@/common/services/HttpClient.js';
import util from '@/common/services/Util.js';

export default {
  props: ['modelValue', 'selectProp'],

  components: {
    Dropdown
  },

  data() {
    return {
      listSource: {
        loadFn: async (opts) => {
          let queryParams = opts = Object.assign({name: opts.query || ''}, opts || {maxResults: 100});
          if (opts.value || opts.value == 0) {
            try {
              let id = parseInt(opts.value);
              return http.get('sites/' + id).then((site) => [site]);
            } catch {
              queryParams = Object.assign(Object.assign({}, opts), {name: opts.value, exactMatch: true});
            }
          }

          let key = util.queryString(queryParams);
          this.cachedQueries = this.cachedQueries || {};
          let options = this.cachedQueries[key];
          if (!options) {
            this.cachedQueries[key] = options = await http.get('sites', queryParams);
          }

          return options;
        },
        selectProp: this.selectProp,
        displayProp: 'name'
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
