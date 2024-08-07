<template>
  <os-home-list-card :icon="'truck-moving'" :title="$t('common.home.dps')"
    :show-star="true" :list-url="{name: 'DpsList', params: {dpId: -1}}" :list="ctx.dps"
    @search="search($event)" @toggle-star="toggleStar($event)" />
</template>

<script>

import dpSvc from '@/administrative/services/DistributionProtocol.js';
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['widget'],

  data() {
    return {
      ctx: {
        dps: [],

        defList: null,

        search: null
      }
    };
  },

  created() {
    this._loadProtocols();
  },

  methods: {
    search: function({searchTerm}) {
      this.ctx.search = searchTerm;
      this._loadProtocols(searchTerm);
    },

    toggleStar: function(dp) {
      const promise = dp.starred ? dpSvc.unstar(dp) : dpSvc.star(dp);
      promise.then(
        () => {
          this.ctx.defList = null;
          this._loadProtocols(this.ctx.search);
        }
      );
    },

    _loadProtocols: function(searchTerm) {
      if (!searchTerm && this.ctx.defList) {
        this.ctx.dps = this.ctx.defList;
        return;
      }

      dpSvc.getDps({includeStats: false, query: searchTerm, orderByStarred: true, maxResults: 25}).then(
        (dps) => {
          this.ctx.dps = dps;
          if (!searchTerm) {
            this.ctx.defList = dps;
          }

          dps.forEach(
            dp => {
              dp.displayName = dp.shortTitle;
              dp.url = routerSvc.getUrl('DpsListItemDetail.Overview', {dpId: dp.id});
            }
          );
        }
      );
    }
  }
}
</script>
