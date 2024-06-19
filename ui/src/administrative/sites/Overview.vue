<template>
  <os-page-toolbar>
    <template #default>
      <span v-show-if-allowed="'institute-admin'">
        <os-button left-icon="edit" label="Edit" @click="$goto('SiteAddEdit', {siteId: ctx.site.id})" />

        <os-button left-icon="trash" label="Delete" @click="deleteSite" />

        <os-button left-icon="history" :label="$t('audit.trail')" @click="viewAuditTrail" />
      </span>
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="12">
      <os-overview :schema="ctx.dict" :object="ctx" :columns="1" v-if="ctx.dict.length > 0" />
    </os-grid-column>
  </os-grid>

  <os-delete-object ref="deleteObj" :input="ctx.deleteOpts" />

  <os-audit-trail ref="auditTrailDialog" :objects="ctx.siteObjs" />
</template>

<script>
import routerSvc from '@/common/services/Router.js';
import siteSvc from '@/administrative/services/Site.js';

export default {
  props: ['site'],

  data() {
    return {
      ctx: {
        site: {},

        deleteOpts: {},

        siteObjs: [],

        dict: [],

        routeQuery: this.$route.query
      }
    };
  },

  async created() {
    this.setupSite();
    this.ctx.dict = await siteSvc.getDict();
  },

  watch: {
    site: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this.setupSite();
      }
    }
  },

  methods: {
    setupSite: function() {
      const ctx = this.ctx;
      ctx.site = this.site;
      ctx.deleteOpts = {
        type: this.$t('sites.singular'),
        title: this.site.name,
        dependents: () => siteSvc.getDependents(this.site),
        deleteObj: () => siteSvc.delete(this.site)
      };
      ctx.siteObjs = [{objectName: 'site', objectId: this.site.id}];
    },

    deleteSite: function() {
      this.$refs.deleteObj.execute().then(
        (resp) => {
          if (resp == 'deleted') {
            routerSvc.goto('SitesList', {siteId: -2}, this.ctx.routeQuery);
          }
        }
      );
    },

    viewAuditTrail: function() {
      this.$refs.auditTrailDialog.open();
    }
  }
}
</script>
