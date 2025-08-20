<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="bcrumb" />
      </template>

      <h3 v-t="'carts.pick_lists'">Pick Lists</h3>
    </os-page-head>

    <os-page-body>
      <os-page-toolbar>
        <template #default>
          <os-button left-icon="plus" :label="$t('common.buttons.create')" @click="showCreatePickListDialog(null)" />
        </template>

        <template #right>
          <os-button left-icon="search" :label="$t('common.buttons.search')" @click="openSearch" />
        </template>
      </os-page-toolbar>

      <os-list-view
        :data="ctx.pickLists"
        :schema="listSchema"
        :query="ctx.query"
        :loading="ctx.loading"
        :showRowActions="true"
        @filtersUpdated="loadPickLists"
        @rowClicked="onPickListRowClick"
        ref="listView">
        <template #rowActions="slotProps">
          <os-button-group>
            <os-button size="small" left-icon="pencil" @click="showCreatePickListDialog(slotProps.rowObject.pickList)" />
            <os-button size="small" left-icon="trash" @click="deletePickList(slotProps.rowObject.pickList)" />
          </os-button-group>
        </template>
      </os-list-view>
    </os-page-body>

    <AddEditPickListDialog ref="addEditPickListDialog" />

    <os-confirm-delete ref="confirmDeletePickList" :captcha="false">
      <template #message>
        <span v-t="{path: 'carts.confirm_delete_picklist', args: ctx.toDeletePickList}">Are you sure you want to delete the pick list?</span>
      </template>
    </os-confirm-delete>
  </os-page>
</template>

<script>

import listSchema from  '@/biospecimen/schemas/carts/pick-lists.js';

import cartSvc     from '@/biospecimen/services/SpecimenCart.js';
import routerSvc   from '@/common/services/Router.js';

import AddEditPickListDialog from './AddEditPickListDialog.vue';

export default {
  props: ['cartId', 'filters'],

  components: {
    AddEditPickListDialog
  },

  data() {
    return {
      ctx: {
        cart: {},

        pickLists: [],

        pickListsCount: -1,

        loading: true,

        query: this.filters,
      },

      listSchema,
    };
  },

  created() {
    this._loadCart();
  },

  watch: {
    cartId: function(newVal, oldVal) {
      if (newVal == oldVal) {
        return;
      }

      this._loadCart();
    }
  },

  computed: {
    bcrumb: function() {
      const {cart} = this.ctx || {};
      if (!cart || !cart.id) {
        return [];
      }

      return [
        {url: routerSvc.getUrl('SpecimenCartsList', {cartId: -1}), label: this.$t('carts.list')},
        {url: routerSvc.getUrl('CartSpecimensList', {cartId: cart.id}), label: cartSvc.getDisplayName(cart)}
      ];
    },
  },

  methods: {
    openSearch: function() {
      this.$refs.listView.toggleShowFilters();
    },

    loadPickLists: async function({filters, uriEncoding, pageSize}) {
      this.ctx.filterValues = filters;
      this.ctx.pageSize     = pageSize;

      this.ctx.loading = true;
      const opts = Object.assign({includeStats: true, maxResults: this.ctx.pageSize}, this.ctx.filterValues || {});
      const pickLists = await cartSvc.getPickLists(this.cartId, opts);

      this.ctx.loading = false;
      this.ctx.pickLists = pickLists.map(pickList => ({pickList}));
      routerSvc.goto('PickLists', {cartId: this.cartId}, {filters: uriEncoding});
      return this.ctx.pickLists;
    },

    getPickListsCount: function() {
      this.ctx.pickListsCount = -1;
      const opts = Object.assign({}, this.ctx.filterValues);
      cartSvc.getPickListsCount(opts).then(({count}) => this.ctx.pickListsCount = count);
    },

    onPickListRowClick: function({pickList}) {
      routerSvc.goto('PickList', {cartId: +this.cartId, listId: pickList.id});
    },

    showCreatePickListDialog: function(pickList) {
      this.$refs.addEditPickListDialog.open(this.ctx.cart, pickList || {});
    },

    deletePickList: function(pickList) {
      this.ctx.toDeletePickList = pickList;
      this.$refs.confirmDeletePickList.open().then(
        resp => {
          if (resp != 'proceed') {
            return;
          }

          cartSvc.deletePickList(pickList).then(() => this.$refs.listView.reload());
        }
      );
    },

    _loadCart: function() {
      cartSvc.getCart(this.cartId).then(cart => this.ctx.cart = cart);
    }
  }
}
</script>
