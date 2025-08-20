<template>
  <os-dialog ref="addEditFormDialog">
    <template #header>
      <span v-t="'carts.update_pick_list'" v-if="ctx.pickList.id > 0">Update Pick List</span>
      <span v-t="'carts.new_pick_list'" v-else>New Pick List</span>
    </template>
    <template #content>
      <os-form ref="addEditForm" :schema="addEditFs" :data="ctx" />
    </template>
    <template #footer>
      <os-button text    :label="$t('common.buttons.cancel')" @click="close" />
      <os-button primary :label="$t('common.buttons.update')" @click="saveOrUpdate" v-if="ctx.pickList.id > 0" />
      <os-button primary :label="$t('common.buttons.create')" @click="saveOrUpdate" v-else />
    </template>
  </os-dialog>
</template>

<script>

import cartSvc   from '@/biospecimen/services/SpecimenCart.js';
import routerSvc from '@/common/services/Router.js';
import util      from '@/common/services/Util.js';

export default {

  data() {
    return {
      ctx: {
        pickList: {}
      }
    }
  },

  computed: {
    addEditFs: function() {
      return {
        rows: [
          {
            fields: [
              {
                name: "pickList.name",
                type: "text",
                labelCode: "carts.name",
                validations: {
                  required: {
                    messageCode: "carts.name_required"
                  }
                }
              }
            ]
          }
        ]
      };
    }
  },

  methods: {
    open: function(cart, pickList) {
      this.ctx.cart = cart;
      this.ctx.pickList = util.clone(pickList || {});
      this.$refs.addEditFormDialog.open();
    },

    close: function() {
      this.ctx.pickList = {};
      this.$refs.addEditFormDialog.close();
    },

    saveOrUpdate: function() {
      if (!this.$refs.addEditForm.validate()) {
        return;
      }

      const {pickList} = this.ctx;
      const payload = {...pickList, cart: this.ctx.cart};
      cartSvc.saveOrUpdatePickList(payload).then(
        savedPickList => {
          this.close();
          routerSvc.goto('PickList', {cartId: savedPickList.cart.id, listId: savedPickList.id});
        }
      );
    }
  }
}
</script>
