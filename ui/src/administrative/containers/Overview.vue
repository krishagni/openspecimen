<template>
  <os-page-toolbar>
    <template #default>
      <os-button left-icon="edit" :label="$t('common.buttons.edit')" @click="editContainer"
        v-show-if-allowed="containerResources.updateOpts" />

      <os-button left-icon="arrows-alt-h" :label="$t('containers.transfer')" @click="showTransferForm"
        v-show-if-allowed="containerResources.updateOpts" v-if="ctx.container.status != 'CHECKED_OUT'" />

      <os-button left-icon="sign-out-alt" :label="$t('containers.check_out_button')"
        @click="showTransferForm('checkout')" v-show-if-allowed="containerResources.updateOpts"
        v-if="ctx.container.status != 'CHECKED_OUT' && ctx.container.storageLocation &&
          ctx.container.storageLocation.name" />

      <os-button left-icon="sign-in-alt" :label="$t('containers.check_in_button')" @click="showTransferForm('checkin')"
        v-show-if-allowed="containerResources.updateOpts" v-if="ctx.container.status == 'CHECKED_OUT'" />

      <os-button left-icon="trash" :label="$t('common.buttons.delete')" @click="deleteContainer"
        v-show-if-allowed="containerResources.deleteOpts" />

      <os-button left-icon="print" :label="$t('common.buttons.print')" @click="printLabels" />

      <os-menu :label="$t('common.buttons.export')" :options="ctx.reportOpts" />
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="6">
      <os-overview :schema="ctx.dict" :object="ctx" :columns="1" v-if="ctx.dict.length > 0" />
    </os-grid-column>

    <os-grid-column width="6">
      <os-audit-overview :objects="ctx.containerObjs" v-if="ctx.container.id" />

      <os-section v-if="ctx.storedSpmns > 0">
        <template #title>
          <span v-t="'containers.top_5_specimen_types'">Top 5 specimen types</span>
        </template>
        <template #content>
          <os-chart type="doughnut" :data="ctx.spmnTypesStorage" />
        </template>
      </os-section>
    </os-grid-column>
  </os-grid>

  <os-dialog ref="transferFormDialog">
    <template #header>
      <span v-t="{path: 'containers.check_out', args: ctx.container}" v-if="trCtx.checkout">
        Checkout {{ctx.container.name}}
      </span>
      <span v-t="{path: 'containers.check_in', args: ctx.container}" v-else-if="trCtx.checkin">
        Checkin {{ctx.container.name}}
      </span>
      <span v-t="{path: 'containers.transfer_to', args: ctx.container}" v-else>
        Transfer {{ctx.container.name}} to ...
      </span>
    </template>
    <template #content>
      <os-form ref="transferForm" :schema="ctx.transferFs.layout" :data="trCtx" @input="handleTransferInput($event)" />
    </template>
    <template #footer>
      <os-button text    :label="$t('common.buttons.cancel')" @click="closeTransferForm" />
      <os-button primary :label="$t(trCtx.checkout ? 'containers.check_out' : (trCtx.checkin ? 'containers.check_in' : 'containers.transfer'))" @click="transfer" />
    </template>
  </os-dialog>

  <os-confirm-delete ref="deleteDialog">
    <template #message>
      <div>
        <span v-t="{path: 'containers.confirm_delete', args: ctx.container}">Container <b>{{ctx.container.name}}</b> and its child containers will be deleted forever. Are you sure you want to proceed?</span>
      </div>

      <table class="os-table muted-header os-border" v-if="ctx.dependents.length > 0" style="margin-top: 1.25rem;">
        <thead>
          <tr>
            <th v-t="'common.name'">Name</th>
            <th v-t="'common.count'">Count</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(dependent, idx) of ctx.dependents" :key="idx">
            <td>{{dependent.name}}</td>
            <td>{{dependent.count}}</td>
          </tr>
        </tbody>
      </table>
    </template>
  </os-confirm-delete>

  <os-dialog ref="defragDialog">
    <template #header>
      <span v-t="{path: 'containers.defrag_container', args: ctx.container}">Defragment {{ctx.container.name}}</span>
    </template>
    <template #content>
      <os-form ref="defragForm" :schema="ctx.defragFs.layout" :data="dfCtx" />
    </template>
    <template #footer>
      <os-button text    :label="$t('common.buttons.cancel')" @click="closeDefragForm" />
      <os-button primary :label="$t('containers.defragment')" @click="defragment" />
    </template>
  </os-dialog>
</template>

<script>
import { reactive } from 'vue';
import { useRoute } from 'vue-router';

import containerSvc from '@/administrative/services/Container.js';
import alertsSvc    from '@/common/services/Alerts.js';
import http         from '@/common/services/HttpClient.js';
import routerSvc    from '@/common/services/Router.js';
import settingsSvc  from '@/common/services/Setting.js';
import util         from '@/common/services/Util.js';

import containerResources from './Resources.js';

export default {
  props: ['container'],

  inject: ['ui'],

  setup() {
    const route = useRoute();
    const ctx = reactive({
      container: {},

      storedSpmns: 0,

      containerObjs: [],

      dict: [],

      transferFs: containerSvc.getTransferFormSchema(),

      defragFs: containerSvc.getDefragFormSchema(),

      dependents: [],

      routeQuery: route.query,

      reportOpts: []
    });

    const trCtx = reactive({
      container: {}
    });

    const dfCtx = reactive({
      defrag: { aliquotsInSameContainer: false }
    });

    return { ctx, trCtx, dfCtx, containerResources };
  },

  created() {
    containerSvc.getDict().then(dict => this.ctx.dict = dict);
    this.setupView();
  },

  watch: {
    'container.id': function(newVal, oldVal) {
      if (newVal == oldVal) {
        return;
      }

      this.setupView();
    }
  },

  methods: {
    setupView: async function() {
      const ctx = this.ctx;
      const container = ctx.container = this.container;
      ctx.containerObjs = [{objectName: 'storage_container', objectId: this.container.id}];

      ctx.reportOpts = [
        {
          caption: this.$t('containers.map'),
          onSelect: () => this.exportMap()
        },
        {
          caption: this.$t('containers.defragment'),
          onSelect: () => this.showDefragForm()
        },
        {
          caption: this.$t('containers.empty_positions'),
          onSelect: () => this.exportEmptyPositions()
        },
        {
          caption: this.$t('containers.utilisation'),
          onSelect: () => this.exportUtilisation()
        }
      ];

      if (!container.storageLocation || !container.storageLocation.name) {
        let types = Object.keys(container.specimensByType)
          .sort((t1, t2) => container.specimensByType[t2] - container.specimensByType[t1]);
        let storedSpmns = 0, spmnTypes = [], spmnCounts = [];
        for (let type of types) {
          storedSpmns += container.specimensByType[type];
          if (spmnTypes.length < 5) {
            spmnTypes.push(type);
            spmnCounts.push(container.specimensByType[type]);
          }
        }

        ctx.spmnTypesStorage = {
          labels: spmnTypes,
          datasets: [
            {
              data: spmnCounts,
              backgroundColor: ['#FEAE65', '#E6F69D', '#AADEA7', '#64C2A6', '#2D87BB']
            }
          ]
        };

        ctx.storedSpmns = storedSpmns;
      } else {
        ctx.storedSpmns = 0;
      }
    },

    editContainer: function() {
      routerSvc.goto('ContainerAddEdit', {containerId: this.container.id});
    },

    showTransferForm: function(action) {
      const container = util.clone(this.container);
      container.transferredBy = this.$ui.currentUser;
      container.transferDate = new Date().getTime();
      container.storageLocation = container.blockedLocation;

      const attrs = ['parent', 'childContainers', 'occupiedPositions', 'childContainersLoaded', 'hasChildren'];
      attrs.forEach(attr => delete container[attr]);

      this.trCtx.checkout = action == 'checkout';
      this.trCtx.checkin  = action == 'checkin';
      this.trCtx.container = container;
      this.$refs.transferFormDialog.open();
    },

    closeTransferForm: function() {
      this.$refs.transferFormDialog.close();
    },

    handleTransferInput: async function({field}) {
      if (field.name == 'container.siteName') {
        this.trCtx.container.storageLocation = {};
      }
    },

    transfer: async function() {
      if (!this.$refs.transferForm.validate()) {
        return;
      }

      const olCont     = this.container;
      const olLocation = olCont.storageLocation || {};

      const trCont = this.trCtx.container;
      const newLocation = trCont.storageLocation || {};

      const locationChanged = (olCont.siteName != trCont.siteName) ||
        (olLocation.name != newLocation.name) ||
        (olLocation.positionY != newLocation.positionY) ||
        (olLocation.positionX != newLocation.positionX);
      if (!locationChanged) {
        alertsSvc.info({code: 'containers.no_change_not_transferred'});
        this.closeTransferForm();
        return;
      }

      trCont.checkOut = this.trCtx.checkout;
      await containerSvc.saveOrUpdate(trCont);
      routerSvc.reload();
    },

    deleteContainer: async function() {
      const container = this.ctx.container;

      this.ctx.dependents = await containerSvc.getDependents(container);
      await this.$refs.deleteDialog.open();

      await containerSvc.delete(container, true);
      alertsSvc.success({code: 'containers.deleted', args: container});

      if (container.storageLocation && container.storageLocation.id) {
        routerSvc.goto('ContainerDetail.Overview', {containerId: container.storageLocation.id});
      } else {
        routerSvc.goto('ContainersList', {containerId: -1});
      }
    },

    printLabels: async function() {
      const job = await containerSvc.printLabels([this.ctx.container.id]);

      const settings = await settingsSvc.getSetting('administrative', 'download_labels_print_file');
      if (util.isTrue(settings[0].value)) {
        const filename = this.ctx.container.name.replace(/[^\w.]+/g, '_').replace(/__+/g, '_') + '.csv';
        const query    = {jobId: job.id, filename};
        http.downloadFile(http.getUrl('container-label-printer/output-file', {query}));
      } else {
        alertsSvc.success({code: 'containers.print_job_created', args: job});
      }
    },

    exportMap: async function() {
      alertsSvc.info({code: 'containers.generating_map'});
      const resp = await containerSvc.exportMap(this.ctx.container);
      if (resp.fileId) {
        alertsSvc.info({code: 'containers.downloading_map'});
        containerSvc.downloadReport(resp.fileId);
      } else {
        alertsSvc.info({code: 'containers.map_will_be_emailed'});
      }
    },

    exportEmptyPositions: async function() {
      alertsSvc.info({code: 'containers.generating_empty_pos_report'});
      const resp = await containerSvc.exportEmptyPositions(this.ctx.container);
      if (resp.fileId) {
        alertsSvc.info({code: 'containers.downloading_empty_pos_report'});
        containerSvc.downloadReport(resp.fileId);
      } else {
        alertsSvc.info({code: 'containers.empty_pos_report_by_email'});
      }
    },

    exportUtilisation: async function() {
      alertsSvc.info({code: 'containers.generating_utilisation_report'});
      const resp = await containerSvc.exportUtilisation(this.ctx.container);
      if (resp.fileId) {
        alertsSvc.info({code: 'containers.downloading_utilisation_report'});
        containerSvc.downloadReport(resp.fileId);
      } else {
        alertsSvc.info({code: 'containers.utilisation_report_by_email'});
      }
    },

    showDefragForm: function() {
      this.$refs.defragDialog.open();
    },

    closeDefragForm: function() {
      this.$refs.defragDialog.close();
    },

    defragment: async function() {
      alertsSvc.info({code: 'containers.generating_defrag_report'});
      const resp = await containerSvc.exportDefragReport(this.ctx.container, this.dfCtx.defrag);
      if (resp.fileId) {
        alertsSvc.info({code: 'containers.downloading_defrag_report'});
        containerSvc.downloadReport(resp.fileId);
      } else {
        alertsSvc.info({code: 'containers.defrag_report_by_email'});
      }

      this.closeDefragForm();
    }
  }
}
</script>
