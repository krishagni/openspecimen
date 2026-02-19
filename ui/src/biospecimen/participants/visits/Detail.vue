<template>
  <os-page>
    <os-page-head :showBreadcrumb="true">
      <template #breadcrumb>
        <os-breadcrumb :items="bcrumb" />
      </template>

      <span class="os-title" v-if="visit">
        <h3>
          <os-dynamic-template v-if="ctx.header.leftTitle" :template="ctx.header.leftTitle"
            :cp="ctx.cp" :cpr="cpr" :visit="visit" :hasPhiAccess="hasPhiAccess" />

          <os-visit-event-desc v-else :visit="visit" />
        </h3>

        <div class="accessories">
          <os-tag :value="status" :rounded="true" :type="statusType" />

          <os-copy-link size="small"
            :route="{
              name: 'ParticipantsListItemVisitDetail.Overview',
              params: {cpId: ctx.cp.id, cprId: cpr.id, visitId: visit.id},
              query: {eventId: visit.eventId}
            }" />
        </div>
      </span>

      <template #right v-if="ctx.header.rightTitle">
        <h3>
          <os-dynamic-template :template="ctx.header.rightTitle" :cp="ctx.cp" :cpr="cpr" :visit="visit"
            :hasPhiAccess="hasPhiAccess" />
        </h3>
      </template>
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

            <li v-if="!ctx.cp.consentsWaived && ctx.cp.visitLevelConsents && hasEc">
              <router-link :to="getRoute('Consents')">
                <os-icon name="file-signature" />
                <span class="label" v-t="'participant_consents.list'">Consents</span>
              </router-link>
            </li>

            <li v-if="ctx.showSpr && visit.status == 'Complete'">
              <router-link :to="getRoute('Report')">
                <os-icon name="file" />
                <span class="label" v-t="'visits.path_report'">Path Report</span>
              </router-link>
            </li>

            <li v-if="visit.id > 0">
              <router-link :to="getRoute('Forms')">
                <os-icon name="file-alt" />
                <span class="label" v-t="'common.forms'">Forms</span>
              </router-link>
            </li>

            <os-plugin-views page="visit-detail" view="tab-menu" />
          </ul>
        </os-side-menu>

        <router-view :cpr="cpr" :visit="visit" v-if="visit && (visit.id > 0 || visit.eventId > 0)" />
      </div>
    </os-page-body>
  </os-page>
</template>

<script>

import authSvc    from '@/common/services/Authorization.js';
import cpSvc     from '@/biospecimen/services/CollectionProtocol.js';
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['cpr', 'visit', 'noNavButton'],

  inject: ['cpViewCtx'],

  data() {
    const cp = this.cpViewCtx.getCp();
    return {
      ctx: {
        cp,

        showSpr: true,

        header: {}
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

    this.cpViewCtx.isSaveSprEnabled().then(
      showSpr => {
        this.ctx.showSpr = showSpr && this.cpViewCtx.isReadSprAllowed(this.cpr);
      }
    );

    cpSvc.getWorkflowProperty(this.ctx.cp.id, 'common', 'visitHeader').then(
      header => {
        if (header) {
          this.ctx.header = header;
        }
      }
    );
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
    },

    hasEc: function() {
      return this.$osSvc.ecDocSvc != null;
    },

    hasPhiAccess: function() {
      return authSvc.isAllowed({resources: ['ParticipantPhi'], cp: this.ctx.cp.shortTitle, operations: ['Read']});
    },
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
