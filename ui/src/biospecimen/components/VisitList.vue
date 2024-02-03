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

import exprUtil from '@/common/services/ExpressionUtil.js';
import http     from '@/common/services/HttpClient.js';
import util     from '@/common/services/Util.js';

export default {
  props: ['modelValue', 'listSource', 'form', 'context', 'entity', 'tabOrder'],

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
        loadFn: ({query, maxResults, _selected}) => {
          const cache = this._getCache();
          const displayLabel = typeof ls.displayLabel == 'function' ? ls.displayLabel : this._displayLabel;

          let promise;
          if (_selected) {
            promise = this._getSelectedVisit(_selected);
          } else {
            const params = {query, maxResults, ...util.queryParams(this.form || this.context || {}, ls || {})};
            if (!params['cprId']) { // for now only CPR visits
              promise = util.promise([]);
            } else {
              const qs = util.queryString(Object.assign(params || {}));
              promise = cache[qs];
              if (!promise) {
                promise = cache[qs] = http.get('visits', params);
              }
            }
          }

          return promise.then(
            (visits) => {
              const filterVisits = typeof ls.uiFilter == 'function' ? ls.uiFilter : () => true;
              visits = visits.filter(filterVisits);
              visits.forEach(visit => visit.displayLabel = displayLabel({visit}));
              return visits;  
            }
          );
        },

        displayProp: 'displayLabel',

        selectProp: ls.selectProp
      }
    },
  },

  watch: {
    inputValue: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this._updateValue(newVal);
      }
    }
  },

  methods: {
    getDisplayValue: function() {
      return this.$refs.dd.getDisplayValue();
    },

    _getCache() {
      let cache = (this.form || this.context || {})._formCache || {};
      cache = cache['visitList'] = cache['visitList'] || {};
      return cache;
    },

    _displayLabel({visit}) {
      let label = visit.eventLabel;
      if (!label) {
        label = 'Unplanned';
      }

      label += ' (' + util.formatDate(new Date(visit.visitDate), window.osUi.global.locale.shortDateFmt) + ')';
      return label;
    },

    _updateValue(value) {
      if (this.entity) {
        const entityObj = exprUtil.getValue(this.form || this.context || {}, this.entity) || {};
        if (!value) {
          entityObj.eventId = entityObj.eventLabel = null;
        } else {
          this._getSelectedVisit(value).then(
            (visits) => {
              if (visits.length != 0) {
                entityObj.eventId    = visits[0].eventId;
                entityObj.eventLabel = visits[0].eventLabel;
              }
            }
          );
        }
      }
    },

    _getSelectedVisit(value) {
      const ls    = this.listSource;
      const oq    = (ls.selectProp || 'query') + ':' + value;
      const cache = this._getCache();
      if (!cache[oq]) {
        if (ls.selectProp == 'id') {
          cache[oq] = http.get('visits/' + value).then(visit => [visit]);
        } else if (ls.selectProp == 'visitName' || ls.selectProp == 'sprNumber') {
          const params = {};
          params[ls.selectProp] = value;
          cache[oq] = http.get('visits', params);
        } else {
          cache[oq] = util.promise([]);
        }
      }

      return cache[oq];
    }
  }
}
</script>
