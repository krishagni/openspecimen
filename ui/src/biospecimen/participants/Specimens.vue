<template>
  <os-grid>
    <os-grid-column :width="12">
      <SpecimenTree class="tree" :cp="cp" :cpr="cpr" :visit="{}" :specimens="specimens" :page-top="115"
        :ref-date="cpr.registrationDate" @reload="reloadSpecimens" />
    </os-grid-column>
  </os-grid>
</template>

<script>
import cprSvc from '@/biospecimen/services/Cpr.js';

import SpecimenTree from '@/biospecimen/components/SpecimenTree.vue';

export default {
  props: ['cpr'],

  inject: ['cpViewCtx'],

  components: {
    SpecimenTree
  },

  data() {
    const cp = this.cpViewCtx.getCp();

    return {
      cp,

      specimens: []
    }
  },

  created() {
    this._loadSpecimens();
  },

  methods: {
    reloadSpecimens: function() {
      this._loadSpecimens();
    },

    _loadSpecimens: function() {
      cprSvc.getSpecimens(this.cpr.cpId, this.cpr.id).then(
        specimens => {
          this._getAllowedEvents().then(
            allowedEvents => {
              this.specimens = specimens.filter(
                spmn => {
                  if (spmn.visitStatus && spmn.visitStatus != 'Pending') {
                    return true;
                  }

                  return !allowedEvents || !spmn.eventCode || allowedEvents.indexOf(spmn.eventCode) >= 0;
                }
              );
              this._setVisitId(this.specimens);
            }
          );
        }
      );
    },

    _setVisitId: function(specimens) {
      for (const specimen of specimens) {
        if (!specimen.visitId) {
          specimen.visitId = -1;
        }

        this._setVisitId(specimen.children || []);
      }
    },

    _getAllowedEvents: function() {
      return this.cpViewCtx.getAnticipatedEventsRules().then(rules => cprSvc.getAllowedEvents(this.cpr, rules));
    }
  },
}
</script>

<style scoped>
.tree {
  height: 100%;
}

.tree :deep(.p-panel-content) {
  height: 100%;
}
</style>
