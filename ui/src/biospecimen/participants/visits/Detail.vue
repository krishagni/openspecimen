<template>
  <os-page>
    <os-page-head :noNavButton="noNavButton" :showBreadcrumb="true">
      <template #breadcrumb>
        <os-breadcrumb :items="bcrumb" />
      </template>

      <span class="os-title" v-if="visit">
        <h3>
          <os-visit-event-desc :visit="visit" />
        </h3>
        <div class="accessories">
          <os-tag :value="status" :rounded="true" :type="statusType" />
          <os-copy-link size="small"
            :route="{name: 'VisitDetail.Overview', params: {cpId: ctx.cp.id, cprId: cpr.id, visitId: visit.id},
              query: {eventId: visit.eventId}}" />
          <os-new-tab size="small"
            :route="{name: 'VisitDetail.Overview', params: {cpId: ctx.cp.id, cprId: cpr.id, visitId: visit.id},
              query: {eventId: visit.eventId}}" />
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

            <li v-if="!ctx.cp.consentsWaived && ctx.cp.visitLevelConsents">
              <router-link :to="getRoute('Consents')">
                <span v-t="'participant_consents.list'">Consents</span>
              </router-link>
            </li>

            <li>
              <router-link :to="getRoute('Report')">
                <span v-t="'visits.path_report'">Path Report</span>
              </router-link>
            </li>

            <li>
              <router-link :to="getRoute('Forms')">
                <span v-t="'common.forms'">Forms</span>
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

        <router-view :cpr="cpr" :visit="visit" v-if="visit && (visit.id > 0 || visit.eventId > 0)" />
      </div>
    </os-page-body>
  </os-page>
</template>

<script>

import routerSvc from '@/common/services/Router.js';

export default {
  props: ['cpr', 'visit', 'noNavButton'],

  inject: ['cpViewCtx'],

  data() {
    const ctx = {
      cp: {},
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

    this.cpViewCtx.getCp().then(cp => this.ctx.cp = cp);
  },

  computed: {
    bcrumb: function() {
      const cp = this.ctx.cp;
      return [
        {
          url: routerSvc.getUrl('ParticipantsList', {cpId: cp.id, cprId: -1}),
          label: cp.shortTitle
        },
        {
          url: routerSvc.getUrl(
                 this.noNavButton ? 'ParticipantsListItemDetail.Overview' : 'ParticipantDetail.Overview',
                 {cpId: cp.id, cprId: this.cpr.id}
               ),
          label: this.cpr.ppid
        }
      ];
    },

    status: function() {
      return this.visit.status || 'Pending';
    },

    statusType: function() {
      switch (this.status) {
        case 'Pending':
          return 'warning';
        case 'Missed Collection':
        case 'Not Collected':
          return 'missed';
        case 'Complete':
          return 'success';
      }

      return '';
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
