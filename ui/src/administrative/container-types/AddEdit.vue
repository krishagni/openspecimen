<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3 v-if="!dataCtx.type.id">
          <span v-t="'container_types.create'">Create Container Type</span>
        </h3>
        <h3 v-else>
          <span v-t="{path: 'common.update', args: dataCtx.type}">Update {{dataCtx.type.name}}</span>
        </h3>
      </span>
    </os-page-head>

    <os-page-body>
      <div v-if="ctx.loading">
        <os-message type="info">
          <span v-t="'common.loading_form'">Loading the form. Please wait for a moment...</span>
        </os-message>
      </div>
      <div v-else>
        <os-form ref="typeForm" :schema="ctx.addEditFs" :data="dataCtx" @input="handleInput($event)">
          <div>
            <os-button primary :label="$t(!dataCtx.type.id ? 'common.buttons.create' : 'common.buttons.update')"
              @click="saveOrUpdate" />

            <os-button text :label="$t('common.buttons.cancel')"  @click="cancel" />
          </div>
        </os-form>
      </div>
    </os-page-body>
  </os-page>
</template>

<script>
import { reactive, inject } from 'vue';

import i18n        from '@/common/services/I18n.js';
import routerSvc   from '@/common/services/Router.js';
import typesSvc    from '@/administrative/services/ContainerType.js';

export default {
  props: ['typeId'],

  inject: ['ui'],

  setup() {
    const ui = inject('ui');

    let ctx = reactive({
      bcrumb: [
        {url: routerSvc.getUrl('ContainerTypesList', {typeId: -1}), label: i18n.msg('container_types.list')}
      ],

      addEditFs: {rows: []},

      loading: true,
    });

    let dataCtx = reactive({
      type: {},

      currentUser: ui.currentUser,
    });

    return { ctx, dataCtx };
  },

  created: async function() {
    const { schema } = typesSvc.getAddEditFormSchema();
    this.ctx.addEditFs = schema;
    this.loadType();
  },

  watch: {
    typeId: function (newVal, oldVal) {
      if (newVal == oldVal) {
        return;
      }

      this.loadType();
    }
  },

  methods: {
    loadType: async function() {
      const ctx        = this.ctx;
      const dataCtx    = this.dataCtx;

      ctx.loading = true;
      dataCtx.type = { };
      if (this.typeId && +this.typeId > 0) {
        const type = dataCtx.type = await typesSvc.getType(this.typeId);
        if (type.noOfRows == null || type.noOfColumns == null) {
          dataCtx.dimensionLess = true;
        }
      }

      ctx.loading = false;
    },

    handleInput: async function({data}) {
      Object.assign(this.dataCtx, data);
    },

    saveOrUpdate: async function() {
      if (!this.$refs.typeForm.validate()) {
        return;
      }

      const type = this.dataCtx.type;
      const savedType = await typesSvc.saveOrUpdate(type);
      if (!type.id) {
        routerSvc.goto('ContainerTypeDetail.Overview', {typeId: savedType.id});
      } else {
        routerSvc.back();
      }

      return savedType;
    },

    cancel: function() {
      routerSvc.back();
    }
  }
}
</script>
