<template>
  <os-page>
    <os-page-head :noNavButton="false">
      <template #breadcrumb>
        <os-breadcrumb :items="bcrumb" />
      </template>

      <span class="os-title" v-if="ctx.cp.id > 0">
        <h3>
          <span>{{ctx.cp.shortTitle}}</span>
        </h3>
        <div class="accessories">
          <os-tag :value="$t('common.draft')" :rounded="true" :type="'warning'" v-if="ctx.cp.draftMode" />

          <os-tag :value="$t('cps.closed')" :rounded="true" :type="'danger'" v-if="ctx.cp.activityStatus == 'Closed'" />

          <os-copy-link size="small" :route="{name: 'CpDetail.Overview', params: {cpId: ctx.cp.id}}" />
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
            <li v-if="!ctx.cp.specimenCentric">
              <router-link :to="getRoute('Consents')">
                <os-icon name="file-signature" />
                <span class="label" v-t="'cps.consents'">Consents</span>
              </router-link>
            </li>
            <li>
              <router-link :to="getRoute('Events')">
                <os-icon name="calendar" />
                <span class="label" v-t="'cps.events'">Events</span>
              </router-link>
            </li>
            <li>
              <router-link :to="getRoute('Settings')">
                <os-icon name="cogs" />
                <span class="label" v-t="'cps.settings'">Settings</span>
              </router-link>
            </li>
            <li v-if="ctx.versioningEnabled">
              <router-link :to="getRoute('Revisions')">
                <os-icon name="history" />
                <span class="label" v-t="'cps.revisions'">Revisions</span>
              </router-link>
            </li>
          </ul>
        </os-side-menu>

        <router-view :cp="ctx.cp" v-if="ctx.cp && ctx.cp.id > 0" @cp-saved="loadCp" :key="ctx.cp.id" />
      </div>
    </os-page-body>
  </os-page>
</template>

<script>

import cpSvc      from '@/biospecimen/services/CollectionProtocol.js';
import formUtil   from '@/common/services/FormUtil.js';
import i18n       from '@/common/services/I18n.js';
import routerSvc  from '@/common/services/Router.js';
import settingSvc from '@/common/services/Setting.js';

export default {
  props: ['cpId'],

  data() {
    return {
      ctx: {
        cp: {}
      }
    }
  },

  created() {
    this.query = {};
    if (this.$route.query) {
      Object.assign(this.query, {filters: this.$route.query.filters});
    }

    settingSvc.getSetting('biospecimen', 'cp_versioning_enabled').then(
      ([setting]) => {
        this.ctx.versioningEnabled = setting.value == 'true' || setting.value == true
      }
    );

    this.loadCp();
  },

  watch: {
    cpId: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this.loadCp();
      }
    }
  },

  computed: {
    bcrumb: function() {
      return [{url: routerSvc.getUrl('CpsList'), label: i18n.msg('cps.list')}];
    }
  },

  methods: {
    loadCp: async function() {
      this.ctx.cp = await cpSvc.getCpById(+this.cpId);
      formUtil.createCustomFieldsMap(this.ctx.cp, true);
    },

    getRoute: function(routeName, params, query) {
      return {
        name: 'CpDetail.' + routeName,
        params: params,
        query: {...this.query, query}
      }
    }
  }
}
</script>
