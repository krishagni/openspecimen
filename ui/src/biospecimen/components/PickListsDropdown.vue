<template>
  <span>
    <os-button left-icon="dolly" :label="$t('carts.pick_lists')" @click="showCreatePickListDialog"
      v-if="!cart || !cart.id" />

    <os-dynamic-menu ref="pickListsMenu" icon="dolly" :label="$t('carts.pick_lists')" :options="pickLists"
      :no-options-label="$t('carts.no_pick_lists')" :search-hint="$t('carts.search_pick_lists')"
      @search-options="loadPickLists($event)" :key="cart.id" v-else>
      <template #fixed-options>
        <li>
          <a @click="showCreatePickListDialog">
            <span v-t="'carts.new_pick_list'">Create New</span>
          </a>
        </li>
        <li>
          <a :href="pickListsUrl">
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

  computed: {
    pickLists: function() {
      return (this.ctx.pickLists || []).map(
        pickList => ({
          ...pickList,
          url: routerSvc.getUrl('PickList', {cartId: this.cart.id, listId: pickList.id})
        })
      );
    },

    pickListsUrl: function() {
      return routerSvc.getUrl('PickLists', {cartId: this.cart.id});
    }
  },

  methods: {
    showCreatePickListDialog: function(event) {
      if (this.cart && this.cart.id > 0) {
        this.$refs.pickListsMenu.toggleMenu(event);
      }

      this.$refs.addEditPickListDialog.open(this.cart, {});
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
