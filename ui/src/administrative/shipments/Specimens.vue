<template>
  <div>
    <os-page-toolbar v-show-if-allowed="shipmentResources.updateOpts" v-if="shipment.type == 'SPECIMEN'">
      <os-button v-if="shipment.status == 'Requested'" left-icon="reply"
        :label="$t('common.specimen_actions.retrieve')" @click="retrieveSpecimens" />
      <os-pick-lists-dropdown v-if="!shipment.request || shipment.status != 'Pending'"
        :cart="ctx.cart" @new-cart="createPickList" />
    </os-page-toolbar>

    <os-retrieve-specimens ref="retrieveSpmnsDialog" :retrieve-fn="submitRetrieveSpecimens"
      @retrieved="specimensRetrieved" />

    <os-grid>
      <os-grid-column width="12">
        <os-list-view
          :data="ctx.specimens"
          :schema="listSchema"
          :filters="listSchema.filters"
          :query="ctx.query"
          :allowSelection="false"
          :loading="ctx.loading"
          ref="listView">

          <template #footer>
            <os-pager :start-at="ctx.startAt" :have-more="ctx.haveMoreSpmns"
              @previous="previousPage" @next="nextPage" />
          </template>
        </os-list-view>
      </os-grid-column>
    </os-grid>
  </div>
</template>

<script>

import listSchema from '@/administrative/schemas/shipments/specimens.js';

import routerSvc   from '@/common/services/Router.js';
import shipmentSvc from '@/administrative/services/Shipment.js';

import shipmentResources from './Resources.js';

const MAX_SPMNS = 50;

export default {
  props: ['shipment'],

  data() {
    return {
      ctx: {
        specimens: [],

        loading: true,

        startAt: 0,

        haveMoreSpmns: false
      },

      listSchema,

      shipmentResources
    }
  },

  created() {
    this.loadSpecimens(0);
    this.ctx.cart = this.shipment.cart || {name: 'Shipment #' + this.shipment.id + ' Cart'};
  },

  watch: {
    shipment: function() {
      this.loadSpecimens(0);
      this.ctx.cart = this.shipment.cart || {name: 'Shipment #' + this.shipment.id + ' Cart'};
    }
  },

  methods: {
    retrieveSpecimens: function() {
      this.$refs.retrieveSpmnsDialog.open();
    },

    submitRetrieveSpecimens: function(input) {
      return shipmentSvc.retrieveSpecimens(this.shipment.id, input);
    },

    specimensRetrieved: function() {
      this.loadSpecimens(this.ctx.startAt);
    },

    loadSpecimens: async function(startAt) {
      const ctx = this.ctx;
      ctx.loading = true;

      const specimens = await shipmentSvc.getSpecimens(this.shipment.id, {startAt: startAt, maxResults: MAX_SPMNS + 1});
      if (specimens.length > MAX_SPMNS) {
        ctx.haveMoreSpmns = true;
        specimens.splice(specimens.length - 1, 1);
      } else {
        ctx.haveMoreSpmns = false;
      }

      ctx.specimens = specimens;
      ctx.startAt = startAt;
      ctx.loading = false;
    },

    previousPage: function() {
      this.loadSpecimens(this.ctx.startAt - MAX_SPMNS);
    },

    nextPage: function() {
      this.loadSpecimens(this.ctx.startAt + MAX_SPMNS);
    },

    createPickList: function({dialog, cart, pickList}) {
      const payload = {
        shipmentId: this.shipment.id,
        name: cart.name,
        sharedWith: cart.sharedWith,
        pickListName: pickList.name
      };

      shipmentSvc.createPickList(this.shipment.id, payload).then(
        ({cart, id}) => {
          routerSvc.goto('PickList', {cartId: cart.id, listId: id});
          dialog.close();
        }
      );
    }
  }
}
</script>
