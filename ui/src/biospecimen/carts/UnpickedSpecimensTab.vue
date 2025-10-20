<template>
    <div class="tab-panel">
      <div class="toolbar" v-if="!ctx.selected || ctx.selected.length == 0">
        <span class="left">
          <os-button :label="$t('carts.use_box_scanner')" @click="useBoxScanner" />

          <os-button :label="$t('carts.use_device_camera')" @click="useDeviceCamera" v-if="!ctx.useCamera" />

          <os-button :label="$t('carts.turn_off_camera')" @click="turnOffCamera" v-else/>

          <os-button :label="$t('carts.change_selected_box')" @click="changeSelectedBox" v-if="ctx.selectedBox" />
        </span>

        <span class="right">
          <os-button left-icon="search" :label="$t('common.buttons.search')" @click="toggleSearch" />
        </span>
      </div>

      <div class="toolbar" v-else>
        <span class="left">
          <os-button :label="$t('carts.pick')" @click="pickSelectedSpecimens" />
        </span>
      </div>

      <div class="content">
        <span v-if="!ctx.selected || ctx.selected.length == 0">
          <div class="input-group" v-if="!ctx.useCamera">
            <os-input-text ref="barcodesTextField" :debounce="500" v-model="ctx.inputBarcodes"
              @change="handleInputBarcodes" :placeholder="$t('carts.scan_or_copy_paste_barcodes')"
              v-if="!ctx.batchMode" />

            <os-button primary :label="$t('carts.pick')" @click="handleInputBarcodes"
              v-if="!ctx.batchMode && ctx.inputBarcodes && ctx.inputBarcodes.trim().length > 0" />
          </div>

          <div v-else>
            <div class="camera-display">
              <qrcode-stream :formats="allFormats" @camera-on="onCameraReady" @detect="onBarcodeDetect">
                <os-message type="info" v-if="!ctx.initCamera">
                  <span v-t="'carts.initialising_camera'">Initialising camera...</span>
                </os-message>
              </qrcode-stream>
            </div>
          </div>

          <div class="input-group" v-if="ctx.batchMode">
            <os-textarea v-model="ctx.inputBarcodes" :placeholder="$t('carts.scan_or_copy_paste_barcodes')" />

            <os-button primary :label="$t('carts.pick')" @click="handleInputBarcodes" />
          </div>

          <div class="scan-options">
            <os-boolean-checkbox :inline-label-code="$t('carts.use_barcode')" v-model="ctx.useBarcode" />

            <os-boolean-checkbox :inline-label-code="$t('carts.turn_off_real_time')" v-model="ctx.batchMode"
              @update:model-value="toggleRealTimePick" />
          </div>

          <div v-if="ctx.batchMode">
            <span v-t="{path: 'carts.scanned_barcodes_count', args: {count: scannedBarcodesCount}}">
              <span>Scanned Barcodes: {{scannedBarcodesCount}}</span>
            </span>
          </div>
        </span>

        <os-list-view
          :data="ctx.list || []"
          :schema="listSchema"
          :query="ctx.query"
          :allowSelection="true"
          :loading="ctx.loading"
          @filtersUpdated="loadSpecimens"
          @selectedRows="selectSpecimens"
          ref="listView"
        />
      </div>
    </div>

  <os-box-scanner-dialog ref="boxScannerDialog" :fetch-specimens="searchSpecimens"
    :show-box-details="pickList.transferToBox"
    :done-label="$t('carts.pick')" @done="pickBoxSpecimens" />

  <os-dialog ref="selectBoxDialog">
    <template #header>
      <span v-t="'carts.select_box'">Select Box</span>
    </template>
    <template #content>
      <os-form ref="selectBoxForm" :schema="selectBoxFs" :data="ctx" />
    </template>
    <template #footer>
      <os-button :label="$t('common.buttons.cancel')" @click="hideSelectBoxDialog" />
      <os-button primary :label="$t('common.buttons.done')" @click="selectBox" />
    </template>
  </os-dialog>
</template>

<script>

import alertsSvc   from '@/common/services/Alerts.js';
import cartSvc     from '@/biospecimen/services/SpecimenCart.js';
import specimenSvc from '@/biospecimen/services/Specimen.js';
import util        from '@/common/services/Util.js';

import listSchema from "@/biospecimen/schemas/carts/unpicked-specimens.js";

import {QrcodeStream}  from 'vue-qrcode-reader';

export default {
  props: ['cart', 'pick-list', 'filters', 'active-tab'],

  components: {
    'qrcode-stream': QrcodeStream
  },

  data() {
    return {
      ctx: {
        list: null,

        query: this.filters,

        picked: 0
      },

      listSchema
    };
  },

  created() {
  },

  mounted() {
    this.$nextTick(() => this.$refs.barcodesTextField?.focus?.());
    if (this.activeTab) {
      this._loadSpecimens();
    }
  },

  computed: {
    allFormats: function() {
      return [
        'codabar', 'code_39', 'code_93', 'code_128', 'databar', 'databar_expanded',
        'dx_film_edge', 'ean_8', 'ean_8', 'itf', 'upc_a', 'upc_e',
        'aztec', 'data_matrix', 'maxi_code', 'pdf417', 'qr_code', 'micro_qr_code', 'rm_qr_code',
        'linear_codes', 'matrix_codes'
      ];
    },

    scannedBarcodesCount: function() {
      if (!this.ctx.batchMode) {
        return -1;
      }

      const barcodes = util.splitStr(this.ctx.inputBarcodes || '', /,|\t|\n/, false);
      return barcodes.length;
    },

    selectBoxFs: function() {
      return {
        rows: [
          {
            fields: [
              {
                name: 'boxName',
                type: 'dropdown',
                labelCode: 'carts.select_box_to_transfer_specimens',
                listSource: {
                  apiUrl: 'storage-containers',
                  displayProp: ({displayName, name}) => (displayName ? (displayName + ' ') : '') + name,
                  selectProp: 'name',
                  searchProp: 'name',
                  queryParams: {
                    static: {
                      onlyFreeContainers: true,
                      storeSpecimensEnabled: true,
                    }
                  }
                },
                validations: {
                  required: {
                    messageCode: 'carts.select_box'
                  }
                }
              }
            ]
          }
        ]
      };
    }
  },

  watch: {
    activeTab: function() {
      if (this.activeTab) {
        this._loadSpecimens();
      }
    }
  },

  methods: {
    nullifyList: function() {
      this.ctx.list = this.ctx.selected = null;
    },
      
    loadSpecimens: function({filters, uriEncoding, pageSize}) {
      this.ctx.filters = filters;
      this.ctx.uriEncodedFilters = uriEncoding;
      this.ctx.pageSize = pageSize;
      this.ctx.list = this.ctx.selected = null;
      this._loadSpecimens();
      this.$emit('specimens-loaded', {uriEncoding});
    },

    selectSpecimens: function(selected) {
      this.ctx.selected = selected.map(({rowObject: {specimen}}) => ({id: specimen.id, cpId: specimen.cpId}));
    },


    hideSelectBoxDialog: function() {
      this.ctx.selectBoxQ = null;
      this.$refs.selectBoxDialog.close();
    },

    showSelectBoxDialog: function() {
      this.ctx.boxName = null;
      this.$refs.selectBoxDialog.open();
      return new Promise(
        (resolve) => {
          this.ctx.selectBoxQ = resolve;
        }
      );
    },

    selectBox: function() {
      if (!this.$refs.selectBoxForm.validate()) {
        return;
      }

      this.ctx.selectedBox = this.ctx.boxName;
      this.ctx.selectBoxQ(this.ctx.boxName);
      this.hideSelectBoxDialog();
    },

    changeSelectedBox: function() {
      this.showSelectBoxDialog();
    },

    pickSelectedSpecimens: async function() {
      if (this.pickList.transferToBox) {
        if (!this.ctx.selectedBox) {
          await this.showSelectBoxDialog();
        }
      }

      this._pickSpecimens(this.ctx.selected, this.ctx.selectedBox).then(
        (resp) => {
          if (resp == 'NO_SPACE_IN_BOX') {
            this.ctx.selectedBox = null;
            this.pickSelectedSpecimens();
          } else if (resp != 'LTD_SPACE_IN_BOX') {
            this.$refs.listView.clearSelection()
          }
        }
      );
    },

    useBoxScanner: function() {
      this.$refs.boxScannerDialog.open();
    },

    pickBoxSpecimens: function(scanResults) {
      const {container, specimens} = scanResults;
      if (!this.pickList.transferToBox) {
         this._pickSpecimens(specimens.filter(spmn => spmn && spmn.id > 0).map(spmn => ({id: spmn.id})))
           .then(() => this.$refs.boxScannerDialog.close());
         return;
      }

      if (!container.storageLocation || !container.storageLocation.name) {
        alertsSvc.error({code: 'containers.parent_container_not_selected'});
        return;
      }

      const spmnClasses = [];
      const spmnTypes   = [];
      for (let type of container.allowedTypes) {
        if (type.all) {
          spmnClasses.push(type.specimenClass);
        } else {
          spmnTypes.push(type.type);
        }
      }

      const payload = {
        id: container.id,
        name: container.name,
        barcode: container.barcode,
        type: container.typeName,
        storageLocation: container.storageLocation,
        allowedCollectionProtocols: container.allowedCollectionProtocols,
        allowedSpecimenClasses: spmnClasses,
        allowedSpecimenTypes: spmnTypes,
        positions: specimens.filter(
          spmn => spmn.id > 0
        ).map(
          spmn => ({
            posOne: spmn.storageLocation.posOne,
            posTwo: spmn.storageLocation.posTwo,
            position: spmn.storageLocation.position,
            occuypingEntity: 'specimen',
            occupyingEntityId: spmn.id
          })
        )
      };

      this._pickBoxSpecimens(payload).then(() => this.$refs.boxScannerDialog.close());
    },

    searchSpecimens: async function(filters) {
      filters = filters || {};
      return specimenSvc.search({specimenListId: this.cart.id, ...filters});
    },

    useDeviceCamera: function() {
      this.ctx.useCamera = true;
      this.ctx.initCamera = false;
    },

    turnOffCamera: function() {
      this.ctx.useCamera = false;
    },

    onCameraReady: function() {
      this.ctx.initCamera = true;
    },

    onBarcodeDetect: function(detectedCodes) {
      const barcodes = [];
      for (const {rawValue} of detectedCodes) {
        barcodes.push(rawValue);
      }

      this.ctx.inputBarcodes = this.ctx.inputBarcodes || '';
      if (this.ctx.inputBarcodes && barcodes.length > 0) {
        this.ctx.inputBarcodes += ',';
      }

      this.ctx.inputBarcodes += barcodes.join(',');
      if (!this.ctx.batchMode) {
        this.handleInputBarcodes();
      } else {
        this.ctx.inputBarcodes = util.dedup(this.ctx.inputBarcodes, true);
      }
    },
    
    handleInputBarcodes: async function() {
      const barcodes = util.splitStr(this.ctx.inputBarcodes || '', /,|\t|\n/, false);
      if (barcodes.length == 0) {
        return;
      }

      if (this.pickList.transferToBox) {
        if (!this.ctx.selectedBox) {
          await this.showSelectBoxDialog();
        }
      }

      const specimens = this.ctx.useBarcode ? barcodes.map(barcode => ({barcode})) : barcodes.map(label => ({label}));
      this._pickSpecimens(specimens, this.ctx.selectedBox).then(
        (resp) => {
          if (resp == 'NO_SPACE_IN_BOX') {
            this.ctx.selectedBox = null;
            this.handleInputBarcodes();
          } else if (resp != 'LTD_SPACE_IN_BOX') {
            this.ctx.inputBarcodes = null;
          }
        }
      );
    },

    toggleRealTimePick: function() {
      if (this.ctx.batchMode) {
        return;
      }

      this.handleInputBarcodes();
    },

    toggleSearch: function() {
      this.$refs.listView.toggleShowFilters();
    },

    _loadSpecimens: function() {
      if (this.ctx.list || !this.activeTab) {
        return;
      }

      this.ctx.loading = true;
      cartSvc.getUnpickedSpecimens(this.cart.id, this.pickList.id, this.ctx.filters || {}).then(
        items => {
          this.ctx.loading = false;
          this.ctx.list = items;
          this.ctx.picked = 0;
        }
      );
    },


    _pickSpecimens: function(specimens, boxName) {
      return cartSvc.pickSpecimens(this.cart.id, this.pickList.id, specimens, boxName)
        .then(resp => this._handlePickSpecimensResp(resp));
    },

    _pickBoxSpecimens: function(boxDetail) {
      return cartSvc.pickBoxSpecimens(this.cart.id, this.pickList.id, boxDetail)
        .then(resp => this._handlePickSpecimensResp(resp));
    },

    _handlePickSpecimensResp: function({error, picked}) {
      if (error) {
        alertsSvc.error(error.message);
        return error.code;
      }

      alertsSvc.success({code: 'carts.specimens_picked', args: {count: picked.length}});
      this.ctx.picked += picked.length;
      this.ctx.selected = null;

      if (this.ctx.list) {
        this.ctx.list = this.ctx.list.filter(item => picked.indexOf(item.specimen.id) == -1);
      }

      if (this.ctx.picked >= 50) {
        this.ctx.list = null;
        this._loadSpecimens();
      }
    
      this.ctx.inputBarcodes = null;
      this.$emit('picked-specimens', picked);
      return picked;
    }
  }
}
</script>

<style scoped>
.tab-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.tab-panel .toolbar {
  margin-bottom: 1.25rem;
  display: flex;
  justify-content: space-between;
}

.tab-panel .toolbar :deep(button:not(:last-child)) {
  margin-right: 1rem;
}

.input-group {
  display: flex;
  flex-direction: row;
  margin-bottom: 1.25rem;
}

.input-group:deep(.os-input-text) {
  flex: 1;
}

.input-group:deep(button) {
  height: auto;
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
}

.list-progress {
  width: 25vw;
}

.camera-display {
  height: 150px;
  width: 300px;
  margin-bottom: 1.25rem;
}

.scan-options {
  display: flex;
  flex-direction: row;
}
</style>
