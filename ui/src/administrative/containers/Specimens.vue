<template>
  <os-page-toolbar>
    <template #default>
      <os-specimen-actions :specimens="selectedSpecimens" @reloadSpecimens="reloadList" />

      <os-add-to-cart :specimens="selectedSpecimens" />

      <os-button left-icon="download" :label="$t('containers.download_report')" @click="downloadReport" />
    </template>

    <template #right v-if="showListSize">
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
    name="container-specimens-list-view"
    :object-id="container.id"
    :allow-selection="true"
    :include-count="includeCount"
    url="'#/specimen-resolver/' + hidden.specimenId"
    :newTab="true"
    @selectedRows="onSpecimenSelection"
    @rowClicked="onSpecimenRowClick"
    @listLoaded="onListLoad"
    ref="specimensList"
  />
</template>

<script>

import containerSvc from '@/administrative/services/Container.js';
import routerSvc    from '@/common/services/Router.js';
import util         from '@/common/services/Util.js';

export default {
  props: ['container'],

  data() {
    return {
      selectedSpecimens: [],

      includeCount: false,

      showListSize: false
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
      const specimenId = +event.hidden.specimenId;
      routerSvc.goto('SpecimenResolver', {specimenId});
    },

    getSpecimensCount: function() {
      this.$refs.specimensList.loadListSize();
    },

    onListLoad: function() {
      this.showListSize = true;
      this.selectedSpecimens.length = 0;
    },

    downloadReport: function() {
      const reportFn = () => containerSvc.generateSpecimensReport(this.container);
      util.downloadReport(reportFn, {filename: this.container.name + '.csv'});
    }
  }
}
</script>
