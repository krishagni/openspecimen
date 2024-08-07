<template>
  <os-home-list-card :icon="'paper-plane'" :title="$t('common.home.shipments')"
    :show-star="false" :list-url="{name: 'ShipmentsList', params: {shipmentId: -1}}" :list="ctx.shipments"
    @search="search($event)" />
</template>

<script>

import shipmentSvc from '@/administrative/services/Shipment.js';
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['widget'],

  data() {
    return {
      ctx: {
        shipments: [],

        defList: null,

        search: null
      }
    };
  },

  created() {
    this._loadShipments();
  },

  methods: {
    search: function({searchTerm}) {
      this.ctx.search = searchTerm;
      this._loadShipments(searchTerm);
    },

    _loadShipments: function(searchTerm) {
      if (!searchTerm && this.ctx.defList) {
        this.ctx.shipments = this.ctx.defList;
        return;
      }

      shipmentSvc.getShipments({name: searchTerm, orderByStarred: true, maxResults: 25}).then(
        (shipments) => {
          this.ctx.shipments = shipments;
          if (!searchTerm) {
            this.ctx.defList = shipments;
          }

          shipments.forEach(
            shipment => {
              shipment.displayName = '#' + shipment.id + ' ' + shipment.name;
              shipment.url = routerSvc.getUrl('ShipmentsListItemDetail.Overview', {shipmentId: shipment.id});
            }
          );
        }
      );
    }
  }
}
</script>
