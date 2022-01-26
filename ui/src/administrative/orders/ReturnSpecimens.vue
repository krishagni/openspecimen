<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span class="os-title">
        <h3>Return Specimens</h3>
      </span>
    </os-page-head>

    <os-page-body>
      <div>
        <os-add-specimens ref="addSpmns"
          label="Add specimens by scanning labels or barcodes separated by a comma, tab, or newline"
          @on-add="addSpecimens"
        />

        <os-message type="error" v-if="ctx.returnItems.length == 0">
          <span>No specimens to return. Add at least one specimen to return.</span>
        </os-message>

        <div v-else>
          <os-table-form ref="returnItemDetails"
            :schema="schema" :data="ctx" :items="ctx.returnItems"
            :remove-items="true" @remove-item="removeItem($event)"
          />

          <os-divider />

          <div class="os-footer">
            <os-button primary label="Submit" @click="returnSpecimens" />

            <os-button text label="Cancel" @click="cancel" />
          </div>
        </div>
      </div>
    </os-page-body>
  </os-page>
</template>

<script>

import ui from '@/global.js';

import orderSvc  from '@/administrative/services/Order.js';
import alertSvc  from '@/common/services/Alerts.js';
import routerSvc from '@/common/services/Router.js';

import schema from '@/administrative/schemas/orders/return-specimens.js';

export default {
  data() {
    return {
      ctx: { 
        returnItems: []
      },

      schema,
    }
  },

  created() {
    this.ctx.bcrumb = [
      {url: routerSvc.getUrl('OrdersList', {orderId: -1}), label: 'Orders'}
    ],

    this.returnDate = new Date();
  },

  methods: {
    addSpecimens: async function({specimens, useBarcode}) {
      const spmnIds = specimens.reduce(
        (acc, spmn) => {
          acc[spmn.id] = spmn;
          return acc;
        },
        {}
      );

      const orderItems = await orderSvc.getOrderItemsBySpecimenIds(Object.keys(spmnIds));

      const returnItemsMap = this.returnItemsMap = this.returnItemsMap || {};
      for (let orderItem of orderItems) {
        delete spmnIds[orderItem.specimen.id];
        if (returnItemsMap[orderItem.id]) {
          continue;
        }

        returnItemsMap[orderItem.id] = true;
        this.ctx.returnItems.push({
          item: {
            specimenLabel: orderItem.specimen.label,
            orderName: orderItem.orderName,
            itemId: orderItem.id,
            quantity: orderItem.quantity,
            distributedQty: orderItem.quantity,
            location: {},
            user: ui.currentUser,
            time: this.returnDate,
            incrFreezeThaw: 1,
            specimen: orderItem.specimen
          }
        });
      }

      const errorSpmns = Object.values(spmnIds);
      if (errorSpmns.length > 0) {
        const attr = useBarcode ? 'barcode' : 'label';
        const labels = errorSpmns.reduce(
          (acc, spmn) => {
            acc += (acc ? ', ' : '') + spmn[attr];
            return acc;
          },
          ''
        );

        alertSvc.error('Following specimens are either not distributed or already returned: ' + labels);
      } 
    },

    removeItem: function({item, idx}) {
      delete this.returnItemsMap[item.item.itemId];
      this.ctx.returnItems.splice(idx, 1);
    },

    returnSpecimens: async function() {
      if (!this.$refs.returnItemDetails.validate()) {
        return;
      }

      const payload = this.ctx.returnItems.map(
        ({item}) => ({
          specimenLabel: item.specimenLabel,
          orderName: item.orderName,
          itemId: item.id,
          quantity: item.quantity,
          location: item.location,
          user: item.user,
          time: item.time,
          incrFreezeThaw: item.incrFreezeThaw,
          comments: item.comments 
        })
      );

      const resp = await orderSvc.returnSpecimens(payload);
      alertSvc.success(resp.length + (resp.length == 1 ? ' specimen' : ' specimens') + ' returned!');
      routerSvc.goto('OrdersList', {orderId: -1});
    },

    cancel: function() {
      routerSvc.goto('OrdersList', {orderId: -1});
    }
  }
}
</script>
