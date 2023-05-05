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
                  <os-storage-position v-model="ctx.box.storageLocation"
                    :list-source="{queryParams: {static: {entityType: 'storage_container'}}}" />
                </span>
              </li>
              <li class="item">
                <strong class="key key-sm">
                  <span v-t="'containers.collection_protocols'">Collection Protocols</span>
                </strong>
                <span style="display: inline-block; min-width: 350px;">
                  <os-multi-select-dropdown v-model="ctx.box.allowedCollectionProtocols"
                    :list-source="ctx.allowedCpOpts" />
                </span>
              </li>
              <li class="item">
                <strong class="key key-sm">
                  <span v-t="'containers.specimen_types'">Specimen Types</span>
                </strong>
                <span style="display: inline-block; min-width: 350px;">
                  <os-multi-select-group-dropdown v-model="ctx.box.allowedTypes"
                    :list-source="ctx.allowedTypeOpts" />
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

import cpSvc        from '@/biospecimen/services/CollectionProtocol.js';

import alertsSvc from '@/common/services/Alerts.js';
import http      from '@/common/services/HttpClient.js';
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

        scannerOpts: {options: [], displayProp: 'name'},

        allowedCpOpts: {
          displayProp: 'shortTitle',
          selectProp: 'shortTitle',
          loadFn: this._getAllowedCps
        },

        allowedTypeOpts: {
          displayProp: 'type',
          groupNameProp: 'specimenClass',
          groupItemsProp: 'types',
          loadFn: this._getAllowedTypes
        }
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

      let promise = null;
      if (this.ctx.scannedField == 'name') {
        promise = containerSvc.getContainerByName(box.barcode, false);
      } else if (this.ctx.scannedField == 'barcode') {
        promise = containerSvc.getContainerByBarcode(box.barcode, false);
      }

      promise.then(
        (container) => {
          ctx.box = box;
          if (container) {
            box.id = container.id;
            box.type = container.typeName;
            box.siteName = container.siteName;
            box.allowedCollectionProtocols = container.allowedCollectionProtocols;
            box.storageLocation = container.storageLocation;

            const allowedTypes = box.allowedTypes = [];
            for (let specimenClass of (container.allowedSpecimenClasses || [])) {
              allowedTypes.push({specimenClass, type: 'All ' + specimenClass, all: true});
            }

            for (let type of (container.allowedSpecimenTypes || [])) {
              allowedTypes.push({type});
            }

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
      if (!box.storageLocation || !box.storageLocation.name) {
        alertsSvc.error({code: 'containers.parent_container_not_selected'});
        return;
      }

      const spmnClasses = [];
      const spmnTypes   = [];
      for (let type of box.allowedTypes) {
        if (type.all) {
          spmnClasses.push(type.specimenClass);
        } else {
          spmnTypes.push(type.type);
        }
      }

      const payload = {
        id: box.id,
        type: box.type,
        storageLocation: box.storageLocation,
        allowedCollectionProtocols: box.allowedCollectionProtocols,
        allowedSpecimenClasses: spmnClasses,
        allowedSpecimenTypes: spmnTypes,
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
    },

    cancel: function() {
      this.$goto('ContainersList');
    },

    _getAllowedCps: async function({query, maxResults}) {
      const container = this.ctx.box;
      const parentContainer = await this._getParentContainer(container);
      const allowedCps      = (parentContainer && parentContainer.calcAllowedCollectionProtocols) || [];

      if (allowedCps.length > 0) {
        return this._filterProtocols(allowedCps, query);
      } else if (parentContainer || container.siteName) {
        const siteName = (parentContainer && parentContainer.siteName) || container.siteName;
        let cacheKey = siteName.toLowerCase();
        if (query) {
          cacheKey += ':' + query.toLowerCase();
        }

        const cpsMap = this.cpsMap = this.cpsMap || {};
        const cachedCps = cpsMap[cacheKey];
        if (cachedCps) {
          return cachedCps;
        }

        return cpSvc.getCps({repositoryName: siteName, query, maxResults}).then(
          (cps) => {
            cpsMap[cacheKey] = cps;
            return cps;
          }
        );
      } else {
        return [];
      }
    },


    _getParentContainer: async function(container) {
      let parentContainer = null;
      if (container.storageLocation && container.storageLocation.name) {
        const containersMap = this.containersMap = this.containersMap || {};
        parentContainer = containersMap[container.storageLocation.name];
        if (!parentContainer) {
          parentContainer = await containerSvc.getContainerByName(container.storageLocation.name);
          containersMap[parentContainer.name] = parentContainer;
        }
      }

      return parentContainer;
    },

    _filterProtocols: async function(protocols, query) {
      let result = protocols;
      if (query) {
        result = result.filter(protocol => protocol.toLowerCase().indexOf(query.toLowerCase()) != -1);
      }

      return result.map(shortTitle => ({ shortTitle }));
    },

    _getAllowedTypes: async function() {
      const container = this.ctx.box;
      const parentTypes = await this._getParentTypes(container);
      if (parentTypes.length > 0) {
        return this._filterVisibleOptions(container, parentTypes);
      }

      const allTypes = await this._getAllSpecimenTypes();
      const groups = {};
      for (let type of allTypes) {
        if (!type.parentValue) {
          continue;
        }

        if (groups[type.parentValue]) {
          groups[type.parentValue].types.push({type: type.value});
        } else {
          groups[type.parentValue] = {
            specimenClass: type.parentValue,
            types: [
              {type: 'All ' + type.parentValue, all: true, specimenClass: type.parentValue},
              {type: type.value}
            ]
          };
        }
      }

      const result = Object.keys(groups).sort().map(group => groups[group]);
      return this._filterVisibleOptions(container, result);
    },

    _getParentTypes: async function(container) {
      const parentContainer = await this._getParentContainer(container);
      const allowedClasses  = (parentContainer && parentContainer.calcAllowedSpecimenClasses) || [];
      const allowedTypes    = (parentContainer && parentContainer.calcAllowedSpecimenTypes) || [];
      if (allowedClasses.length == 0 && allowedTypes.length == 0) {
        return [];
      }

      const groups   = {};
      for (let specimenClass of allowedClasses) {
        groups[specimenClass] = {specimenClass, types: [{type: 'All ' + specimenClass, all: true, specimenClass}]};
      }

      const allTypes = await this._getAllSpecimenTypes();
      for (let type of allTypes) {
        if (allowedClasses.indexOf(type.parentValue) != -1) {
          groups[type.parentValue].types.push({type: type.value});
        } else if (allowedTypes.indexOf(type.value) != -1) {
          let specimenClass = type.parentValue;
          let groupTypes = groups[specimenClass] = groups[specimenClass] || {specimenClass, types: []};
          groupTypes.types.push({type: type.value});
        }
      }

      return Object.keys(groups).sort().map(group => groups[group]);
    },

    _getAllSpecimenTypes: async function() {
      if (!this.allSpecimenTypes) {
        const pvOpts = {attribute: 'specimen_type', includeParentValue: true};
        this.allSpecimenTypes = await http.get('permissible-values', pvOpts);
      }

      return this.allSpecimenTypes;
    },

    _filterVisibleOptions: function(container, types) {
      const allowedClasses = container.allowedSpecimenClasses || [];
      const allowedTypes   = container.allowedSpecimenTypes || [];

      for (let type of types) {
        if (allowedClasses.indexOf(type.specimenClass) != -1) {
          type.types.splice(1, type.types.length - 1);
        } else if (type.types[0].all && type.types.some(at => allowedTypes.indexOf(at.type) != -1)) {
          type.types.splice(0, 1);
        }
      }

      return types;
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

.os-key-values .item {
  margin-bottom: 20px;
}
</style>
