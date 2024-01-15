
<template>
  <MultiSelectDropdown ref="msDd" v-model="inputValue" :list-source="listSource"
    :tab-order="tabOrder" v-if="multiple" />
  <Dropdown ref="ssDd" v-model="inputValue" :list-source="listSource"
    :tab-order="tabOrder" v-else />
</template>

<script>
import Dropdown from '@/common/components/Dropdown.vue';
import MultiSelectDropdown from '@/common/components/MultiSelectDropdown.vue';

import http from '@/common/services/HttpClient.js';
import util from '@/common/services/Util.js';

export default {
  props: ['modelValue', 'selectProp', 'attribute', 'leafValue', 'context', 'multiple', 'tabOrder'],

  components: {
    Dropdown,
    MultiSelectDropdown
  },

  data() {
    return {
      listSource: {
        loadFn: async (opts) => {
          let pvAttr = this.attribute;
          if (pvAttr == 'vital-status') {
            pvAttr = 'vital_status';
          }

          let queryParams = Object.assign(
            {
              searchString: opts.query || '',
              attribute: pvAttr,
              includeOnlyLeafValue: this.leafValue == true
            },
            opts || {maxResults: 100}
          );

          let cache = (this.context && this.context._formCache) || {};
          cache = cache['pvs'] = cache['pvs'] || {};

          if (opts.value || opts.value == 0) {
            try {
              let id = +opts.value;
              if (!isNaN(id)) {
                let key = 'id: ' + id;
                if (!cache[key]) {
                  cache[key] = http.get('permissible-values/v/' + id).then(pv => [pv]);
                }

                return await cache[key];
              }
            } catch {
              console.log('PvDropdown: Error getting value: ' + opts.value);
            }
          }

          let key = util.queryString(queryParams);
          if (!cache[key]) {
            cache[key] = http.get('permissible-values/v', queryParams);
          }

          return await cache[key];
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
    getDisplayValue() {
      return this.multiple ? this.$refs.msDd.getDisplayValue() : this.$refs.ssDd.getDisplayValue();
    }
  }
}
</script>
