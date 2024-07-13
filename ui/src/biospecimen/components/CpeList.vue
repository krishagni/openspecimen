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
    this.seedObj = util.clone(this._getFormDataItem(this.entity) || {});
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
        loadFn: ({_selected}) => {
          const cache = this._getCache();

          let promise;
          if (_selected) {
            promise = this._getSelectedEvent(_selected);
          } else {
            const params = {...util.queryParams(this.form || this.context || {}, ls || {})};
            if (!params['cpId']) {
              console.log('Unknown CP. Returning empty events list.');
              promise = util.promise([]);
            } else {
              const qs = util.queryString(params);
              promise = cache[qs];
              if (!promise) {
                promise = cache[qs] = http.get('collection-protocol-events', params);
              }
            }
          }

          const displayLabel = typeof ls.displayLabel == 'function' ? ls.displayLabel : this._displayLabel;
          return promise.then(
            (events) => {
              if (!events || events.length == 0) {
                events = ls.options || [];
              } else if (ls.includeOptions) {
                events = events.concat(ls.options || []);
              }

              events.forEach(event => event.displayLabel = displayLabel({event: event}));
              return events;
            }
          );
        },

        displayProp: 'displayLabel',

        selectProp: 'id'
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
      cache = cache['cpeList'] = cache['cpeList'] || {};
      return cache;
    },

    _displayLabel({event}) {
      let label = event.eventLabel;
      if (event.eventPoint != undefined && event.eventPoint != null) {
        label = ((event.eventPoint < 0) ? '-T' : 'T') + Math.abs(event.eventPoint) +
          event.eventPointUnit.charAt(0) + ': ' + label;
      }

      return label;
    },

    _updateValue(value) {
      if (this.entity) {
        const entityObj = this._getFormDataItem(this.entity) || {};
        if (!value) {
          entityObj.eventId = null;
          this._presetValues(entityObj, {});
        } else {
          this._getSelectedEvent(value).then(
            (events) => {
              if (events.length != 0) {
                this._presetValues(entityObj, events[0]);
              }
            }
          );
        }
      }
    },

    _presetValues(entityObj, event) {
      const {clinicalDiagnoses, clinicalStatus, site} = this.seedObj;
      if (clinicalDiagnoses && clinicalDiagnoses.length > 0) {
        entityObj['clinicalDiagnoses'] = clinicalDiagnoses;
      } else {
        entityObj['clinicalDiagnoses'] = (event.clinicalDiagnosis && [event.clinicalDiagnosis]) || [];
      }

      entityObj['clinicalStatus'] = clinicalStatus || event.clinicalStatus;
      entityObj['site'] = site || event.defaultSite;
    },

    _getSelectedEvent(value) {
      const oq    = 'id:' + value;
      const cache = this._getCache();
      if (!cache[oq]) {
        if (value > 0) {
          cache[oq] = http.get('collection-protocol-events/' + value).then(cpe => [cpe]);
        } else {
          const event = (this.listSource.options || []).find(e => e.id == value);
          cache[oq] = util.promise([event]);
        }
      }

      return cache[oq];
    },

    _getFormDataItem(attr) {
      const form = this.form || this.context || {};
      if (typeof form.fd == 'function') {
        return form.fd(attr);
      }

      return exprUtil.getValue(form, attr);
    }
  }
}
</script>
