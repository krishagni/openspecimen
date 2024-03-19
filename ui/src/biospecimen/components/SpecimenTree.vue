
<template>
  <os-panel ref="specimenTree">
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

    <os-button class="scroll-top" left-icon="arrow-circle-up" @click="scrollToTop" v-if="panelFixed" />
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

      panelFixed: false,

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

    window.addEventListener('wheel', this.onScroll);
  },

  unmounted() {
    window.removeEventListener('wheel', this.onScroll);
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

    onScroll: function() {
      const treeEl = this.$refs.specimenTree.$el;
      if (treeEl.getBoundingClientRect().top <= 100) {
        const width = treeEl.offsetWidth + 'px';
        const height = 'calc(100% - 120px)';
        Object.assign(treeEl.style, {position: 'fixed', top: '100px', width, height, overflow: 'scroll'});
        this.panelFixed = true;
      }
    },

    scrollToTop: function() {
      const treeEl = this.$refs.specimenTree.$el;
      Object.assign(treeEl.style, {position: '', top: '', width: 'initial', height: '', overflow: 'visible'});
      this.panelFixed = false;
      setTimeout(() => window.scrollTo(0, 0));
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

<style scoped>
.scroll-top {
  position: absolute;
  right: 1rem;
  bottom: 1rem;
  padding: 0.25rem 0.5rem;
  font-size: 1.5rem!important;
  height: 2.625rem!important;
  width: 2.625rem!important;
  z-index: 100;
  background: #fff!important;
}
</style>
