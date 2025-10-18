<template>
  <div>
    <div class="scanner">
      <os-label v-t="'common.select_scanner'">Select Scanner</os-label>
      <div class="input-group">
        <os-dropdown :list-source="scannerOpts" v-model="scanner" />
        <os-button primary :label="$t('common.buttons.scan')" :disabled="!scanner" @click="scan" />
      </div>
    </div>

    <os-box-details v-model="box" :scanned-box-id="scannedBoxId" :box-id="boxBarcode"
      @search-box="searchBox($event)" v-if="fetchBoxDetails && scanner && box && showBoxDetails == true" />
  </div>
</template>

<script>
import boxUtil          from '@/common/services/BoxUtil.js';
import containerSvc     from '@/administrative/services/Container.js';
import containerTypeSvc from '@/administrative/services/ContainerType.js';
import numUtil          from '@/common/services/NumberConverterUtil.js';
import scanner          from '@/administrative/services/BoxScanner.js';
import util             from '@/common/services/Util.js';

export default {
  //
  // scanned-box-id either name or barcode
  //
  props: ['scanned-box-id', 'scanners', 'show-box-details', 'fetch-box-details', 'fetch-specimen-details'],

  emits: ['scan-started', 'scan-results', 'scan-error'],

  data() {
    return {
      scanner: null,

      box: null,

      boxBarcode: null,

      tubes: []
    }
  },

  computed: {
    scannerOpts: function() {
      const options = (this.scanners || []).map((s, idx) => ({id: idx, ...s}));
      return {displayProp: 'name', options};
    }
  },

  methods: {
    scan: async function() {
      if (!this.scanner) {
        return; 
      }
    
      this.$emit('scan-started', {scanner: this.scanner});
      let {box, tubes} = await scanner.scan(this.scanner) || {};
      this.tubes = tubes;
      this.boxBarcode = box.barcode;

      let noTubesCount = 0, readErrorsCount = 0, barcodes = [];
      for (const {barcode} of tubes || []) {
        if (!barcode) {
          noTubesCount++;
        } else if (barcode == 'READ_ERROR') {
          readErrorsCount++;
        } else {
          barcodes.push(barcode);
        }
      }

      if (typeof this.fetchSpecimenDetails == 'function') {
        await this._loadSpecimenDetails(this.tubes, barcodes)
      }

      let container = null;
      if (this.fetchBoxDetails) {
        this.box = container = await this._getBoxDetails(this.scanner, box, this.scannedBoxId);
        tubes = this._assignPositions(container, util.clone(tubes));
      } else {
        this.box = box;
      }

      let notFound = null;
      if (typeof this.fetchSpecimenDetails == 'function') {
        notFound = tubes.filter(tube => tube.barcode && tube.barcode != 'READ_ERROR' && !tube.specimen);
      }

      this.scanResults = {scanner: this.scanner, box, tubes, barcodes, container, noTubesCount, readErrorsCount, notFound};
      this.$emit('scan-results', this.scanResults);
    },

    searchBox: async function(input) {
      this.box = await this._getBoxDetails(this.scanner, input, this.scannedBoxId);

      const tubes = this._assignPositions(this.box, util.clone(this.tubes));
      let notFound = null;
      if (typeof this.fetchSpecimenDetails == 'function') {
        notFound = tubes.filter(tube => tube.barcode && tube.barcode != 'READ_ERROR' && !tube.specimen);
      }

      this.scanResults.box = this.scanResults.container = this.box
      this.scanResults.tubes = tubes;
      this.scanResults.notFound = notFound;
      this.$emit('scan-results', this.scanResults);
    },

    _getBoxDetails: async function(scanner, box, boxIdFieldName) {
      const container = {}; 
      let promise;
      if (box && box.barcode) {
        if (boxIdFieldName == 'name') {
          promise = containerSvc.getContainerByName(box.barcode, false);
        } else {
          promise = containerSvc.getContainerByBarcode(box.barcode, false);
        }   

        const dbContainer = await promise;
        Object.assign(container, dbContainer);
      }   

      if (!container.id) {
        const containerType = await containerTypeSvc.getTypeByName(scanner.containerType);
        Object.assign(container, containerType);
        container.typeName = container.name;
        delete container.id;
        delete container.name;

        container[boxIdFieldName] = box.barcode;
      }

      const allowedTypes = container.allowedTypes = [];
      for (const specimenClass of (container.allowedSpecimenClasses || [])) {
        allowedTypes.push({specimenClass, type: 'All ' + specimenClass, all: true});
      }

      for (const type of (container.allowedSpecimenTypes || [])) {
        allowedTypes.push({type});
      }

      return container;
    },

    _assignPositions: function(container, tubes) {
      if (!tubes) {
        return [];
      }

      const {
        noOfRows, noOfColumns,
        rowLabelingScheme, columnLabelingScheme,
        positionAssignment, positionLabelingMode
      } = container || {}; 
          
      const assigner = boxUtil.getPositionAssigner(positionAssignment);
      for (const tube of tubes) {
        const {barcode, row, column} = tube;
        const position = assigner.pos({row, col: column, nr: noOfRows, nc: noOfColumns})
        
        const posOne = numUtil.fromNumber(columnLabelingScheme, column);
        const posTwo = numUtil.fromNumber(rowLabelingScheme, row);
        Object.assign(tube, {
          mode:     positionLabelingMode,
          row:      posTwo,
          column:   posOne,
          posOne,
          posTwo,
          posOneOrdinal: column,
          posTwoOrdinal: row,
          position: position,
          barcode:  barcode
        });
      }

      return tubes;
    },

    _loadSpecimenDetails: async function(tubes, barcodes) {
      if (!tubes || !barcodes) {
        return;
      }

      const filters = {barcode: barcodes};
      const specimens = await this.fetchSpecimenDetails(filters);
      const specimensMap = {};
      for (const specimen of specimens) {
        specimensMap[specimen.barcode.toLowerCase()] = specimen;
      }

      for (const tube of tubes) {
        if (!tube.barcode) {
          continue;
        }

        const spmn = specimensMap[tube.barcode.toLowerCase()];
        tube.occuypingEntity = 'specimen',
        tube.displayName = tube.barcode;
        tube.specimen = spmn;
        if (spmn) {
          tube.id = spmn.id;
          tube.occupyingEntityId = spmn.id;
          tube.colorCode = util.getContainerColorCode(spmn);
          tube.tooltip = util.getSpecimenDescription(spmn, {detailed: true});
        } else if (tube.barcode != 'READ_ERROR') {
          tube.tooltip = this.$t('specimens.not_found');
        } else {
          tube.tooltip = this.$t('common.box_scanner_read_error');
        }
      }
    }
  }
}
</script>

<style scoped>
.scanner {
  margin-bottom: 1.25rem;
}

.scanner .input-group {
  display: flex;
  flex-direction: row;
}

.scanner .input-group :deep(.os-dropdown) {
  flex: 1;
}

.scanner .input-group :deep(.os-dropdown .p-dropdown) {
  border-top-right-radius: 0px;
  border-bottom-right-radius: 0px;
}

.scanner .input-group :deep(button) {
  border-top-left-radius: 0px;
  border-bottom-left-radius: 0px;
  border-top-right-radius: 4px;
  border-bottom-right-radius: 4px;
}
</style>
