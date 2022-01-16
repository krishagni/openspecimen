<template>
  <os-page>
    <os-page-head :noNavButton="noNavButton">
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span class="os-title">
        <h3>{{ctx.site.name}}</h3>
      </span>
    </os-page-head>
    <os-page-body>
      <div>
        <os-tab-menu v-if="noNavButton">
          <ul>
            <li>
              <router-link :to="getRoute('Overview')">
                <span>Overview</span>
              </router-link>
            </li>

            <os-plugin-views page="site-detail" view="tab-menu" />
          </ul>
        </os-tab-menu>

        <os-side-menu v-else>
          <ul>
            <li v-os-tooltip.right="'Overview'">
              <router-link :to="getRoute('Overview')">
                <os-icon name="eye" />
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
import { reactive, watchEffect } from 'vue';

import routerSvc   from '@/common/services/Router.js';
import formUtil    from '@/common/services/FormUtil.js';
import siteSvc     from '@/administrative/services/Site.js';

export default {
  props: ['siteId', 'noNavButton'],

  setup(props) {
    let ctx = reactive({
      site: {},

      bcrumb: [
        {url: routerSvc.getUrl('SitesList', {siteId: -1}), label: 'Sites'}
      ]
    });

    watchEffect(
      () => {
        siteSvc.getSite(+props.siteId).then(
          (site) => {
            ctx.site = site;
            formUtil.createCustomFieldsMap(site, true);
          }
        );
      }
    );
    return { ctx };
  },

  created() {
    const route = this.$route.matched[this.$route.matched.length - 1];
    this.detailRouteName = route.name.split('.')[0];
    this.query = {};
    if (this.$route.query) {
      Object.assign(this.query, {filters: this.$route.query.filters});
    }
  },

  methods: {
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
