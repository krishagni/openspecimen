<template>
  <os-popover ref="addEditOverlay" :align="align">
    <div class="filter-po">
      <div class="title">
        <span v-if="ctx.filter.id > 0" v-t="'queries.edit_filter'">Edit Filter</span>
        <span v-else v-t="'queries.add_filter'">Add Filter</span>
      </div>

      <os-form ref="addEditFilterForm" :schema="addEditFilterFs" :data="ctx" @input="handleInput">
        <div>
          <os-button primary :label="$t('common.buttons.edit')" v-if="ctx.filter.id > 0" @click="saveFilter" />
          <os-button primary :label="$t('common.buttons.add')" v-else @click="saveFilter" />
          <os-button :label="$t('common.buttons.cancel')" @click="cancel" />
        </div>
      </os-form>
    </div>
  </os-popover>
</template>

<script>
import {format, parse} from 'date-fns';

import formSvc from '@/forms/services/Form.js';
import pvSvc   from '@/common/services/PermissibleValue.js';
import siteSvc from '@/administrative/services/Site.js';
import userSvc from '@/administrative/services/User.js';
import util    from '@/common/services/Util.js';

import addEditFilter from '@/queries/schemas/addedit-filter.js';

export default {
  props: ['align'],

  data() {
    return {
      ctx: {
        filter: {},

        valueType: 'none',

        getAllowedOps: async () => this._getAllowedOps(),

        getPvs: async (searchString) => this._getPvs(searchString)
      },

      addEditFilterFs: addEditFilter.layout
    }
  },

  methods: {
    open: function(event, filter) {
      const self = this;
      this.ctx.filter       = util.clone(filter);
      this.ctx.filter.hasSq = filter.subQueryId > 0;

      this._setValueType();
      return new Promise((resolve) => {
        self.ctx.resolve = resolve;
        self.$refs.addEditOverlay.open(event);
      });
    },

    handleInput: function({field}) {
      if (field.name == 'filter.op') {
        this.ctx.filter.hasSq = false;
        this.ctx.filter.subQuery = this.ctx.filter.subQueryId = null;
        this.ctx.filter.values = [];
        this._setValueType();
      } else if (field.name == 'filter.hasSq') {
        this.ctx.filter.subQuery = this.ctx.filter.subQueryId = null;
        this.ctx.filter.values = [];
        this._setValueType();
      }
    },

    saveFilter: function() {
      if (!this.$refs.addEditFilterForm.validate()) {
        return;
      }

      const filter = util.clone(this.ctx.filter);

      const {valueType} = this.ctx;
      if (valueType == 'none') {
        filter.values = [];
        if (filter.hasSq && filter.subQuery) {
          filter.subQueryId = filter.subQuery.id;
        }
      } else if (valueType == 'textarea') {
        filter.values = util.splitStr(filter.valuesCsv, /,|\t|\n/, true);
      } else if (valueType == 'dropdown') {
        filter.values = [filter.valuePv];
      } else if (valueType == 'dateRange') {
        filter.values = [this._formatDate(filter.minDateValue), this._formatDate(filter.maxDateValue)];
      } else if (valueType == 'numericRange') {
        filter.values = [filter.minNumValue, filter.maxNumValue];
      } else if (valueType == 'date') {
        filter.values = [this._formatDate(filter.dateValue)];
      } else if (valueType != 'multiselect') {
        filter.values = [filter.value];
      }

      const toDelete = [
        'valuesCsv', 'valuePv', 'minDateValue', 'maxDateValue',
        'minNumValue', 'maxNumValue', 'dateValue', 'value'
      ];
      for (const attr of toDelete) {
        delete filter[attr];
      }

      this.ctx.resolve(filter);
      this.$refs.addEditOverlay.close();
    },

    cancel: function() {
      this.$refs.addEditOverlay.close();
    },

    _getAllowedOps: function() {
      const {fieldObj} = this.ctx.filter || {};
      if (fieldObj && fieldObj.type == 'STRING') {
        return this._getStringOps();
      } else {
        return this._getNumericOps();
      }
    },

    _getStringOps: function() {
      return this._getOps([
        'EQ', 'NE', 'EXISTS', 'NOT_EXISTS',
        'ANY', 'STARTS_WITH', 'ENDS_WITH',
        'CONTAINS', 'IN', 'NOT_IN'
      ]);
    },

    _getNumericOps: function() {
      return this._getOps([
        'EQ', 'NE', 'LT', 'LE', 'GT', 'GE',
        'EXISTS', 'NOT_EXISTS', 'ANY',
        'IN', 'NOT_IN', 'BETWEEN'
      ]);
    },

    _getOps: function(ops) {
      return ops.map(op => ({name: op, label: this.$t('queries.op.' + op)}));
    },

    _getPvs: function(searchTerm) {
      searchTerm = searchTerm || '';

      const {fieldObj} = this.ctx.filter || {};
      if (fieldObj.pvs) {
        if (fieldObj.pvs.length < 100) {
          return fieldObj.pvs.map(pv => ({name: pv, value: pv}));
        } else {
          let [formName, ...fieldParts] = fieldObj.id.split('.');
          let fieldName = fieldParts.join('.');
          if (fieldParts[0] == 'extensions' || fieldParts[0] == 'customFields') {
            fieldParts.shift(); // consume 'extensions' or 'customFields'
            formName = fieldParts.shift();
            fieldName = fieldParts.join('.');
          }

          return formSvc.getPvsByName(formName, fieldName, searchTerm)
            .then(pvs => pvs.map(({value}) => ({name: value, value})));
        }
      } else {
        const {apiUrl, filters} = fieldObj.lookupProps || {apiUrl: '', filters: {}};
        if (apiUrl.indexOf('permissible-values') >= 0) {
          return pvSvc.getPvs(filters.attribute, searchTerm, filters).then(
            pvs => pvs.map(({value}) => ({name: value, value}))
          );
        } else if (apiUrl.indexOf('users') >= 0) {
          return userSvc.getUsers({...filters, searchString: searchTerm}).then(
            users => users.map(({firstName, lastName}) => ({name: firstName + ' ' + lastName, value: firstName + ' ' + lastName}))
          );
        } else if (apiUrl.indexOf('sites') >= 0) {
          return siteSvc.getSites({...filters, name: searchTerm}).then(
            sites => sites.map(site => ({name: site.name, value: site.name}))
          );
        }

        return [];
      }
    },

    _setValueType: function() {
      const {op, fieldObj, values, hasSq} = this.ctx.filter;

      if (!op || op == 'ANY' || op == 'EXISTS' || op == 'NOT_EXISTS') {
        this.ctx.valueType = 'none';
        this.ctx.filter.values = [];
      } else if (op == 'IN' || op == 'NOT_IN') {
        if (fieldObj.lookupProps || fieldObj.pvs) {
          this.ctx.valueType = 'multiselect';
          this.ctx.filter.values = values || [];
        } else if (hasSq) {
          this.ctx.valueType = 'none';
          this.ctx.filter.values = [];
        } else {
          this.ctx.valueType = 'textarea';
          this.ctx.filter.valuesCsv = (values && values.join(',')) || '';
        }
      } else if ((fieldObj.lookupProps || fieldObj.pvs) && ['CONTAINS', 'STARTS_WITH', 'ENDS_WITH'].indexOf(op) < 0) {
        this.ctx.valueType = 'dropdown';
        this.ctx.filter.valuePv = (values && values.length > 0 && values[0]) || undefined;
      } else if (op == 'BETWEEN') {
        if (fieldObj.type == 'DATE') {
          this.ctx.valueType = 'dateRange';
          this.ctx.filter.minDateValue = this._parseDate((values && values.length > 0 && values[0]));
          this.ctx.filter.maxDateValue = this._parseDate((values && values.length > 1 && values[1]));
        } else {
          this.ctx.valueType = 'numericRange';
          this.ctx.filter.minNumValue = (values && values.length > 0 && values[0]) || undefined;
          this.ctx.filter.maxNumValue = (values && values.length > 1 && values[1]) || undefined;
        }
      } else if (fieldObj.type == 'DATE') {
        this.ctx.valueType = 'date';
        this.ctx.filter.dateValue = this._parseDate((values && values.length > 0 && values[0]));
      } else {
        this.ctx.valueType = 'text';
        this.ctx.filter.value = (values && values.length > 0 && values[0]) || undefined;
      }
    },

    _parseDate: function(input) {
      if (!input) {
        return null;
      }

      return parse(input, this.$ui.global.locale.shortDateFmt, new Date());
    },

    _formatDate: function(input) {
      if (!input) {
        return null;
      }

      return format(input, this.$ui.global.locale.shortDateFmt);
    }
  }
}
</script>

<style scoped>
.filter-po {
  width: 350px;
  margin: -1.25rem;
}

.filter-po .title {
  background: #efefef;
  color: #212529;
  padding: 0.5rem 1rem;
}

.filter-po :deep(form) {
  max-width: 100%;
}

.filter-po :deep(.p-inputtext) {
  max-width: 100%;
}
</style>
