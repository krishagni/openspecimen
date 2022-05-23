<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3 v-if="dataCtx.cart.id >= 0">Update {{ctx.cartDisplayName}}</h3>
        <h3 v-else>Create Cart</h3>
      </span>
    </os-page-head>

    <os-page-body>
      <os-form ref="cartForm" :schema="ctx.addEditFs" :data="dataCtx" @input="handleInput($event)">
        <div>
          <os-button primary :label="!dataCtx.cart.id ? 'Create' : 'Update'" @click="saveOrUpdate" />
          <os-button danger  label="Delete" v-if="deleteAllowed" @click="deleteCart" />
          <os-button text label="Cancel" @click="cancel" />
        </div>
      </os-form>
    </os-page-body>

    <os-confirm-delete ref="deleteConfirm" :captcha="false">
      <template #message>
        <span>Cart <b>{{dataCtx.cart.name}}</b> and any dependent data will be deleted. Are you sure you want to proceed?</span>
      </template>
    </os-confirm-delete>
  </os-page>
</template>

<script>
import { reactive, inject } from 'vue';

import alertSvc  from '@/common/services/Alerts.js';
import routerSvc from '@/common/services/Router.js';

import cartSvc   from '@/biospecimen/services/SpecimenCart.js';
import itemsSvc  from '@/common/services/ItemsHolder.js';
import util      from '@/common/services/Util.js';

export default {
  props: ['cartId'],

  inject: ['ui'],

  setup(props) {
    const ui = inject('ui');

    let ctx = reactive({
      bcrumb: [
        {url: routerSvc.getUrl('SpecimenCartsList', {cartId: -1}), label: 'Carts'}
      ],

      addEditFs: {rows: []}
    });

    let dataCtx = reactive({
      cart: {sharedWithUserGroups: []},

      currentUser: ui.currentUser,
    });

    const { schema } = cartSvc.getAddEditFormSchema();
    ctx.addEditFs = schema;

    if (props.cartId && +props.cartId >= 0) {
      cartSvc.getCart(props.cartId).then(
        cart => {
          dataCtx.cart        = cart;
          dataCtx.defaultCart = cartSvc.isDefaultCart(cart);
          ctx.cartDisplayName = cartSvc.getDisplayName(cart)
        }
      );
    }

    ctx.inputSpmns = itemsSvc.getItems('specimens');
    dataCtx.noInputSpecimens = !ctx.inputSpmns || ctx.inputSpmns.length == 0;
    itemsSvc.clearItems('specimens', null);

    return { ctx, dataCtx };
  },

  computed: {
    deleteAllowed: function() {
      const { cart } = this.dataCtx;
      return cart.id > 0 && cart.name.indexOf('$$$$user_') != 0 &&
        (cart.owner.id == this.$ui.currentUser.id || this.$ui.currentUser.admin);
    }
  },

  methods: {
    handleInput: function({data}) {
      Object.assign(this.dataCtx, data);
    },

    saveOrUpdate: async function() {
      if (!this.$refs.cartForm.validate()) {
        return;
      }

      let specimens = this.ctx.inputSpmns || [];
      if (specimens.length == 0) {
        const fieldRef = this.$refs.cartForm.getFieldRef('cart.specimenLabels');
        const value    = await fieldRef.getSpecimens();
        specimens = value.specimens || [];
      }

      const toSave = util.clone(this.dataCtx.cart);
      toSave.specimenIds = specimens.map(spmn => spmn.id);
      cartSvc.saveOrUpdate(toSave).then(
        (savedCart) => {
          routerSvc.goto('CartSpecimensList', {cartId: savedCart.id})
          alertSvc.success('Cart ' + savedCart.name + (toSave.id ? ' updated.' : ' created.'));
        }
      );
    },

    deleteCart: async function() {
      const resp = await this.$refs.deleteConfirm.open();
      if (resp != 'proceed') {
        return;
      }

      cartSvc.delete(this.dataCtx.cart).then(
        () => {
          alertSvc.success('Cart ' + this.dataCtx.cart.name + ' deleted.');
          routerSvc.goto('CartSpecimensList', {cartId: -2});
        }
      );
    },

    cancel: function() {
      routerSvc.back();
    }
  }
}
</script>
