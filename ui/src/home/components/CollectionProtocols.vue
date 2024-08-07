<template>
  <os-home-list-card :icon="'calendar-alt'" :title="$t('common.home.cps')"
    :show-star="true" :list-url="{ngState: 'cps'}" :list="ctx.cps"
    @search="search($event)" @toggle-star="toggleStar($event)" />
</template>

<script>

import cpSvc from '@/biospecimen/services/CollectionProtocol.js';
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['widget'],

  data() {
    return {
      ctx: {
        cps: [],

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

    toggleStar: function(cp) {
      const promise = cp.starred ? cpSvc.unstar(cp) : cpSvc.star(cp);
      promise.then(
        () => {
          this.ctx.defList = null;
          this._loadProtocols(this.ctx.search);
        }
      );
    },

    _loadProtocols: function(searchTerm) {
      if (!searchTerm && this.ctx.defList) {
        this.ctx.cps = this.ctx.defList;
        return;
      }

      cpSvc.getCps({detailedList: false, query: searchTerm, orderByStarred: true, maxResults: 25}).then(
        (cps) => {
          this.ctx.cps = cps;
          if (!searchTerm) {
            this.ctx.defList = cps;
          }

          cps.forEach(
            cp => {
              cp.displayName = cp.shortTitle;
              cp.url = routerSvc.getUrl('ParticipantsList', {cpId: cp.id, cprId: -1});
            }
          );
        }
      );
    }
  }
}
</script>
