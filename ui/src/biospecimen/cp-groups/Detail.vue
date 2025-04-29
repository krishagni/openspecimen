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
          </ul>
        </os-tab-menu>

        <router-view :cpg="ctx.cpg" v-if="ctx.cpg && ctx.cpg.id > 0" :key="ctx.cpg.id" />
      </div>
    </os-page-body>
  </os-page>
</template>

<script>
import cpgSvc from '@/biospecimen/services/CollectionProtocolGroup.js';

export default {
  name: 'CpgDetail',

  props: ['cpgId'],

  data() {
    return {
      ctx: {
        cpg: {}
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

      this.ctx.cpg = await cpgSvc.getGroupById(+this.cpgId);
    },

    getRoute: function(routeName, params, query) {
      return { name: 'CpgDetail.' + routeName, params, query: {...this.query, query} };
    }
  }
}
</script>
