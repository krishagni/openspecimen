<template>
  <os-page-toolbar>
    <template #default>
      <os-button-link left-icon="edit" :label="$t('common.buttons.edit')" :url="editShipmentUrl"
        v-show-if-allowed="shipmentResources.updateOpts" />

      <os-menu :label="$t('shipments.request_status')" :options="ctx.reqStatuses"
        v-show-if-allowed="shipmentResources.updateOpts" />

      <os-button-link left-icon="paper-plane" :label="$t('shipments.ship')" :url="shipShipmentUrl"
        v-if="(!ctx.shipment.request && ctx.shipment.status == 'Pending') || ctx.shipment.status == 'Requested'"
        v-show-if-allowed="shipmentResources.updateOpts" />

      <os-button-link left-icon="inbox" :label="$t('shipments.receive')" :url="receiveShipmentUrl"
        v-if="ctx.shipment.status == 'Shipped'" v-show-if-allowed="shipmentResources.updateOpts" />

      <os-button left-icon="trash" :label="$t('common.buttons.delete')" v-show-if-allowed="shipmentResources.deleteOpts"
        @click="deleteShipment" />

      <os-button left-icon="download" :label="$t('shipments.download_report')" @click="downloadReport" />

      <os-button left-icon="history" :label="$t('audit.trail')" @click="viewAuditTrail" />
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="12">
      <os-overview :schema="ctx.dict" :columns="1" :object="ctx" v-if="ctx.dict.length > 0" />
    </os-grid-column>
  </os-grid>

  <os-confirm-delete ref="deleteDialog" :captcha="false">
    <template #message>
      <span v-t="{path: 'shipments.confirm_delete_msg', args: ctx.shipment}">Shipment '{name}' and any dependent data will be deleted. Are you sure you want to proceed?</span>
    </template>
  </os-confirm-delete>

  <os-audit-trail ref="auditTrailDialog" :objects="ctx.shipmentObjs" />
</template>

<script>
import util from '@/common/services/Util.js';
import shipmentSvc  from '@/administrative/services/Shipment.js';
import alertsSvc    from '@/common/services/Alerts.js';
import pvSvc        from '@/common/services/PermissibleValue.js';
import routerSvc    from '@/common/services/Router.js';


import shipmentResources from './Resources.js';

export default {
  props: ['shipment'],

  inject: ['ui'],

  data() {
    return {
      ctx: {
        shipment: {},

        shipmentObjs: [],

        dict: [],

        reqStatuses: []
      },

      shipmentResources
    };
  },

  created() {
    this.ctx.shipment = this.shipment;
    this.ctx.shipmentObjs = [{objectName: 'shipment', objectId: this.shipment.id}];
    shipmentSvc.getDict().then(dict => this.ctx.dict = dict);
    if (this.shipment.request) {
      pvSvc.getPvs('shipment_request_status', null, {maxResults: 1000}).then(
        (statuses) => {
          this.ctx.reqStatuses = statuses.map(
            status => ({caption: status.value, onSelect: () => this.changeReqStatus(status.value)})
          );
        }
      );
    }
  },

  watch: {
    shipment: function() {
      this.ctx.shipment = this.shipment;
      this.ctx.shipmentObjs = [{objectName: 'shipment', objectId: this.shipment.id}];
    }
  },

  computed: {
    editShipmentUrl: function() {
      return routerSvc.getUrl('ShipmentAddEdit', {shipmentId: this.ctx.shipment.id}, {shipmentType: this.ctx.shipment.type});
    },

    shipShipmentUrl: function() {
      return routerSvc.getUrl(
        'ShipmentAddEdit',
        {shipmentId: this.ctx.shipment.id},
        {shipmentType: this.ctx.shipment.type, action: 'ship'}
      );
    },

    receiveShipmentUrl: function() {
      return routerSvc.getUrl('ShipmentReceive', {shipmentId: this.ctx.shipment.id}, {shipmentType: this.ctx.shipment.type});
    }
  },

  methods: {
    deleteShipment: function() {
      this.$refs.deleteDialog.open().then(
        (resp) => {
          if (resp == 'proceed') {
            shipmentSvc.delete(this.ctx.shipment).then(
              () => {
                alertsSvc.success({code: 'shipments.deleted', args: this.ctx.shipment});
                routerSvc.goto('ShipmentsList', {shipmentId: -2}, this.$route.query);
              }
            );
          }
        }
      );
    },

    downloadReport: function() {
      const reportFn = () => shipmentSvc.generateReport(this.shipment.id);
      util.downloadReport(reportFn, {filename: this.shipment.name + '.csv'})
    },

    viewAuditTrail: function() {
      this.$refs.auditTrailDialog.open();
    },

    changeReqStatus: function(status) {
      shipmentSvc.updateRequestStatus(this.ctx.shipment, status).then(
        savedShipment => {
          Object.assign(this.ctx.shipment, savedShipment);
          alertsSvc.success({code: 'shipments.request_status_updated', args: this.ctx.shipment});
        }
      );
    }
  }
}
</script>
