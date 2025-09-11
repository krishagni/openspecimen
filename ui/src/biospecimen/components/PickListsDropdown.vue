<template>
  <span>
    <os-button left-icon="dolly" :label="$t('carts.pick_lists')" @click="showCreatePickListDialog"
      v-if="!cart || !cart.id" />

    <os-dynamic-menu ref="pickListsMenu" icon="dolly" :label="$t('carts.pick_lists')" :options="ctx.pickLists"
      :no-options-label="$t('carts.no_pick_lists')" :search-hint="$t('carts.search_pick_lists')"
      @option-selected="viewPickList($event)" @search-options="loadPickLists($event)" :key="cart.id" v-else>
      <template #fixed-options>
        <li>
          <a @click="showCreatePickListDialog">
            <span v-t="'carts.new_pick_list'">Create New</span>
          </a>
        </li>
        <li>
          <a @click="viewPickLists">
            <span v-t="'carts.manage_pick_lists'">Manage Pick Lists</span>
          </a>
        </li>
      </template>
    </os-dynamic-menu>

    <os-addedit-pick-list-dialog ref="addEditPickListDialog" @new-cart="$emit('new-cart', $event)"/>
  </span>
</template>

<script>
import cartSvc   from '@/biospecimen/services/SpecimenCart.js';
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['cart'],

  emits: ['new-cart'],

  data() {
    return {
      ctx: {
        pickLists: []
      }
    }
  },

  methods: {
    showCreatePickListDialog: function(event) {
      if (this.cart && this.cart.id > 0) {
        this.$refs.pickListsMenu.toggleMenu(event);
      }

      this.$refs.addEditPickListDialog.open(this.cart, {});
    },

    viewPickLists: function() {
      routerSvc.goto('PickLists', {cartId: this.cart.id});
    },

    viewPickList: function(pickList) {
      routerSvc.goto('PickList', {cartId: this.cart.id, listId: pickList.id});
    },

    loadPickLists: function(name) {
      cartSvc.getPickLists(+this.cart.id, {name}).then(
        lists => {
          this.ctx.pickLists = lists = lists || [];
          lists.forEach(list => list.displayName = list.name);
        }
      );
    }
  }
}
</script>
