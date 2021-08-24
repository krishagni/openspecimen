
<template>
  <Dropdown v-model="inputValue" :list-source="listSource" />
</template>

<script>
import Dropdown from '@/common/components/Dropdown.vue';

import http from '@/common/services/HttpClient.js';

export default {
  props: ['modelValue', 'selectProp', 'attribute', 'leafValue'],

  components: {
    Dropdown
  },

  data() {
    return {
      listSource: {
        loadFn: (opts) => {
          opts = Object.assign(
            {
              searchString: opts.query || '',
              attribute: this.attribute,
              includeOnlyLeafValue: this.leafValue
            },
            opts || {maxResults: 100}
          );

          if (opts.value || opts.value == 0) {
            try {
              let id = parseInt(opts.value);
              return http.get('permissible-values/v/' + id).then((pv) => [pv]);
            } catch {
              return http.get('permissible-values/v', opts);
            }
          } else {
            return http.get('permissible-values/v', opts);
          }
        },
        selectProp: this.selectProp,
        displayProp: 'value'
      }
    }
  },

  computed: {
    inputValue: {
      get() {
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
