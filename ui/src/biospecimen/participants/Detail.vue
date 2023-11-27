<template>
  <os-page>
    <os-page-head :noNavButton="noNavButton">
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span class="os-title" v-if="cpr && cpr.id > 0">
        <h3>
          <span>{{cpr.ppid}}</span>
          <span v-if="hasPhiAccess && participantName"> ({{participantName}})</span>
        </h3>
        <div class="accessories">
          <os-copy-link size="small"
            :route="{name: 'ParticipantDetail.Overview', params: {cpId: ctx.cp.id, cprId: cpr.id}}" />
          <os-new-tab size="small" 
            :route="{name: 'ParticipantDetail.Overview', params: {cpId: ctx.cp.id, cprId: cpr.id}}" />
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

            <os-plugin-views page="participant-detail" view="tab-menu" />
          </ul>
        </os-tab-menu>

        <os-side-menu v-else>
          <ul>
            <li v-os-tooltip.right="$t('common.overview')">
              <router-link :to="getRoute('Overview')">
                <os-icon name="eye" />
              </router-link>
            </li>

            <os-plugin-views page="participant-detail" view="side-menu" />
          </ul>
        </os-side-menu>

        <router-view :cpr="cpr" v-if="cpr && cpr.id"> </router-view>
      </div>
    </os-page-body>
  </os-page>
</template>

<script>

import { inject, reactive } from 'vue';

import authSvc    from '@/common/services/Authorization.js';
import i18n       from '@/common/services/I18n.js';
import routerSvc  from '@/common/services/Router.js';

export default {
  props: ['cpr', 'noNavButton'],

  async setup() {
    const cpViewCtx = inject('cpViewCtx', {value: {}}).value;

    const cp = await cpViewCtx.getCp();

    const ctx = reactive({
      cp: cp,

      bcrumb: [
        {url: routerSvc.getUrl('ParticipantsList', {cprId: -1}), label: cp.shortTitle}, // TODO: CP conf list view
        {url: routerSvc.getUrl('ParticipantsList', {cprId: -1}), label: i18n.msg('participants.list')}
      ]
    });

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

  computed: {
    hasPhiAccess: function() {
      return authSvc.isAllowed({resources: ['ParticipantPhi'], cp: this.ctx.cp.shortTitle, operations: ['Read']});
    },

    participantName: function() {
      const participant = this.cpr.participant;
      if (!participant) {
        return '';
      }

      let result = participant.firstName;
      if (participant.middleName) {
        if (result) {
          result += ' ';
        }

        result += participant.middleName;
      }

      if (participant.lastName) {
        if (result) {
          result += ' ';
        }

        result += participant.lastName;
      }

      return result;
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
