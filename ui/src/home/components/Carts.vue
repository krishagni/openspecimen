<template>
  <os-home-list-card :icon="'shopping-cart'" :title="$t('common.home.carts')"
    :show-star="true" :list-url="{name: 'SpecimenCartsList', params: {cartId: -1}}" :list="ctx.carts"
    @search="search($event)" @toggle-star="toggleStar($event)" />
</template>

<script>
import cartSvc from '@/biospecimen/services/SpecimenCart.js';
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['widget'],

  data() {
    return {
      ctx: {
        carts: [],

        defList: null,

        search: null
      }
    };
  },

  created() {
    this._loadCarts();
  },

  methods: {
    search: function({searchTerm}) {
      this.ctx.search = searchTerm;
      this._loadCarts(this.ctx.search);
    },

    toggleStar: function(cart) {
      const promise = cart.starred ? cartSvc.unstar(cart) : cartSvc.star(cart);
      promise.then(
        () => {
          this.ctx.defList = null;
          this._loadCarts(this.ctx.search);
        }
      );
    },

    _loadCarts: function(searchTerm) {
      if (!searchTerm && this.ctx.defList) {
        this.ctx.carts = this.ctx.defList;
        return;
      }

      const filterOpts = {includeStats: false, name: searchTerm, orderByStarred: true, maxResults: 25};
      cartSvc.getCarts(filterOpts).then(
        (carts) => {
          this.ctx.carts = carts;
          carts.forEach(
            cart => {
              cart.displayName = cartSvc.getDisplayName(cart)
              cart.url = routerSvc.getUrl('CartSpecimensList', {cartId: cart.id});
            }
          );

          if (!searchTerm) {
            this.ctx.defList = carts;
          }
        }
      );
    }
  }
}
</script>
