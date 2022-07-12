<template>
  <os-page-toolbar>
    <template #default>
      <os-button left-icon="edit" :label="$t('common.buttons.edit')" v-show-if-allowed="shipmentResources.updateOpts"
        @click="$goto('ShipmentAddEdit', {shipmentId: ctx.shipment.id}, {shipmentType: ctx.shipment.type})" />

      <os-button left-icon="inbox" :label="$t('shipments.receive')" v-if="ctx.shipment.status == 'Shipped'"
        v-show-if-allowed="shipmentResources.updateOpts"
        @click="$goto('ShipmentReceive', {shipmentId: ctx.shipment.id}, {shipmentType: ctx.shipment.type})" />

      <os-button left-icon="download" :label="$t('shipments.download_report')" @click="downloadReport" />
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="8">
      <os-overview :schema="ctx.dict" :object="ctx" v-if="ctx.dict.length > 0" />
    </os-grid-column>

    <os-grid-column width="4">
      <os-audit-overview :objects="ctx.shipmentObjs" v-if="ctx.shipment.id" />
    </os-grid-column>
  </os-grid>
</template>

<script>
import util from '@/common/services/Util.js';
import shipmentSvc  from '@/administrative/services/Shipment.js';

import shipmentResources from './Resources.js';

export default {
  props: ['shipment'],

  inject: ['ui'],

  data() {
    return {
      ctx: {
        shipment: {},

        shipmentObjs: [],

        dict: []
      },

      shipmentResources
    };
  },

  created() {
    this.ctx.shipment = this.shipment;
    this.ctx.shipmentObjs = [{objectName: 'shipment', objectId: this.shipment.id}];
    shipmentSvc.getDict().then(dict => this.ctx.dict = dict);
  },

  watch: {
    shipment: function() {
      this.ctx.shipment = this.shipment;
      this.ctx.shipmentObjs = [{objectName: 'shipment', objectId: this.shipment.id}];
    }
  },

  methods: {
    downloadReport: function() {
      const reportFn = () => shipmentSvc.generateReport(this.shipment.id);
      util.downloadReport(reportFn, {filename: this.shipment.name + '.csv'})
    }
  }
}
</script>
