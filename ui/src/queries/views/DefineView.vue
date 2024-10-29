<template>
  <os-dialog ref="columnsDialog" size="lg">
    <template #header>
      <span v-t="'queries.columns'">Columns</span>
    </template>

    <template #content>
      <os-steps ref="columnsWizard">
        <os-step :title="$t('queries.select_fields')">
          <os-tree-select-panel :items="ctx.fieldsTree" :selected="ctx.selectedFields"
            @selected-items="onFieldsSelection" />
        </os-step>
        <os-step :title="$t('queries.field_labels')">
          <span>UI to assign names to the selected fields</span>
        </os-step>
        <os-step :title="$t('queries.aggregates')">
          <span>Choose aggregate functions</span>
        </os-step>
        <os-step :title="$t('queries.reporting_options')">
          <span>Choose reporting options - pivot, column summary etc</span>
        </os-step>
      </os-steps>
    </template>

    <template #footer>
      <os-button text :label="$t('common.buttons.cancel')" @click="closeColumnsDialog" />
      <os-button secondary :label="$t('common.buttons.previous')" @click="previous" v-if="!isFirstStep()" />
      <os-button primary :label="$t('common.buttons.next')" @click="next" v-if="!isLastStep()"
        :disabled="!ctx.selectedFields || ctx.selectedFields.length == 0" />
      <os-button primary :label="$t('common.buttons.done')" @click="done"
        :disabled="!ctx.selectedFields || ctx.selectedFields.length == 0" />
    </template>
  </os-dialog> 
</template>

<script>
import util from '@/common/services/Util.js';
import formsCache from '@/queries/services/FormsCache.js';

export default {
  data() {
    return {
      ctx: {
        fieldsTree: []
      }
    }
  },

  methods: {
    open: async function(query) {
      const self = this;
      this.ctx.query = util.clone(query);
      this.ctx.fieldsTree     = await self._getFieldsTree(self.ctx.query);
      this.ctx.selectedFields = (this.ctx.query.selectList || []).map(
        field => ({...field, id: field.name})
      );

      return new Promise((resolve) => {
        self.resolve = resolve;
        self.$refs.columnsDialog.open();
      });
    },

    isFirstStep: function() {
      if (!this.$refs.columnsWizard) {
        return false;
      }

      return this.$refs.columnsWizard.isFirstStep();
    },

    isLastStep: function() {
      if (!this.$refs.columnsWizard) {
        return false;
      }

      return this.$refs.columnsWizard.isLastStep();
    },

    next: function() {
      this.$refs.columnsWizard.next();
    },

    previous: function() {
      this.$refs.columnsWizard.previous();
    },

    closeColumnsDialog: function() {
      this.$refs.columnsDialog.close();
      this.resolve('cancel');
    },

    done: function() {
      this.resolve({selectedFields: this.ctx.selectedFields});
      this.$refs.columnsDialog.close();
    },

    onFieldsSelection: function(selectedFields) {
      const fieldsMap = this.ctx.selectedFields.reduce(
        (map, field) => {
          map[field.id] = field;
          return map;
        },
        {}
      );

      const result = [];
      for (let field of selectedFields) {
        if (fieldsMap[field.id]) {
          result.push(fieldsMap[field.id]);
        } else {
          result.push({name: field.id, displayLabel: null, aggFns: null, id: field.id});
        }
      }

      this.ctx.selectedFields = result;
    },

    _getFieldsTree: async function(query) {
      const forms = await formsCache.getForms();
      const tree = [];
      for (let form of forms) {
        const fields = await formsCache.getFields(form, query.cpId, query.cpGroupId);

        const item = {id: form.name, label: form.caption, children: []};
        item.children = this._getFieldsTree0(form, null, form.name, fields);
        tree.push(item);
      }

      return tree;
    },

    _getFieldsTree0: function(rootForm, form, prefix, fields) {
      const subTree = [];
      for (let field of fields) {
        if (field.type == 'SUBFORM') {
          const children = this._getFieldsTree0(rootForm, form, prefix + '.' + field.name, field.subFields);
          if (field.name == 'extensions' || field.name == 'customFields') {
            Array.prototype.push.apply(subTree, children);
          } else {
            subTree.push({id: prefix + '.' + field.name, label: field.caption, children});
          }
        } else {
          subTree.push({id: prefix + '.' + field.name, label: field.caption, children: []});
        }
      }

      subTree.sort((n1, n2) => n1.label < n2.label ? -1 : (n1.label > n2.label ? 1 : 0));
      return subTree;
    }
  }
}
</script>


