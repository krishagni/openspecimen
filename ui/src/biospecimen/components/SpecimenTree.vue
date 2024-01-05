
<template>
  <os-table-form :ref="'spmnsTable'" :tree-layout="true" :read-only="true"
    :data="{}" :items="items" :schema="{columns: fields}">
  </os-table-form>
</template>

<script>

import cpSvc from '@/biospecimen/services/CollectionProtocol.js';
import formUtil from '@/common/services/FormUtil.js';
import routerSvc from '@/common/services/Router.js';
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
          showStatus: true,
          labelCode: 'specimens.description',
          displayType: 'specimen-description',
          detailed: false,
          href: (args) => {
            const specimen = args.specimen;

            let url = '';
            if (specimen) {
              const route = routerSvc.getCurrentRoute();
              const params = {
                cpId: specimen.cpId,
                cprId: specimen.cprId,
                visitId: specimen.visitId,
                eventId: specimen.eventId,
                specimenId: specimen.id || -1
              };

              if (route.name && route.name.indexOf('ParticipantsListItem') >= 0) {
                url = routerSvc.getUrl('ParticipantsListItemSpecimenDetail.Overview', params);
              } else {
                url = routerSvc.getUrl('VisitDetail.Overview', params);
              }
            }

            const currentView = window.location.href;
            const path = currentView.substring(0, currentView.indexOf('/#'));
            return path + '/' + url;
          },
          hrefTarget: '_self'
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
