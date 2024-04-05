<template>
  <os-page>
    <os-page-head :noNavButton="noNavButton" :showBreadcrumb="true">
      <template #breadcrumb>
        <os-breadcrumb :items="bcrumb" />
      </template>

      <span class="os-title">
        <h3>
          <span>{{specimen.label || specimen.type}}</span>
        </h3>

        <div class="accessories">
          <os-tag :value="status" :rounded="true" :type="statusType" />

          <os-tag :value="$t('specimens.checked_out')" :rounded="true" :type="'danger'" v-if="specimen.checkedOut" />

          <os-copy-link size="small" :route="{
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

            <li v-if="specimen.id > 0">
              <router-link :to="getRoute('Forms')">
                <span v-t="'common.forms'">Forms</span>
              </router-link>
            </li>

            <os-plugin-views page="specimen-detail" view="tab-menu" />
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

        <router-view :cpr="cpr" :visit="visit" :specimen="specimen"
          v-if="specimen && (specimen.id > 0 || specimen.reqId > 0)" />
      </div>
    </os-page-body>
  </os-page>
</template>

<script>

import cpSvc from '@/biospecimen/services/CollectionProtocol.js';
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['cpr', 'visit', 'specimen', 'noNavButton'],

  inject: ['cpViewCtx'],

  data() {
    const cp = this.cpViewCtx.getCp();
    return { ctx: { cp } };
  },

  created() {
    const route = this.$route.matched[this.$route.matched.length - 1];
    this.detailRouteName = route.name.split('.')[0];
    this.query = {};
    if (this.$route.query) {
      Object.assign(this.query, {filters: this.$route.query.filters});
    }
  },

  computed: {
    bcrumb: function() {
      const cp = this.ctx.cp;
      const {query} = routerSvc.getCurrentRoute();
      const {cpId, cprId, visitId, eventId, parentId, parentLabel} = this.specimen;
      const parentSpmnUrl = [];
      if (parentId > 0) {
        parentSpmnUrl.push({
          url: routerSvc.getUrl(
            'ParticipantsListItemSpecimenDetail.Overview',
            {cpId, cprId, visitId, specimenId: parentId},
            Object.assign({eventId}, query || {})
          ),
          label: parentLabel
        });
      }

      if (cp.specimenCentric) {
        return [
          {
            url: routerSvc.getUrl('ParticipantsList', {cpId, cprId: -1}, {view: 'specimens_list'}),
            label: cp.shortTitle
          },

          ...parentSpmnUrl
        ];
      }

      return [
        {
          url: routerSvc.getUrl('ParticipantsList', {cpId, cprId: -1}),
          label: cp.shortTitle
        },
        {
          url: routerSvc.getUrl('ParticipantsListItemDetail.Overview', {cpId, cprId}),
          label: this.cpr.ppid
        },
        {
          url: routerSvc.getUrl('ParticipantsListItemVisitDetail.Overview', {cpId, cprId, visitId}, {eventId}),
          label: cpSvc.getEventDescription(this.visit)
        },
        ...parentSpmnUrl
      ];
    },

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
