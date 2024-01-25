
<template>
  <os-page-toolbar>
    <template #default>
      <span v-if="specimen.id > 0">
        <os-menu icon="plus" :label="$t('common.buttons.create')"
          :options="[
            {icon: 'flask',     caption: $t('specimens.derived'),  onSelect: createDerivatives},
            {icon: 'share-alt', caption: $t('specimens.aliquots'), onSelect: createAliquots}
          ]"

          v-if="specimen.availabilityStatus == 'Available'"
        />

        <os-button left-icon="print" :label="$t('common.buttons.print')" @click="confirmPrint" />

        <os-add-to-cart :specimens="[{id: specimen.id}]" />

        <os-button left-icon="trash" :label="$t('common.buttons.delete')" @click="deleteSpecimen" />
      </span>
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="8">
      <os-overview :schema="ctx.dict" :object="ctx" v-if="ctx.dict.length > 0" />

      <SpecimenTree :cp="ctx.cp" :specimens="ctx.children" v-if="ctx.cp.id > 0" />
    </os-grid-column>

    <os-grid-column width="4">
      <os-audit-overview :objects="ctx.auditObjs" v-if="ctx.specimen.id > 0" />
    </os-grid-column>
  </os-grid>

  <os-dialog ref="printDialog">
    <template #header>
      <span v-t="'specimens.confirm_print'">Confirm Print</span>
    </template>

    <template #content>
      <span v-t="'specimens.print_child_labels_q'">Do you want to print child specimen labels as well?</span>
    </template>

    <template #footer>
      <os-button text      :label="$t('common.buttons.cancel')"  @click="cancelPrint" />
      <os-button secondary :label="$t('specimens.no_print_current_specimen')" @click="printLabels(false)" />
      <os-button primary   :label="$t('common.buttons.yes')" @click="printLabels(true)" />
    </template>
  </os-dialog>

  <os-delete-object ref="deleteSpmnDialog" :input="ctx.deleteOpts" />
</template>

<script>

import SpecimenTree from '@/biospecimen/components/SpecimenTree.vue';

import specimenSvc from '@/biospecimen/services/Specimen.js';
import wfSvc from '@/biospecimen/services/Workflow.js';

import routerSvc from '@/common/services/Router.js';
import util  from '@/common/services/Util.js';

export default {
  props: ['visit', 'specimen'],

  components: {
    SpecimenTree
  },

  inject: ['cpViewCtx'],

  data() {
    return {
      ctx: {
        cp: {},

        specimen: {},

        dict: [],

        auditObjs: [],

        routeQuery: this.$route.query,

        children: []
      }
    };
  },

  async created() {
    this._setupSpecimen();
    this.ctx.dict = await this.cpViewCtx.getSpecimenDict();
  },

  watch: {
    specimen: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this._setupSpecimen();
      }
    }
  },

  methods: {
    createAliquots: function() {
      wfSvc.createAliquots(this.ctx.specimen);
    },

    createDerivatives: function() {
      wfSvc.createDerivedSpecimens(this.ctx.specimen);
    },

    confirmPrint: function() {
      this.$refs.printDialog.open();
    },

    printLabels: function(includeChildren) {
      const {specimen, children} = this.ctx;
      const ids = includeChildren ? this._getChildrenIds(children) : [];
      ids.unshift(specimen.id);

      const ts = util.formatDate(new Date(), 'yyyyMMdd_HHmmss');
      const outputFilename = [
        specimen.cpShortTitle, specimen.ppid,
        specimen.visitName, specimen.label || specimen.id,
        ts
      ].join('_') + '.csv';
      specimenSvc.printLabels({specimenIds: ids}, outputFilename);
      this.$refs.printDialog.close();
    },

    cancelPrint: function() {
      this.$refs.printDialog.close();
    },

    deleteSpecimen: function() {
      this.$refs.deleteSpmnDialog.execute().then(
        (resp) => {
          if (resp != 'deleted') {
            return;
          }

          const route = routerSvc.getCurrentRoute();
          if (route.name.indexOf('ParticipantsListItemSpecimenDetail') >= 0) {
            routerSvc.goto('ParticipantsListItemVisitDetail.Overview', this.specimen);
          } else {
            routerSvc.goto('VisitDetail.Overview', this.specimen);
          }

          specimenSvc.clearSpecimens(this.visit);
        }
      );
    },

    _setupSpecimen: function() {
      this.cpViewCtx.getCp().then(cp => this.ctx.cp = cp);

      const specimen = this.ctx.specimen = this.specimen;
      if (specimen.id > 0) {
        this.ctx.auditObjs = [
          {objectId: specimen.id, objectName: 'specimen'}
        ]

        this.ctx.deleteOpts = {
          type: this.$t('specimens.specimen'),
          title: specimen.label + (specimen.barcode ? ' (' + specimen.barcode + ')' : ''),
          dependents: () => specimenSvc.getDependents(specimen),
          forceDelete: true,
          askReason: true,
          deleteObj: (reason) => specimenSvc.deleteSpecimen(specimen.id, true, reason)
        };
      }

      this.ctx.children = specimen.children || [];
    },

    _getChildrenIds: function(children) {
      const result = [];
      for (let child of (children || [])) {
        result.push(child.id);
        Array.prototype.push.apply(result, this._getChildrenIds(child.children));
      }

      return result;
    }
  }
}
</script>
