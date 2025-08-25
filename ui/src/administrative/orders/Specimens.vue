
<template>
  <os-page-toolbar>
    <template #default>
      <os-button left-icon="print" :label="$t('common.buttons.print')" @click="printLabels" v-if="printDistLabels" />

      <os-button left-icon="trash" :label="$t('common.buttons.delete')" @click="deleteItems" v-if="updateAllowed" />

      <os-button left-icon="reply" :label="$t('orders.retrieve')" @click="retrieveSpecimens"
        v-if="showRetrieveSpmns && selectedSpecimens.length == 0" />
    </template>

    <template #right v-if="$refs.specimensList && $refs.specimensList.list && $refs.specimensList.list.rows">
      <os-list-size
        :list="$refs.specimensList.list.rows"
        :page-size="$refs.specimensList.pageSize"
        :list-size="$refs.specimensList.size"
        @updateListSize="getSpecimensCount"
      />

      <os-button left-icon="search" :label="$t('common.buttons.search')" @click="toggleSearch" />
    </template>
  </os-page-toolbar>

  <os-query-list-view
    name="order-specimens-list-view"
    :object-id="order.id"
    :allow-selection="allowSelection"
    :include-count="includeCount"
    url="'#/specimen-resolver/' + hidden.specimenId"
    @selectedRows="onSpecimenSelection"
    @rowClicked="onSpecimenRowClick"
    ref="specimensList"
  />

  <os-confirm ref="confirmPrintAllDialog">
    <template #title>
      <span v-t="'orders.print_all'">Print All Order Items?</span>
    </template>
    <template #message>
      <span v-t="'orders.confirm_print_all'">Are you sure you want to print labels of all specimens of the order?</span>
    </template>
  </os-confirm>

  <os-dialog ref="retrieveSpmnComments">
    <template #header>
      <span v-t="'orders.retrieve_specimens'"> Retrieve Specimens </span>
    </template>
    <template #content>
      <os-textarea :placeholder="$t('orders.retrieve_reason')" rows="5" v-model="retrieveSpmnsReason" />
    </template>
    <template #footer>
      <os-button text    :label="$t('common.buttons.cancel')" @click="cancelRetrieveSpecimens" />
      <os-button primary :label="$t('orders.retrieve')" @click="submitRetrieveSpecimens" />
    </template>
  </os-dialog>

  <os-confirm-delete ref="deleteDialog" :captcha="false">
    <template #message>
      <span v-t="'orders.confirm_rm_specimens'">Are you sure you want to remove the selected specimens from the order?</span>
    </template>
  </os-confirm-delete>
</template>

<script>

import orderSvc   from '@/administrative/services/Order.js';
import alertSvc   from '@/common/services/Alerts.js';
import authSvc    from '@/common/services/Authorization.js';
import routerSvc  from '@/common/services/Router.js';
import settingSvc from '@/common/services/Setting.js';

import orderResources from './Resources.js'

export default {
  props: ['order'],

  data() {
    return {
      updateAllowed: false,

      printDistLabels: false,

      allowSelection: false,

      showRetrieveSpmns: false,

      selectedSpecimens: [],

      retrieveSpmnsReason: '',

      includeCount: false,

      orderResources
    }
  },

  async created() {
    const setting = await settingSvc.getSetting('administrative', 'allow_dist_label_printing');
    this.printDistLabels = setting[0].value == 'true' || setting[0].value == true;

    this.updateAllowed = authSvc.isAllowed(orderResources.updateOpts);
    this.allowSelection = this.printDistLabels || this.updateAllowed;

    if (this.order.status == 'EXECUTED') {
      const items = await orderSvc.getOrderItems(this.order.id, {maxResults: 1, storedInDistributionContainer: true});
      this.showRetrieveSpmns = items.length > 0;
    }
  },

  methods: {
    toggleSearch: function() {
      this.$refs.specimensList.toggleShowFilters();
    },

    onSpecimenSelection: function(specimens) {
      this.selectedSpecimens = specimens.map(({rowObject}) => +rowObject.hidden.orderItemId_);
    },

    onSpecimenRowClick: function(event) {
      const spmnId = +event.hidden.specimenId;
      routerSvc.goto('SpecimenResolver', {specimenId: spmnId});
    },

    getSpecimensCount: function() {
      this.$refs.specimensList.loadListSize();
    },

    printLabels: async function() {
      if (this.selectedSpecimens.length == 0) {
        const resp = await this.$refs.confirmPrintAllDialog.open();
        if (resp != 'proceed') {
          return;
        }
      }

      const printJob = await orderSvc.printLabels(this.order.id, this.selectedSpecimens);
      if (!printJob) {
        alertSvc.success({code: 'orders.print_job_submitted'});
        return;
      }

      let downloadEnabled = this.$ui.currentUser.downloadLabelsPrintFile;
      if (!downloadEnabled) {
        const setting = await settingSvc.getSetting('administrative', 'download_labels_print_file');
        downloadEnabled = setting[0].value == 'true' || setting[0].value == true;
      }

      if (downloadEnabled) {
        orderSvc.downloadLabelsFile(printJob.id, this.order.name + '.csv');
      } else {
        alertSvc.success({code: 'orders.print_job_created', args: printJob});
      }
    },

    deleteItems: async function() {
      if (this.selectedSpecimens.length == 0) {
        alertSvc.error({code: 'orders.no_specimen_selected'});
        return;
      }

      const resp = await this.$refs.deleteDialog.open();
      if (resp != 'proceed') {
        return;
      }

      const result = await orderSvc.deleteOrderItems(this.order.id, this.selectedSpecimens);
      alertSvc.success({code: 'orders.spmns_removed', args: {count: result.deleted}});
      if (result.deleted > 0) {
        this.$refs.specimensList.reload();
      }
    },

    retrieveSpecimens: function() {
      this.retrieveSpmnsReason = '';
      this.$refs.retrieveSpmnComments.open();
    },

    submitRetrieveSpecimens: async function() {
      await orderSvc.retrieveSpecimens(this.order.id, this.retrieveSpmnsReason);
      this.showRetrieveSpmns = false;
      this.$refs.specimensList.reload();
      this.$refs.retrieveSpmnComments.close();
    },

    cancelRetrieveSpecimens: async function() {
      this.$refs.retrieveSpmnComments.close();
    }
  }
}
</script>
