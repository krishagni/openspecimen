<template>
  <os-page>
    <os-page-head :noNavButton="noNavButton">
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span class="os-title">
        <h3>{{ctx.site.name}}</h3>
        <div class="accessories" v-if="ctx.site && ctx.site.id > 0">
          <os-copy-link size="small" :route="{name: 'SiteDetail.Overview', params: {siteId: ctx.site.id}}" />
          <os-new-tab size="small" :route="{name: 'SiteDetail.Overview', params: {siteId: ctx.site.id}}" />
        </div>
      </span>
    </os-page-head>
    <os-page-body>
      <div>
        <os-tab-menu v-if="noNavButton">
          <ul>
            <li>
              <router-link :to="getRoute('Overview')">
                <span v-t="'common.overview'">Overview</span>
              </router-link>
            </li>

            <os-plugin-views page="site-detail" view="tab-menu" />
          </ul>
        </os-tab-menu>

        <os-side-menu v-else>
          <ul>
            <li>
              <router-link :to="getRoute('Overview')">
                <os-icon name="eye" />
                <span class="label" v-t="'common.overview'">Overview</span>
              </router-link>
            </li>

            <os-plugin-views page="site-detail" view="side-menu" />
          </ul>
        </os-side-menu>

        <router-view :site="ctx.site" v-if="ctx.site.id"> </router-view>
      </div>
    </os-page-body>
  </os-page>
</template>

<script>

import routerSvc   from '@/common/services/Router.js';
import formUtil    from '@/common/services/FormUtil.js';
import siteSvc     from '@/administrative/services/Site.js';

export default {
  props: ['siteId', 'noNavButton'],

  data() {
    return {
      ctx : {
        site: {},

        bcrumb: [
          {url: routerSvc.getUrl('SitesList', {siteId: -1}), label: 'Sites'}
        ]
      }
    };
  },

  created() {
    const route = this.$route.matched[this.$route.matched.length - 1];
    this.detailRouteName = route.name.split('.')[0];
    this.query = {};
    if (this.$route.query) {
      Object.assign(this.query, {filters: this.$route.query.filters});
    }

    this.loadSite();
  },

  watch: {
    siteId: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this.loadSite();
      }
    }
  },

  methods: {
    loadSite: async function() {
      this.ctx.site = await siteSvc.getSite(+this.siteId);
      formUtil.createCustomFieldsMap(this.ctx.site, true);
    },

    getRoute: function(routeName, params, query) {
      return {
        name: this.detailRouteName + '.' + routeName,
        params: params,
        query: {...this.query, query}
      }
    }
  }
}
</script>
