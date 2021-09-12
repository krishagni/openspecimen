
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
          opts = Object.assign( { name: opts.query || '' }, opts || {maxResults: 100});
          if (opts.value || opts.value == 0) {
            try {
              let id = parseInt(opts.value);
              return http.get('storage-containers/' + id).then((container) => [container]);
            } catch {
              return http.get('storage-containers/byname/' + opts.value).then((container) => [container]);
            }
          } else {
            let key = util.queryString(opts);
            this.cachedQueries = this.cachedQueries || {};
            let containers = this.cachedQueries[key];
            if (!containers) {
              containers = this.cachedQueries[key] = await http.get('storage-containers', opts);
            }

            return containers;
          }
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
