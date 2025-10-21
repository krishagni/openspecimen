<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="bcrumb" />
      </template>

      <span class="os-title">
        <h3>{{ctx.list.name}}</h3>

        <div class="accessories" v-if="ctx.list.transferToBox">
          <os-tag :value="$t('carts.box_transfer_enabled')" :rounded="true" type="info" />
        </div>
      </span>

      <template #right v-if="ctx.list.totalSpecimens > 0">
        <os-progress-bar class="list-progress"
          :value="ctx.list.pickedSpecimens" :total="ctx.list.totalSpecimens"
          v-os-tooltip.bottom="$t('carts.picklist_progress', ctx.list)" />
      </template>
    </os-page-head>

    <os-page-body>
      <os-grid>
        <os-grid-column :width="12" v-if="ctx.list.id > 0">
          <os-tabs @tab-change="onTabChange">
            <os-tab>
              <template #header>
                <span v-t="'carts.unpicked_specimens'">Unpicked Specimens</span>
              </template>

              <UnpickedSpecimensTab ref="unpickedListView" class="tab-panel"
                :cart="ctx.list.cart" :pick-list="ctx.list"
                :filters="upf" :active-tab="ctx.activeTab == 0"
                @specimens-loaded="reloadRoute({upf: $event.uriEncoding})"
                @picked-specimens="onSpecimensPick($event)" />
            </os-tab>

            <os-tab>
              <template #header>
                <span v-t="'carts.picked_specimens'">Picked Specimens</span>
              </template>

              <PickedSpecimensTab ref="pickedListView" class="tab-panel"
                :cart="ctx.list.cart" :pick-list="ctx.list"
                :filters="ppf" :active-tab="ctx.activeTab == 1"
                @specimens-loaded="reloadRoute({ppf: $event.uriEncoding})"
                @unpicked-specimens="onSpecimensUnpick($event)" />
            </os-tab>
          </os-tabs>
        </os-grid-column>
      </os-grid>
    </os-page-body>
  </os-page>
</template>

<script>
import cartSvc     from '@/biospecimen/services/SpecimenCart.js';
import routerSvc   from '@/common/services/Router.js';

import PickedSpecimensTab   from './PickedSpecimensTab.vue';
import UnpickedSpecimensTab from './UnpickedSpecimensTab.vue';

export default {
  props: ['cartId', 'listId', 'upf', 'ppf'],

  components: {
    PickedSpecimensTab,

    UnpickedSpecimensTab
  },

  data() {
    return {
      ctx: {
        list: {cart: { }},

        activeTab: 0
      }
    };
  },

  created() {
  },

  mounted() {
    this._loadList();
  },

  computed: {
    bcrumb: function() {
      const {cart} = this.ctx.list || {};
      if (!cart || !cart.id) {
        return [];
      }

      return [
        {url: routerSvc.getUrl('SpecimenCartsList', {cartId: -1}), label: this.$t('carts.list')},
        {url: routerSvc.getUrl('CartSpecimensList', {cartId: cart.id}), label: cartSvc.getDisplayName(cart)},
        {url: routerSvc.getUrl('PickLists', {cartId: cart.id}), label: this.$t('carts.pick_lists')}
      ];
    },

    viewKey: function() {
      return this.cartId + '_' + this.listId;
    },
  },

  watch: {
    viewKey: function() {
      this._loadList();
    }
  },

  methods: {
    onSpecimensPick: function(picked) {
      this.ctx.list.pickedSpecimens += picked.length;
      this.$refs.pickedListView.nullifyList();
    },

    onSpecimensUnpick: function(unpicked) {
      this.ctx.list.pickedSpecimens -= unpicked.length;
      this.$refs.unpickedListView.nullifyList();
    },

    onTabChange: function({index}) {
      this.ctx.activeTab = index;
    },

    reloadRoute: function(queryParams) {
      const {name, params, query} = routerSvc.getCurrentRoute();
      routerSvc.goto(name, params, {...query, ...queryParams});
    },

    _loadList: async function() {
      this.ctx.list = {};
      cartSvc.getPickList(+this.cartId, +this.listId).then(list => this.ctx.list = list);
    }
  }
}
</script>

<style scoped>
:deep(.tab-panel) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

:deep(.tab-panel .toolbar) {
  margin-bottom: 1.25rem;
  display: flex;
  justify-content: space-between;
}

:deep(.tab-panel .toolbar button:not(:last-child)) {
  margin-right: 1rem;
}

.list-progress {
  width: 25vw;
}
</style>
