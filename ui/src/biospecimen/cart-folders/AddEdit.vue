<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3 v-if="dataCtx.folder.id >= 0">
          <span v-t="{path: 'common.update', args: dataCtx.folder}">Update {{dataCtx.folder.name}}</span>
        </h3>
        <h3 v-else>
          <span v-t="'carts.create_folder'">Create Folder</span>
        </h3>
      </span>
    </os-page-head>

    <os-page-body>
      <os-form ref="folderForm" :schema="ctx.addEditFs" :data="dataCtx" @input="handleInput($event)">
        <div>
          <os-button primary :label="$t(!dataCtx.folder.id ? 'common.buttons.create' : 'common.buttons.update')"
            @click="saveOrUpdate" />
          <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />
        </div>
      </os-form>
    </os-page-body>
  </os-page>
</template>

<script>
import { reactive, inject } from 'vue';

import alertSvc  from '@/common/services/Alerts.js';
import routerSvc from '@/common/services/Router.js';

import folderSvc from '@/biospecimen/services/SpecimenCartsFolder.js';
import i18n      from '@/common/services/I18n.js';
import itemsSvc  from '@/common/services/ItemsHolder.js';
import util      from '@/common/services/Util.js';

export default {
  props: ['folderId'],

  inject: ['ui'],

  setup(props) {
    const ui = inject('ui');

    let ctx = reactive({
      bcrumb: [
        {url: routerSvc.getUrl('SpecimenCartsFoldersList', {}), label: i18n.msg('carts.folders')}
      ],

      addEditFs: {rows: []}
    });

    let dataCtx = reactive({
      folder: {userGroups: []},

      currentUser: ui.currentUser,
    });

    const { schema } = folderSvc.getAddEditFormSchema();
    ctx.addEditFs = schema;

    if (props.folderId && +props.folderId >= 0) {
      folderSvc.getFolder(props.folderId).then(folder => dataCtx.folder = folder);
    }

    ctx.inputCarts = itemsSvc.getItems('carts');
    itemsSvc.clearItems('carts', null);
    return { ctx, dataCtx };
  },

  methods: {
    handleInput: function({data}) {
      Object.assign(this.dataCtx, data);
    },

    saveOrUpdate: async function() {
      if (!this.$refs.folderForm.validate()) {
        return;
      }

      let carts = this.ctx.inputCarts || [];
      const toSave = util.clone(this.dataCtx.folder);
      toSave.cartIds = carts.map(cart => cart.id);
      folderSvc.saveOrUpdate(toSave).then(
        (savedFolder) => {
          alertSvc.success({code: toSave.id ? 'carts.folder_updated' : 'carts.folder_created', args: savedFolder});
          routerSvc.back();
        }
      );
    },

    cancel: function() {
      routerSvc.back();
    }
  }
}
</script>
