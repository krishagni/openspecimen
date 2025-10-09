<template>
  <os-page-toolbar>
    <template #default>
      <span v-show-if-allowed="cpUpdateOpts">
        <os-button left-icon="edit" :label="$t('common.buttons.edit')" @click="editRateList" />

        <os-button left-icon="trash" :label="$t('common.buttons.delete')" @click="deleteRateList" />

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

  <os-audit-trail ref="auditTrailDialog" :objects="ctx.rateListObjs" />

  <AddEditRateList ref="editRateListDialog" />
</template>

<script>
import rateListSvc from '@/biospecimen/services/RateList.js';
// import routerSvc from '@/common/services/Router.js';

import AddEditRateList from '@/biospecimen/rate-lists/AddEditRateList.vue';

export default {
  props: ['rate-list'],

  emits: ['rate-list-saved'],

  components: {
    AddEditRateList
  },

  data() {
    return {
      ctx: {
        rateList: {},

        deleteOpts: {},

        rateListObjs: [],

        dict: rateListSvc.getDict(),

        routeQuery: this.$route.query
      },

      cpUpdateOpts: {resource: 'CollectionProtocol', operations: ['Create', 'Update']}
    };
  },

  async created() {
    this._setupRateList();
    this.ctx.dict = await rateListSvc.getDict();
  },

  watch: {
    rateList: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this._setupRateList();
      }
    }
  },

  methods: {
    editRateList: function() {
      this.$refs.editRateListDialog.open(this.ctx.rateList).then(
        savedRateList => {
          this.$emit('rate-list-saved', savedRateList);
        }
      );
    },

    _setupRateList: function() {
      const ctx = this.ctx;
      ctx.rateList = this.rateList;
      ctx.deleteOpts = { };
      ctx.rateListObjs = [{objectName: 'rate_list', objectId: this.rateList.id}];
    },

    deleteRateList: function() {
      /*this.$refs.deleteObj.execute().then(
        (resp) => {
          if (resp == 'deleted') {
            routerSvc.goto('SitesList', {siteId: -2}, this.ctx.routeQuery);
          }
        }
      ); */
    },

    viewAuditTrail: function() {
      this.$refs.auditTrailDialog.open();
    }
  }
}
</script>
