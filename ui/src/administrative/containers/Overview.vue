<template>
  <os-page-toolbar>
    <template #default>
      <os-button left-icon="edit" label="Edit" @click="editContainer"
        v-show-if-allowed="containerResources.updateOpts" />

      <os-button left-icon="arrows-alt-h" label="Transfer" @click="showTransferForm"
        v-show-if-allowed="containerResources.updateOpts" />

      <os-button left-icon="trash" label="Delete" @click="deleteContainer"
        v-show-if-allowed="containerResources.deleteOpts" />

      <os-button left-icon="print" label="Print" @click="printLabels" />

      <os-menu label="Export" :options="ctx.reportOpts" />
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
          <span>Top 5 specimen types</span>
        </template>
        <template #content>
          <os-chart type="doughnut" :data="ctx.spmnTypesStorage" />
        </template>
      </os-section>
    </os-grid-column>
  </os-grid>

  <os-dialog ref="transferFormDialog">
    <template #header>
      <span>Transfer {{ctx.container.name}} to ...</span>
    </template>
    <template #content>
      <os-form ref="transferForm" :schema="ctx.transferFs.layout" :data="trCtx" @input="handleTransferInput($event)" />
    </template>
    <template #footer>
      <os-button text label="Cancel" @click="closeTransferForm" />
      <os-button primary label="Transfer" @click="transfer" />
    </template>
  </os-dialog>

  <os-confirm-delete ref="deleteDialog">
    <template #message>
      <div>
        <span>Container <b>{{ctx.container.name}}</b> and its child containers will be deleted forever. Are you sure you want to proceed?</span>
      </div>

      <table class="os-table muted-header os-border" v-if="ctx.dependents.length > 0" style="margin-top: 1.25rem;">
        <thead>
          <tr>
            <th>Name</th>
            <th>Count</th>
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
      <span>Defragment {{ctx.container.name}}</span>
    </template>
    <template #content>
      <os-form ref="defragForm" :schema="ctx.defragFs.layout" :data="dfCtx" />
    </template>
    <template #footer>
      <os-button text label="Cancel" @click="closeDefragForm" />
      <os-button primary label="Defragment" @click="defragment" />
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
          caption: 'Map',
          onSelect: () => this.exportMap()
        },
        {
          caption: 'Defragment',
          onSelect: () => this.showDefragForm()
        },
        {
          caption: 'Empty Positions',
          onSelect: () => this.exportEmptyPositions()
        },
        {
          caption: 'Utilisation',
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

    showTransferForm: function() {
      const container = util.clone(this.container);
      container.transferredBy = this.$ui.currentUser;
      container.transferDate = new Date().getTime();

      const attrs = ['parent', 'childContainers', 'occupiedPositions', 'childContainersLoaded', 'hasChildren'];
      attrs.forEach(attr => delete container[attr]);

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
        alertsSvc.info('No change in location detected. Container not transferred!');
        this.closeTransferForm();
        return;
      }

      await containerSvc.saveOrUpdate(trCont);
      routerSvc.reload();
    },

    deleteContainer: async function() {
      const container = this.ctx.container;

      this.ctx.dependents = await containerSvc.getDependents(container);
      await this.$refs.deleteDialog.open();

      await containerSvc.delete(container, true);
      alertsSvc.success('Container ' + container.name + ' deleted!');

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
        alertsSvc.success('Container labels print job ' + job.id + ' created!');
      }
    },

    exportMap: async function() {
      alertsSvc.info('Generating container map...');
      const resp = await containerSvc.exportMap(this.ctx.container);
      if (resp.fileId) {
        alertsSvc.info('Downloading the container map...');
        containerSvc.downloadReport(resp.fileId);
      } else {
        alertsSvc.info('Container map generation is taking more time than anticipated. Link to download the map will be sent to you by email.');
      }
    },

    exportEmptyPositions: async function() {
      alertsSvc.info('Generating empty positions report...');
      const resp = await containerSvc.exportEmptyPositions(this.ctx.container);
      if (resp.fileId) {
        alertsSvc.info('Downloading the empty positions report...');
        containerSvc.downloadReport(resp.fileId);
      } else {
        alertsSvc.info('Empty positions report generation is taking more time than anticipated. Link to download the report will be sent to you by email.');
      }
    },

    exportUtilisation: async function() {
      alertsSvc.info('Generating utilisation report...');
      const resp = await containerSvc.exportUtilisation(this.ctx.container);
      if (resp.fileId) {
        alertsSvc.info('Downloading the utilisation report...');
        containerSvc.downloadReport(resp.fileId);
      } else {
        alertsSvc.info('Utilisation report generation is taking more time than anticipated. Link to download the report will be sent to you by email.');
      }
    },

    showDefragForm: function() {
      this.$refs.defragDialog.open();
    },

    closeDefragForm: function() {
      this.$refs.defragDialog.close();
    },

    defragment: async function() {
      alertsSvc.info('Generating defragmentation report...');
      const resp = await containerSvc.exportDefragReport(this.ctx.container, this.dfCtx.defrag);
      if (resp.fileId) {
        alertsSvc.info('Downloading the defragmentation report...');
        containerSvc.downloadReport(resp.fileId);
      } else {
        alertsSvc.info('Defragmentation report generation is taking more time than anticipated. Link to download the report will be sent to you by email.');
      }

      this.closeDefragForm();
    }
  }
}
</script>
