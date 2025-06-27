<template>
  <os-screen>
    <os-screen-panel :width="ctx.detailView ? 3 : 12">
      <os-page>
        <os-page-head>
          <template #breadcrumb v-if="ctx.folder && !ctx.detailView">
            <os-breadcrumb :items="ctx.bcrumb" />
          </template>

          <span>
            <h3 v-if="ctx.folder">{{ctx.folder.name}}</h3>
            <h3 v-else>
              <span v-t="'carts.list'">Carts</span>
            </h3>
          </span>

          <template #right>
            <os-button v-if="ctx.detailView"
              size="small" left-icon="expand-alt"
              v-os-tooltip.bottom="$t('common.switch_to_table_view')"
              @click="showTable"
            />

            <os-list-size v-else
              :list="ctx.carts"
              :page-size="ctx.pageSize"
              :list-size="ctx.cartsCount"
              @updateListSize="getCartsCount"
            />
          </template>
        </os-page-head>

        <os-page-body>
          <os-page-toolbar v-if="!ctx.detailView">
            <template #default>
              <span v-if="!ctx.folder">
                <span v-if="ctx.selectedCarts.length == 0">
                  <os-button left-icon="plus" :label="$t('common.buttons.create')" @click="createCart" />

                  <os-button :label="$t('carts.view_my_def_cart')" @click="viewDefaultCart" />
                </span>

                <os-button left-icon="folder" :label="$t('carts.view_folders')" @click="viewFolders" />

                <AssignCart :carts="ctx.selectedCarts" />
              </span>

              <span v-else>
                <span v-if="ctx.selectedCarts.length > 0">
                  <os-button left-icon="times" :label="$t('common.buttons.remove')"
                    v-os-tooltip.right="$t('carts.remove_from_folder')"
                    @click="removeFromFolder" />
                </span>
              </span>

              <os-button-link left-icon="question-circle" :label="$t('common.buttons.help')"
                url="https://openspecimen.atlassian.net/wiki/x/DgAwAw" new-tab="true" />
            </template>

            <template #right>
              <os-button left-icon="search" :label="$t('common.buttons.search')" @click="openSearch" />
            </template>
          </os-page-toolbar>

          <os-list-view
            :data="ctx.carts"
            :schema="listSchema"
            :query="ctx.query"
            :allowSelection="true"
            :loading="ctx.loading"
            :selected="ctx.selectedCart"
            @filtersUpdated="loadCarts"
            @selectedRows="onCartsSelection"
            @rowClicked="onCartRowClick"
            @rowStarToggled="onToggleStar"
            ref="listView"
          />
        </os-page-body>
      </os-page>
    </os-screen-panel>

    <os-screen-panel :width="9" v-if="$route.params && $route.params.cartId >= 0 && ctx.selectedCart">
      <router-view :cartId="ctx.selectedCart.cart.id" />
    </os-screen-panel>
  </os-screen>
</template>

<script>

import listSchema from  '@/biospecimen/schemas/carts/list.js';

import alertSvc    from '@/common/services/Alerts.js';
import cartSvc     from '@/biospecimen/services/SpecimenCart.js';
import folderSvc   from '@/biospecimen/services/SpecimenCartsFolder.js';
import routerSvc   from '@/common/services/Router.js';
import util        from '@/common/services/Util.js';

import AssignCart  from '@/biospecimen/carts/AssignCart.vue';

export default {
  props: ['cartId', 'folderId', 'filters'],

  components: {
    AssignCart
  },

  data() {
    return {
      ctx: {
        carts: [],

        cartsCount: -1,

        loading: true,

        query: this.filters,

        detailView: false,

        selectedCart: null,

        selectedCarts: [],

        folder: null,

        bcrumb: [ { url: routerSvc.getUrl('SpecimenCartsFoldersList'), label: this.$t('carts.folders') } ]
      },

      listSchema,
    };
  },

  created() {
    if (this.folderId > 0) {
      folderSvc.getFolder(this.folderId).then(folder => this.ctx.folder = folder);

      const ls = this.listSchema = util.clone(this.listSchema);
      ls.filters = ls.filters.filter(f => f.name != 'folderId');
    }
  },

  watch: {
    'folderId': function(newFolderId, oldFolderId) {
      if (newFolderId == oldFolderId) {
        return;
      }

      if (newFolderId > 0) {
        folderSvc.getFolder(newFolderId).then(folder => this.ctx.folder = folder);
      } else {
        this.ctx.folder = null;
      }

      this.$refs.listView.reload();
    },

    'cartId': function(newCartId, oldCartId) {
      if (newCartId == oldCartId) {
        return;
      }

      if (newCartId >= 0) {
        let selectedRow = this.findCart(this.ctx.carts, newCartId);
        if (!selectedRow) {
          selectedRow = {cart: {id: newCartId}};
        }

        this.showDetails(selectedRow);
      } else {
        this.showTable(newCartId == -2);
      }
    }
  },

  methods: {
    openSearch: function() {
      this.$refs.listView.toggleShowFilters();
    },

    loadCarts: async function({filters, uriEncoding, pageSize}) {
      this.ctx.filterValues = filters;
      this.ctx.pageSize     = pageSize;

      const carts = await this.reloadCarts();
      if (this.cartId < 0) {
        routerSvc.goto('SpecimenCartsList', {cartId: -1}, {filters: uriEncoding, folderId: this.folderId});
      } else {
        let selectedRow = this.findCart(carts, this.cartId);
        if (!selectedRow) {
          selectedRow = {cart: {id: this.cartId}};
        }

        this.showDetails(selectedRow);
      }
    },

    findCart: function(carts, cartId) {
      const defCartName = '$$$$user_' + this.$ui.currentUser.id;
      return carts.find(({cart}) => cart.id == cartId || (cartId == 0 && cart.name == defCartName));
    },

    reloadCarts: async function() {
      this.ctx.loading = true;

      const opts = Object.assign({
        orderByStarred: true,
        includeStats: true,
        maxResults: this.ctx.pageSize,
        folderId: this.folderId
      }, this.ctx.filterValues || {});
      const carts   = await cartSvc.getCarts(opts);

      this.ctx.loading = false;
      this.ctx.carts = carts.map(cart => ({cart: cart}));
      this.ctx.selectedCarts = [];
      return this.ctx.carts;
    },

    getCartsCount: function() {
      this.ctx.cartsCount = -1;
      const opts = Object.assign({}, this.ctx.filterValues);
      cartSvc.getCartsCount(opts).then(({count}) => this.ctx.cartsCount = count);
    },

    onCartRowClick: function({cart}) {
      routerSvc.goto('CartSpecimensList', {cartId: cart.id}, {filters: this.filters, folderId: this.folderId});
    },

    showDetails: function(rowObject) {
      this.ctx.selectedCart = rowObject;
      if (!this.ctx.detailview) {
        this.ctx.detailView = true;
        this.$refs.listView.switchToSummaryView();
      }
    },

    showTable: function(reload) {
      this.ctx.detailView = false;
      this.$refs.listView.switchToTableView();
      routerSvc.goto('SpecimenCartsList', {cartId: -1}, {filters: this.filters, folderId: this.folderId});
      if (reload) {
        this.$refs.listView.reload();
      }
    },

    createCart: function() {
      routerSvc.goto('SpecimenCartAddEdit', {cartId: -1});
    },

    viewDefaultCart: function() {
      routerSvc.goto('CartSpecimensList', {cartId: 0}, {filters: this.filters, folderId: this.folderId});
    },

    onToggleStar: async function({cart}) {
      let resp;
      if (cart.starred) {
        resp = await cartSvc.unstar(cart);
      } else {
        resp = await cartSvc.star(cart);
      }

      if (resp.status) {
        cart.starred = !cart.starred;
      }
    },

    onCartsSelection: function(carts) {
      this.ctx.selectedCarts = carts.map(({rowObject}) => ({id: +rowObject.cart.id}));
    },

    viewFolders: function() {
      routerSvc.goto('SpecimenCartsFoldersList');
    },

    removeFromFolder: function() {
      folderSvc.removeCarts(this.ctx.folder, this.ctx.selectedCarts).then(
        ({count}) => {
          if (count == 0) {
            alertSvc.info({code: 'carts.folder_no_carts_removed', args: this.ctx.folder});
          } else {
            alertSvc.success({code: 'carts.folder_carts_removed', args: {count: count, name: this.ctx.folder.name}});
            this.$refs.listView.reload();
          }
        }
      );
    }
  }
}
</script>
