<template>
  <os-dropdown
    ref="dd"
    v-model="inputValue"
    :list-source="ddLs"
    :form="form"
    :context="context"
    :tab-order="tabOrder" />
</template>

<script>

import http from '@/common/services/HttpClient.js';
import util from '@/common/services/Util.js';

export default {
  props: ['modelValue', 'listSource', 'form', 'context', 'tabOrder'],

  emits: ['update:modelValue'],

  data() {
    return { };
  },

  computed: {
    inputValue: {
      get() {
        return this.modelValue;
      },

      set(value) {
        this.$emit('update:modelValue', value);
      }
    },

    ddLs: function() {
      const ls = this.listSource;
      return {
        loadFn: ({context, query, maxResults, _selected}) => {
          let cache = (context || {})._formCache || {};
          cache = cache['cpList'] = cache['cpList'] || {};

          let promise;
          if (_selected) {
            const oq = (ls.selectProp || 'query') + ':' + _selected;
            if (!cache[oq]) {
              if (ls.selectProp == 'id') {
                cache[oq] = http.get('collection-protocols/' + _selected).then(cp => [cp]);
              } else {
                cache[oq] = http.get('collection-protocols', {query: _selected});
              }
            }

            promise = cache[oq];
          } else {
            const params = {query, maxResults, ...util.queryParams(this.form || this.context || {}, ls || {})};
            const qs = util.queryString(params || {});
            promise = cache[qs];
            if (!promise) {
              promise = cache[qs] = http.get('collection-protocols', params);
            }
          }

          const displayLabel = typeof ls.displayLabel == 'function' ? ls.displayLabel : this._displayLabel;
          return promise.then(
            (cps) => {
              cps.forEach(cp => cp.displayLabel = displayLabel({cp}));
              return cps;
            }
          );
        },

        displayProp: 'displayLabel',

        selectProp: ls.selectProp
      }
    },
  },

  methods: {
    getDisplayValue: function() {
      return this.$refs.dd.getDisplayValue();
    },

    _displayLabel({cp}) {
      let label = cp.shortTitle;
      if (cp.code) {
        label += ' (' + cp.code + ')';
      }

      return label;
    }
  }
}
</script>
