<template>
  <os-page-toolbar>
    <template #default>
      <os-button-link v-show-if-allowed="typeResources.createOpts"
        left-icon="box-open" :label="$t('container_types.create_container')" :url="createContainerUrl" />

      <os-button-link v-show-if-allowed="'institute-admin'"
        left-icon="edit" :label="$t('common.buttons.edit')" :url="editTypeUrl" />

      <os-button v-show-if-allowed="'institute-admin'"
        left-icon="trash" :label="$t('common.buttons.delete')" @click="deleteType" />

      <os-button left-icon="history" :label="$t('audit.trail')" @click="viewAuditTrail" />
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="12">
      <os-overview :schema="ctx.dict" :object="ctx" :columns="1" v-if="ctx.dict.length > 0" />
    </os-grid-column>
  </os-grid>

  <os-delete-object ref="deleteObj" :input="ctx.deleteOpts" />

  <os-audit-trail ref="auditTrailDialog" :objects="ctx.typeObjs" />
</template>

<script>

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

  computed: {
    createContainerUrl: function() {
      return routerSvc.getUrl('ContainerAddEdit', {containerId: -1}, {typeId: this.type.id, mode: 'hierarchy'});
    },

    editTypeUrl: function() {
      return routerSvc.getUrl('ContainerTypeAddEdit', {typeId: this.type.id});
    }
  },

  methods: {
    setupView: async function() {
      this.ctx.type = this.type;
      this.ctx.typeObjs = [{objectName: 'container_type', objectId: this.type.id}];
      this.ctx.deleteOpts = {
        type: this.$t('container_types.singular'),
        title: this.type.name,
        dependents: () => typesSvc.getDependents(this.type),
        deleteObj: () => typesSvc.delete(this.type)
      };
    },

    deleteType: function() {
      this.$refs.deleteObj.execute().then(
        (resp) => {
          if (resp == 'deleted') {
            routerSvc.goto('ContainerTypesList', {typeId: -2}, this.$route.query);
          }
        }
      );
    },

    viewAuditTrail: function() {
      this.$refs.auditTrailDialog.open();
    }
  }
}
</script>
