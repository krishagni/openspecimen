<template>
  <os-page>
    <os-page-head :noNavButton="listItemDetailView">
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span class="os-title">
        <h3>{{ctx.type.name}}</h3>
        <div class="accessories" v-if="ctx.type && ctx.type.id > 0">
          <os-copy-link size="small" :route="{name: 'ContainerTypeDetail.Overview', params: {typeId: ctx.type.id}}" />
          <os-new-tab size="small" :route="{name: 'ContainerTypeDetail.Overview', params: {typeId: ctx.type.id}}" />
        </div>
      </span>
    </os-page-head>
    <os-page-body>
      <div>
        <os-tab-menu v-if="listItemDetailView">
          <ul>
            <li>
              <router-link :to="getRoute('Overview')">
                <span>Overview</span>
              </router-link>
            </li>
          </ul>
        </os-tab-menu>

        <os-side-menu v-else>
          <ul>
            <li v-os-tooltip.right="'Overview'">
              <router-link :to="getRoute('Overview')">
                <os-icon name="eye" />
              </router-link>
            </li>
          </ul>
        </os-side-menu>

        <router-view :type="ctx.type" v-if="ctx.type.id"> </router-view>
      </div>
    </os-page-body>
  </os-page>
</template>

<script>
import routerSvc   from '@/common/services/Router.js';
import typesSvc    from '@/administrative/services/ContainerType.js';

export default {
  props: ['typeId', 'listItemDetailView'],

  data() {
    return {
      ctx: {
        type: {},

        bcrumb: [
          {url: routerSvc.getUrl('ContainerTypesList', {typeId: -1}), label: 'Container Types'}
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

    this.loadType();
  },

  watch: {
    typeId: function(newVal, oldVal) {
      if (oldVal != newVal) {
        this.loadType();
      }
    }
  },

  methods: {
    loadType: async function() {
      this.ctx.type = await typesSvc.getType(+this.typeId);
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
