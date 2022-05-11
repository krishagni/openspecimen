<template>
  <os-page-toolbar>
    <template #default>
      <os-button left-icon="download" label="Export Map" @click="exportMap"
        v-if="!anySelected && !isDimensionless" />

      <span v-show-if-allowed="containerResources.updateOpts">
        <span v-if="isSpecimenContainer && !anySelected && (ctx.hasFreeSlots || ctx.hasBlockedSlots)">
          <os-button left-icon="ban" label="Block All" @click="blockAll"
            v-if="ctx.hasFreeSlots" />

          <os-button left-icon="undo" label="Unblock All" @click="unblockAll"
            v-if="ctx.hasBlockedSlots" />
        </span>

        <span v-if="isSpecimenContainer && anySelected">
          <os-button left-icon="ban" label="Block" @click="block" />

          <os-button left-icon="undo" label="Unblock" @click="unblock" />
        </span>

        <span v-if="isSpecimenContainer && !anySelected && !ctx.showLabelsScanArea">
          <os-button left-icon="save" label="Store Specimens" @click="showLabelsScanArea" />
        </span>
      </span>

      <os-button left-icon="palette" label="View Color Coding" @click="showColorCoding" 
        v-if="ctx.occupants.length > 0 && Object.keys(ctx.colorCoding).length > 0" />
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column class="os-container-locations" :width="12">
      <div class="labels-scan-area" v-if="isSpecimenContainer && !anySelected && ctx.showLabelsScanArea">
        <os-add-specimens ref="specimensScanner" @labels-scanned="updateMap" :optionsAtBottom="true">
          <os-button primary label="Store" @click="assignPositions" />

          <os-button label="Cancel" @click="hideLabelsScanArea" />

          <template #options v-if="!isDimensionless">
            <os-boolean-checkbox v-model="ctx.vacateOccupant" @change="updateMap(ctx.labels)">
              <label>Vacate existing specimens</label>
            </os-boolean-checkbox>
          </template>
        </os-add-specimens>
      </div>

      <Layout ref="layout" class="map" :container="ctx.container" :occupants="ctx.occupants" 
        :selected-positions="ctx.selectedPositions" @occupant-clicked="showOccupantDetails"
        v-if="!isDimensionless">
        <template #occupant_container="slotProps">
          <a class="occupant" @click="showOccupantDetails($event, slotProps.occupant)">
            <os-icon class="container-icon" name="box-open" />
            <span class="name" v-os-tooltip="slotProps.occupant.displayName">
              <span>{{slotProps.occupant.displayName}}</span>
            </span>
          </a>
        </template>
        <template #occupant_specimen="slotProps">
          <a class="occupant" 
            :class="{'new-occupant': !slotProps.occupant.id, 'displaced-occupant': !!slotProps.occupant.oldOccupant}"
            @click="showOccupantDetails($event, slotProps.occupant)">

            <os-icon class="specimen-icon" name="vial" :style="slotProps.occupant.colorCode" 
              v-show="slotProps.occupant.displayName" v-os-tooltip="slotProps.occupant.tooltip" />

            <os-icon class="specimen-reserved" name="ban" v-os-tooltip="'Reserved Specimen'"
              v-if="slotProps.occupant.occupantProps && slotProps.occupant.occupantProps.reserved" />

            <span class="name" v-os-tooltip="slotProps.occupant.displayName">
              <span>{{slotProps.occupant.displayName}}</span>
            </span>
          </a>
        </template>
        <template #occupant_empty="slotProps">
          <os-boolean-checkbox class="position-selector" v-model="ctx.selectedPositions[slotProps.position.position]" />
          <a class="occupant" v-os-tooltip="'Blocked'" v-if="slotProps.occupant.blocked">
            <os-icon class="blocked" name="ban" />
          </a>
        </template>
        <template #empty="slotProps">
          <div v-if="!ctx.container.storeSpecimensEnabled">
            <os-button text left-icon="plus" v-os-tooltip="'Click to add container'"
              @click="createContainer(slotProps)" /> 
          </div>
          <div v-else>
            <os-boolean-checkbox class="position-selector"
              v-model="ctx.selectedPositions[slotProps.position.position]" />
          </div>
        </template>
      </Layout>

      <os-message v-else type="info">
        <span>The map view is not available for dimensionless container.</span>
      </os-message>
    </os-grid-column>

    <os-overlay ref="occupantDetails">
      <div class="os-container-occupant">
        <div v-if="ctx.occupant.container">
          <h4 class="title">
            <span>Container</span>
          </h4>

          <os-overview :schema="ctx.containerDict" :object="ctx.occupant"
            :columns="1" v-if="ctx.containerDict.length > 0" />
        </div>

        <div v-else-if="ctx.occupant.specimen">
          <h4 class="title">
            <span>Specimen</span>
          </h4>

          <os-overview :schema="ctx.specimenDict" :object="ctx.occupant"
            :columns="1" v-if="ctx.specimenDict.length > 0" />
        </div>
      </div>
    </os-overlay>

    <os-dialog ref="colorCoding">
      <template #header>
        <span>Color Coding</span>
      </template>
      <template #content>
        <table class="os-specimen-color-coding">
          <thead>
            <tr>
              <th>Color</th>
              <th>Specimen Type</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="({style, types}, key) in ctx.color2TypesMap" :key="key">
              <td>
                <os-icon class="specimen-icon" name="vial" :style="style" />
              </td>
              <td>
                <span>{{types.join(', ')}}</span>
              </td>
            </tr>
          </tbody>
        </table>
      </template>
      <template #footer>
        <os-button primary label="Done" @click="closeColorCoding" />
      </template>
    </os-dialog>

    <os-confirm ref="confirmTransferDialog">
      <template #title>
        <span>Transfer Specimens</span>
      </template>
      <template #message>
        <span>{{ctx.toTransferSpmns.length == 1 ? 'Specimen' : 'Specimens'}} {{ctx.toTransferSpmns.join(', ')}} {{ctx.toTransferSpmns.length == 1 ? 'is' : 'are'}} already stored in a container. Do you really want to move {{ctx.toTransferSpmns.length == 1 ? 'it' : 'them'}} to a new location?</span>
      </template>
    </os-confirm>
  </os-grid>
</template>

<script>
import { reactive } from 'vue';

import containerSvc from '@/administrative/services/Container.js';
import alertsSvc    from '@/common/services/Alerts.js';
import routerSvc    from '@/common/services/Router.js';
import util         from '@/common/services/Util.js';

import Layout             from './Layout.vue';
import containerResources from './Resources.js';

export default {
  props: ['container'],

  inject: ['ui'],

  components: {
    Layout
  },

  setup() {
    const ctx = reactive({
      container: {},

      occupants: [],

      hasFreeSlots: false,

      hasBlockedSlots: false,

      containerDict: null,

      specimenDict: null,

      colorCoding: {},

      occupant: {},

      selectedPositions: {},

      labels: '',

      showLabelsScanArea: false
    });

    return { ctx, containerResources };
  },

  created() {
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

  computed: {
    anySelected: function() {
      return Object.values(this.ctx.selectedPositions || {}).some(b => b);
    },

    isDimensionless: function() {
      return !(this.ctx.container.noOfRows > 0 && this.ctx.container.noOfColumns > 0);
    },

    isSpecimenContainer: function() {
      return this.ctx.container.storeSpecimensEnabled && this.ctx.container.usedFor == 'STORAGE';
    }
  },

  methods: {
    setupView: async function() {
      const ctx           = this.ctx;
      ctx.container       = this.container;
      ctx.occupants       = [];
      ctx.hasFreeSlots    = false;
      ctx.hasBlockedSlots = false;
      ctx.labels          = '';
      ctx.showLabelsScanArea = false;

      if (ctx.container.noOfRows > 0 && ctx.container.noOfColumns > 0) {
        const occupants = await containerSvc.getOccupiedPositions(ctx.container);
        this.ctx.pOccupants = null;
        this.setupMap(occupants);
      }
    },

    setupMap: function(occupants) {
      const ctx        = this.ctx;
      const container  = ctx.container;

      ctx.occupants    = occupants;
      ctx.hasFreeSlots = false;
      if (container.noOfRows > 0 && container.noOfColumns > 0) {
        ctx.hasFreeSlots = ctx.occupants.length < (container.noOfRows * container.noOfColumns);
      }

      const colorCoding     = ctx.colorCoding = {};
      ctx.hasBlockedSlots   = false;
      ctx.selectedPositions = {};
      ctx.occupants.forEach(
        (occupant) => {
          ctx.hasBlockedSlots = ctx.hasBlockedSlots || occupant.blocked;

          if (occupant.occuypingEntity == 'specimen' && occupant.occupantProps) {
            const {ppid, barcode, specimenClass, type} = occupant.occupantProps;
            switch (ctx.container.cellDisplayProp) {
              case 'SPECIMEN_PPID':
                occupant.displayName = ppid;
                break;

              case 'SPECIMEN_BARCODE':
                occupant.displayName = barcode;
                break;
            }

            occupant.displayName = occupant.displayName || occupant.occupyingEntityName;
            if (!colorCoding[specimenClass + ':' + type]) {
              colorCoding[specimenClass + ':' + type] = util.getContainerColorCode(occupant.occupantProps) || {};
            }

            occupant.colorCode = colorCoding[specimenClass + ':' + type];
            occupant.tooltip   = specimenClass + ', ' + type;
          } else {
            occupant.displayName = occupant.occupyingEntityName;
          }
        }
      ); 
    },

    showOccupantDetails: async function(event, occupant) {
      if (!occupant.id) {
        return;
      }

      const currentTarget = event.currentTarget;
      this.ctx.occupant = {};

      const entityId = occupant.occupyingEntityId;
      if (occupant.occuypingEntity == 'container') {
        if (!this.ctx.containerDict) {
          this.ctx.containerDict = containerSvc.getSummaryDict();
        }

        this.containers = this.containers || {};
        let container = this.containers[entityId];
        if (!container) {
          container = this.containers[entityId] = await containerSvc.getContainer(entityId);
        }

        this.ctx.occupant = {container: container};
      } else {
        if (!this.ctx.specimenDict) {
          this.ctx.specimenDict = containerSvc.getSpecimenDict();
        }

        this.specimens = this.specimens || {};
        let specimen = this.specimens[entityId];
        if (!specimen) {
          specimen = this.specimens[entityId] = await containerSvc.getSpecimen(entityId);
        }

        this.ctx.occupant = {specimen: specimen};
      }

      setTimeout(() => this.$refs.occupantDetails.toggle({currentTarget}), 100);
    },

    createContainer: function({position}) {
      routerSvc.goto('ContainerAddEdit', {containerId: -1}, { parentContainerName: this.container.name,
        row: position.rowStr, column: position.columnStr, position: position.position});
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

    blockAll: async function() {
      const occupants = await containerSvc.blockPositions(this.ctx.container, []);
      this.ctx.pOccupants = null;
      this.setupMap(occupants);
      alertsSvc.success('All free positions blocked!');
    },

    unblockAll: async function() {
      const occupants = await containerSvc.unblockPositions(this.ctx.container, []);
      this.ctx.pOccupants = null;
      this.setupMap(occupants);
      alertsSvc.success('All blocked positions freed!');
    },

    block: async function() {
      const selectedPositions = this.$refs.layout.getSelectedPositions();
      if (selectedPositions.length == 0) {
        alertsSvc.error('No positions selected');
        return;
      }

      for (const position of selectedPositions) {
        if (position.occupied) {
          alertsSvc.error('Only free positions can be blocked!');
          return;
        }
      }

      const toBlock = selectedPositions.map(p => ({
        posTwo: p.rowStr,
        posOne: p.columnStr,
        posTwoOrdinal: p.row,
        posOneOrdinal: p.column,
        position: p.position
      })); 

      const occupants = await containerSvc.blockPositions(this.ctx.container, toBlock);
      this.ctx.pOccupants = null;
      this.setupMap(occupants);
      alertsSvc.success('Blocked ' + toBlock.length + ' positions!');
    },

    unblock: async function() {
      const selectedPositions = this.$refs.layout.getSelectedPositions();
      if (selectedPositions.length == 0) {
        alertsSvc.error('No positions selected');
        return;
      }

      for (const position of selectedPositions) {
        if (!position.occupied || !position.occupied.blocked) {
          alertsSvc.error('Only blocked positions can be freed!');
          return;
        }
      }

      const toUnblock = selectedPositions.map(p => ({
        posTwo: p.rowStr,
        posOne: p.columnStr,
        posTwoOrdinal: p.row,
        posOneOrdinal: p.column,
        position: p.position
      }));

      const occupants = await containerSvc.unblockPositions(this.ctx.container, toUnblock);
      this.ctx.pOccupants = null;
      this.setupMap(occupants);
      alertsSvc.success('Unblocked ' + toUnblock.length + ' positions!');
    },

    showColorCoding: function() {
      const color2TypesMap = this.ctx.color2TypesMap = {};

      for (let klassType in this.ctx.colorCoding) {
        const color         = JSON.stringify(this.ctx.colorCoding[klassType] || {});
        const [klass, type] = klassType.split(':');

        color2TypesMap[color] = color2TypesMap[color] || {style: this.ctx.colorCoding[klassType], types: []}
        const typesList     = color2TypesMap[color].types;
        const displayType   = type + ' (' + klass + ')';
        if (typesList.indexOf(displayType) == -1) {
          typesList.push(displayType);
        }
      }

      this.$refs.colorCoding.open();
    },

    closeColorCoding: function() {
      this.$refs.colorCoding.close();
    },

    showLabelsScanArea: function() {
      this.ctx.showLabelsScanArea = true;
      this.ctx.labels = '';
      this.ctx.vacateOccupant = false;
    },

    hideLabelsScanArea: function() {
      this.ctx.showLabelsScanArea = false;
      if (this.ctx.labels) {
        this.ctx.labels = '';
      }

      this.updateMap(this.ctx.labels);
    },

    updateMap: function(labels) {
      this.ctx.labels = labels;
      if (this.isDimensionless) {
        return;
      }

      if (this.labelsDebounceTimer) {
        clearTimeout(this.labelsDebounceTimer);
        this.labelsDebounceTimer = null;
      }

      const self = this;
      const ctx = this.ctx;
      this.labelsDebounceTimer = setTimeout(
        () => {
          if (!ctx.pOccupants) {
            ctx.pOccupants = util.clone(ctx.occupants);
          }

          const cOccupants = util.clone(ctx.pOccupants);
          const {occupants, noFreeLocs} = this.$refs.layout.assignPositions(cOccupants, labels, ctx.vacateOccupant);
          if (noFreeLocs) {
            alertsSvc.error('Container does not have enough free locations to accommodate input specimen labels');
          }

          ctx.noFreeLocs = noFreeLocs;
          self.setupMap(occupants);
        }, 500
      );
    },

    assignPositions: async function() {
      const container = this.ctx.container;
      if (this.isDimensionless) {
        const labels = util.splitStr(this.ctx.labels, /,|\t|\n/, false);
        if (labels.length == 0) {
          return;
        }

        const {useBarcode, specimens} = await this.$refs.specimensScanner.getSpecimensByLabel(labels);
        if (!await this.confirmTransfer(useBarcode, specimens)) {
          return;
        }

        const positions = specimens.map(spmn => ({occuypingEntity: 'specimen', occupyingEntityId: spmn.id}));
        await containerSvc.assignPositions(container, {positions});
        alertsSvc.success('Successfully added ' + positions.length + ' specimens to the container!');
        this.$refs.specimensScanner.clearInput();
        return;
      }

      if (this.ctx.noFreeLocs) {
        alertsSvc.error('Container does not have enough free locations to accommodate input specimen labels');
        return;
      }

      const addedEntities = [], vacatedEntities = [];
      for (let pos of this.ctx.occupants) {
        if (pos.id) {
          continue;
        }

        if (!pos.occupyingEntityName || pos.occupyingEntityName.trim().length == 0) {
          vacatedEntities.push(pos.oldOccupant);
        } else {
          addedEntities.push(pos);
        }
      }

      if (addedEntities.length == 0 && vacatedEntities.length == 0) {
        return;
      }

      const addedLabels = addedEntities.map(pos => pos.occupyingEntityName);
      const {useBarcode, specimens} = await this.$refs.specimensScanner.getSpecimensByLabel(addedLabels);
      if (!specimens || specimens.length < addedLabels.length) {
        alertsSvc.error('One or more specimens not found!');
        return;
      }

      if (!await this.confirmTransfer(useBarcode, specimens)) {
        return;
      }

      const specimensMap = specimens.reduce((acc, spmn) => { acc[spmn.id] = spmn; return spmn }, {});
      const positions = [];
      for (let entity of vacatedEntities) {
        if (!specimensMap[entity.occupyingEntityId]) {
          //
          // specimen is not reassigned a new position, vacate it from container
          //
          positions.push({occuypingEntity: 'specimen', occupyingEntityId: entity.occupyingEntityId});
        }
      }

      addedEntities.forEach((pos, idx) => {
        const toAdd = {...pos, occupyingEntityId: specimens[idx].id};
        delete toAdd.oldOccupant;
        positions.push(toAdd);
      });

      const assignOp = {vacateOccupant: this.ctx.vacateOccupant, positions };
      const occupants = await containerSvc.assignPositions(this.ctx.container, assignOp);
      this.ctx.pOccupants = null;
      this.setupMap(occupants);
      this.$refs.specimensScanner.clearInput();
    },

    confirmTransfer: async function(useBarcode, specimens) {
      const storedSpmns = this.ctx.toTransferSpmns = specimens
        .filter(spmn => spmn.storageLocation && spmn.storageLocation.id > 0)
        .map(spmn => (useBarcode && spmn.barcode) || spmn.label);
      if (storedSpmns.length == 0) {
        return true;
      }

      const resp = await this.$refs.confirmTransferDialog.open();
      return resp == 'proceed';
    }
  }
}
</script>

<style scoped>
.os-container-occupant h4.title {
  margin-top: 0;
  border-bottom: 1px solid #ddd;
  padding-bottom: 4px;
}

.os-container-occupant :deep(ul) {
  margin-bottom: 0;
}

.os-container-occupant {
  max-height: 375px;
  overflow: auto;
}

.os-container-locations {
  display: flex;
  flex-direction: column;
}

.os-container-locations .labels-scan-area :deep(.os-add-specimens .buttons .btn:first-child) {
  display: none;
}

.os-container-locations .map {
  flex: 1;
}

.os-container-locations .map .occupant {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-decoration: none;
}

.os-container-locations .map .occupant.new-occupant:before {
  content: ' ';
  position: absolute;
  display: inline-block;
  background: #ffc;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
}

.os-container-locations .map .occupant.new-occupant.displaced-occupant:before {
  background: pink;
}

.os-container-locations .map .occupant .container-icon {
  font-size: 175%;
  color: #3a87ad;
}

.os-specimen-color-coding .specimen-icon,
.os-container-locations .map .occupant .specimen-icon {
  display: inline-block;
  font-size: 175%;
  height: 35px;
  width: 35px;
  border-radius: 50%;
  background: #3a87ad;
  color: #fff;
  text-align: center;
  z-index: 10;
}

.os-container-locations .map .occupant .blocked {
  font-size: 175%;
  height: 35px;
  width: 35px;
  text-align: center;
  background: gray;
  color: #fff;
  border-radius: 50%;
}

.os-container-locations .map .occupant:hover .icon {
  color: #23527c;
  color: #fff;
}

.os-container-locations .map .occupant .name {
  width: 100%;
  text-overflow: ellipsis;
  white-space: nowrap;
  overflow: hidden;
  border: #3a87ad;
  color: #666;
  border-radius: 3px;
  padding: 2px;
  display: block;
  text-align: center;
  margin-top: 2px;
  z-index: 10;
}

.os-container-locations .map .occupant .name.reserved {
  background: rgba(128, 0, 128, 0.7);
  color: #fff;
}
  
.os-container-locations .map .occupant:hover .name {
  color: #333;
}

.os-container-locations .map .occupant .specimen-reserved {
  position: absolute;
  top: 0.25rem;
  left: 0.25rem;
  font-size: 85%;
  color: rgba(128, 0, 128, 0.7);
}

.os-container-locations .map .position-selector {
  position: absolute;
  top: 0.25rem;
  left: 0.25rem;
}

.os-container-locations .map .position-selector :deep(.p-checkbox),
.os-container-locations .map .position-selector :deep(.p-checkbox .p-checkbox-box) {
  height: 1rem;
  width: 1rem;
}

.os-container-locations .map :deep(button) {
  width: 100%;
}

table.os-specimen-color-coding {
  width: 100%;
}

table.os-specimen-color-coding th,
table.os-specimen-color-coding td {
  border-bottom: 1px solid #ddd;
  padding: 1rem;
  text-align: left;
}
</style>
