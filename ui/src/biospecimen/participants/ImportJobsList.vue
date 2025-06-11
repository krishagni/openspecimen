<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="bcrumb" />
      </template>

      <h3 v-t="'participants.import_jobs_list'">Import Jobs List</h3>
    </os-page-head>

    <os-page-body>
      <os-grid>
        <os-grid-column :width="12">
          <os-import-jobs :object-types="objectTypes" :object-params="objectParams"
            :create-job="createJobRoute" />
        </os-grid-column>
      </os-grid>
    </os-page-body>
  </os-page>
</template>

<script>
import pluginViews from '@/common/services/PluginViewsRegistry.js';
import routerSvc   from '@/common/services/Router.js';

export default {
  props: ['cpId'],

  inject: ['cpViewCtx'],

  data() {
    return {
    };
  },

  computed: {
    bcrumb: function() {
      if (+this.cpId > 0) {
        const cp = this.cpViewCtx.getCp();
        return [ {url: routerSvc.getUrl('ParticipantsList', {cpId: this.cpId}), label: cp.shortTitle } ];
      } else {
        return [ {url: routerSvc.getUrl('CpsList'), label: this.$t('cps.list') } ];
      }
    },

    objectTypes: function() {
      let objectTypes = [];
      if (+this.cpId > 0) {
        objectTypes = [
          'cpr', 'participant', 'consent', 'econsentsDocumentResponse',
          'visit', 'specimen', 'specimenDerivative', 'specimenAliquot',
          'masterSpecimen', 'specimenDisposal', 'extensions'
        ];
      } else {
        objectTypes = [
          'cp', 'cpe', 'sr',
          'cprMultiple', 'otherCpr', 'cpr', 'participant', 'consent', 'econsentsDocumentResponse',
          'visit', 'specimen', 'specimenDerivative', 'specimenAliquot',
          'masterSpecimen', 'containerSpecimen', 'specimenDisposal', 'extensions',
          'containerTransferEvent', 'specimenDisposalEvent', 'specimenReservedEvent',
          'specimenReservationCancelEvent', 'specimenReturnEvent', 'specimenTransferEvent'
        ];
      }

      const pluginTypes = (pluginViews.getAllImportTypes() || []).map(({type}) => type);
      Array.prototype.push.apply(objectTypes, pluginTypes);
      return objectTypes;
    },

    objectParams: function() {
      return {cpId: +this.cpId > 0 ? +this.cpId : -1};
    },

    createJobRoute: function() {
      const name = +this.cpId > 0 ? 'CpImportRecords' : 'MultiCpImportRecords';
      return {name, params: {cpId: this.cpId}};
    }
  }
}
</script>
