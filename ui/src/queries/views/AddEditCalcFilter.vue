<template>
  <os-dialog ref="addEditDialog">
    <template #header>
      <div>
        <span v-if="ctx.filter.id > 0" v-t="'queries.edit_filter'">Edit Filter</span>
        <span v-else v-t="'queries.add_filter'">Add Filter</span>
      </div>
    </template>

    <template #content>
      <div>
        <div class="toolbar">
          <os-button-group>
            <os-menu label="𝐹𝑛" :options="dateFns" />
            <os-button label="{𝑥0,𝑥1,𝑥2,𝑥3,…}" right-icon="caret-down" @click="toggleFieldOptions" />
          </os-button-group>
        </div>

        <os-form ref="addEditFilterForm" :schema="addEditFilterFs" :data="ctx" />
      </div>

      <os-overlay class="fields-list" ref="fieldsList">
        <ul class="search">
          <li>
            <os-input-text v-model="ctx.searchTerm" :placeholder="$t('common.buttons.search')"
              @update:modelValue="searchFields" />
          </li>
        </ul>
        <ul class="fields" v-if="fields && fields.length > 0">
          <li v-for="field of fields" :key="field.id" @click="insertFieldExpr($event, field)">
            <a> {{field.caption}} </a>
          </li>
        </ul>
      </os-overlay>
    </template>

    <template #footer>
      <div>
        <os-button :label="$t('common.buttons.cancel')" @click="cancel" />
        <os-button primary :label="$t('common.buttons.edit')" v-if="ctx.filter.id > 0" @click="saveFilter" />
        <os-button primary :label="$t('common.buttons.add')" v-else @click="saveFilter" />
      </div>
    </template>
  </os-dialog>
</template>

<script>

import formsCache from '@/queries/services/FormsCache.js';
import util from '@/common/services/Util.js';

import addEditFilter from '@/queries/schemas/addedit-calc-filter.js';

export default {
  props: ['align'],

  data() {
    return {
      ctx: {
        filter: {},

        searchTerm: '',

        fields: []
      },

      addEditFilterFs: addEditFilter.layout
    }
  },

  mounted() {
  },

  computed: {
    dateFns: function() {
      const t = (fn) => this.$t('queries.fns.' + fn);
      return [
        {caption: t('current_date'),    onSelect: () => this.insertExpr('current_date()')},
        {caption: t('months_between'),  onSelect: () => this.insertExpr('months_between(<minuend>, <subtrahend>)')},
        {caption: t('years_between'),   onSelect: () => this.insertExpr('years_between(<minuend>, <subtrahend>)')},
        {caption: t('minutes_between'), onSelect: () => this.insertExpr('minutes_between(<minuend>, <subtrahend>)')},
        {caption: t('round'),           onSelect: () => this.insertExpr('round(<number>, <digits_after_decimal>)')},
        {caption: t('date_range'),      onSelect: () => this.insertExpr('date_range(<date>, <range_type>, [range=1])')}
      ];
    },

    fields: function() {
      const searchTerm = (this.ctx.searchTerm || '').toLowerCase();
      if (searchTerm) {
        return this.ctx.fields.filter(field => field.caption.toLowerCase().indexOf(searchTerm) >= 0);
      }

      return this.ctx.fields;
    }
  },

  methods: {
    open: function(event, cpId, cpGroupId, filter) {
      const self = this;
      this.ctx.filter = util.clone(filter || {});
      this.ctx.fields = [];
      return new Promise((resolve) => {
        self.ctx.resolve = resolve;
        self.$refs.addEditDialog.open();
      });
    },

    saveFilter: function() {
      if (!this.$refs.addEditFilterForm.validate()) {
        return;
      }

      const filter = util.clone(this.ctx.filter);
      this.ctx.resolve(filter);
      this.$refs.addEditDialog.close();
    },

    cancel: function() {
      this.$refs.addEditDialog.close();
    },

    toggleFieldOptions: function(event) {
      this._loadFields();
      this.$refs.fieldsList.show(event);
    },

    insertExpr: function(expr) {
      const [txtEl] = this.$refs.addEditFilterForm.$el.getElementsByTagName('textarea');
      const start   = txtEl.selectionStart;
      const end     = txtEl.selectionEnd;

      const {filter} = this.ctx;
      filter.expr = txtEl.value.substring(0, start) + expr + txtEl.value.substring(end, txtEl.value.length);
    },

    insertFieldExpr: function(event, field) {
      this.$refs.fieldsList.hide(event);
      this.insertExpr(field.id);
    },

    _loadFields: async function() {
      if (this.ctx.fields && this.ctx.fields.length > 0) {
        return;
      }

      const forms = await formsCache.getForms();
      const result = [];
      for (const form of forms) {
        const fields = await formsCache.getFields(form, this.ctx.cpId, this.ctx.cpGroupId);
        Array.prototype.push.apply(result, this._flattenFields(form.name + '.', fields));
      }

      this.ctx.fields = result;
    },

    _flattenFields: function(prefix, fields) {
      const result = [];
      for (let field of fields) {
        if (field.type == 'SUBFORM') {
          Array.prototype.push.apply(result, this._flattenFields(prefix + field.name + '.', field.subFields));
        } else if (field.type == 'INTEGER' || field.type == 'FLOAT' || field.type == 'DATE') {
          result.push({id: prefix + field.name, caption: field.caption});
        }
      }

      return result;
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

.filter-po .toolbar {
  padding: 0.5rem 1rem;
}

.filter-po :deep(form) {
  width: 100%;
}

.toolbar {
  padding: 0.5rem 1rem;
}

.fields-list .os-input-text {
  padding: 0.5rem 1rem;
}

.fields-list ul {
  margin: 0rem -1.25rem;
  padding: 0rem;
  padding-bottom: 0.5rem;
  list-style: none;
  max-height: 300px;
  overflow: scroll;
}

.fields-list ul li a {
  padding: 0.5rem 1rem;
  text-decoration: none;
  display: inline-block;
  width: 100%;
  color: #212529;
}

.fields-list ul li a:hover {
  background: #e9ecef;
}
</style>
