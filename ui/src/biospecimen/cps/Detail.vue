<template>
  <os-page>
    <os-page-head :noNavButton="true">
      <span class="os-title" v-if="ctx.cp.id > 0">
        <h3>
          <span>{{ctx.cp.shortTitle}}</span>
        </h3>
        <div class="accessories">
          <os-tag :value="$t('common.draft')" :rounded="true" :type="'warning'" v-if="ctx.cp.draftMode" />

          <os-copy-link size="small" :route="{name: 'CpsListItemDetail.Overview', params: {cpId: ctx.cp.id}}" />
        </div>
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

        <router-view :cp="ctx.cp" v-if="ctx.cp && ctx.cp.id > 0" :key="ctx.cp.id"> </router-view>
      </div>
    </os-page-body>
  </os-page>
</template>

<script>

import cpSvc from '@/biospecimen/services/CollectionProtocol.js';
import formUtil from '@/common/services/FormUtil.js';

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
  },

  methods: {
    loadCp: async function() {
      this.ctx.cp = await cpSvc.getCpById(+this.cpId);
      formUtil.createCustomFieldsMap(this.ctx.cp, true);
    },

    getRoute: function(routeName, params, query) {
      return {
        name: 'CpsListItemDetail.' + routeName,
        params: params,
        query: {...this.query, query}
      }
    }
  }
}
</script>
