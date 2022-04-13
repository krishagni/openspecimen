<template>
  <os-page>
    <os-page-head :noNavButton="noNavButton">
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span class="os-title">
        <h3>{{ctx.dp.shortTitle}}</h3>
        <div class="accessories" v-if="ctx.dp && ctx.dp.id > 0">
          <os-tag value="Closed" :rounded="true" type="danger" v-if="ctx.dp.activityStatus == 'Closed'" />
          <os-copy-link size="small" :route="{name: 'DpDetail.Overview', params: {dpId: ctx.dp.id}}" />
          <os-new-tab size="small" :route="{name: 'DpDetail.Overview', params: {dpId: ctx.dp.id}}" />
        </div>
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

            <li>
              <router-link :to="getRoute('Consents')">
                <span>Consents</span>
              </router-link>
            </li>

            <li>
              <router-link :to="getRoute('ReservedSpecimens')">
                <span>Reserved Specimens</span>
              </router-link>
            </li>

            <li>
              <router-link :to="getRoute('Requirements')">
                <span>Requirements</span>
              </router-link>
            </li>

            <os-plugin-views page="dp-detail" view="tab-menu" :view-props="{dp: ctx.dp}" />
          </ul>
        </os-tab-menu>

        <os-side-menu v-else>
          <ul>
            <li v-os-tooltip.right="'Overview'">
              <router-link :to="getRoute('Overview')">
                <os-icon name="eye" />
              </router-link>
            </li>

            <li v-os-tooltip.right="'Consents'">
              <router-link :to="getRoute('Consents')">
                <os-icon name="check-square" />
              </router-link>
            </li>

            <li v-os-tooltip.right="'Reserved Specimens'">
              <router-link :to="getRoute('ReservedSpecimens')">
                <os-icon name="flask" />
              </router-link>
            </li>

            <li v-os-tooltip.right="'Requirements'">
              <router-link :to="getRoute('Requirements')">
                <os-icon name="list-ol" />
              </router-link>
            </li>

            <os-plugin-views page="dp-detail" view="side-menu" :view-props="{dp: ctx.dp}" />
          </ul>
        </os-side-menu>

        <router-view :dp="ctx.dp" v-if="ctx.dp.id" />
      </div>
    </os-page-body>
  </os-page>
</template>

<script>
import formUtil  from '@/common/services/FormUtil.js';
import routerSvc from '@/common/services/Router.js';
import dpSvc     from '@/administrative/services/DistributionProtocol.js';

export default {
  props: ['dpId', 'noNavButton'],

  data() {
    return {
      ctx: {
        dp: {},

        bcrumb: [
          {url: routerSvc.getUrl('DpsList', {dpId: -1}), label: 'Distribution Protocols'}
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

    this.loadDp();
  },

  watch: {
    dpId: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this.loadDp();
      }
    }
  },

  methods: {
    loadDp: async function() {
      this.ctx.dp = await dpSvc.getDp(+this.dpId);
      formUtil.createCustomFieldsMap(this.ctx.dp, true);
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
