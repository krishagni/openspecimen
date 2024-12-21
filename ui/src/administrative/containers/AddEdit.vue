<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3 v-if="!dataCtx.container.id">
          <span v-t="'containers.create'">Create Container</span>
        </h3>
        <h3 v-else>
          <span v-t="{path: 'common.update', args: dataCtx.container}"></span>
        </h3>
      </span>
    </os-page-head>

    <os-page-body>
      <div v-if="ctx.loading">
        <os-message type="info">
          <span v-t="'common.loading_form'">Loading the form. Please wait for a moment...</span>
        </os-message>
      </div>
      <div v-else-if="ctx.view == 'basic_detail'">
        <os-form ref="containerForm" :schema="ctx.addEditFs" :data="dataCtx" @input="handleInput($event)">
          <div>
            <os-button primary :label="$t('common.buttons.next')"
              v-if="dataCtx.createType == 'multiple'" @click="saveOrUpdate" />

            <os-button primary :label="$t(!dataCtx.container.id ? 'common.buttons.create' : 'common.buttons.update')"
              v-else @click="saveOrUpdate" />

            <os-button text :label="$t('common.buttons.cancel')"  @click="cancel" />
          </div>
        </os-form>
      </div>
      <div v-else-if="ctx.view == 'review_multiple_containers'">
        <os-table-form ref="multiContainersForm"
          :data="dataCtx" :items="dataCtx.containers" :schema="ctx.locationsSchema"
          :remove-items="true" @remove-item="removeContainer($event)">

          <os-button primary :label="$t('common.buttons.create')" @click="saveMultipleContainers" />

          <os-button text :label="$t('common.buttons.back')" @click="ctx.view = 'basic_detail'" />
        </os-table-form>
      </div>
    </os-page-body>
  </os-page>
</template>

<script>
import { reactive, inject } from 'vue';

import alertsSvc     from '@/common/services/Alerts.js';
import formUtil      from '@/common/services/FormUtil.js';
import http          from '@/common/services/HttpClient.js';
import i18n          from '@/common/services/I18n.js';
import routerSvc     from '@/common/services/Router.js';
import util          from '@/common/services/Util.js';
import containerSvc  from '@/administrative/services/Container.js';
import typeSvc       from '@/administrative/services/ContainerType.js';
import dpSvc         from '@/administrative/services/DistributionProtocol.js';
import cpSvc         from '@/biospecimen/services/CollectionProtocol.js';

export default {
  props: ['containerId', 'parentContainerName', 'row', 'column', 'position', 'mode', 'typeId'],

  inject: ['ui'],

  setup() {
    const ui = inject('ui');

    let ctx = reactive({
      bcrumb: [
        {url: routerSvc.getUrl('ContainersList', {containerId: -1}), label: i18n.msg('containers.list')}
      ],

      addEditFs: {rows: []},

      loading: true,

      view: 'basic_detail',
    });

    let dataCtx = reactive({
      container: {},

      currentUser: ui.currentUser,

      showCreateType: true,

      createType: 'single',

      containers: [],

      parentContainer: null,

      displayNames: '',

      uniqueNames: '',

      barcodes: '',

      objName: 'container',

      objCustomFields: 'container.extensionDetail.attrsMap'
    });

    return { ctx, dataCtx };
  },

  created: async function() {
    containerSvc.getAddEditFormSchema().then(
      ({schema, defaultValues}) => {
        this.ctx.addEditFs = schema;
        this.ctx.defValues = defaultValues;
        this.loadContainer(defaultValues);
      }
    );

    this.dataCtx.getAllowedCps   = this._getAllowedCps;
    this.dataCtx.getAllowedTypes = this._getAllowedTypes;
    this.dataCtx.getAllowedDps   = this._getAllowedDps;
  },

  watch: {
    containerId: function (newVal, oldVal) {
      if (newVal == oldVal) {
        return;
      }

      this.loadContainer(this.ctx.defValues);
    },
  },

  methods: {
    loadContainer: async function(defValues) {
      const ctx        = this.ctx;
      const dataCtx    = this.dataCtx;

      ctx.loading = true;
      dataCtx.container = { };
      if (this.containerId && +this.containerId > 0) {
        const container = dataCtx.container = await containerSvc.getContainer(this.containerId);
        formUtil.createCustomFieldsMap(container);
        /*if (container.blockedLocation) {
          delete container.storageLocation;
        }*/

        const allowedTypes = container.allowedTypes = [];
        for (let specimenClass of (container.allowedSpecimenClasses || [])) {
          allowedTypes.push({specimenClass, type: 'All ' + specimenClass, all: true});
        }

        for (let type of (container.allowedSpecimenTypes || [])) {
          allowedTypes.push({type});
        }

        dataCtx.showCreateType = false;
        dataCtx.dimensionLess  = !(container.noOfRows > 0 && container.noOfColumns > 0);
      } else {
        //
        // new container
        //
        dataCtx.dimensionLess = false;
        if (defValues && Object.keys(defValues).length > 0) {
          dataCtx.container.extensionDetail = { attrsMap: defValues };
        }

        if (this.parentContainerName) {
          const parentContainer = await this._getParentContainer({storageLocation: {name: this.parentContainerName}});
          dataCtx.parentContainer    = parentContainer;
          dataCtx.container.usedFor  = parentContainer.usedFor;
          dataCtx.container.siteName = parentContainer.siteName;
          dataCtx.container.storageLocation = {
            id: parentContainer.id, name: parentContainer.name,
            positionY: this.row, positionX: this.column, position: this.position
          }
        } else {
          dataCtx.container.usedFor = 'STORAGE';
        }

        dataCtx.createType = this.mode || 'single';
        if (this.typeId && +this.typeId > 0) {
          const type = await typeSvc.getType(+this.typeId);
          this.setTypeProps(type);
        }
      }

      ctx.bcrumb  = this._getBcrumbs();
      ctx.loading = false;
    },

    handleInput: async function({field, oldValue, value, data}) {
      if (oldValue == value || !this.$refs.containerForm) {
        return;
      }

      const fieldRef  = this.$refs.containerForm.$refs['osField-' + field.name][0];
      const container = this.dataCtx.container;
      if (field.name == 'container.allowedTypes') {
        const spmnClasses = container.allowedSpecimenClasses = [];
        const spmnTypes   = container.allowedSpecimenTypes = [];
        for (let type of value) {
          if (type.all) {
            spmnClasses.push(type.specimenClass);
          } else {
            spmnTypes.push(type.type);
          }
        }

        fieldRef.reloadOptions();
      } else if (field.name == 'container.typeName') {
        if (!container.id) {
          setTimeout(() => this.setTypeProps(fieldRef.getSelectedOption()), 100);
        } else {
          container.typeId = null;
        }
      } else if (field.name == 'createType') {
        if (value != 'single') {
          container.name = container.barcode = container.displayName = null;
        } else {
          this.dataCtx.numOfContainers = null;
        }
      } else if (field.name == 'container.usedFor') {
        if (value == 'DISTRIBUTION') {
          container.allowedCollectionProtocols = container.allowedSpecimenClasses = container.allowedSpecimenTypes = [];
        } else {
          container.allowedDistributionProtocols = [];
        }

        container.storageLocation = {};
      } else if (field.name == 'container.siteName') {
        container.storageLocation = {};
      }

      Object.assign(this.dataCtx, data);
    },

    setTypeProps: function(type) {
      const ctx = this.dataCtx;
      const container = ctx.container;
      if (!type) {
        container.typeId = container.typeName = undefined;
        ctx.typeNameFormat = undefined;
        return;
      }

      ctx.dimensionLess = false;
      ctx.typeNameFormat = type.nameFormat;
      container.typeId = type.id;
      container.typeName = type.name;
      container.noOfRows = type.noOfRows;
      container.noOfColumns = type.noOfColumns;
      container.positionLabelingMode = type.positionLabelingMode;
      container.rowLabelingScheme = type.rowLabelingScheme;
      container.columnLabelingScheme = type.columnLabelingScheme;
      container.positionAssignment = type.positionAssignment;
      container.temperature = type.temperature;
      container.storeSpecimensEnabled = type.storeSpecimenEnabled;
      ctx.dimensionLess  = !(type.noOfRows > 0 && type.noOfColumns > 0);
      if (type.nameFormat && ctx.createType == 'single') {
        container.name = undefined;
      } else if (type.nameFormat && (ctx.createType == 'multiple' || ctx.createType == 'hierarchy')) {
        ctx.uniqueNames = undefined;
      }
    },

    saveOrUpdate: async function() {
      if (!this.$refs.containerForm.validate()) {
        return;
      }

      const dataCtx   = this.dataCtx;
      const container = dataCtx.container;
      if (dataCtx.createType == 'single') {
        const isUpdateOp     = container.id > 0;
        const savedContainer = await containerSvc.saveOrUpdate(container);
        alertsSvc.success({code: container.id ? 'containers.updated' : 'containers.created', args: savedContainer});
        if (dataCtx.parentContainer) {
          routerSvc.goto('ContainerDetail.Locations', {containerId: dataCtx.parentContainer.id});
        } else {
          const view = isUpdateOp ? 'ContainerDetail.Overview' : 'ContainerDetail.Locations';
          routerSvc.goto(view, {containerId: savedContainer.id});
        }
      } else if (dataCtx.createType == 'multiple') {
        this.createMultipleContainers(dataCtx, container);
      } else if (dataCtx.createType == 'hierarchy') {
        this.createHierarchy(container);
      }
    },

    createMultipleContainers: async function(ctx, container) {
      let names = [];
      if (ctx.uniqueNames) {
        names = util.splitStr(ctx.uniqueNames || '', /,|\t|\n/, true);
        ctx.numOfContainers = names.length;
        ctx.showNames = true;
      }

      const position = container.storageLocation;
      let positions = [];
      if (position.name) {
        positions = await containerSvc.getVacantPositions(position, ctx.numOfContainers);
      }

      const containers = [];
      const displayNames   = util.splitStr(ctx.displayNames || '', /,|\t|\n/, true);
      ctx.showDisplayNames = displayNames.length > 0

      const barcodes   = util.splitStr(ctx.barcodes || '', /,|\t|\n/, true);
      ctx.showBarcodes = barcodes.length > 0

      for (var i = 0; i < ctx.numOfContainers; ++i) {
        const copy = util.clone(container);
        delete copy.numOfContainers;
        if (i < names.length) {
          copy.name = names[i];
        }

        if (i < barcodes.length) {
          copy.barcode = barcodes[i];
        }

        if (i < displayNames.length) {
          copy.displayName = displayNames[i];
        }

        if (i < positions.length && positions.length > 0) {
          copy.storageLocation = positions[i];
        }

        containers.push({ container: copy });
      }

      this.ctx.view           = 'review_multiple_containers';
      this.dataCtx.containers = containers;
      if (!this.ctx.locationsSchema) {
        this.ctx.locationsSchema = containerSvc.getLocationsSchema();
      }
    },

    removeContainer: function({idx}) {
      this.dataCtx.containers.splice(idx, 1);
    },

    saveMultipleContainers: function() {
      if (!this.$refs.multiContainersForm.validate()) {
        return;
      }

      containerSvc.createContainers(this.dataCtx.containers.map(({container}) => container)).then(
        (saved) => {
          alertsSvc.success({code: 'containers.multiple_created', args: {count: saved.length}});
          if (saved.length > 0) {
            const parentId = saved[0].storageLocation && saved[0].storageLocation.id;
            if (saved.every(c => c.storageLocation && c.storageLocation.id == parentId)) {
              routerSvc.goto('ContainerDetail.Locations', {containerId: parentId});
              return;
            }
          }

          routerSvc.goto('ContainersList');
        }
      );
    },

    createHierarchy: function(container) {
      let names = [];
      if (this.dataCtx.uniqueNames) {
        names = util.splitStr(this.dataCtx.uniqueNames || '', /,|\t|\n/, true);
        this.dataCtx.numOfContainers = names.length;
      }

      container.numOfContainers = this.dataCtx.numOfContainers;
      container.names = names;
      containerSvc.createHierarchy(container).then(
        (containers) => {
          if (containers.length == 1) {
            //
            // created only one container. go to that container detail
            //
            routerSvc.goto('ContainerDetail.Locations', {containerId: containers[0].id});
          } else if (containers[0].storageLocation && containers[0].storageLocation.id) {
            //
            // hierarchy created under an existing container
            // go to that container detail
            //
            routerSvc.goto('ContainerDetail.Locations', {containerId: containers[0].storageLocation.id});
          } else {
            //
            // hierarchy created for top-level container. go to list view with success message
            //
            alertsSvc.success({code: 'containers.hierarchy_created', args: containers[0]});
            routerSvc.goto('ContainersList');
          }
        }
      );
    },

    cancel: function() {
      routerSvc.back();
    },

    _getBcrumbs: function() {
      const { parentContainer } = this.dataCtx;
      const bcrumbs = [
        {url: routerSvc.getUrl('ContainersList', {containerId: -1}), label: this.$t('containers.list')}
      ];

      if (parentContainer && parentContainer.name) {
        const {id, name, displayName} = parentContainer;
        let label = name;
        if (displayName) {
          label = displayName + ' (' + name + ')';
        }

        bcrumbs.push({url: routerSvc.getUrl('ContainerDetail.Overview', {containerId: id}), label: label});
      }

      return bcrumbs;
    },

    _getAllowedTypes: async function() {
      const container = this.dataCtx.container;
      if (container.usedFor != 'STORAGE') {
        return [];
      }

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

    _getAllowedCps: async function({query, maxResults}) {
      const container = this.dataCtx.container;
      if (container.usedFor != 'STORAGE') {
        return [];
      }

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

    _getAllowedDps: async function({query, maxResults}) {
      const container = this.dataCtx.container;
      if (container.usedFor != 'DISTRIBUTION') {
        return [];
      }

      const parentContainer = await this._getParentContainer(container);
      const allowedDps      = (parentContainer && parentContainer.calcAllowedDistributionProtocols) || [];
      if (allowedDps.length > 0) {
        return this._filterProtocols(allowedDps, query);
      } else {
        let cacheKey = (query && query.toLowerCase()) || '';
        const dpsMap = this.dpsMap = this.dpsMap || {};
        const cachedDps = dpsMap[cacheKey];
        if (cachedDps) {
          return cachedDps;
        }

        return dpSvc.getDps({query, maxResults}).then(
          (dps) => {
            dpsMap[cacheKey] = dps;
            return dps;
          }
        );
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
    }
  }
}
</script>
