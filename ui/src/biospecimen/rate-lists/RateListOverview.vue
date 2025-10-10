<template>
  <os-page-toolbar>
    <template #default>
      <span v-show-if-allowed="cpUpdateOpts">
        <os-button left-icon="edit" :label="$t('common.buttons.edit')" @click="editRateList" />

        <os-button left-icon="trash" :label="$t('common.buttons.delete')" @click="deleteRateList" />

        <os-button left-icon="copy" :label="$t('common.buttons.clone')" @click="cloneRateList" />
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

  <AddEditRateList ref="editDialog" />

  <os-confirm-delete ref="confirmDeleteDialog" :captcha="false" :collect-reason="false">
    <template #message>
      <span v-t="{path: 'lab_services.confirm_delete_rate_list', args: ctx.rateList}">
        Are you sure you want to delete the service {0}?
      </span>
    </template>
  </os-confirm-delete>
</template>

<script>
import alertsSvc   from '@/common/services/Alerts.js';
import rateListSvc from '@/biospecimen/services/RateList.js';
import routerSvc   from '@/common/services/Router.js';
import util        from '@/common/services/Util.js';

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
      this.$refs.editDialog.open(this.ctx.rateList).then(
        savedRateList => {
          this.$emit('rate-list-saved', savedRateList);
        }
      );
    },

    cloneRateList: function() {
      const toClone = util.clone(this.ctx.rateList);
      toClone.cloneOf = toClone.id;
      toClone.id = toClone.startDate = toClone.endDate = null;
      this.$refs.editDialog.open(toClone).then(
        savedRateList => {
          routerSvc.goto('RateListsItemDetail.Overview', {rateListId: savedRateList.id});
        }
      );
    },

    deleteRateList: function() {
      const {rateList} = this.ctx;
      this.$refs.confirmDeleteDialog.open().then(
        resp => {
          if (resp != 'proceed') {
            return;
          }

          rateListSvc.deleteRateList(rateList).then(
            () => {
              alertsSvc.success({code: 'lab_services.rate_list_deleted', args: rateList});
              routerSvc.goto('RateLists', {rateListId: -2})
            }
          );
        }
      );
    },

    viewAuditTrail: function() {
      this.$refs.auditTrailDialog.open();
    },

    _setupRateList: function() {
      const ctx = this.ctx;
      ctx.rateList = this.rateList;
      ctx.rateListObjs = [{objectName: 'rate_list', objectId: this.rateList.id}];
    }
  }
}
</script>
