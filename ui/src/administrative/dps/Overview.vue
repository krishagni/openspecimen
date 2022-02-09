<template>
  <os-page-toolbar>
    <template #default>
      <span v-show-if-allowed="dpResources.updateOpts">
        <os-button left-icon="edit" label="Edit" @click="edit" />

        <os-button left-icon="times" label="Close" @click="confirmClose"
          v-if="ctx.dp.activityStatus == 'Active'" />

        <os-button left-icon="check" label="Reopen" @click="reopen"
          v-if="ctx.dp.activityStatus == 'Closed'" />
      </span>

      <os-button left-icon="trash" label="Delete" @click="deleteDp"
        v-show-if-allowed="dpResources.deleteOpts" />

      <os-button left-icon="share" label="View Orders" @click="viewOrders"
        v-show-if-allowed="dpResources.orderOpts" />

      <os-plugin-views page="dp-overview" view="more-menu" :view-props="{dp: ctx.dp}" />
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="8">
      <os-overview :schema="ctx.dict" :object="ctx" v-if="ctx.dict.length > 0" />
    </os-grid-column>

    <os-grid-column width="4">
      <os-audit-overview :objects="ctx.dpObjs" v-if="ctx.dp.id" />
    </os-grid-column>

    <os-dialog ref="closeConfirmDialog">
      <template #header>
        <span>Confirm Close</span>
      </template>
      <template #content>
        <span>Are you sure you want to close the distribution protocol - <b>{{ctx.dp.shortTitle}}</b>?</span>
      </template>
      <template #footer>
        <os-button text label="Cancel" @click="cancelClose" />
        <os-button primary label="Yes, close" @click="close" />
      </template>
    </os-dialog>

    <os-delete-object ref="deleteDp" :input="ctx.deleteOpts" />
  </os-grid>
</template>

<script>

import alertsSvc from '@/common/services/Alerts.js';
import routerSvc from '@/common/services/Router.js';
import util      from '@/common/services/Util.js';

import dpSvc     from '@/administrative/services/DistributionProtocol.js';

import dpResources from './Resources.js';

export default {
  props: ['dp'],

  inject: ['ui'],

  data() {
    return {
      ctx: {
        dp: {},

        dpObjs: [],

        dict: []
      },

      dpResources
    };
  },

  created() {
    this.setupDp();
    dpSvc.getDict().then(dict => this.ctx.dict = dict);
  },

  watch: {
    dp: function() {
      this.setupDp();
    }
  },

  methods: {
    setupDp: function() {
      const dp = this.ctx.dp = this.dp;
      dp.disableEmailNotifs = dp.disableEmailNotifs == true;

      const sites = dp.distributingSites || {};
      if (!(sites instanceof Array)) {
        dp.distributingSites = Object.keys(sites).map(institute => ({institute: institute, sites: sites[institute]}));
      }

      this.ctx.dpObjs = [{objectName: 'distribution_protocol', objectId: this.dp.id}];

      this.ctx.deleteOpts = {
        type: 'Distribution Protocol',
        title: this.dp.shortTitle,
        dependents: () => dpSvc.getDependents(this.dp),
        deleteObj: () => dpSvc.delete(this.dp)
      };
    },

    edit: function() {
      routerSvc.goto('DpAddEdit', {dpId: this.ctx.dp.id});
    },

    confirmClose: async function() {
      this.$refs.closeConfirmDialog.open();
    },

    cancelClose: function() {
      this.$refs.closeConfirmDialog.close();
    },

    close: async function() {
      const savedDp = await dpSvc.updateStatus(this.ctx.dp, 'Closed');
      this.ctx.dp.activityStatus = savedDp.activityStatus;

      this.$refs.closeConfirmDialog.close();
      alertsSvc.success('Distribution protocol ' + this.ctx.dp.shortTitle + ' closed!');
    },

    reopen: async function() {
      const savedDp = await dpSvc.updateStatus(this.ctx.dp, 'Active');
      this.ctx.dp.activityStatus = savedDp.activityStatus;
      alertsSvc.success('Distribution protocol ' + this.ctx.dp.shortTitle + ' reopened!');
    },

    deleteDp: async function() {
      const resp = await this.$refs.deleteDp.execute();
      if (resp == 'deleted') {
        routerSvc.goto('DpsList', {dpId: -2}, this.ctx.routeQuery);
      }
    },

    viewOrders: function() {
      const fb = util.uriEncode({dpShortTitle: this.ctx.dp.shortTitle});
      routerSvc.goto('OrdersList', {orderId: -1}, {filters: fb});
    }
  }
}
</script>
