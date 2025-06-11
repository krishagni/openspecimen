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
            <ul class="os-key-values os-one-col">
              <li class="item">
                <strong class="key key-sm">
                  <span v-t="{path: 'containers.' + ctx.scannedField}">Barcode</span>
                </strong>
                <span class="value value-md">
                  <span>{{ctx.box.barcode}}</span>
                  <span v-if="ctx.noBoxBarcode" style="margin-left: 1rem;">
                    <os-button-link style="color: inherit;" left-icon="edit" @click="editBoxBarcode" />
                  </span>
                </span>
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

        <os-section v-else-if="ctx.box && !ctx.box.barcode">
          <template #title>
            <span v-t="'containers.box_details'">Box Details</span>
          </template>

          <template #content>
            <os-message type="error">
              <span v-t="{path: 'containers.box_' + ctx.scannedField + '_not_detected'}">Box barcode not detected</span>
            </os-message>

            <ul class="os-key-values">
              <li class="item">
                <strong class="key key-sm">
                  <span v-t="{path: 'containers.' + ctx.scannedField}">Barcode</span>
                </strong>
                <span style="display: flex; min-width: 350px;">
                  <os-input-text style="flex: 1; margin-right: 1rem;" v-model="ctx.box.inputBarcode" />

                  <os-button left-icon="search" @click="searchBox" />
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
            <os-message type="info" v-if="!ctx.scanning">
              <span v-t="{ path: 'containers.scan_barcodes_summary', args: ctx }"> </span>
            </os-message>

            <os-message v-if="ctx.error" type="error">
              <span v-t="{path: ctx.error, args: ctx.errorArgs}"></span>
            </os-message>

            <os-message v-if="ctx.readError" type="error">
              <span v-t="{path: ctx.readError, args: ctx.readErrorArgs}"></span>
            </os-message>

            <span v-if="ctx.specimens.length > 0">
              <os-button left-icon="map" :label="$t('containers.view_map')" @click="showBoxMap" />
            </span>

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
                <tr v-for="(spmn, index) of ctx.specimens" :key="index + '_' + (spmn.id || 'nu')" :class="{'error': !spmn.id}">
                  <td>{{$filters.noValue(spmn.barcode)}}</td>
                  <td>{{$filters.noValue(spmn.label)}}</td>
                  <td>{{$filters.noValue(spmn.cpShortTitle)}}</td>
                  <td>
                    <span>{{$filters.noValue(spmn.type)}}</span>
                    <span v-show="spmn.type">&nbsp; ({{spmn.specimenClass}})</span>
                  </td>
                  <td>
                    <os-specimen-measure v-model="spmn.availableQty" entity="specimen"
                      :context="{specimen: spmn}" :read-only="true" />
                  </td>
                  <td>
                    <span v-if="spmn.storageLocation.mode=='LINEAR'">{{spmn.storageLocation.position}}</span>
                    <span v-else>{{spmn.storageLocation.posTwo}}, {{spmn.storageLocation.posOne}}</span>
                  </td>
                </tr>
              </tbody>
            </table>

            <os-message type="info" v-else>
              <span v-t="'containers.no_tube_barcodes_detected'">No specimen barcodes detected in the box</span>
            </os-message>
          </template>
        </os-section>

        <div v-if="ctx.box || ctx.specimens">
          <os-divider />

          <div class="os-buttons">
            <os-button primary :label="$t('common.buttons.save')" @click="save(false)"
              v-show="ctx.box && ctx.box.barcode" />

            <os-button primary :label="$t('containers.save_n_scan_another')" @click="save(true)"
              v-show="ctx.box && ctx.box.barcode" />

            <os-button secondary :label="$t('common.buttons.clear')" @click="clear" />

            <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />
          </div>
        </div>
      </div>

      <os-dialog ref="boxMapView" :closable="true" :style="{width: '100vw'}">
        <template #header>
          <span>{{ctx.box.barcode}}</span>
        </template>
        <template #content>
          <os-message type="info" style="margin-top: 0">
            <span v-t="{ path: 'containers.scan_barcodes_summary', args: ctx }"></span>
          </os-message>
          <Layout class="map" :container="ctx.boxMap.container" :occupants="ctx.boxMap.occupants">
            <template #occupant_specimen="slotProps">
              <a class="occupant" @click="showOccupantDetails($event, slotProps.occupant)">
                <os-icon class="specimen-icon"
                  :class="{
                    'read-error': slotProps.occupant.readError,
                    'not-found': !slotProps.occupant.readError && !slotProps.occupant.occupyingEntityId
                  }"
                  name="vial" :style="slotProps.occupant.colorCode"
                  v-os-tooltip="slotProps.occupant.tooltip" />

                <span class="name" v-os-tooltip="slotProps.occupant.displayName">
                  <span>{{slotProps.occupant.displayName}}</span>
                </span>
              </a>
            </template>
          </Layout>
        </template>
        <template #footer>
          <os-button primary :label="$t('common.buttons.done')" @click="closeBoxMap" />
        </template>
      </os-dialog>

      <os-overlay ref="occupantDetails">
        <div class="os-container-occupant">
          <div>
            <h4 class="title">
              <span v-t="'containers.specimen.singular'">Specimen</span>
            </h4>

            <os-overview :schema="ctx.specimenDict" :object="ctx.occupant"
              :columns="1" v-if="ctx.specimenDict.length > 0" />
          </div>
        </div>
      </os-overlay>

      <os-dialog ref="missingSpecimensDialog" :size="'md'" :closable="'false'">
        <template #header>
          <span v-t="'containers.missing_specimens'">Missing Specimens...</span>
        </template>
        <template #content>
          <os-steps ref="missingSpecimensWizard">
            <os-step :title="$t('containers.specimens')">
              <template #default>
                <div>
                  <div>
                    <p v-t="'containers.missing_specimens_list'">Following specimens are missing in the box: </p>
                  </div>
                  <div>
                    <table class="os-table">
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
                        <tr v-for="spmn of ctx.missingSpmns" :key="spmn.id">
                          <td>
                            <span>{{$filters.noValue(spmn.barcode)}}</span>
                          </td>
                          <td>
                            <span>{{spmn.label}}</span>
                          </td>
                          <td>
                            <span>{{spmn.cpShortTitle}}</span>
                          </td>
                          <td>
                            <span>{{spmn.type}} ({{spmn.specimenClass}})</span>
                          </td>
                          <td>
                            <os-specimen-measure v-model="spmn.availableQty" :read-only="true" entity="specimen"
                              :context="{specimen: spmn}" />
                          </td>
                          <td>
                            <span>{{spmn.storageLocation.positionY}}, {{spmn.storageLocation.positionX}}</span>
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
              </template>
            </os-step>
            <os-step :title="$t('common.reason')">
              <template #default>
                <div>
                  <os-form ref="missingSpecimensForm"
                    :schema="ctx.missingSpmnsReasonForm" :data="ctx.missingSpmnsReason" />
                </div>
              </template>
            </os-step>
          </os-steps>
        </template>
        <template #footer>
          <div>
            <os-button text :label="$t('common.buttons.cancel')" @click="abortSave" />
            <span v-if="ctx.reviewMissingSpmns">
              <os-button secondary :label="$t('containers.download_report')" @click="downloadMissingSpmnsRpt" />
              <os-button primary :label="$t('common.buttons.proceed')" @click="moveToMissingSpmnsReason" />
            </span>
            <span v-else>
              <os-button secondary :label="$t('common.buttons.previous')" @click="backToReviewSpmns" />
              <os-button primary :label="$t('common.buttons.save')" @click="continueWithSave" />
            </span>
          </div>
        </template>
      </os-dialog>
    </os-page-body>
  </os-page>
</template>

<script>

import containerSvc     from '@/administrative/services/Container.js';
import containerTypeSvc from '@/administrative/services/ContainerType.js';
import scanner          from '@/administrative/services/BoxScanner.js';

import cpSvc        from '@/biospecimen/services/CollectionProtocol.js';

import alertsSvc from '@/common/services/Alerts.js';
import boxUtil   from '@/common/services/BoxUtil.js';
import http      from '@/common/services/HttpClient.js';
import numUtil   from '@/common/services/NumberConverterUtil.js';
import routerSvc from '@/common/services/Router.js';
import util      from '@/common/services/Util.js';
import wfSvc     from '@/common/services/Workflow.js';


import Layout from './Layout.vue';

export default {
  components: { Layout },

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
        },

        missingSpmnsReasonForm: {
          rows: [
            {
              fields: [
                {
                  type: 'radio',
                  name: 'reason',
                  labelCode: 'containers.action',
                  options: [
                    { captionCode: 'containers.dispose_specimens', value: 'DISPOSED' },
                    { captionCode: 'containers.move_specimens_as_not_stored', value: 'NOT_STORED' }
                  ],
                  optionsPerRow: 2,
                  validations: {
                    required: {
                      messageCode: "containers.action_required"
                    }
                  }
                }
              ]
            },
            {
              fields: [
                {
                  type: 'textarea',
                  name: 'comments',
                  labelCode: 'common.comments',
                  rows: 5,
                  validations: {
                    required: {
                      messageCode: "common.comments_required"
                    }
                  }
                }
              ]
            }
          ]
        },

        missingSpmnsReason: { }
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
    editBoxBarcode: function() {
      const ctx = this.ctx;
      ctx.box.inputBarcode = ctx.box.barcode;
      ctx.box.barcode = null;
    },

    searchBox: async function() {
      const ctx = this.ctx;
      if (!ctx.box.inputBarcode) {
        alertsSvc.error({code: 'containers.box_' + ctx.scannedField + '_not_specified'});
        return;
      }

      ctx.box = await this._loadBoxDetails({barcode: ctx.box.inputBarcode});
      ctx.specimens = await this._loadTubeDetails(ctx.box, ctx.scannedTubes);
    },


    scan: async function() {
      const ctx = this.ctx;
      if (!ctx.scanner) {
        return;
      }

      this.clear();
      Object.assign(ctx, {scanning: true, noBoxBarcode: false,
        readErrorsCount: 0, noTubesCount: 0, scannedBarcodesCount: 0});

      const {box, tubes} = await scanner.scan(ctx.scanner);
      for (let tube of tubes) {
        if (!tube.barcode) {
          ctx.noTubesCount++;
        } else if (tube.barcode == 'READ_ERROR') {
          ctx.readErrorsCount++;
        } else {
          ctx.scannedBarcodesCount++;
        }
      }

      ctx.scannedBox   = box;
      ctx.scannedTubes = tubes;
      ctx.specimensMap = await this._getSpecimens(tubes);
      if (!box || !box.barcode) {
        ctx.noBoxBarcode = true;
      }

      ctx.box = await this._loadBoxDetails(box);
      ctx.specimens = await this._loadTubeDetails(ctx.box, ctx.scannedTubes);
      ctx.scanning = false;
    },

    showBoxMap: async function() {
      const ctx = this.ctx;

      this.containerTypes = this.containerTypes || {};
      let containerType = this.containerTypes[ctx.box.type];
      if (!containerType && ctx.box.type) {
        containerType = this.containerTypes[ctx.box.type] = await containerTypeSvc.getTypeByName(ctx.box.type);
      }

      const colorCoding = this.colorCoding = this.colorCoding || {};
      const container = ctx.box.container || containerType
      if (!container) {
        alertsSvc.error({code: 'containers.no_layout_detail_to_render'});
        return;
      } else if (!container.noOfRows || !container.noOfColumns) {
        alertsSvc.error({code: 'containers.no_map_for_dimless'});
        return;
      }

      ctx.boxMap = {
        container: container,
        occupants: ctx.specimens.map(
          (specimen) => {
            const spmnClass = specimen.specimenClass, type = specimen.type;
            const occupantProps = {
              ppid: specimen.ppid, barcode: specimen.barcode,
              specimenClass: spmnClass, type: type
            }

            let colorCode = colorCoding[spmnClass + ':' + type];
            if (!colorCode) {
              colorCode = colorCoding[spmnClass + ':' + type] = util.getContainerColorCode(occupantProps) || {};
            }

            let tooltip = null;
            if (specimen.id) {
              tooltip = (specimen.specimenClass + ', ' + specimen.type);
            } else if (specimen.barcode == 'READ_ERROR') {
              tooltip = 'Error detecting the barcode.';
            } else {
              tooltip = 'Specimen ' + specimen.barcode + ' not found.';
            }

            return {
              mode: container.positionLabelingMode,
              posOne: specimen.storageLocation.posOne,
              posTwo: specimen.storageLocation.posTwo,
              posOneOrdinal: numUtil.toNumber(container.columnLabelingScheme, specimen.storageLocation.posOne),
              posTwoOrdinal: numUtil.toNumber(container.rowLabelingScheme, specimen.storageLocation.posTwo),
              occuypingEntity: 'specimen',
              occupyingEntityId: specimen.id,
              occupyingEntityName: specimen.label,
              occupantProps: occupantProps,
              cpShortTitle: specimen.cpShortTitle,
              colorCode: colorCode,
              tooltip: tooltip,
              displayName: specimen.barcode,
              readError: specimen.barcode == 'READ_ERROR'
            };
          }
        )
      };

      this.$refs.boxMapView.open();
    },

    closeBoxMap: function() {
      this.ctx.boxMap = {};
      this.$refs.boxMapView.close();
    },

    showOccupantDetails: async function(event, occupant) {
      const currentTarget = event.currentTarget;
      this.ctx.occupant = {};

      const entityId = occupant.occupyingEntityId;
      if (!entityId) {
        return;
      }

      if (!this.ctx.specimenDict) {
        this.ctx.specimenDict = containerSvc.getSpecimenDict();
      }

      this.specimens = this.specimens || {};
      let specimen = this.specimens[entityId];
      if (!specimen) {
        specimen = this.specimens[entityId] = await containerSvc.getSpecimen(entityId);
      }

      this.ctx.occupant = {specimen: specimen};
      setTimeout(() => this.$refs.occupantDetails.toggle({currentTarget}), 100);
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
        positions: ctx.specimens.filter(
          (spmn) => spmn.id > 0
        ).map(
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
        result = await this.updateBoxSpecimens(payload);
      }

      if (!result) {
        return;
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

    updateBoxSpecimens: function(payload) {
      return containerSvc.getMissingSpecimens(payload).then(
        (missingSpmns) => {
          if (missingSpmns.length == 0) {
            return containerSvc.updateBoxSpecimens(payload);
          }

          this.ctx.missingSpmns = missingSpmns;
          this.payload = payload;
          return new Promise((resolver) => {
            this.$refs.missingSpecimensDialog.open();
            this.ctx.reviewMissingSpmns = true;
            this.ctx.missingSpmnsReason = {};
            this.missingSpmnsResolver = resolver;
          });
        }
      );
    },

    abortSave: function() {
      this.missingSpmnsResolver(null);
      this.$refs.missingSpecimensDialog.close();
    },

    downloadMissingSpmnsRpt: async function() {
      containerSvc.downloadMissingSpecimensReport(this.payload);
    },

    moveToMissingSpmnsReason: function() {
      this.$refs.missingSpecimensWizard.next();
      this.ctx.reviewMissingSpmns = false;
    },

    backToReviewSpmns: function() {
      this.$refs.missingSpecimensWizard.previous();
      this.ctx.reviewMissingSpmns = true;
    },

    continueWithSave: async function() {
      if (!this.$refs.missingSpecimensForm.validate()) {
        return;
      }

      const reason = this.ctx.missingSpmnsReason;
      this.payload.removeSpecimensReason = reason.reason;
      this.payload.removeSpecimensComments = reason.comments;
      const result = await containerSvc.updateBoxSpecimens(this.payload);
      this.missingSpmnsResolver(result);
      this.$refs.missingSpecimensDialog.close();
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
    },

    _getContainerType: async function(type) {
      this.containerTypes = this.containerTypes || {};
      let containerType = this.containerTypes[type];
      if (!containerType) {
        containerType = this.containerTypes[type] = await containerTypeSvc.getTypeByName(type);
      }

      return containerType;
    },

    _getSpecimens: async function(tubes) {
      const barcodes = [];
      for (let tube of tubes) {
        if (tube.barcode && tube.barcode != 'READ_ERROR') {
          barcodes.push(tube.barcode);
        }
      }

      return containerSvc.searchSpecimens({barcode: barcodes}).then(
        (specimens) => {
          return specimens.reduce(
            (map, spmn) => {
              map[spmn.barcode.toLowerCase()] = spmn;
              return map;
            },
            {}
          );
        }
      );
    },

    _assignPositions: function(tubes, containerType, container) {
      const result = [];
      const {
        noOfRows, noOfColumns,
        rowLabelingScheme, columnLabelingScheme,
        positionAssignment, positionLabelingMode
      } = container || containerType || {};

      const assigner = boxUtil.getPositionAssigner(positionAssignment);
      for (let {barcode, row, column} of tubes) {
        const position = assigner.pos({row, col: column, nr: noOfRows, nc: noOfColumns})
        result.push({
          mode:     positionLabelingMode,
          row:      numUtil.fromNumber(rowLabelingScheme, row),
          column:   numUtil.fromNumber(columnLabelingScheme, column),
          position: position,
          barcode:  barcode
        });
      }

      return result;
    },

    _loadBoxDetails: async function(box) {
      const ctx = this.ctx;
      if (!box || !box.barcode) {
        ctx.box = {
          type: ctx.scanner.containerType,
          allowedCollectionProtocols: [],
          allowedTypes: []
        };

        alertsSvc.error({code: 'containers.box_' + this.ctx.scannedField + '_not_detected'});
        return ctx.box;
      }

      ctx.error = null;
      ctx.readError = null;

      let promise = null;
      if (ctx.scannedField == 'name') {
        promise = containerSvc.getContainerByName(box.barcode, false);
      } else {
        promise = containerSvc.getContainerByBarcode(box.barcode, false);
      }

      return promise.then(
        (container) => {
          ctx.box = box;
          if (container) {
            box.id = container.id;
            box.container = container;
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
            box.id = box.container = null;
            box.type = ctx.scanner.containerType;
            box.allowedCollectionProtocols = [];
            box.allowedTypes = [];
          }

          return box;
        }
      );
    },

    _loadTubeDetails: async function(box, tubes) {
      const ctx = this.ctx;
      ctx.error = null;
      ctx.readError = null;

      const containerType = box.type ? await this._getContainerType(box.type) : null;
      const container     = box.container;
      tubes               = this._assignPositions(tubes, containerType, container);

      const readErrors = [];
      for (let tube of tubes) {
        if (tube.barcode == 'READ_ERROR') {
          readErrors.push({mode: tube.mode, position: tube.position, row: tube.row, column: tube.column});
        }
      }

      if (readErrors.length > 0) {
        ctx.readError = 'containers.cannot_read_barcodes';
        ctx.readErrorArgs = {
          locations: readErrors.map(
            ({mode, position, row, column}) => mode == 'LINEAR' ? position :  ('(' + row + ', ' + column + ')')
          ).join(', ')
        }
      }

      const specimens = [];
      if (ctx.scannedBarcodesCount == 0) {
        return specimens;
      }

      const notFound = [];
      for (let tube of tubes) {
        if (!tube.barcode) {
          continue;
        }

        const spmn = ctx.specimensMap[tube.barcode.toLowerCase()];
        if (spmn) {
          spmn.storageLocation = {mode: tube.mode, position: tube.position, posOne: tube.column, posTwo: tube.row};
          specimens.push(spmn);
        } else {
          specimens.push({
            barcode: tube.barcode,
            storageLocation: {mode: tube.mode, position: tube.position, posOne: tube.column, posTwo: tube.row}
          });

          if (tube.barcode != 'READ_ERROR') {
            notFound.push(tube);
          }
        }
      }

      if (notFound.length > 0) {
        ctx.error = 'containers.specimens_not_found_ids';
        ctx.errorArgs = {
          ids: notFound.map(({barcode, mode, position, row, column}) =>
            barcode + ' (' + (mode == 'LINEAR' ? position : (row + ', ' + column)) + ')'
          ).join(', ')
        }
      }

      return specimens;
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
  margin-bottom: 0.5rem;
}

.os-key-values .item.section .key {
  font-weight: bold;
}

.os-container-occupant h4.title {
  margin-top: 0;
  border-bottom: 1px solid #ddd;
  padding-bottom: 4px;
}
</style>
