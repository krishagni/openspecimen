<template>
  <os-page>
    <os-page-head :noNavButton="noNavButton">
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span class="os-title">
        <h3>{{ctx.institute.name}}</h3>
        <div class="accessories" v-if="ctx.institute && ctx.institute.id > 0">
          <os-copy-link size="small"
            :route="{name: 'InstituteDetail.Overview', params: {instituteId: ctx.institute.id}}" />
          <os-new-tab size="small"
            :route="{name: 'InstituteDetail.Overview', params: {instituteId: ctx.institute.id}}" />
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

            <os-plugin-views page="institute-detail" view="tab-menu" />
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

            <os-plugin-views page="institute-detail" view="side-menu" />
          </ul>
        </os-side-menu>

        <router-view :institute="ctx.institute" v-if="ctx.institute.id" />
      </div>
    </os-page-body>
  </os-page>
</template>

<script>

import routerSvc    from '@/common/services/Router.js';
import instituteSvc from '@/administrative/services/Institute.js';

export default {
  props: ['instituteId', 'noNavButton'],

  data() {
    return {
      ctx: {
        institute: {},

        bcrumb: [
          {url: routerSvc.getUrl('InstitutesList', {instituteId: -1}), label: this.$t('institutes.list')}
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

    this.loadInstitute();
  },

  watch: {
    instituteId: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this.loadInstitute();
      }
    }
  },

  methods: {
    loadInstitute: async function() {
      this.ctx.institute = await instituteSvc.getInstitute(+this.instituteId);
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
