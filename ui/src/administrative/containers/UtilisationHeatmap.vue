<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <h3>
        <span v-t="'containers.view_utilisation_heatmap'">Utilisation Heatmap</span>
      </h3>
    </os-page-head>

    <os-page-body>
      <os-page-toolbar>
        <template #default>
          <os-container-utilisation-legend />
        </template>

        <template #right>
          <os-button left-icon="search" :label="$t('common.buttons.search')" @click="openSearch" />
        </template>
      </os-page-toolbar>

      <os-list-view :data="ctx.containers" :schema="listSchema" :query="filters"
        :hide-page-size-selector="true" :filters-only="true"
        @filtersUpdated="loadContainers" ref="listView">
        <template #results>
          <div class="os-freezer-utilisation">
            <os-message type="info" v-if="ctx.loading">
              <span v-t="'containers.loading_utilisation_map'">
                Loading utilisation map. Please wait for a moment
              </span>
            </os-message>

            <div class="utilisation-grid" v-if="ctx.containers.length > 0">
              <os-container-utilisation-tile :container="container"
                v-for="container of ctx.containers" :key="container.id" />
            </div>

            <os-message type="info" v-else-if="!ctx.loading">
              <span v-t="'containers.no_containers_match_criteria'">No containers match the criteria.</span>
            </os-message>

            <div class="view-more" v-if="ctx.hasMore">
              <os-button :label="$t('common.buttons.view_more')" @click="loadMore" />
            </div>
          </div>
        </template>
      </os-list-view>
    </os-page-body>
  </os-page>
</template>

<script>
import { reactive } from 'vue';

import listSchema   from '@/administrative/schemas/containers/list.js';
import containerSvc from '@/administrative/services/Container.js';
import routerSvc    from '@/common/services/Router.js';

const PAGE_SIZE = 20;

export default {
  props: ['filters'],

  setup() {
    const ctx = reactive({
      bcrumb: [
        {
          url: routerSvc.getUrl('ContainersList', {containerId: -1}),
          label: window.osSvc.i18nSvc.msg('containers.list')
        }
      ],

      containers: [],

      filterValues: {},

      hasMore: false,

      loading: true,

      loadId: 0
    });

    return { ctx, listSchema };
  },

  methods: {
    openSearch: function() {
      this.$refs.listView.toggleShowFilters();
    },

    loadContainers: async function({filters, uriEncoding}) {
      this.ctx.filterValues = {...filters};
      this.ctx.containers = [];
      this.ctx.hasMore = false;
      const loadId = ++this.ctx.loadId;
      await this._loadMore(loadId);
      if (loadId == this.ctx.loadId) {
        routerSvc.replace('ContainersUtilisation', {}, {filters: uriEncoding});
      }
    },

    loadMore: async function() {
      if (this.ctx.loading) {
        return;
      }

      await this._loadMore(this.ctx.loadId);
    },

    _loadMore: async function(loadId) {
      this.ctx.loading = true;
      try {
        const containers = await containerSvc.getTopLevelContainersUtilisation({
          ...this.ctx.filterValues,
          startAt: this.ctx.containers.length,
          maxResults: PAGE_SIZE + 1
        });
        if (loadId != this.ctx.loadId) {
          return;
        }

        this.ctx.containers.push(...containers.slice(0, PAGE_SIZE));
        this.ctx.hasMore = containers.length > PAGE_SIZE;
      } finally {
        if (loadId == this.ctx.loadId) {
          this.ctx.loading = false;
        }
      }
    }
  }
}
</script>

<style scoped>
.os-freezer-utilisation {
  padding: 1rem;
}

.utilisation-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(10rem, 1fr));
  gap: 0.75rem;
}

.view-more {
  display: flex;
  justify-content: center;
  margin-top: 1rem;
}
</style>
