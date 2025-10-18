<template>
  <os-section v-if="box && box.barcode">
    <template #title>
      <span v-t="'containers.box_details'">Box Details</span>
    </template>

    <template #content>
      <ul class="os-key-values os-one-col">
        <li class="item">
          <strong class="key key-sm">
            <span v-t="{path: 'containers.' + scannedBoxId}">Barcode</span>
          </strong>
          <span class="value value-md">
            <span>{{box[scannedBoxId]}}</span>
            <span v-if="!boxId" style="margin-left: 1rem;">
              <os-button-link style="color: inherit;" left-icon="edit" @click="editBoxBarcode" />
            </span>
          </span>
        </li>
        <li class="item">
          <strong class="key key-sm">
            <span v-t="'containers.type'">Type</span>
          </strong>
          <span class="value value-md">{{$filters.noValue(box.typeName)}}</span>
        </li>
        <li class="item">
          <strong class="key key-sm">
            <span v-t="'containers.parent_container'">Parent Container</span>
          </strong>
          <span style="display: inline-block; min-width: 350px;">
            <os-storage-position v-model="box.storageLocation"
              :list-source="{queryParams: {static: {entityType: 'storage_container'}}}" />
          </span>
        </li>
        <li class="item section">
          <strong class="key key-sm">
            <span v-t="'containers.box_restrictions'">Box Restrictions</span>
          </strong>
        </li>
        <li class="item">
          <strong class="key key-sm">
            <span v-t="'containers.collection_protocols'">Collection Protocols</span>
          </strong>
          <span style="display: inline-block; min-width: 350px;">
            <os-multi-select-dropdown v-model="box.allowedCollectionProtocols" :list-source="allowedCpOpts" />
          </span>
        </li>
        <li class="item">
          <strong class="key key-sm">
            <span v-t="'containers.specimen_types'">Specimen Types</span>
          </strong>
          <span style="display: inline-block; min-width: 350px;">
            <os-multi-select-group-dropdown v-model="box.allowedTypes" :list-source="allowedTypeOpts" />
          </span>
        </li>
      </ul>
    </template>
  </os-section>

  <os-section v-else-if="box && !box.barcode">
    <template #title>
      <span v-t="'containers.box_details'">Box Details</span>
    </template>

    <template #content>
      <os-message type="error">
        <span v-t="{path: 'containers.box_' + scannedBoxId + '_not_detected'}">Box barcode not detected</span>
      </os-message>

      <ul class="os-key-values">
        <li class="item">
          <strong class="key key-sm">
            <span v-t="{path: 'containers.' + scannedBoxId}">Barcode</span>
          </strong>
          <span style="display: flex; min-width: 350px;">
            <os-input-text style="flex: 1; margin-right: 1rem;" v-model="ctx.inputBarcode" />

            <os-button left-icon="search" @click="searchBox" />
          </span>
        </li>
      </ul>
    </template>
  </os-section>
</template>

<script>

import alertsSvc    from '@/common/services/Alerts.js';
import containerSvc from '@/administrative/services/Container.js';
import cpSvc        from '@/biospecimen/services/CollectionProtocol.js';
import http         from '@/common/services/HttpClient.js';

export default {
  props: ['model-value', 'scanned-box-id', 'box-id'],

  data() {
    return {
      ctx: {
      }
    }
  },

  created() {
    this.ctx.boxIdDetected = this.box && !!this.box.barcode;
  },

  computed: {
    box: {
      get() {
        return this.modelValue;
      },

      set(value) {
        this.$emit('update:modelValue', value);
        this.$emit('change', value);
      }
    },

    allowedCpOpts: function() {
      return {
        displayProp: 'shortTitle',
        selectProp: 'shortTitle',
        loadFn: this._getAllowedCps
      };
    },

    allowedTypeOpts: function() {
      return {
        displayProp: 'type',
        groupNameProp: 'specimenClass',
        groupItemsProp: 'types',
        loadFn: this._getAllowedTypes
      }
    }
  },

  watch: {
    box: function() {
      this.ctx.boxIdDetected = this.box && !!this.box.barcode;
    },

    scannedBoxId: function(newVal) {
      alert(newVal);
    }
  },

  methods: {
    searchBox: async function() {
      if (!this.ctx.inputBarcode) {
        alertsSvc.error({code: 'containers.box_' + this.scannedBoxId + '_not_specified'});
        return;
      }

      this.$emit('search-box', {barcode: this.ctx.inputBarcode});
    },

    editBoxBarcode: function() {
      this.$emit('search-box', {barcode: null});
    },

    _getAllowedCps: async function({query, maxResults}) {
      const container = this.box;
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
      const container = this.box;
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
.os-key-values .item {
  margin-bottom: 0.5rem;
}

.os-key-values .item.section .key {
  font-weight: bold;
}
</style>

