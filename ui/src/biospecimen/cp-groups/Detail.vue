<template>
  <os-page v-if="cpgId > 0">
    <os-page-head :noNavButton="true">
      <span class="os-title">
        <h3>{{ctx.cpg.name}}</h3>
      </span>
    </os-page-head>
    <os-page-body>
      <div>
        <os-tab-menu>
          <ul>
            <li>
              <router-link :to="getRoute('Overview')">
                <span v-t="'common.overview'">Overview</span>
              </router-link>
            </li>
            <li>
              <router-link :to="getRoute('Forms')">
                <span v-t="'cpgs.forms'">Forms</span>
              </router-link>
            </li>
            <li v-if="ctx.hasCatalogPlugin">
              <router-link :to="getRoute('Settings')">
                <span v-t="'cps.settings'">Settings</span>
              </router-link>
            </li>
          </ul>
        </os-tab-menu>

        <router-view :cpg="ctx.cpg" :perm-opts="ctx.permOpts" :key="ctx.cpg.id"
          @cpg-saved="onCpgSaved" v-if="ctx.cpg && ctx.cpg.id > 0" />
      </div>
    </os-page-body>
  </os-page>
</template>

<script>
import authSvc from '@/common/services/Authorization.js';
import cpgSvc from '@/biospecimen/services/CollectionProtocolGroup.js';

export default {
  name: 'CpgDetail',

  props: ['cpgId'],

  inject: ['ui'],

  data() {
    const {global: {appProps: {plugins}}} = this.ui;
    return {
      ctx: {
        cpg: {},

        permOpts: {},

        hasCatalogPlugin: (plugins || []).indexOf('sc') >= 0
      }
    };
  },

  created() {
    this.query = {};

    const {query} = this.$route;
    if (query) {
      Object.assign(this.query, {filters: query.filters, cpgId: query.cpgId});
    }

    this._loadCpg();
  },

  watch: {
    cpgId: function(newVal, oldVal) {
      if (newVal == oldVal) {
        return;
      }

      this._loadCpg();
    }
  },

  methods: {
    _loadCpg: async function() {
      const {cpg} = this.ctx;
      if (cpg && cpg.id == +this.cpgId) {
        return;
      }

      this.ctx.cpg = {};
      cpgSvc.getGroupById(+this.cpgId).then(group => this.ctx.cpg = group);

      cpgSvc.getPermissions(+this.cpgId).then(
        ({updateAllowed}) => {
          const eximAllowed = authSvc.isAllowed({resource: 'CollectionProtocol', operations: ['Export Import']});
          const importAllowed = updateAllowed && eximAllowed;
          this.ctx.permOpts = {updateAllowed, importAllowed, exportAllowed: eximAllowed};
        }
      );
    },

    getRoute: function(routeName, params, query) {
      return { name: 'CpgDetail.' + routeName, params, query: {...this.query, query} };
    },

    onCpgSaved: function(cpg) {
      this.ctx.cpg = cpg;
    }
  }
}
</script>
