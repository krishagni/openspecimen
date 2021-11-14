
<template>
  <Dropdown v-model="inputValue" :list-source="listSource" />
</template>

<script>
import Dropdown from '@/common/components/Dropdown.vue';

import http from '@/common/services/HttpClient.js';
import util from '@/common/services/Util.js';

export default {
  props: ['modelValue', 'selectProp', 'context'],

  components: {
    Dropdown
  },

  data() {
    return {
      listSource: {
        loadFn: async (opts) => {
          let cache = (this.context && this.context._formCache) || {};
          cache = cache['storage-container'] = cache['storage-container'] || {};

          let containers = [];
          opts = Object.assign( { name: opts.query || '' }, opts || {maxResults: 100});
          if (opts.value || opts.value == 0) {
            let id = parseInt(opts.value);
            if (!isNaN(id)) {
              containers = cache[id];
              if (!containers) {
                containers = cache[id] = [await http.get('storage-containers/' + id)];
              }
            } else {
              containers = cache[opts.value];
              if (!containers) {
                containers = cache[opts.value] = [await http.get('storage-containers/byname/' + opts.value)];
              }
            }
          } else {
            const qs = util.queryString(opts);
            containers = cache[qs];
            if (!containers) {
              containers = cache[qs] = await http.get('storage-containers', opts);
            }
          }

          return containers;
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
  }
}
</script>
