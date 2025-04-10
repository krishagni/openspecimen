<template>
  <os-page>
    <os-page-head :noNavButton="noNavButton">
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span class="os-title">
        <h3>{{ctx.dp.shortTitle}}</h3>
        <div class="accessories" v-if="ctx.dp && ctx.dp.id > 0">
          <os-tag :value="$t('common.status.closed')" :rounded="true" type="danger"
            v-if="ctx.dp.activityStatus == 'Closed'" />
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
                <span v-t="'common.overview'">Overview</span>
              </router-link>
            </li>

            <li>
              <router-link :to="getRoute('Consents')">
                <span v-t="'dps.consents.list'">Consents</span>
              </router-link>
            </li>

            <li>
              <router-link :to="getRoute('ReservedSpecimens')">
                <span v-t="'dps.reserved_specimens'">Reserved Specimens</span>
              </router-link>
            </li>

            <li>
              <router-link :to="getRoute('Requirements')">
                <span v-t="'dps.requirements'">Requirements</span>
              </router-link>
            </li>

            <os-plugin-views page="dp-detail" view="tab-menu" :view-props="{dp: ctx.dp}" />
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

            <li>
              <router-link :to="getRoute('Consents')">
                <os-icon name="check-square" />
                <span class="label" v-t="'dps.consents.list'">Consents</span>
              </router-link>
            </li>

            <li>
              <router-link :to="getRoute('ReservedSpecimens')">
                <os-icon name="flask" />
                <span class="label" v-t="'dps.reserved_specimens'">Reserved Specimens</span>
              </router-link>
            </li>

            <li>
              <router-link :to="getRoute('Requirements')">
                <os-icon name="list-ol" />
                <span class="label" v-t="'dps.requirements'">Requirements</span>
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
          {url: routerSvc.getUrl('DpsList', {dpId: -1}), label: this.$t('dps.list')}
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
