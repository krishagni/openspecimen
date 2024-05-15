
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
        loadFn: ({_selected}) => {
          const cache = this._getCache();
          const displayLabel = typeof ls.displayLabel == 'function' ? ls.displayLabel : this._displayLabel;

          let promise;
          if (_selected) {
            promise = this._getSelectedReq(_selected);
          } else {
            const params = {includeChildReqs: false, ...util.queryParams(this.form || this.context || {}, ls || {})};
            if (!params['eventId'] && (!params['cpId'] || !params['eventLabel'])) {
              promise = util.promise([]);
            } else {
              const qs = util.queryString(Object.assign(params || {}));
              promise = cache[qs];
              if (!promise) {
                promise = cache[qs] = http.get('specimen-requirements', params);
              }
            }
          }

          return promise.then(
            (reqs) => {
              if (!reqs || reqs.length == 0) {
                reqs = ls.options || [];
              } else if (ls.includeOptions) {
                reqs = reqs.concat(ls.options || []);
              }

              reqs.forEach(req => req.displayLabel = displayLabel({sr: req}));
              return reqs;
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
      cache = cache['reqsList'] = cache['reqsList'] || {};
      return cache;
    },

    _displayLabel({sr}) {
      let label = sr.name || sr.displayLabel;
      if (!label) {
        label = sr.type;
        if (sr.code) {
          label += ' (' + sr.code + ')';
        }
      }

      return label;
    },

    _updateValue(value) {
      if (this.entity) {
        const cp = exprUtil.eval(this.form || this.context || {}, 'cp') || {};
        const entityObj = exprUtil.eval(this.form || this.context || {}, this.entity) || {};
        if (!value) {
          entityObj.reqId = null;
          this._presetValues(cp, entityObj, {lineage: entityObj.lineage});
        } else if (value < 0) {
          const req = (this.listSource.options || []).find(req => req.id == value);
          this._presetValues(cp, entityObj, req || {lineage: entityObj.lineage});
        } else {
          this._getSelectedReq(value).then(
            (reqs) => {
              if (reqs.length != 0) {
                this._presetValues(cp, entityObj, reqs[0]);
              }
            }
          );
        }
      }
    },

    _presetValues(cp, entityObj, req) {
      const attrs = [
        'specimenClass', 'type', 'lineage', 'anatomicSite', 'laterality',
        'pathologyStatus', 'initialQty', 'concentration', 'labelFmt'
      ];

      attrs.forEach(attr => entityObj[attr] = req[attr]);
      if (req.lineage == 'New') {
        entityObj.collectionEvent = entityObj.collectionEvent || {};
        entityObj.collectionEvent.user = req.collector;
        entityObj.collectionEvent.container = req.collectionContainer;

        entityObj.receivedEvent = entityObj.receivedEvent || {};
        entityObj.receivedEvent.user = req.receiver;
      } else {
        delete entityObj.collectionEvent;
        delete entityObj.receivedEvent;
      }

      if (!req.labelFmt) {
        if (req.lineage == 'Aliquot') {
          entityObj.labelFmt = cp.aliquotLabelFmt;
        } else if (req.lineage == 'Derived') {
          entityObj.labelFmt = cp.derivativeLabelFmt;
        } else {
          entityObj.labelFmt = cp.specimenLabelFmt;
        }
      }
    },

    _getSelectedReq(value) {
      const oq    = 'id:' + value;
      const cache = this._getCache();
      if (!cache[oq]) {
        if (value > 0) {
          cache[oq] = http.get('specimen-requirements/' + value).then(req => [req]);
        } else {
          const req = (this.listSource.options || []).find(r => r.id == value);
          cache[oq] = util.promise([req]);
        }
      }

      return cache[oq];
    }
  }
}
</script>
