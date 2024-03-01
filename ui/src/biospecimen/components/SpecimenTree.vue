
<template>
  <os-panel>
    <template #header>
      <span v-t="'specimens.list'">Specimens</span>
    </template>

    <template #actions>
      <os-add-to-cart :specimens="selectedExistingSpecimens"  v-if="selectedExistingSpecimens.length > 0" />

      <os-specimen-actions :cp="cp" :specimens="selectedExistingSpecimens" @reloadSpecimens="reloadSpecimens"
        v-if="selectedExistingSpecimens.length > 0" />
    </template>

    <os-table-form ref="spmnsTable" :tree-layout="true" :read-only="true" selection-mode="checkbox"
      :data="{}" :items="items" :schema="{columns: fields}" @selected-items="onItemsSelection($event)"
      v-if="items.length > 0">
    </os-table-form>

    <os-message :type="info" v-else>
      <span v-t="'specimens.no_specimens'"> </span>
    </os-message>
  </os-panel>
</template>

<script>

import cpSvc from '@/biospecimen/services/CollectionProtocol.js';
import exprUtil from '@/common/services/ExpressionUtil.js';
import formUtil from '@/common/services/FormUtil.js';
import specimenSvc from '@/biospecimen/services/Specimen.js';
import util from '@/common/services/Util.js';

export default {
  props: ['cp', 'cpr', 'visit', 'specimens'],

  emits: ['reload'],

  data() {
    return {
      treeCfg: {},

      ctx: {
        selectedSpecimens: []
      }
    }
  },

  created() {
    const treeCfgQ = cpSvc.getSpecimenTreeCfg((this.cp && this.cp.id) || -1);
    const dictQ    = specimenSvc.getDict((this.cp && this.cp.id) || -1);
    Promise.all([dictQ, treeCfgQ]).then(
      ([dict, treeCfg]) => {
        this.treeCfg = util.clone(treeCfg || {});
        this.treeCfg.fields = formUtil.sdeFieldsToDict(this.treeCfg.fields || [], dict);
        for (let field of this.treeCfg.fields) {
          if (field.type == 'specimen-description') {
            field.showStatus = true;
          }

          if (field.name.indexOf('calc') == 0 && field.displayExpr) {
            field.value = (row) => exprUtil.eval({...row, fns: util.fns()}, field.displayExpr)
          }
        }
      }
    );
  },
   
  computed: {
    items: function() {
      return this._flattenSpecimens(this.specimens || [], 0);
    },

    selectedSpecimens: function() {
      return this.ctx.selectedSpecimens.map(({specimen}) => specimen);
    },

    selectedExistingSpecimens: function() {
      return this.selectedSpecimens.filter(({id}) => id > 0);
    },

    fields: function() {
      if (this.treeCfg.fields && this.treeCfg.fields.length > 0) {
        return this.treeCfg.fields;
      }

      return [
        {
          type: 'specimen-description',
          name: 'specimen',
          labelCode: 'specimens.description',
          showStatus: true,
          detailed: false,
        },
        {
          type: 'text',
          name: 'specimen.label',
          labelCode: 'specimens.label',
        },
        {
          type: 'storage-position',
          name: 'specimen.storageLocation',
          labelCode: 'specimens.location',
        }
      ];
    }
  },

  methods: {
    onItemsSelection: function(items) {
      this.ctx.selectedSpecimens = items;
    },

    reloadSpecimens: function() {
      this.$emit('reload');
      this.ctx.selectedSpecimens = [];
      this.$refs.spmnsTable.setSelection([]);
    },

    _flattenSpecimens: function(specimens, depth, parentUid) {
      let idx = 0;
      let result = [];
      for (let specimen of specimens) {
        formUtil.createCustomFieldsMap(specimen, true);

        const uid = parentUid !== undefined && parentUid !== null ? parentUid + '_' + idx : idx;
        const item = {cpr: this.cpr, visit: this.visit, specimen, depth, expanded: true, show: true, uid, parentUid};
        item.hasChildren = (specimen.children || []).length > 0;
        result.push(item);
        ++idx;
      
        Array.prototype.push.apply(result, this._flattenSpecimens(specimen.children || [], depth + 1, uid));
      }

      return result;
    }
  }
}
</script>
