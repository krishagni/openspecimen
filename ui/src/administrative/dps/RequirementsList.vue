<template>
  <os-page-toolbar v-show-if-allowed="dpResources.updateOpts">
    <template #default>
      <os-button left-icon="plus" :label="$t('common.buttons.add')" @click="addRequirement" />
    </template>
  </os-page-toolbar>

  <os-list-view
    :data="ctx.requirements"
    :schema="listSchema"
    :loading="ctx.loading"
    :showRowActions="true"
    ref="listView"
  >
    <template #rowActions="slotProps">
      <os-button-group :style="{'min-width': '3.25rem'}" v-show-if-allowed="dpResources.updateOpts">
        <os-button left-icon="edit"  @click="editRequirement(slotProps.rowObject)"  />
        <os-button left-icon="trash" @click="confirmDeleteReq(slotProps.rowObject)" />
      </os-button-group>
    </template>
  </os-list-view>

  <os-dialog ref="deleteReqDialog">
    <template #header>
      <span v-t="'dps.confirm_req_delete'">Confirm requirement deletion...</span>
    </template>
    <template #content>
      <span v-t="'dps.confirm_req_delete_msg'">Are you sure you want to delete the distribution protocol requirement?</span>
    </template>
    <template #footer>
      <os-button text   :label="$t('common.buttons.cancel')" @click="cancelDeleteReq" />
      <os-button danger :label="$t('common.buttons.delete')" @click="deleteRequirement" />
    </template>
  </os-dialog>
</template>

<script>

import alertsSvc from '@/common/services/Alerts.js';
import routerSvc from '@/common/services/Router.js';
import dpSvc from '@/administrative/services/DistributionProtocol.js';

import dpResources from './Resources.js';

export default {
  props: ['dp'],

  data() {
    return {
      ctx: {
        requirements: [],

        loading: false
      },

      listSchema: { columns: [] },

      dpResources
    }
  },

  created() {
    dpSvc.getRequirementsListViewSchema().then(listSchema => this.listSchema = listSchema);
    this.loadRequirements();
  },

  methods: {
    loadRequirements: async function() {
      this.ctx.loading = true;

      const reqs = await dpSvc.getRequirements(this.dp);
      this.ctx.requirements = reqs.map(requirement => ({requirement}));
      this.ctx.loading = false;
    },

    addRequirement: function() {
      const route = this.$route.matched[this.$route.matched.length - 1];
      const view  = route.name.split('.')[0];

      routerSvc.goto(view + '.Requirements.AddEdit', {dpId: this.dp.id, reqId: -1});
    },

    editRequirement: function({requirement}) {
      const route = this.$route.matched[this.$route.matched.length - 1];
      const view  = route.name.split('.')[0];

      routerSvc.goto(view + '.Requirements.AddEdit', {dpId: this.dp.id, reqId: requirement.id});
    },

    confirmDeleteReq: function({requirement}) {
      this.ctx.toDeleteReq = requirement;
      this.$refs.deleteReqDialog.open();
    },

    cancelDeleteReq: function() {
      this.$refs.deleteReqDialog.close();
      this.ctx.toDeleteReq = undefined;
    },

    deleteRequirement: async function() {
      await dpSvc.deleteRequirement(this.ctx.toDeleteReq);
      alertsSvc.success({code: 'dps.req_deleted'});
      this.cancelDeleteReq();
      this.loadRequirements();
    }
  }
}
</script>
