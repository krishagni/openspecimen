<template>
  <os-page>
    <os-page-head :noNavButton="noNavButton">
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span class="os-title">
        <os-breadcrumb :items="ctx.bcrumb" v-if="noNavButton" />

        <h3>
          <os-visit-event-desc :visit="visit" />
        </h3>
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

            <os-plugin-views page="visit-detail" view="tab-menu" />
          </ul>
        </os-tab-menu>

        <os-side-menu v-else>
          <ul>
            <li v-os-tooltip.right="$t('common.overview')">
              <router-link :to="getRoute('Overview')">
                <os-icon name="eye" />
              </router-link>
            </li>

            <os-plugin-views page="visit-detail" view="side-menu" />
          </ul>
        </os-side-menu>

        <router-view :cpr="cpr" :visit="visit" v-if="visit && visit.id > 0" />
      </div>
    </os-page-body>
  </os-page>
</template>

<script>

export default {
  props: ['cpr', 'visit', 'noNavButton'],

  data() {
    const ctx = {
      bcrumb: [
        // {url: routerSvc.getUrl('ParticipantsList', {cprId: -1}), label: cp.shortTitle}, // TODO: CP conf list view
        // {url: routerSvc.getUrl('ParticipantsList', {cprId: -1}), label: cpr.ppid}
      ]
    };

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
