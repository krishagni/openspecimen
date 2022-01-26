
<template>
  <span v-if="multiple">
    <MultiSelectDropdown v-model="inputValue" :list-source="ddListSource" :disabled="disabled"
      :md-type="$attrs['md-type']" :placeholder="$attrs['placeholder']" />
  </span>
  <span v-else>
    <Dropdown v-model="inputValue" :list-source="ddListSource" :disabled="disabled"
      :md-type="$attrs['md-type']" :placeholder="$attrs['placeholder']" />
  </span>
</template>

<script>
import Dropdown from '@/common/components/Dropdown.vue';
import MultiSelectDropdown from '@/common/components/MultiSelectDropdown.vue';

import http from '@/common/services/HttpClient.js';
import util from '@/common/services/Util.js';
import exprUtil from '@/common/services/ExpressionUtil.js';

export default {
  props: ['modelValue', 'selectProp', 'listSource', 'context', 'multiple', 'disabled'],

  components: {
    Dropdown,
    MultiSelectDropdown
  },

  data() {
    return {
      ddListSource: {
        loadFn: async (opts) => {
          let cache = (this.context && this.context._formCache) || {};
          cache = cache['user-dropdown'] = cache['user-dropdown'] || {};

          opts = opts || {maxResults: 100};
          if (opts.value || opts.value == 0) {
            let id = parseInt(opts.value);
            if (!isNaN(id)) {
              if (!cache[id]) {
                cache[id] = await http.get('users/' + id);
              }

              return [cache[id]];
            }
          }

          let params = Object.assign({searchString: opts.query || ''}, opts || {maxResults: 100});
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

          const qs = util.queryString(params);
          if (!cache[qs]) {
            cache[qs] = await http.get('users', params);
          }

          return cache[qs];
        },
        selectProp: this.selectProp || (this.listSource && this.listSource.selectProp),
        displayProp: (user) => user.firstName + ' ' + user.lastName
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
