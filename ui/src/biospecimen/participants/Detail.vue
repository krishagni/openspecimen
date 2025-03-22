<template>
  <os-page>
    <os-page-head :borderless="false" :showBreadcrumb="true">
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span class="os-title" v-if="cpr && cpr.id > 0">
        <h3>
          <span>{{participantName}}</span>
        </h3>
        <div class="accessories">
          <os-tag :value="$t('common.draft')" :rounded="true" :type="'warning'" v-if="cpr.dataEntryStatus == 'DRAFT'" />

          <os-copy-link size="small"
            :route="{name: 'ParticipantsListItemDetail.Overview', params: {cpId: ctx.cp.id, cprId: cpr.id}}" />
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

            <li v-if="!ctx.cp.consentsWaived && !ctx.cp.visitLevelConsents && isReadConsentAllowed">
              <router-link :to="getRoute('Consents')">
                <os-icon name="file-signature" />
                <span class="label" v-t="'participant_consents.list'">Consents</span>
              </router-link>
            </li>

            <li>
              <router-link :to="getRoute('Forms')">
                <os-icon name="file-alt" />
                <span class="label" v-t="'common.forms'">Forms</span>
              </router-link>
            </li>

            <li v-show-if-allowed="{resources: ['PrimarySpecimen', 'Specimen'], operations: ['Read']}">
              <router-link :to="getRoute('Specimens')">
                <os-icon name="flask" />
                <span class="label" v-t="'specimens.list'">Forms</span>
              </router-link>
            </li>

            <os-plugin-views page="participant-detail" view="tab-menu" />
          </ul>
        </os-side-menu>

        <router-view :cpr="cpr" v-if="cpr && cpr.id"> </router-view>
      </div>
    </os-page-body>
  </os-page>
</template>

<script>

import authSvc    from '@/common/services/Authorization.js';
import i18n       from '@/common/services/I18n.js';
import routerSvc  from '@/common/services/Router.js';

import cprSvc from '@/biospecimen/services/Cpr.js';

export default {
  props: ['cpr', 'noNavButton'],

  inject: ['cpViewCtx'],

  data() {
    const cp =  this.cpViewCtx.getCp();
    return {
      ctx: {
        cp: cp,

        bcrumb: [
          {url: routerSvc.getUrl('ParticipantsList', {cprId: -1}), label: cp.shortTitle}, // TODO: CP conf list view
          {url: routerSvc.getUrl('ParticipantsList', {cprId: -1}), label: i18n.msg('participants.list')}
        ]
      }
    }
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
    hasPhiAccess: function() {
      return authSvc.isAllowed({resources: ['ParticipantPhi'], cp: this.ctx.cp.shortTitle, operations: ['Read']});
    },

    participantName: function() {
      return cprSvc.getFormattedTitle(this.cpr);
    },

    isReadConsentAllowed: function() {
      return this.cpViewCtx.isReadConsentAllowed(this.cpr)
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
