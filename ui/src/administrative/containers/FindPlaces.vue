<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3 v-t="'containers.find_places.title'">Find Places</h3>
      </span>
    </os-page-head>

    <os-page-body>
      <os-steps ref="wizard">
        <os-step :title="$t('containers.find_places.criteria')">
          <os-form ref="findPlacesForm" :schema="findPlacesSchema.layout" :data="ctx">
            <div>
              <os-button primary :label="$t('common.buttons.find')" @click="findPlaces" />

              <os-button text :label="$t('common.buttons.cancel')"  @click="cancel" />
            </div>
          </os-form>
        </os-step>

        <os-step :title="$t('containers.find_places.select_container')">
          <div>
            <os-list-view
              :data="ctx.containers"
              :schema="containersListSchema"
              :loading="ctx.loading"
              :show-row-actions="true"
              ref="listView">
              <template #rowActions="slotProps">
                <os-button size="small" left-icon="check" :label="$t('common.buttons.select')"
                  @click="transferTo(slotProps.rowObject)" />
              </template>
            </os-list-view>
          </div>

          <os-divider />

          <div class="os-form-footer">
            <os-button primary :label="$t('containers.find_places.search_again')" @click="searchAgain" />

            <os-button text :label="$t('common.buttons.cancel')"  @click="cancel" />
          </div>
        </os-step>

        <os-step :title="$t('containers.find_places.transfer')">
          <div>
            <os-message type="info">
              <span v-t="{path: 'containers.find_places.transfer_to', args: ctx.transferTo}"></span>
            </os-message>

            <os-add-items ref="addItems" :placeholder="$t('containers.scan_names_or_barcodes')"
              @on-add="addContainers($event)" style="margin-bottom: 0.75rem;" />

            <os-boolean-checkbox name="useBarcode" v-model="ctx.useBarcode">
              <label v-t="'containers.use_barcode'">Use Barcode</label>
            </os-boolean-checkbox>

            <div>
              <os-table-form ref="transferForm" :schema="transferSchema"
                :data="ctx" :items="ctx.transferItems"
                :remove-items="true" @remove-item="removeContainer($event.item)"
                v-if="ctx.transferItems.length > 0">
              </os-table-form>

              <os-message type="error" v-else>
                <span v-t="'containers.find_places.add_atleast_one'">Add at least one container to transfer...</span>
              </os-message>

              <os-divider />

              <div class="os-form-footer">
                <os-button primary :label="$t('common.buttons.submit')" @click="submit"
                  v-if="ctx.transferItems.length > 0" />

                <os-button secondary :label="$t('containers.find_places.select_another')" @click="selectContainer" />

                <os-button secondary :label="$t('containers.find_places.search_again')" @click="searchAgain" />

                <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />
              </div>
            </div>
          </div>
        </os-step>
      </os-steps>

      <os-dialog ref="addContainers">
        <template #header>
          <span v-t="'containers.find_places.not_found'"   v-if="ctx.showNotFound">Containers not found</span>
          <span v-t="'containers.find_places.new_details'" v-else>New Container Details</span>
        </template>

        <template #content>
          <div class="message" v-if="ctx.showNotFound">
            <div style="margin-bottom: 1.25rem">
              <span v-t="{path: 'containers.find_places.following_not_found', args: ctx}"></span>
            </div>

            <div v-t="'containers.find_places.create_them'">Do you want to create them?</div>
          </div>

          <div class="review" v-else-if="ctx.showNewContainerDetails">
            <os-message type="info">
              <span v-t="{path: 'containers.find_places.create_new', args: ctx}"></span>
            </os-message>

            <os-form ref="boxDetailsForm" :schema="boxDetailsSchema.layout" :data="ctx" />
          </div>
        </template>

        <template #footer>
          <os-button text :label="$t('common.buttons.no')" @click="cancelAddContainers" />

          <os-button primary :label="$t('common.buttons.yes')"
            @click="reviewContainerDetails" v-if="ctx.showNotFound" />

          <os-button primary :label="$t('common.buttons.submit')"
            @click="submitAddContainers" v-if="ctx.showNewContainerDetails" />
        </template>
      </os-dialog>
    </os-page-body>
  </os-page>
</template>

<script>
import boxDetailsSchema     from '@/administrative/schemas/containers/box-details.js';
import containersListSchema from '@/administrative/schemas/containers/free-containers-list.js';
import findPlacesSchema from '@/administrative/schemas/containers/find-places.js';
import transferSchema from '@/administrative/schemas/containers/bulk-transfer.js';

import alertsSvc    from '@/common/services/Alerts.js';
import containerSvc from '@/administrative/services/Container.js';
import cpSvc         from '@/biospecimen/services/CollectionProtocol.js';
import http          from '@/common/services/HttpClient.js';
import routerSvc    from '@/common/services/Router.js';

export default {
  props: [],

  data() {
    const bcrumb = [
      {url: routerSvc.getUrl('ContainersList', {}), label: this.$t('containers.list')}
    ];

    return {
      ctx: {
        bcrumb: bcrumb,

        findPlaces: true, /* Used by transfer schema */

        criteria: {},

        containers: [],

        loading: false,

        transferTo: {},

        transferItems: []
      },

      boxDetailsSchema,

      containersListSchema,

      findPlacesSchema,

      transferSchema
    }
  },

  created: function() {
    this.ctx.getAllowedCps   = this._getAllowedCps; 
    this.ctx.getAllowedTypes = this._getAllowedTypes;
  },

  methods: {
    searchAgain: function() {
      this.$refs.wizard.navTo(0);
    },

    findPlaces: function() {
      if (!this.$refs.findPlacesForm.validate()) {
        return;
      }

      const crit = this.ctx.criteria;
      const payload = {};
      if (crit.freezer) {
        payload.freezerId = crit.freezer.id;
      }

      if (crit.cp) {
        payload.cpId = crit.cp.id;
      }

      if (crit.type) {
        payload.typeId = crit.type.id;
      }

      payload.requiredPlaces = crit.requiredPlaces;
      if (+payload.requiredPlaces > 1) {
        payload.allInOneContainer = crit.allInOneContainer;
      }

      this.ctx.containers = [];
      containerSvc.findEmptyPlaces(payload).then(
        (containers) => {
          this.ctx.containers = containers.map(container => ({container}));
          this.$refs.wizard.next();
        }
      );
    },

    transferTo: function({container}) {
      this.ctx.transferTo = container;
      if (container.displayLabel) {
        container.displayLabel = container.displayLabel + ' (' + container.name + ')';
      } else {
        container.displayLabel = container.name;
      }

      this.ctx.transferItems = this.ctx.transferItems || [];
      this.$refs.wizard.next();
    },

    selectContainer: function() {
      this.$refs.wizard.navTo(1);
    },

    addContainers: function({itemLabels}) {
      const ctx = this.ctx;
      if (itemLabels.length == 0) {
        return;
      }

      const filterOpts = {};
      if (ctx.useBarcode) {
        filterOpts.barcode = itemLabels;
      } else {
        filterOpts.naam = itemLabels;
      }

      ctx.notFound = [];
      containerSvc.getContainers(filterOpts).then(
        async (containers) => {
          const itemLabelsLc = itemLabels.map(i => i.toLowerCase());

          let idxFn, cmpFn;
          if (ctx.useBarcode) {
            idxFn = (c) => itemLabelsLc.indexOf(c.barcode.toLowerCase());
            cmpFn = (c, barcode) => barcode.toLowerCase() == c.barcode.toLowerCase();
          } else {
            idxFn = (c) => itemLabelsLc.indexOf(c.name.toLowerCase());
            cmpFn = (c, label) => label.toLowerCase() == c.name.toLowerCase();
          }

          containers.sort((c1, c2) => idxFn(c1) - idxFn(c2));

          const notFound = [];
          for (let label of itemLabels) {
            let found = false;
            for (let container of containers) {
              if (cmpFn(container, label)) {
                found = true;
                break;
              }
            }

            if (!found) {
              const c = {};
              if (ctx.criteria.type) {
                c.typeId = ctx.criteria.type.id;
                c.typeName = ctx.criteria.type.name;
              }

              if (ctx.useBarcode) {
                c.barcode = label;
              } else {
                c.name = label;
              }

              c.inputLabel = label;
              notFound.push(c);
            }
          }

          if (notFound.length > 0) {
            ctx.notFound = notFound;
            ctx.notFoundLabels = notFound.map(c => c.inputLabel).join(', ');
            const promise = new Promise((resolve) => {
              this.ctx.addContainersResolve = resolve;
              this.ctx.showNotFound = true;
              this.ctx.showNewContainerDetails = false;
              this.$refs.addContainers.open();
            });

            const newBox = await promise;
            if (newBox == 'cancel') {
              return;
            }

            for (let c of notFound) {
              c.typeId = undefined;

              newBox.allowedTypes = newBox.allowedTypes || [];
              newBox.allowedSpecimenClasses = newBox.allowedTypes.filter(t => t.all).map(t => t.specimenClass);
              newBox.allowedSpecimenTypes = newBox.allowedTypes.filter(t => !t.all).map(t => t.type);
              Object.assign(c,newBox);
              containers.splice(idxFn(c), 0, c);
            }
          }

          for (let toAdd of containers) {
            if (ctx.transferItems.some(
                 ({container}) =>
                   (toAdd.name && container.name == toAdd.name) ||
                   (toAdd.barcode && container.barcode == toAdd.barcode))) {
              continue;
            }

            toAdd.storageLocation = {name: ctx.transferTo.name};
            toAdd.transferredBy = this.$ui.currentUser;
            toAdd.transferDate = Date.now();
            ctx.transferItems.push({container: toAdd});
          }

          this.$refs.addItems.clearInput();
        }
      );
    },

    reviewContainerDetails: function() {
      const ctx = this.ctx;
      ctx.showNewContainerDetails = true;
      ctx.showNotFound = false;

      const criteria = ctx.criteria;
      ctx.newBox = {
        typeName: criteria.type && criteria.type.name,
        allowedCollectionProtocols: criteria.cp && [criteria.cp.shortTitle]
      }
    },

    cancelAddContainers: function() {
      this.ctx.addContainersResolve('cancel');
      this.$refs.addContainers.close();
    },

    submitAddContainers: function() {
      if (!this.$refs.boxDetailsForm.validate()) {
        return;
      }

      
      this.ctx.addContainersResolve(this.ctx.newBox);
      this.$refs.addContainers.close();
    },

    removeContainer: function(item) {
      const idx = this.ctx.transferItems.indexOf(item);
      this.ctx.transferItems.splice(idx, 1);
    },
    
    submit: function() {
      if (!this.$refs.transferForm.validate()) {
        return;
      }

      const containers = this.ctx.transferItems.map(
        ({container}) => {
          return {
            id: container.id,
            name: container.name,
            barcode: container.barcode,
            typeName: container.typeName || undefined,
            storageLocation: container.storageLocation,
            allowedCollectionProtocols: (!container.id && container.allowedCollectionProtocols) || undefined,
            allowedSpecimenClasses: (!container.id && container.allowedSpecimenClasses) || undefined,
            allowedSpecimenTypes: (!container.id && container.allowedSpecimenTypes) || undefined,
            transferredBy: container.transferredBy,
            transferDate: container.transferDate,
            transferComments: container.transferComments,
            createIfAbsent: true
          }
        }
      );

      containerSvc.bulkUpdate(containers).then(
        (result) => {
          let success = 'containers.transferred';
          let pending = 'containers.transfer_pending';
          if (result) {
            alertsSvc.success({code: success, args: {count: result.length}});
          } else {
            alertsSvc.info({code: pending});
          }

          if (result[0] && result[0].storageLocation && result[0].storageLocation.id > 0) {
            routerSvc.goto('ContainerDetail.Locations', {containerId: result[0].storageLocation.id});
          } else {
            routerSvc.goto('ContainersList');
          }
        }
      );
    },

    cancel: function() {
      routerSvc.goto('ContainersList');
    },

    _getAllowedTypes: async function() {
      const container = await this._getContainer(this.ctx.transferTo.name);
      const allowedSpecimenTypes = await this._getContainerAllowedSpecimenTypes(container);
      if (allowedSpecimenTypes.length > 0) {
        return this._filterVisibleOptions(container, allowedSpecimenTypes);
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

    _getContainerAllowedSpecimenTypes: async function(container) {
      const allowedClasses  = (container && container.calcAllowedSpecimenClasses) || [];
      const allowedTypes    = (container && container.calcAllowedSpecimenTypes) || [];
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
        this.allSpecimenTypes = http.get('permissible-values', pvOpts);
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
    },

    _getAllowedCps: async function({query, maxResults}) {
      const parentContainer = await this._getContainer(this.ctx.transferTo.name);
      const allowedCps      = (parentContainer && parentContainer.calcAllowedCollectionProtocols) || [];
      if (allowedCps.length > 0) {
        return this._filterProtocols(allowedCps, query);
      } else {
        const siteName = parentContainer.siteName;
        let cacheKey = siteName.toLowerCase();
        if (query) {
          cacheKey += ':' + query.toLowerCase();
        }
    
        const cpsMap = this.cpsMap = this.cpsMap || {};
        let cpsQ = cpsMap[cacheKey];
        if (!cpsQ) {
          cpsQ = cpsMap[cacheKey] = cpSvc.getCps({repositoryName: siteName, query, maxResults});
        }

        return await cpsQ;
      }
    },

    _getContainer: async function(name) {
      let container = null;
      if (name) {
        const containersMap = this.containersMap = this.containersMap || {};
        let containerQ = containersMap[name];
        if (!containerQ) {
          containerQ = containersMap[name] = containerSvc.getContainerByName(name);
        }

        container = await containerQ;
      } 
          
      return container;
    },
        
    _filterProtocols: async function(protocols, query) {
      let result = protocols;
      if (query) { 
        result = result.filter(protocol => protocol.toLowerCase().indexOf(query.toLowerCase()) != -1);
      } 
      
      return result.map(shortTitle => ({ shortTitle }));
    } 

  }
}
</script>
