<template>
  <os-page>
    <os-page-head :noNavButton="listItemDetailView">
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span class="os-title">
        <h3>{{displayName}}</h3>
        <div class="accessories" v-if="ctx.cart && ctx.cart.id > 0">
          <os-button left-icon="info-circle" size="small" @click="toggleCartInfo" />
        </div>
      </span>
    </os-page-head>

    <os-page-body>
      <os-page-toolbar>
        <template #default>
          <os-specimen-actions :cart-id="ctx.cart.id" :specimens="selectedSpecimens" @reloadSpecimens="reloadList" />

          <os-plugin-views page="cart-specimens" view="page-header"
            :view-props="{cart: ctx.cart, selectedSpecimens}" />

          <os-menu :label="$t('common.buttons.more')" :options="moreOpts" />

          <os-pick-lists-dropdown :cart="ctx.cart" v-if="ctx.cart && ctx.cart.id > 0" />
        </template>

        <template #right>
          <os-list-size
            :list="listInfo.rows"
            :page-size="listInfo.pageSize"
            :list-size="listInfo.size"
            @updateListSize="getSpecimensCount"
          />

          <os-button left-icon="search" :label="$t('common.buttons.search')" @click="toggleSearch" />
        </template>
      </os-page-toolbar>

      <os-query-list-view
        name="cart-specimens-list-view"
        :object-id="cartId"
        :allow-selection="true"
        :query="query"
        url="'#/specimen-resolver/' + hidden.specimenId"
        @selectedRows="onSpecimenSelection"
        @rowClicked="onSpecimenRowClick"
        @listLoaded="onSpecimensLoaded"
        ref="specimensList"
      />
    </os-page-body>

    <os-confirm ref="rmSpecimensConfirm">
      <template #title>
        <span v-t="'carts.remove_specimens_q'">Remove specimens from the cart...</span>
      </template>

      <template #message>
        <span v-t="{path: 'carts.confirm_remove_specimens', args: {name: displayName}}">Are you sure you want to remove the selected specimens from the cart <b>{{displayName}}</b>?</span>
      </template>
    </os-confirm>

    <os-overlay ref="cartInfoOverlay">
      <os-overview :schema="ctx.dict" :object="ctx" :columns="1" v-if="ctx.dict.length > 0" />
    </os-overlay>
  </os-page>
</template>

<script>

import alertSvc   from '@/common/services/Alerts.js';
import cartSvc    from '@/biospecimen/services/SpecimenCart.js';
import routerSvc  from '@/common/services/Router.js';
import util       from '@/common/services/Util.js';

export default {
  props: ['cartId', 'query', 'listItemDetailView'],

  data() {
    return {
      ctx: {
        cart: {},

        bcrumb: [
          {url: routerSvc.getUrl('SpecimenCartsList', {cartId: -1}), label: this.$t('carts.list')}
        ],

        dict: cartSvc.getDict(),
      },

      listInfo: { rows: [], size: 0, pageSize: 0 },

      selectedSpecimens: [],

      moreOpts: [
        {
          icon: 'edit',
          caption: this.$t('carts.edit_or_delete'),
          onSelect: () => routerSvc.goto('SpecimenCartAddEdit', {cartId: this.cartId})
        },
        {
          icon: 'plus',
          caption: this.$t('carts.include_child_specimens'),
          onSelect: () => this.addChildSpecimens()
        },
        {
          icon: 'download',
          caption: this.$t('carts.download_report'),
          onSelect: () => this.downloadReport()
        },
        {
          icon: 'trash',
          caption: this.$t('carts.remove_from_cart'),
          onSelect: () => this.removeFromCart()
        }
      ],
    };
  },

  created() {
    this.loadCart();
  },

  watch: {
    cartId: function(newCartId, oldCartId) {
      if (oldCartId != newCartId) {
        this.loadCart();
      }
    }
  },

  computed: {
    displayName: function() {
      return cartSvc.getDisplayName(this.ctx.cart);
    }
  },

  methods: {
    loadCart: async function() {
      cartSvc.getCart(+this.cartId).then(cart => this.ctx.cart = cart);
    },

    toggleSearch: function() {
      this.$refs.specimensList.toggleShowFilters();
    },

    toggleCartInfo: function(event) {
      this.$refs.cartInfoOverlay.toggle(event);
    },

    getSpecimensCount: function() {
      this.$refs.specimensList.loadListSize().then(
        () => {
          this.listInfo.size = this.$refs.specimensList.size;
        }
      );
    },

    onSpecimenRowClick: function(rowObject) {
      routerSvc.goto('SpecimenResolver', {specimenId: rowObject.hidden.specimenId});
    },

    onSpecimensLoaded: function({widget, list, filters}) {
      this.listInfo = { rows: list.rows, size: widget.size, pageSize: widget.pageSize };

      const query = {...this.$route.query, specimenFilters: filters};
      routerSvc.goto(this.$route.name, this.$route.params, query);
    },

    reloadList: function() {
      this.$refs.specimensList.reload();
    },

    onSpecimenSelection: function(specimens) {
      this.selectedSpecimens = specimens.map(({rowObject}) => ({id: +rowObject.hidden.specimenId, cpId: +rowObject.hidden.cpId}));
    },

    addChildSpecimens: function() {
      cartSvc.addChildSpecimens(this.ctx.cart).then(
        () => {
          alertSvc.success({code: 'carts.child_specimens_added'});
          this.reloadList();
        }
      );
    },

    downloadReport: function() {
      const selectedSpmns = (this.selectedSpecimens || []).map(spmn => spmn.id);
      const params = {};
      if (selectedSpmns.length > 0) {
        params.specimenId = selectedSpmns;
      }

      const reportFn = () => cartSvc.generateReport(this.ctx.cart, params);
      util.downloadReport(reportFn, {filename: cartSvc.getDisplayName(this.ctx.cart) + '.csv'})
    },

    removeFromCart: async function() {
      if (!this.selectedSpecimens || this.selectedSpecimens.length == 0) {
        alertSvc.error({code: 'carts.select_one_spmn_to_rm'});
        return;
      }

      const resp = await this.$refs.rmSpecimensConfirm.open();
      if (resp != 'proceed') {
        return;
      }

      cartSvc.removeFromCart(this.ctx.cart, this.selectedSpecimens).then(
        ({count}) => {
          alertSvc.success({code: 'carts.specimens_removed', args: {count: count, name: this.displayName}});
          this.reloadList();
        }
      );
    },

    showCreatePickListDialog: function(event) {
      this.$refs.pickListsMenu.toggleMenu(event);
      this.$refs.addEditPickListDialog.open(this.ctx.cart, {});
    },

    viewPickLists: function() {
      routerSvc.goto('PickLists', {cartId: this.cartId});
    },

    viewPickList: function(pickList) {
      routerSvc.goto('PickList', {cartId: this.cartId, listId: pickList.id});
    },

    loadPickLists: function(name) {
      cartSvc.getPickLists(+this.cartId, {name}).then(
        lists => {
          this.ctx.pickLists = lists = lists || [];
          lists.forEach(list => list.displayName = list.name);
        }
      );
    }
  }
}
</script>
