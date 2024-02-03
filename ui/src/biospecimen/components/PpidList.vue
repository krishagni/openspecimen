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
        loadFn: ({context, query:searchStr, maxResults, _selected}) => {
          let cache = (context || {})._formCache || {};
          cache = cache['ppidList'] = cache['ppidList'] || {};

          const displayLabel = typeof ls.displayLabel == 'function' ? ls.displayLabel : this._displayLabel;
          let promise;
          if (_selected) {
            const oq = (ls.selectProp || 'searchStr') + ':' + _selected;
            if (!cache[oq]) {
              const params = {};
              if (ls.selectProp == 'id') {
                params.ids = [_selected];
              } else {
                params[ls.selectProp || 'searchStr'] = _selected;
              }
              
              cache[oq] = http.post('collection-protocol-registrations/list', params);
            }

            promise = cache[oq];
          } else {
            const params = {searchStr, maxResults, ...util.queryParams(this.form || this.context || {}, ls || {})};
            const qs = util.queryString(Object.assign(params || {}));
            promise = cache[qs];
            if (!promise) {
              promise = cache[qs] = http.post('collection-protocol-registrations/list', params);
            } 
          }

          return promise.then(
            (cprsList) => {
              cprsList.forEach(cpr => cpr.displayLabel = displayLabel({cpr}));
              return cprsList;  
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

    _displayLabel({cpr}) {
      let label = cpr.ppid;
      if (cpr.participant.firstName || cpr.participant.lastName) {
        let name = cpr.participant.firstName;
        if (cpr.participant.lastName) {
          if (name) {
            name += ' ' + cpr.participant.lastName;
          } else {
            name = cpr.participant.lastName;
          }
        }

        if (name) {
          label += ' (' + name + ')';
        }
      }

      return label;
    }
  }
}
</script>
