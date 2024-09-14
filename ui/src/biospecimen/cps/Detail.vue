<template>
  <os-page>
    <os-page-head :noNavButton="false">
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
            <li v-os-tooltip.right="$t('common.overview')">
              <router-link :to="getRoute('Overview')">
                <os-icon name="eye" />
              </router-link>
            </li>
            <li v-os-tooltip.right="$t('cps.consents')">
              <router-link :to="getRoute('Consents')">
                <os-icon name="file-signature" />
              </router-link>
            </li>
            <li v-os-tooltip.right="$t('cps.events')">
              <router-link :to="getRoute('Events')">
                <os-icon name="list-alt" />
              </router-link>
            </li>
          </ul>
        </os-side-menu>

        <router-view :cp="ctx.cp" v-if="ctx.cp && ctx.cp.id > 0" @cp-saved="loadCp" :key="ctx.cp.id"> </router-view>
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
        name: 'CpDetail.' + routeName,
        params: params,
        query: {...this.query, query}
      }
    }
  }
}
</script>
