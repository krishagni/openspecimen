<template>
  <os-page-toolbar>
    <template #default>
      <os-button v-show-if-allowed="typeResources.createOpts"
        left-icon="box-open" label="Create Container" @click="createContainer" />

      <os-button v-show-if-allowed="typeResources.updateOpts"
        left-icon="edit" label="Edit" @click="editType" />

      <os-button v-show-if-allowed="typeResources.deleteOpts"
        left-icon="trash" label="Delete" @click="deleteType" />
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="8">
      <os-overview :schema="ctx.dict" :object="ctx" :columns="1" v-if="ctx.dict.length > 0" />
    </os-grid-column>

    <os-grid-column width="4">
      <os-audit-overview :objects="ctx.typeObjs" v-if="ctx.type.id" />
    </os-grid-column>
  </os-grid>

  <os-confirm-delete ref="deleteDialog" :captcha="false">
    <template #message>
      <span>Container type '{{ctx.type.name}}' and any dependent data will be deleted. Are you sure you want to proceed?</span>
    </template>
  </os-confirm-delete>
</template>

<script>

import alertsSvc  from '@/common/services/Alerts.js';
import routerSvc  from '@/common/services/Router.js';
import typesSvc   from '@/administrative/services/ContainerType.js';

import typeResources from './Resources.js'

export default {
  props: ['type'],

  inject: ['ui'],

  data() {
    return {
      ctx: {
        type: {},

        typeObjs: [],

        dict: []
      },

      typeResources
    };
  },


  created() {
    this.ctx.dict = typesSvc.getDict();
    this.setupView();
  },

  watch: {
    'type.id': function(newVal, oldVal) {
      if (newVal == oldVal) {
        return;
      }

      this.setupView();
    }
  },

  methods: {
    setupView: async function() {
      this.ctx.type = this.type;
      this.ctx.typeObjs = [{objectName: 'container_type', objectId: this.type.id}];
    },

    createContainer: function() {
      routerSvc.goto('ContainerAddEdit', {containerId: -1}, {typeId: this.type.id, mode: 'hierarchy'});
    },

    editType: function() {
      routerSvc.goto('ContainerTypeAddEdit', {typeId: this.type.id});
    },

    deleteType: function() {
      this.$refs.deleteDialog.open().then(
        () => typesSvc.delete(this.type).then(
          () => {
            alertsSvc.success('Container type ' + this.type.name + ' deleted!');
            routerSvc.goto('ContainerTypesList', {typeId: -2});
          }
        )
      );
    }
  }
}
</script>
