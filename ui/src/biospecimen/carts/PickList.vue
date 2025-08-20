<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="bcrumb" />
      </template>

      <span class="os-title">
        <h3>{{ctx.list.name}}</h3>
      </span>

      <template #right v-if="ctx.list.totalSpecimens > 0">
        <os-progress-bar class="list-progress"
          :value="ctx.list.pickedSpecimens" :total="ctx.list.totalSpecimens"
          v-os-tooltip.bottom="$t('carts.picklist_progress', ctx.list)" />
      </template>
    </os-page-head>

    <os-page-body>
      <os-grid>
        <os-grid-column :width="12">
          <os-tabs @tab-change="onTabChange">
            <os-tab>
              <template #header>
                <span v-t="'carts.unpicked_specimens'">Unpicked Specimens</span>
              </template>

              <div class="tab-panel">
                <div class="toolbar">
                  <span class="left">
                    <os-button :label="$t('carts.use_box_scanner')" @click="useBoxScanner"
                      v-if="ctx.scannerOpts.options && ctx.scannerOpts.options.length > 0" />

                    <os-button :label="$t('carts.use_device_camera')" @click="useDeviceCamera" v-if="!ctx.useCamera" />

                    <os-button :label="$t('carts.turn_off_camera')" @click="turnOffCamera" v-else/>
                  </span>

                  <span class="right">
                    <os-button left-icon="search" :label="$t('common.buttons.search')" @click="toggleUnpickedSearch" />
                  </span>
                </div>

                <div class="content">
                  <div v-if="!ctx.useCamera">
                    <os-input-text ref="barcodesTextField" :debounce="500" v-model="ctx.inputBarcodes"
                      @change="handleInputBarcodes" :placeholder="$t('carts.scan_or_copy_paste_barcodes')"
                      style="margin-bottom: 1.25rem;" v-if="!ctx.batchMode" />
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
                    <os-textarea v-model="ctx.inputBarcodes"
                      :placeholder="$t('carts.scan_or_copy_paste_barcodes')" />

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

                  <os-list-view
                    :data="unpickedCtx.list || []"
                    :schema="unpickedSpecimensListSchema"
                    :query="unpickedCtx.query"
                    :allowSelection="false"
                    :loading="unpickedCtx.loading"
                    @filtersUpdated="loadUnpickedSpecimens"
                    ref="unpickedListView"
                  />
                </div>
              </div>
            </os-tab>

            <os-tab>
              <template #header>
                <span v-t="'carts.picked_specimens'">Picked Specimens</span>
              </template>

              <div class="tab-panel">
                <div class="toolbar">
                  <span class="left">
                    <os-button left-icon="times" :label="$t('common.buttons.remove')"
                      v-os-tooltip.bottom="$t('carts.rm_spmns_from_picked_list')" @click="removeFromPickedList" 
                      v-if="pickedCtx.selected && pickedCtx.selected.length > 0" />

                    <os-specimen-actions :specimens="pickedCtx.selected" @reloadSpecimens="reloadPickedList"
                      v-if="pickedCtx.selected && pickedCtx.selected.length > 0" />
                  </span>
                  <span class="right">
                    <os-button left-icon="search" :label="$t('common.buttons.search')" @click="togglePickedSearch" />
                  </span>
                </div>

                <div class="content">
                  <os-list-view
                    :data="pickedCtx.list || []"
                    :schema="pickedSpecimensListSchema"
                    :query="pickedCtx.query"
                    :allowSelection="true"
                    :loading="pickedCtx.loading"
                    @filtersUpdated="loadPickedSpecimens"
                    @selectedRows="selectPickedSpecimens"
                    ref="pickedListView"
                  />
                </div>
              </div>
            </os-tab>
          </os-tabs>
        </os-grid-column>
      </os-grid>
    </os-page-body>

    <os-dialog ref="boxScannerDialog" :size="'lg'">
      <template #header>
        <span v-t="'common.scan_box'">Scan box</span>
      </template>
      <template #content>
        <div>
          <div class="row">
            <os-box-scanner ref="scanner" :scanners="ctx.scanners" :fetch-box-details="true"
              :fetch-specimen-details="searchSpecimens"
              @scan-started="onBoxScanningStart" @scan-results="handleBoxScanResults" />
          </div>

          <div class="results" v-if="boxScanCtx.results">
            <os-box-scan-results :scan-results="boxScanCtx" />
          </div>

          <div class="row" v-if="boxScanCtx.container">
            <box-layout class="map" :container="boxScanCtx.container" :occupants="boxScanCtx.tubes">
              <template #occupant_specimen="slotProps">
                <specimen-cell :specimen="slotProps.occupant" />
              </template>
            </box-layout>
          </div>
        </div>
      </template>
      <template #footer>
        <os-button text :label="$t('common.buttons.close')" @click="hideBoxScannerDialog" />

        <os-button secondary :label="$t('common.buttons.rescan')" @click="rescanBox"
          :disabled="!boxScanCtx.results" />

        <os-button primary :label="$t('carts.pick')" @click="pickBoxSpecimens"
          :disabled="!boxScanCtx.specimens || boxScanCtx.specimens.length == 0" />
      </template>
    </os-dialog>
  </os-page>
</template>

<script>

import alertsSvc   from '@/common/services/Alerts.js';
import cartSvc     from '@/biospecimen/services/SpecimenCart.js';
import routerSvc   from '@/common/services/Router.js';
import specimenSvc from '@/biospecimen/services/Specimen.js';
import util        from '@/common/services/Util.js';
import wfSvc       from '@/common/services/Workflow.js';

import pickedSpecimensListSchema   from "@/biospecimen/schemas/carts/picked-specimens.js";
import unpickedSpecimensListSchema from "@/biospecimen/schemas/carts/unpicked-specimens.js";

import BoxSpecimenCell from '@/administrative/containers/BoxSpecimenCell.vue';
import Layout          from '@/administrative/containers/Layout.vue';
import {QrcodeStream}  from 'vue-qrcode-reader';

export default {
  props: ['cartId', 'listId', 'upf', 'ppf'],

  components: {
    'qrcode-stream': QrcodeStream,

    'box-layout': Layout,

    'specimen-cell': BoxSpecimenCell
  },

  data() {
    return {
      ctx: {
        list: {cart: { }},

        activeTab: 0,

        scannerOpts: {options: [], displayProp: 'name'},
      },

      boxScanCtx: {
      },

      pickedCtx: {
        list: null,

        query: this.ppf,

        selected: null,

        unpicked: 0
      },

      pickedSpecimensListSchema,

      unpickedCtx: {
        list: null,

        query: this.upf,

        picked: 0
      },

      unpickedSpecimensListSchema
    };
  },

  created() {
  },

  mounted() {
    this._loadList();
    this.$refs.barcodesTextField.focus();

    wfSvc.getSysWorkflow('box-scanners').then(
      ({scannedBoxField, scanners}) => {
        this.ctx.scanners = scanners;

        const options = this.ctx.scannerOpts.options = scanners.map((s, idx) => ({id: idx, ...s}));
        if (options.length == 1) {
          this.ctx.scanner = options[0];
        }

        this.ctx.scannedBoxField = scannedBoxField || 'barcode';
      }
    );
  },

  computed: {
    bcrumb: function() {
      const {cart} = this.ctx.list || {};
      if (!cart || !cart.id) {
        return [];
      }

      return [
        {url: routerSvc.getUrl('SpecimenCartsList', {cartId: -1}), label: this.$t('carts.list')},
        {url: routerSvc.getUrl('CartSpecimensList', {cartId: cart.id}), label: cartSvc.getDisplayName(cart)},
        {url: routerSvc.getUrl('PickLists', {cartId: cart.id}), label: this.$t('carts.pick_lists')}
      ];
    },

    viewKey: function() {
      return this.cartId + '_' + this.listId;
    },

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
    }
  },

  watch: {
    viewKey: function() {
      this._loadList();
    }
  },

  methods: {
    onTabChange: function({index}) {
      this.ctx.activeTab = index;
      if (index == 1) {
        this._loadPickedSpecimens();
      } else if (index == 0) {
        this._loadUnpickedSpecimens();
      }
    },

    loadUnpickedSpecimens: function({filters, uriEncoding, pageSize}) {
      this.unpickedCtx.filters = filters;
      this.unpickedCtx.uriEncodedFilters = uriEncoding;
      this.unpickedCtx.pageSize = pageSize;
      this.unpickedCtx.list = null;
      this._loadUnpickedSpecimens();
      routerSvc.goto(
        'PickList',
        {cartId: this.cartId, listId: this.listId},
        {upf: uriEncoding, ppf: this.pickedCtx.uriEncodedFilters}
      );
    },

    loadPickedSpecimens: function({filters, uriEncoding, pageSize}) {
      this.pickedCtx.filters = filters;
      this.pickedCtx.uriEncodedFilters = uriEncoding;
      this.pickedCtx.pageSize = pageSize;
      this.pickedCtx.list = this.pickedCtx.selected = null;
      this._loadPickedSpecimens();
      routerSvc.goto(
        'PickList',
        {cartId: this.cartId, listId: this.listId},
        {upf: this.unpickedCtx.uriEncodedFilters, ppf: uriEncoding}
      );
    },

    selectPickedSpecimens: function(selected) {
      this.pickedCtx.selected = selected.map(({rowObject: {specimen}}) => ({id: specimen.id, cpId: specimen.cpId}));
    },

    useBoxScanner: function() {
      this.$refs.boxScannerDialog.open();
    },

    rescanBox: function() {
      this.boxScanCtx = {};
      this.$refs.scanner.scan();
    },

    pickBoxSpecimens: function() {
      const specimens = this.boxScanCtx.specimens.map(specimen => ({id: specimen.id}));
      this._pickSpecimens(specimens).then(() => this.hideBoxScannerDialog());
    },

    hideBoxScannerDialog: function() {
      this.boxScanCtx = {};
      this.$refs.boxScannerDialog.close();
    },

    onBoxScanningStart: function() {
      this.boxScanCtx = {scanning: true};
    },

    handleBoxScanResults: async function({scanner, box, tubes, container, readErrorsCount, noTubesCount, barcodes, notFound}) {

      this.boxScanCtx = {
        scanning: false,
        results: true,
        scanner,
        noBoxBarcode: !box || !box.barcode,
        readErrorsCount,
        noTubesCount,
        container,
        tubes,
        scannedBarcodesCount: barcodes.length,
        notFound,
        specimens: (tubes || []).filter(tube => tube.specimen && tube.specimen.id > 0).map(tube => tube.specimen)
      };
    },

    searchSpecimens: async function(filters) {
      filters = filters || {};
      return specimenSvc.search({specimenListId: this.cartId, ...filters});
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
    
    handleInputBarcodes: function() {
      const barcodes = util.splitStr(this.ctx.inputBarcodes || '', /,|\t|\n/, false);
      this.ctx.inputBarcodes = null;
      if (barcodes.length == 0) {
        return;
      }

      const specimens = this.ctx.useBarcode ? barcodes.map(barcode => ({barcode})) : barcodes.map(label => ({label}));
      this._pickSpecimens(specimens);
    },

    toggleRealTimePick: function() {
      if (this.ctx.batchMode) {
        return;
      }

      this.handleInputBarcodes();
    },

    toggleUnpickedSearch: function() {
      this.$refs.unpickedListView.toggleShowFilters();
    },

    togglePickedSearch: function() {
      this.$refs.pickedListView.toggleShowFilters();
    },

    reloadPickedList: function() {
      this.$refs.pickedListView.reload();
    },

    removeFromPickedList: function() {
      cartSvc.unpickSpecimens(+this.cartId, +this.listId, this.pickedCtx.selected).then(
        ({unpicked}) => {
          alertsSvc.success({code: 'carts.specimens_unpicked', args: {count: unpicked.length}});

          this.$refs.pickedListView.clearSelection();
          this.unpickedCtx.list = null;
          this.pickedCtx.selected = null;
          this.pickedCtx.unpicked += unpicked.length;
            
          if (this.pickedCtx.list) {
            this.pickedCtx.list = this.pickedCtx.list.filter(item => unpicked.indexOf(item.specimen.id) == -1);
          }
            
          if (this.pickedCtx.unpicked >= 50) {
              this.pickedCtx.list = null;
              this._loadPickedSpecimens();
          }

          this.ctx.list.pickedSpecimens -= unpicked.length;
        }
      );
    },

    _loadList: async function() {
      this.ctx.list = {};
      this.pickedCtx   = {list: null, query: this.ppf};
      this.unpickedCtx = {list: null, query: this.upf};
      cartSvc.getPickList(+this.cartId, +this.listId).then(list => this.ctx.list = list);
    },

    _loadUnpickedSpecimens: function() {
      if (this.unpickedCtx.list || this.ctx.activeTab != 0) {
        return;
      }

      this.unpickedCtx.loading = true;
      cartSvc.getUnpickedSpecimens(+this.cartId, +this.listId, this.unpickedCtx.filters || {}).then(
        items => {
          this.unpickedCtx.loading = false;
          this.unpickedCtx.list = items;
          this.unpickedCtx.picked = 0;
        }
      );
    },

    _loadPickedSpecimens: function() {
      if (this.pickedCtx.list || this.ctx.activeTab != 1) {
        return;
      }

      this.pickedCtx.loading = true;
      cartSvc.getPickedSpecimens(+this.cartId, +this.listId, this.pickedCtx.filters || {}).then(
        items => {
          this.pickedCtx.loading = false;
          this.pickedCtx.list = items;
          this.pickedCtx.unpicked = 0;
        }
      );
    },

    _pickSpecimens: function(specimens) {
      return cartSvc.pickSpecimens(+this.cartId, +this.listId, specimens).then(
        ({picked}) => {
          alertsSvc.success({code: 'carts.specimens_picked', args: {count: picked.length}});
          this.pickedCtx.list = this.pickedCtx.selected = null;
          this.unpickedCtx.picked += picked.length;

          if (this.unpickedCtx.list) {
            this.unpickedCtx.list = this.unpickedCtx.list.filter(item => picked.indexOf(item.specimen.id) == -1);
          }

          if (this.unpickedCtx.picked >= 50) {
            this.unpickedCtx.list = null;
            this._loadUnpickedSpecimens();
          }
    
          this.ctx.inputBarcodes = null;
          this.ctx.list.pickedSpecimens += picked.length;
          return picked;
        }
      );
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
