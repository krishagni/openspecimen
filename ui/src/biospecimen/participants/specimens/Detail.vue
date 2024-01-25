<template>
  <os-page>
    <os-page-head :noNavButton="noNavButton">
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span class="os-title">
        <os-breadcrumb :items="ctx.bcrumb" v-if="noNavButton" />

        <h3>
          <span>{{specimen.label || specimen.type}}</span>
        </h3>

        <div class="accessories">
          <os-tag :value="status" :rounded="true" :type="statusType" />

          <os-copy-link size="small" :route="{
            name: 'ParticipantsListItemSpecimenDetail.Overview',
            params: {cpId: ctx.cp.id, cprId: cpr.id, visitId: visit.id, specimenId: specimen.id},
            query: {eventId: visit.eventId, srId: specimen.reqId}}" />

          <os-new-tab size="small" :route="{
            name: 'ParticipantsListItemSpecimenDetail.Overview',
            params: {cpId: ctx.cp.id, cprId: cpr.id, visitId: visit.id, specimenId: specimen.id},
            query: {eventId: visit.eventId, srId: specimen.reqId}}" />
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

            <os-plugin-views page="specimen-detail" view="side-menu" />
          </ul>
        </os-side-menu>

        <router-view :cpr="cpr" :visit="visit" :specimen="specimen" v-if="specimen && specimen.id > 0" />
      </div>
    </os-page-body>
  </os-page>
</template>

<script>

export default {
  props: ['cpr', 'visit', 'specimen', 'noNavButton'],

  inject: ['cpViewCtx'],

  data() {
    const ctx = {
      cp: {},

      bcrumb: [
        // {url: routerSvc.getUrl('ParticipantsList', {cprId: -1}), label: cp.shortTitle}, // TODO: CP conf list view
        // {url: routerSvc.getUrl('ParticipantsList', {cprId: -1}), label: cpr.ppid}
      ]
    };

    return { ctx };
  },

  created() {
    this.cpViewCtx.getCp().then(cp => this.ctx.cp = cp);

    const route = this.$route.matched[this.$route.matched.length - 1];
    this.detailRouteName = route.name.split('.')[0];
    this.query = {};
    if (this.$route.query) {
      Object.assign(this.query, {filters: this.$route.query.filters});
    }
  },

  computed: {
    status: function() {
      return this.specimen.availabilityStatus || 'Pending';
    },

    statusType: function() {
      switch(this.status) {
        case 'Available':
          return 'success';
        case 'Distributed':
          return 'distributed';
        case 'Reserved':
          return 'reserved';
        case 'Closed':
          return 'danger';
        case 'Missed Collection':
        case 'Not Collected':
          return 'missed';
        case 'Pending':
        default:
          return 'warning';
      }
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
