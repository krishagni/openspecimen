<template>
  <Page>
    <PageHeader>
      <template #breadcrumb>
        <Breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3>{{ctx.site.name}}</h3>
      </span>
    </PageHeader>
    <PageBody>
      <SideMenu>
        <ul>
          <li>
            <router-link :to="{name: 'SiteOverview'}">
              <Icon name="eye" />
            </router-link>
          </li>

          <PluginViews page="site-detail" view="side-menu" />
        </ul>
      </SideMenu>

      <router-view :site="ctx.site" v-if="ctx.site.id"> </router-view>
    </PageBody>
  </Page>
</template>

<script>
import { reactive } from 'vue';

import Page from '@/common/components/Page.vue';
import PageHeader from '@/common/components/PageHeader.vue';
import PageBody from '@/common/components/PageBody.vue';
import Breadcrumb from '@/common/components/Breadcrumb.vue';
import SideMenu from '@/common/components/SideMenu.vue';
import Icon from '@/common/components/Icon.vue';
import PluginViews from '@/common/components/PluginViews.vue';

import routerSvc   from '@/common/services/Router.js';
import siteSvc     from '@/administrative/services/Site.js';

export default {
  props: ['siteId'],

  components: {
    Page,
    PageHeader,
    PageBody,
    Breadcrumb,
    SideMenu,
    Icon,
    PluginViews
  },

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
