<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="bcrumb" />
      </template>

      <span class="os-title">
        <h3>{{ctx.list.name}}</h3>

        <div class="accessories" v-if="ctx.list.transferToBox">
          <os-tag :value="$t('carts.box_transfer_enabled')" :rounded="true" type="info" />
        </div>
      </span>

      <template #right v-if="ctx.list.totalSpecimens > 0">
        <os-progress-bar class="list-progress"
          :value="ctx.list.pickedSpecimens" :total="ctx.list.totalSpecimens"
          v-os-tooltip.bottom="$t('carts.picklist_progress', ctx.list)" />
      </template>
    </os-page-head>

    <os-page-body>
      <os-grid>
        <os-grid-column :width="12" v-if="ctx.list.id > 0">
          <os-tabs @tab-change="onTabChange">
            <os-tab>
              <template #header>
                <span v-t="'carts.unpicked_specimens'">Unpicked Specimens</span>
              </template>

              <UnpickedSpecimensTab ref="unpickedListView" :cart="ctx.list.cart" :pick-list="ctx.list"
                :filters="upf" :active-tab="ctx.activeTab == 0"
                @specimens-loaded="reloadRoute({upf: $event.uriEncoding})"
                @picked-specimens="onSpecimensPick($event)" />
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

    <os-box-scanner-dialog ref="boxScannerDialog" :fetch-specimens="searchSpecimens"
      :show-box-details="ctx.list.transferToBox"
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
  </os-page>
</template>

<script>

import alertsSvc   from '@/common/services/Alerts.js';
import cartSvc     from '@/biospecimen/services/SpecimenCart.js';
import routerSvc   from '@/common/services/Router.js';
import util        from '@/common/services/Util.js';

import pickedSpecimensListSchema   from "@/biospecimen/schemas/carts/picked-specimens.js";
import unpickedSpecimensListSchema from "@/biospecimen/schemas/carts/unpicked-specimens.js";

import UnpickedSpecimensTab from './UnpickedSpecimensTab.vue';

export default {
  props: ['cartId', 'listId', 'upf', 'ppf'],

  components: {
    UnpickedSpecimensTab
  },

  data() {
    return {
      ctx: {
        list: {cart: { }},

        activeTab: 0,

        scannerOpts: {options: [], displayProp: 'name'},
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
    viewKey: function() {
      this._loadList();
    }
  },

  methods: {
    onSpecimensPick: function() {
      this.pickedCtx.list = this.pickedCtx.selected = null;
    },

    onTabChange: function({index}) {
      this.ctx.activeTab = index;
      if (index == 1) {
        this._loadPickedSpecimens();
      }
    },

    reloadRoute: function(queryParams) {
      const {name, params, query} = routerSvc.getCurrentRoute();
      routerSvc.goto(name, params, {...query, ...queryParams});
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

          this.$refs.unpickedListView.nullifyList();
        }
      );
    },

    _loadList: async function() {
      this.ctx.list = {};
      this.pickedCtx   = {list: null, query: this.ppf};
      this.unpickedCtx = {list: null, query: this.upf};
      cartSvc.getPickList(+this.cartId, +this.listId).then(list => this.ctx.list = list);
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
