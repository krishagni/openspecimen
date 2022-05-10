
<template>
  <os-page-toolbar>
    <template #default>
      <os-specimen-actions :specimens="selectedSpecimens" @reloadSpecimens="reloadList" />

      <os-add-to-cart :specimens="selectedSpecimens" />

      <os-button left-icon="download" label="Download Report" @click="downloadReport" />
    </template>

    <template #right v-if="showListSize">
      <os-list-size
        :list="$refs.specimensList.list.rows"
        :page-size="$refs.specimensList.pageSize"
        :list-size="$refs.specimensList.size"
        @updateListSize="getSpecimensCount"
      />

      <os-button left-icon="search" label="Search" @click="toggleSearch" />
    </template>
  </os-page-toolbar>

  <os-query-list-view
    name="container-specimens-list-view"
    :object-id="container.id"
    :allow-selection="true"
    :include-count="includeCount"
    url="'#/specimens/' + hidden.specimenId"
    :newTab="true"
    @selectedRows="onSpecimenSelection"
    @rowClicked="onSpecimenRowClick"
    @listLoaded="onListLoad"
    ref="specimensList"
  />
</template>

<script>

import containerSvc from '@/administrative/services/Container.js';
import routerSvc  from '@/common/services/Router.js';
import util       from '@/common/services/Util.js';

export default {
  props: ['container'],

  data() {
    return {
      selectedSpecimens: [],

      includeCount: false,

      showListSize: false,

      hasRows: false
    }
  },

  methods: {
    toggleSearch: function() {
      this.$refs.specimensList.toggleShowFilters();
    },

    reloadList: function() {
      this.$refs.specimensList.reload();
    },

    onSpecimenSelection: function(specimens) {
      this.selectedSpecimens = specimens.map(({rowObject}) => ({id: +rowObject.hidden.specimenId, cpId: +rowObject.hidden.cpId}));
    },

    onSpecimenRowClick: function(event) {
      const spmnId = +event.hidden.specimenId;
      routerSvc.ngGoto('specimens/' + spmnId, {}, true);
    },

    getSpecimensCount: function() {
      this.$refs.specimensList.loadListSize();
    },

    onListLoad: function(list) {
      this.showListSize = true;
      this.selectedSpecimens.length = 0;
      this.hasRows = list.rows && list.rows.length > 0;
    },

    downloadReport: function() {
      const reportFn = () => containerSvc.generateSpecimensReport(this.container);
      util.downloadReport(reportFn, {filename: this.container.name + '.csv'});
    }
  }
}
</script>
