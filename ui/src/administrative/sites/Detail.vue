<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3>{{ctx.site.name}}</h3>
      </span>
    </os-page-head>
    <os-page-body>
      <os-side-menu>
        <ul>
          <li>
            <router-link :to="{name: 'SiteOverview'}">
              <os-icon name="eye" />
            </router-link>
          </li>

          <os-plugin-views page="site-detail" view="side-menu" />
        </ul>
      </os-side-menu>

      <router-view :site="ctx.site" v-if="ctx.site.id"> </router-view>
    </os-page-body>
  </os-page>
</template>

<script>
import { reactive } from 'vue';

import routerSvc   from '@/common/services/Router.js';
import siteSvc     from '@/administrative/services/Site.js';

export default {
  props: ['siteId'],

  setup(props) {
    let ctx = reactive({
      site: {},

      bcrumb: [
        {url: routerSvc.getUrl('SitesList'), label: 'Sites'}
      ]
    });

    
    siteSvc.getSite(+props.siteId).then(site => ctx.site = site);
    return { ctx };
  },

  methods: {
  }
}
</script>
