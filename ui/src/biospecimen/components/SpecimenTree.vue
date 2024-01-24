
<template>
  <os-panel>
    <template #header>
      <span v-t="'specimens.list'">Specimens</span>
    </template>

    <os-table-form ref="spmnsTable" :tree-layout="true" :read-only="true"
      :data="{}" :items="items" :schema="{columns: fields}">
    </os-table-form>
  </os-panel>
</template>

<script>

import cpSvc from '@/biospecimen/services/CollectionProtocol.js';
import formUtil from '@/common/services/FormUtil.js';
import util from '@/common/services/Util.js';

export default {
  props: ['cp', 'specimens'],

  data() {
    return {
      treeCfg: {}
    }
  },

  created() {
    cpSvc.getSpecimenTreeCfg((this.cp && this.cp.id) || -1).then(
      (treeCfg) => {
        this.treeCfg = util.clone(treeCfg || {});
      }
    );
  },
   
  computed: {
    items: function() {
      return this._flattenSpecimens(this.specimens || [], 0);
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
    _flattenSpecimens: function(specimens, depth, parentUid) {
      let idx = 0;
      let result = [];
      for (let specimen of specimens) {
        formUtil.createCustomFieldsMap(specimen, true);

        const uid = parentUid !== undefined && parentUid !== null ? parentUid + '_' + idx : idx;
        const item = {specimen, depth, expanded: true, show: true, uid, parentUid};
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
