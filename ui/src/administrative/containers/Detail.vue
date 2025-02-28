<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span class="os-title">
        <h3>{{displayTitle}}</h3>
        <div class="accessories" v-if="ctx.container && ctx.container.id > 0">
          <os-tag :value="status" :rounded="true" :type="tagType" v-show="status" />
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
            <li>
              <router-link :to="getRoute('Overview')">
                <os-icon name="eye" />
                <span class="label" v-t="'common.overview'">Overview</span>
              </router-link>
            </li>
            <li v-if="ctx.container.activityStatus == 'Active'">
              <router-link :to="getRoute('Locations')">
                <os-icon name="map" />
                <span class="label" v-t="'containers.locations'">Locations</span>
              </router-link>
            </li>
            <li v-if="ctx.container.activityStatus == 'Active'">
              <router-link :to="getRoute('Specimens')">
                <os-icon name="flask" />
                <span class="label" v-t="'containers.specimens'">Specimens</span>
              </router-link>
            </li>
            <li>
              <router-link :to="getRoute('TransferEvents')">
                <os-icon name="arrows-alt-h" />
                <span class="label" v-t="'containers.transfer_events'">Transfer</span>
              </router-link>
            </li>
            <li v-if="!ctx.container.storageLocation || !ctx.container.storageLocation.id &&
              ctx.container.activityStatus == 'Active'">
              <router-link :to="getRoute('Maintenance')">
                <os-icon name="calendar" />
                <span class="label" v-t="'containers.maintenance'">Maintenance</span>
              </router-link>
            </li>
          </ul>
        </os-side-menu>

        <os-grid style="height: 100%;">
          <os-grid-column :width="3" v-show="ctx.showTree">
            <ContainerTree :container="ctx.container" @container-selected="selectContainer" v-if="ctx.container.id" />
          </os-grid-column>
          <os-grid-column :width="ctx.showTree ? 9 : 12">
            <router-view :container="ctx.container" v-if="ctx.container.id" @toggle-container-tree="toggleTree" />
          </os-grid-column>
        </os-grid>
      </div>
    </os-page-body>
  </os-page>
</template>

<script>
import { reactive } from 'vue';

import i18n         from '@/common/services/I18n.js';
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
        {url: routerSvc.getUrl('ContainersList', {containerId: -1}), label: i18n.msg('containers.list')}
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

  computed: {
    displayTitle: function() {
      let title = this.ctx.container.name;
      if (this.ctx.container.displayName) {
        title = this.ctx.container.displayName + ' (' + title + ')';
      }

      return title;
    },

    status: function() {
      const container = this.ctx.container;
      if (container.activityStatus == 'Closed') {
        return this.$t('containers.status.archived');
      } else if (container.status == 'CHECKED_OUT') {
        return this.$t('containers.status.checked_out');
      }

      return null;
    },

    tagType: function() {
      const container = this.ctx.container;
      if (container.activityStatus == 'Closed' || container.status == 'CHECKED_OUT') {
        return 'danger';
      }

      return null;
    }
  },

  methods: {
    loadContainer: async function() {
      this.ctx.container = await containerSvc.getContainer(+this.containerId);
      formUtil.createCustomFieldsMap(this.ctx.container, true);
    },

    selectContainer: function(container) {
      const route = this.$route.matched[this.$route.matched.length - 1];
      this.$goto(route.name, {containerId: container.id});
    },

    toggleTree: function({show}) {
      this.ctx.showTree = show;
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
