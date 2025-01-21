<template>
  <os-page>
    <os-page-head>
      <span>
        <h3 v-t="'specimens.bulk_update'">Bulk Edit Specimens</h3>
      </span>
    </os-page-head>

    <os-page-body>
      <os-bulk-update :dict="ctx.dict" object-prefix="specimen." :exclusion-list="ctx.exclusionList"
        @update="update($event)" @cancel="cancel" />
    </os-page-body>
  </os-page>
</template>

<script>
import alertsSvc from '@/common/services/Alerts.js';
import itemsSvc  from '@/common/services/ItemsHolder.js';
import routerSvc from '@/common/services/Router.js';
import spmnSvc   from '@/biospecimen/services/Specimen.js';

const EXCLUSION_LIST = [
  'specimen.label',
  'specimen.barcode',
  'specimen.additionalLabel',
  'specimen.storageLocation',
  'specimen.lineage',
  'specimen.parentLabel'
];

export default {
  data() {
    return {
      ctx: {
        dict: [],

        exclusionList: EXCLUSION_LIST
      }
    }
  },

  created() {
    const spmns = this.specimens = itemsSvc.getItems('specimens');
    itemsSvc.clearItems('specimens');

    let cpId = -1;
    if (spmns && spmns.length > 0) {
      cpId = spmns[0].cpId;
      if (spmns.some(spmn => spmn.cpId != cpId)) {
        cpId = -1;
      }
    }

    this.cpId = cpId;
    spmnSvc.getDict(cpId).then(dict => this.ctx.dict = dict);
  },

  methods: {
    update: function(detail) {
      const spmnIds = this.specimens.map(({id}) => id);
      spmnSvc.bulkEdit(spmnIds, detail.specimen).then(
        savedSpmns => {
          const msg = !savedSpmns ? 'specimens.bulk_update_more_time' : (savedSpmns.length == 1 ? 'specimens.one_updated' : 'specimens.bulk_updated');
          alertsSvc.success({code: msg, args: {count: savedSpmns.length}});
          this.cancel();
        }
      );
    },

    cancel: function() {
      routerSvc.back();
    }
  }
}
</script>
