<template>
  <os-page-toolbar>
    <template #default>
      <os-button v-show-if-allowed="orderResources.updateOpts"
        left-icon="edit" label="Edit" @click="editOrder" />

      <os-button left-icon="download" label="Download Report" @click="downloadReport" />

      <os-button v-show-if-allowed="orderResources.deleteOpts"
        left-icon="trash" label="Delete" @click="deleteOrder" />

      <os-plugin-views page="order-detail" view="more-menu" :view-props="{order: ctx.order}" />
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="8">
      <os-overview :schema="ctx.dict" :object="ctx" v-if="ctx.dict.length > 0" />
    </os-grid-column>

    <os-grid-column width="4">
      <os-audit-overview :objects="ctx.orderObjs" v-if="ctx.order.id" />
    </os-grid-column>
  </os-grid>

  <os-confirm-delete ref="deleteDialog" :captcha="false">
    <template #message>
      <span>Order '{{order.name}}' and any dependent data will be deleted. Are you sure you want to proceed?</span>
    </template>
  </os-confirm-delete>
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
            alertsSvc.info('Deletion of order is taking more time than anticipated. An email notification will be sent to you on deletion.');
          } else {
            alertsSvc.success('Order #' + this.order.id + ': ' + this.order.name + ' deleted!');
          }

          routerSvc.goto('OrdersList', {orderId: -2}, this.ctx.routeQuery);
        }
      );
    }
  }
}
</script>
