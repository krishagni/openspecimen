<template>
  <os-page-toolbar v-if="hasRows">
    <template #default>
      <span v-if="allowOrderCreation">
        <os-button left-icon="share" :label="$t('dps.distribute_all')" @click="distributeAll"
          v-if="!selectedSpecimens || selectedSpecimens.length == 0" />

        <os-button left-icon="share" :label="$t('dps.distribute')" @click="distribute"
          v-if="selectedSpecimens.length > 0" />

        <os-button left-icon="times" :label="$t('dps.cancel_reservation')" @click="cancelReservation"
          v-if="selectedSpecimens.length > 0" />
      </span>
    </template>

    <template #right>
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
    name="reserved-specimens-list-view"
    :object-id="dp.id"
    :allow-selection="allowOrderCreation"
    url="'#/specimen-resolver/' + hidden.specimenId"
    @selectedRows="onSpecimenSelection"
    @rowClicked="onSpecimenRowClick"
    @listLoaded="onListLoad"
    ref="specimensList"
  />
</template>

<script>

import authSvc   from '@/common/services/Authorization.js';
import routerSvc from '@/common/services/Router.js';
import util      from '@/common/services/Util.js';
import dpSvc     from '@/administrative/services/DistributionProtocol.js';

export default {
  props: ['dp'],

  data() {
    return {
      allowOrderCreation: false,

      selectedSpecimens: [],

      hasRows: false
    }
  },

  created() {
    this.allowOrderCreation = authSvc.isAllowed({resource: 'Order', operations: ['Create']});
  },

  methods: {
    onSpecimenSelection: function(specimens) {
      this.selectedSpecimens = specimens.map(({rowObject}) => +rowObject.hidden.specimenId);
    },

    onSpecimenRowClick: function(event) {
      const spmnId = +event.hidden.specimenId;
      routerSvc.goto('SpecimenResolver', {specimenId: spmnId});
    },

    getSpecimensCount: function() {
      this.$refs.specimensList.loadListSize();
    },

    onListLoad: function({list}) {
      this.selectedSpecimens.length = 0;
      this.hasRows = list.rows && list.rows.length > 0;
    },

    toggleSearch: function() {
      this.$refs.specimensList.toggleShowFilters();
    },

    distributeAll: function() {
      const dp = util.clone(this.dp);
      delete dp.distributingSites;

      localStorage['os.orderDetails'] = JSON.stringify({
        allReserved: true,
        dp: dp
      });

      routerSvc.goto('OrderAddEdit', {orderId: -1});
    },

    distribute: function() {
      const dp = util.clone(this.dp);
      delete dp.distributingSites;

      localStorage['os.orderDetails'] = JSON.stringify({
        specimenIds: this.selectedSpecimens,
        dp: dp
      });

      routerSvc.goto('OrderAddEdit', {orderId: -1});
    },

    cancelReservation: async function() {
      await dpSvc.cancelReservation(this.dp, this.selectedSpecimens);
      this.$refs.specimensList.reload();
    }
  }
}
</script>
