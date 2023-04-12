<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <h3 v-t="'containers.scan_boxes'"> </h3>
    </os-page-head>

    <os-page-body>
      <div class="form">
        <div class="row">
          <os-label v-t="'containers.select_scanner'">Select Scanner</os-label>
          <div class="scanner-chooser">
            <os-dropdown :list-source="ctx.scannerOpts" v-model="ctx.scanner" />
            <os-button primary :label="$t('containers.scan')" :disabled="!ctx.scanner" @click="scan" />
          </div>
        </div>

        <os-section v-if="ctx.box && ctx.box.barcode">
          <template #title>
            <span v-t="'containers.box_details'">Box Details</span>
          </template>

          <template #content>
            <ul class="os-key-values">
              <li class="item">
                <strong class="key key-sm">
                  <span v-t="{path: 'containers.' + ctx.scannedField}">Barcode</span>
                </strong>
                <span class="value value-md">{{ctx.box.barcode}}</span>
              </li>
              <li class="item">
                <strong class="key key-sm">
                  <span v-t="'containers.type'">Type</span>
                </strong>
                <span class="value value-md">{{ctx.box.type}}</span>
              </li>
              <li class="item">
                <strong class="key key-sm">
                  <span v-t="'containers.parent_container'">Parent Container</span>
                </strong>
                <span style="display: inline-block; min-width: 350px;">
                  <os-storage-position v-model="ctx.parentContainer"
                    :list-source="{queryParams: {static: {entityType: 'storage_container'}}}" />
                </span>
              </li>
            </ul>
          </template>
        </os-section>

        <os-section v-if="ctx.specimens">
          <template #title>
            <span v-t="'containers.specimens'">Specimens</span>
          </template>

          <template #content>
            <os-message v-if="ctx.error" type="error">
              <span v-t="{path: ctx.error, args: ctx.errorArgs}"></span>
            </os-message>

            <table class="os-table" v-if="ctx.specimens.length > 0">
              <thead>
                <tr>
                  <th v-t="'containers.specimen.barcode'">Barcode</th>
                  <th v-t="'containers.specimen.label'">Label</th>
                  <th v-t="'containers.specimen.cp'">Collection Protocol</th>
                  <th v-t="'containers.specimen.type'">Type</th>
                  <th v-t="'containers.specimen.available_qty'">Available Quantity</th>
                  <th v-t="'containers.specimen.location'">Location</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="spmn of ctx.specimens" :key="spmn.id">
                  <td>{{spmn.barcode}}</td>
                  <td>{{spmn.label}}</td>
                  <td>{{spmn.cpShortTitle}}</td>
                  <td>{{spmn.type}} ({{spmn.specimenClass}})</td>
                  <td>
                    <os-specimen-measure v-model="spmn.availableQty" entity="specimen"
                      :context="{specimen: spmn}" :read-only="true" />
                  </td>
                  <td>
                    <span>{{spmn.storageLocation.posTwo}}, {{spmn.storageLocation.posOne}}</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </template>
        </os-section>

        <div v-if="ctx.box || ctx.specimens">
          <os-divider />

          <div class="os-buttons">
            <os-button primary :label="$t('common.buttons.save')" @click="save(false)" />

            <os-button primary :label="$t('containers.save_n_scan_another')" @click="save(true)" />

            <os-button secondary :label="$t('common.buttons.clear')" @click="clear" />

            <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />
          </div>
        </div>
      </div>
    </os-page-body>
  </os-page>
</template>

<script>

import containerSvc from '@/administrative/services/Container.js';
import scanner      from '@/administrative/services/BoxScanner.js';

import alertsSvc from '@/common/services/Alerts.js';
import routerSvc from '@/common/services/Router.js';
import wfSvc     from '@/common/services/Workflow.js';

export default {
  data() {
    const bcrumb = [
      {url: routerSvc.getUrl('ContainersList', {}), label: this.$t('containers.list')}
    ];

    return {
      ctx: {
        bcrumb,

        scannedField: 'barcode',

        scannerOpts: {options: [], displayProp: 'name'}
      }
    };
  },

  created() {
    wfSvc.getSysWorkflow('box-scanners').then(
      ({scannedBoxField, scanners}) => {
        const options = this.ctx.scannerOpts.options = scanners.map((s, idx) => ({id: idx, ...s}));
        if (options.length == 1) {
          this.ctx.scanner = options[0];
        }

        this.ctx.scannedField = scannedBoxField || 'barcode';
      }
    );
  },

  watch: {
    'ctx.scanner': function(newVal) {
      if (!newVal) {
        this.clear();
      }
    }
  },

  methods: {
    scan: async function() {
      const ctx = this.ctx;
      if (!ctx.scanner) {
        return;
      }

      const {box, tubes} = await scanner.scan(ctx.scanner);
      if (!box || !box.barcode) {
        alertsSvc.error({code: 'containers.no_box_barcode'});
        return;
      }

      const filterOpts = {};
      filterOpts[this.ctx.scannedField] = box.barcode;
      containerSvc.getContainers(filterOpts).then(
        (containers) => {
          ctx.box = box;
          if (containers.length > 0) {
            box.id = containers[0].id;
            box.type = containers[0].typeName;
            ctx.parentContainer = containers[0].storageLocation;
          } else {
            box.type = ctx.scanner.containerType;
          }
        }
      );

      const spmnBarcodesMap = {};
      const spmnBarcodes = [];
      for (let tube of tubes) {
        if (!tube.barcode) {
          continue;
        }

        spmnBarcodesMap[tube.barcode.toLowerCase()] = tube;
        spmnBarcodes.push(tube.barcode);
      }

      containerSvc.searchSpecimens({barcode: spmnBarcodes}).then(
        (specimens) => {
          ctx.specimens = specimens;
          for (let spmn of specimens) {
            const barcode = spmn.barcode.toLowerCase();
            const tube = spmnBarcodesMap[barcode];
            spmn.storageLocation = {posOne: tube.column, posTwo: tube.row};

            delete spmnBarcodesMap[barcode];
          }

          if (Object.keys(spmnBarcodesMap).length > 0) {
            ctx.error = 'containers.specimens_not_found_ids';
            ctx.errorArgs = {ids: Object.keys(spmnBarcodesMap).join(', ')}
          }
        }
      );
    },

    save: async function(scanAnother) {
      const ctx = this.ctx;
      const box = ctx.box;
      if (!ctx.parentContainer || !ctx.parentContainer.name) {
        alertsSvc.error({code: 'containers.parent_container_not_selected'});
        return;
      }

      const payload = {
        id: box.id,
        type: box.type,
        storageLocation: ctx.parentContainer,
        positions: ctx.specimens.map(
          (spmn) => ({
            posOne: spmn.storageLocation.posOne,
            posTwo: spmn.storageLocation.posTwo,
            position: spmn.storageLocation.position,
            occuypingEntity: 'specimen',
            occupyingEntityId: spmn.id
          })
        )
      };
      payload[this.ctx.scannedField] = box.barcode;

      let result = null;
      if (!box.id) {
        result = await containerSvc.addBoxSpecimens(payload);
      } else {
        result = await containerSvc.updateBoxSpecimens(payload);
      }

      alertsSvc.success({code: 'containers.specimens_added', args: {box: result[ctx.scannedField], count: result.specimens}});
      if (scanAnother) {
        this.clear();
      } else {
        this.cancel();
      }
    },

    clear: function() {
      const ctx = this.ctx;
      ctx.box = ctx.specimens = ctx.error = ctx.errorArgs = null;

      const location = ctx.parentContainer;
      if (location) {
        location.positionX = location.positionY = location.position = null;
      }
    },

    cancel: function() {
      this.$goto('ContainersList');
    }
  }
}
</script>

<style scoped>
.form {
  width: 80%;
  margin: auto;
}

.form .row {
  margin-bottom: 20px;
}

.form .row:after {
  content: ' ';
  display: block;
  clear: both;
}

.form .row .scanner-chooser {
  display: flex;
}

.form .row .scanner-chooser > div {
  flex: 1;
  margin-right: 10px;
}

.form .os-buttons button {
  margin-right: 10px;
  margin-bottom: 20px;
}
</style>
