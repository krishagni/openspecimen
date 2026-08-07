
<template>
  <span v-if="multiple">
    <MultiSelectDropdown ref="msDd" v-model="inputValue" :list-source="ddListSource" :disabled="disabled"
      :tabOrder="tabOrder" :md-type="$attrs['md-type']" :placeholder="$attrs['placeholder']" />
  </span>
  <span v-else>
    <Dropdown ref="ssDd" v-model="inputValue" :list-source="ddListSource" :disabled="disabled"
      :tabOrder="tabOrder" :md-type="$attrs['md-type']" :placeholder="$attrs['placeholder']"
      :context="context" :unique="unique" />
  </span>
</template>

<script>
import Dropdown from '@/common/components/Dropdown.vue';
import MultiSelectDropdown from '@/common/components/MultiSelectDropdown.vue';

import http     from '@/common/services/HttpClient.js';
import exprUtil from '@/common/services/ExpressionUtil.js';
import util     from '@/common/services/Util.js';

export default {
  props: ['modelValue', 'selectProp', 'listSource', 'context', 'multiple', 'disabled', 'tabOrder', 'unique'],

  components: {
    Dropdown,
    MultiSelectDropdown
  },

  data() {
    return {
      ddListSource: {
        loadFn: async (opts) => {
          let cache = (this.context && this.context._formCache) || {};
          cache = cache['site-dropdown'] = cache['site-dropdown'] || {};

          const selectProp = this.selectProp || (this.listSource && this.listSource.selectProp);
          if (selectProp == 'id' && opts.id instanceof Array) {
            return await util.getObjects(cache, params => http.get('sites', params), {id: opts.id});
          }

          if (selectProp == 'id' && (opts.name || opts.name == 0)) {
            const id = parseInt(opts.name);
            if (!isNaN(id)) {
              let promise = cache[id];
              if (!promise) {
                promise = cache[id] = http.get('sites/byid/' + id);
              }

              return promise.then(site => [site]);
            }
          }

          let params = Object.assign({name: opts.query || ''}, opts || {maxResults: 100});
          let ls = this.listSource || {};
          let qp = ls.queryParams || {};
          if (qp.static) {
            Object.keys(qp.static).forEach(name => params[name] = qp.static[name]);
          }

          if (qp.dynamic && this.context) {
            Object.keys(qp.dynamic).forEach(name => params[name] = exprUtil.eval(this.context, qp.dynamic[name]));
          }

          if (this.listAll) {
            params.listAll = true;
          }

          return await util.getObjects(cache, params => http.get('sites', params), params);
        },
        selectProp: this.selectProp || (this.listSource && this.listSource.selectProp),
        valueProp: (this.selectProp || (this.listSource && this.listSource.selectProp)) == 'id' ? 'id' : undefined,
        displayProp: 'name',
        searchProp: 'name',
        idProp: this.listSource && this.listSource.idProp
      }
    }
  },

  computed: {
    inputValue: {
      get() {
        if (this.multiple) {
          if (this.ddListSource.selectProp == 'id' && this.modelValue instanceof Array) {
            return this.modelValue.map(value => value != null && !isNaN(value) ? +value : value);
          }

          return this.modelValue;
        }

        if (this.ddListSource.selectProp != 'name' && this.modelValue != null && !isNaN(this.modelValue)) {
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
