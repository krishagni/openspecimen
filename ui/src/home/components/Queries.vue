<template>
  <os-home-list-card :icon="'tachometer-alt'" :title="$t('common.home.queries')"
    :show-star="true" :list-url="{name: 'QueriesList'}" :list="ctx.queries"
    @search="search($event)" @toggle-star="toggleStar($event)" />
</template>

<script>

import http from '@/common/services/HttpClient.js';
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['widget'],

  data() {
    return {
      ctx: {
        queries: [],

        defList: null,

        search: null
      }
    };
  },

  created() {
    this._loadQueries();
  },

  methods: {
    search: function({searchTerm}) {
      this.ctx.search = searchTerm;
      this._loadQueries(searchTerm);
    },

    toggleStar: function(query) {
      const promise = query.starred ? this._unstar(query) : this._star(query);
      promise.then(
        () => {
          this.ctx.defList = null;
          this._loadQueries(this.ctx.search);
        }
      );
    },

    _loadQueries: function(searchTerm) {
      if (!searchTerm && this.ctx.defList) {
        this.ctx.queries = this.ctx.defList;
        return;
      }

      const userId = this.$ui.currentUser.id;
      http.get('saved-queries', {searchString: searchTerm, orderByStarred: true, userId, max: 25}).then(
        ({queries}) => {
          this.ctx.queries = queries;
          if (!searchTerm) {
            this.ctx.defList = queries;
          }

          queries.forEach(
            query => {
              query.displayName = '#' + query.id + ' ' + query.title;
              query.url = routerSvc.getUrl('QueryDetail.Results', {queryId: query.id});
            }
          );
        }
      );
    },

    _star: function({id}) {
      return http.post('saved-queries/' + id + '/labels', {});
    },

    _unstar: function({id}) {
      return http.delete('saved-queries/' + id + '/labels', {});
    }
  }
}
</script>
