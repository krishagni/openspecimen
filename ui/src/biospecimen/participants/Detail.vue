<template>
  <os-page>
    <os-page-head :noNavButton="noNavButton">
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span class="os-title">
        <h3>
          <span>{{ctx.cpr.ppid}}</span>
          <span v-if="hasPhiAccess && participantName"> ({{participantName}})</span>
        </h3>
        <div class="accessories" v-if="ctx.cpr && ctx.cpr.id > 0">
          <os-copy-link size="small"
            :route="{name: 'ParticipantDetail.Overview', params: {cpId: ctx.cp.id, cprId: ctx.cpr.id}}" />
          <os-new-tab size="small" 
            :route="{name: 'ParticipantDetail.Overview', params: {cpId: ctx.cp.id, cprId: ctx.cpr.id}}" />
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

        <router-view :cpr="ctx.cpr" v-if="ctx.cpr.id"> </router-view>
      </div>
    </os-page-body>
  </os-page>
</template>

<script>

import { inject, reactive } from 'vue';

import authSvc    from '@/common/services/Authorization.js';
import cprSvc     from '@/biospecimen/services/Cpr.js';
import formUtil   from '@/common/services/FormUtil.js';
import i18n       from '@/common/services/I18n.js';
import routerSvc  from '@/common/services/Router.js';

export default {
  props: ['cprId', 'noNavButton'],

  setup() {
    const cp = inject('cp', {value: {}}).value;

    const ctx = reactive({
      cp: cp,

      cpr: {},

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

    this.loadCpr();
  },

  watch: {
    cprId: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this.loadCpr();
      }
    }
  },

  computed: {
    hasPhiAccess: function() {
      return authSvc.isAllowed({resources: ['ParticipantPhi'], cp: this.ctx.cp.shortTitle});
    },

    participantName: function() {
      const participant = this.ctx.cpr.participant;
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
    loadCpr: async function() {
      this.ctx.cpr = await cprSvc.getCpr(+this.cprId);
      formUtil.createCustomFieldsMap(this.ctx.cpr, true);
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
