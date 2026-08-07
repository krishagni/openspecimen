
<template>
  <MultiSelectDropdown ref="msDd" v-model="inputValue" :list-source="listSource"
    :tab-order="tabOrder" v-if="multiple" />
  <Dropdown ref="ssDd" v-model="inputValue" :list-source="listSource" :tab-order="tabOrder" v-else />
</template>

<script>
import Dropdown from '@/common/components/Dropdown.vue';
import MultiSelectDropdown from '@/common/components/MultiSelectDropdown.vue';

import http from '@/common/services/HttpClient.js';
import util from '@/common/services/Util.js';

export default {
  props: ['modelValue', 'selectProp', 'context', 'multiple', 'tabOrder'],

  components: {
    Dropdown,
    MultiSelectDropdown
  },

  data() {
    return {
      listSource: {
        loadFn: async (opts) => {
          let cache = (this.context && this.context._formCache) || {};
          cache = cache['storage-container'] = cache['storage-container'] || {};

          let containers = [];
          opts = Object.assign( { name: opts.query || '' }, opts || {maxResults: 100});
          if (opts.id instanceof Array) {
            return await util.getObjects(
              cache, params => http.get('storage-containers', params), {id: opts.id}
            );
          }

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
            containers = await util.getObjects(cache, params => http.get('storage-containers', params), opts);
          }

          return containers;
        },
        selectProp: this.selectProp,
        valueProp: this.selectProp == 'id' ? 'id' : undefined,
        displayProp: 'name'
      }
    }
  },

  computed: {
    inputValue: {
      get() {
        if (this.multiple) {
          if (this.listSource.selectProp == 'id' && this.modelValue instanceof Array) {
            return this.modelValue.map(value => value != null && !isNaN(value) ? +value : value);
          }

          return this.modelValue;
        }

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
