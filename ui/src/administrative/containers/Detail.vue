<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span class="os-title">
        <h3>{{ctx.container.name}}</h3>
        <div class="accessories" v-if="ctx.container && ctx.container.id > 0">
          <os-copy-link size="small"
            :route="{name: 'ContainerDetail.Overview', params: {containerId: ctx.container.id}}" />
          <os-new-tab size="small"
            :route="{name: 'ContainerDetail.Overview', params: {containerId: ctx.container.id}}" />
        </div>
      </span>
    </os-page-head>
    <os-page-body>
      <div>
        <os-side-menu>
          <ul>
            <li v-os-tooltip.right="'Overview'">
              <router-link :to="getRoute('Overview')">
                <os-icon name="eye" />
              </router-link>
            </li>
            <li v-os-tooltip.right="'Locations'">
              <router-link :to="getRoute('Locations')">
                <os-icon name="map" />
              </router-link>
            </li>
            <li v-os-tooltip.right="'Specimens'">
              <router-link :to="getRoute('Specimens')">
                <os-icon name="flask" />
              </router-link>
            </li>
            <li v-os-tooltip.right="'Transfer Events'">
              <router-link :to="getRoute('TransferEvents')">
                <os-icon name="arrows-alt-h" />
              </router-link>
            </li>
            <li v-os-tooltip.right="'Maintenance'"
              v-if="!ctx.container.storageLocation || !ctx.container.storageLocation.id">
              <router-link :to="getRoute('Maintenance')">
                <os-icon name="calendar" />
              </router-link>
            </li>
          </ul>
        </os-side-menu>

        <os-grid style="height: 100%;">
          <os-grid-column :width="3" v-show="ctx.showTree">
            <ContainerTree :container="ctx.container" @container-selected="selectContainer" v-if="ctx.container.id" />
          </os-grid-column>
          <os-grid-column :width="ctx.showTree ? 9 : 12">
            <router-view :container="ctx.container" v-if="ctx.container.id"> </router-view>
          </os-grid-column>
        </os-grid>
      </div>
    </os-page-body>
  </os-page>
</template>

<script>
import { reactive } from 'vue';

import routerSvc    from '@/common/services/Router.js';
import formUtil     from '@/common/services/FormUtil.js';
import containerSvc from '@/administrative/services/Container.js';

import ContainerTree from '@/administrative/containers/Tree.vue';

export default {
  props: ['containerId'],

  components: {
    ContainerTree
  },

  setup() {
    const ctx = reactive({
      container: {},

      bcrumb: [
        {url: routerSvc.getUrl('ContainersList', {containerId: -1}), label: 'Containers'}
      ],

      showTree: true
    });

    return { ctx };
  },

  created() {
    const route = this.$route.matched[this.$route.matched.length - 1];
    this.detailRouteName = route.name.split('.')[0];
    this.query = {};
    if (this.$route.query) {
      Object.assign(this.query, {filters: this.$route.query.filters});
    }

    this.ctx.showTree = (this.$route.name != 'ContainerDetail.Maintenance');
    this.loadContainer();
  },

  watch: {
    containerId: function(newVal, oldVal) {
      if (oldVal != newVal) {
        this.loadContainer();
      }
    },

    '$route': function(currRoute) {
      this.ctx.showTree = (currRoute.name != 'ContainerDetail.Maintenance');
    }
  },

  methods: {
    loadContainer: async function() {
      const container = this.ctx.container = await containerSvc.getContainer(+this.containerId, true);
      const root = !container.storageLocation || !container.storageLocation.id;
      if (root) {
        let storedSpmns = 0;
        for (let type of Object.keys(container.specimensByType)) {
          storedSpmns += container.specimensByType[type];
        }

        container.storedSpecimens = storedSpmns;
      }

      formUtil.createCustomFieldsMap(this.ctx.container, true);
    },

    selectContainer: function(container) {
      const route = this.$route.matched[this.$route.matched.length - 1];
      this.$goto(route.name, {containerId: container.id});
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
