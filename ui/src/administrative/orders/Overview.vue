<template>
  <os-page-toolbar>
    <template #default>
      <os-button v-show-if-allowed="orderResources.updateOpts"
        left-icon="edit" :label="$t('common.buttons.edit')" @click="editOrder" />

      <os-button left-icon="download" :label="$t('orders.download_report')" @click="downloadReport" />

      <os-button v-show-if-allowed="orderResources.deleteOpts"
        left-icon="trash" :label="$t('common.buttons.delete')" @click="deleteOrder" />

      <os-plugin-views page="order-detail" view="more-menu" :view-props="{order: ctx.order}" />

      <os-button left-icon="history" :label="$t('audit.trail')" @click="viewAuditTrail" />
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="12">
      <os-overview :schema="ctx.dict" :object="ctx" v-if="ctx.dict.length > 0" />
    </os-grid-column>
  </os-grid>

  <os-confirm-delete ref="deleteDialog" :captcha="false">
    <template #message>
      <span v-t="{path: 'orders.confirm_delete_msg', args: ctx.order}">Order '{{order.name}}' and any dependent data will be deleted. Are you sure you want to proceed?</span>
    </template>
  </os-confirm-delete>

  <os-audit-trail ref="auditTrailDialog" :objects="ctx.orderObjs" />
</template>

<script>
import { reactive } from 'vue';
import { useRoute } from 'vue-router';

import orderSvc   from '@/administrative/services/Order.js';
import alertsSvc  from '@/common/services/Alerts.js';
import routerSvc  from '@/common/services/Router.js';
import settingSvc from '@/common/services/Setting.js';
import util       from '@/common/services/Util.js';

import orderResources from './Resources.js'

export default {
  props: ['order'],

  inject: ['ui'],

  setup() {
    const route = useRoute();
    const ctx = reactive({
      order: {},

      orderObjs: [],

      dict: [],

      rptTmplConfigured: false,

      routeQuery: route.query
    });

    return { ctx, orderResources };
  },

  created() {
    this.setupView();
  },

  watch: {
    'order.id': function(newVal, oldVal) {
      if (newVal == oldVal) {
        return;
      }

      this.setupView();
    }
  },

  methods: {
    setupView: async function() {
      const ctx = this.ctx;
      ctx.order = this.order;
      ctx.orderObjs = [{objectName: 'distribution_order', objectId: this.order.id}];
      ctx.dict = await orderSvc.getDict(ctx.order.distributionProtocol);

      if (ctx.order.distributionProtocol.report && ctx.order.distributionProtocol.report.id) {
        ctx.rptTmplConfigured = true;
      } else {
        const setting = await settingSvc.getSetting('common', 'distribution_report_query');
        ctx.rptTmplConfigured = !!setting[0].value;
      }
    },

    editOrder: function() {
      routerSvc.goto('OrderAddEdit', {orderId: this.order.id});
    },

    downloadReport: function() {
      const reportFn = () => orderSvc.generateReport(this.order.id);
      util.downloadReport(reportFn, {filename: this.order.name + '.csv'})
    },

    deleteOrder: function() {
      this.$refs.deleteDialog.open().then(
        async (resp) => {
          if (resp != 'proceed') {
            return;
          }

          const result = await orderSvc.delete(this.order);
          if (!result.completed) {
            alertsSvc.info({code: 'orders.delete_more_time'});
          } else {
            alertsSvc.success({code: 'orders.deleted', args: this.order});
          }

          routerSvc.goto('OrdersList', {orderId: -2}, this.ctx.routeQuery);
        }
      );
    },

    viewAuditTrail: function() {
      this.$refs.auditTrailDialog.open();
    }
  }
}
</script>
