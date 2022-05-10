
<template>
  <os-button-group>
    <os-button left-icon="cart-plus" label="Add to Cart" @click="addToMyDefaultCart" />
    <os-button left-icon="caret-down" @click="toggleCartOptions" />
  </os-button-group>

  <os-overlay class="os-carts" ref="cartOptions" style="margin-left: -112px;">
    <ul>
      <li>
        <os-input-text v-model="searchTerm" @update:modelValue="searchCarts" />
      </li>
    </ul>
    <ul class="carts">
      <li v-for="(cart, idx) of carts" :key="idx" @click="addToCart($event, cart)">
        <a> {{cart.displayName}} </a>
      </li>

      <li v-if="carts.length == 0">
        <a class="no-click"> No carts to show </a>
      </li>
    </ul>
    <ul class="actions">
      <li> <a @click="viewDefaultCart" > View My Default Cart </a> </li>
      <li> <a @click="createNewCart"> Create New </a></li>
      <li> <a @click="viewCarts" v-if="carts.length > 0"> Manage Carts </a> </li>
    </ul>
  </os-overlay>
</template>

<script>

import alertsSvc from '@/common/services/Alerts.js';
import http from '@/common/services/HttpClient.js';
import itemsSvc from '@/common/services/ItemsHolder.js';
import routerSvc from '@/common/services/Router.js';
import cartsSvc from '@/biospecimen/services/SpecimenCart.js';

export default {
  props: ['specimens'],

  data() {
    return {
      searchTerm: '',

      carts: []
    }
  },

  methods: {
    toggleCartOptions: function(event) {
      this.$refs.cartOptions.toggle(event);
      if (this.cartsLoaded) {
        return;
      }

      this.cartsLoaded = true;
      this.getCarts().then(carts => this.carts = this.defCarts = carts);
    },

    searchCarts: function(searchTerm) {
      if (this.timer) {
        clearTimeout(this.timer);
        this.timer = null;
      }

      if (!searchTerm) {
        if (this.defCarts) {
          this.carts = this.defCarts;
        }
      } else {
        this.timer = setTimeout(
          () => {
            if (this.defCarts && this.defCarts.length < 100) {
              searchTerm = searchTerm.toLowerCase();
              this.carts = this.defCarts.filter(cart => cart.name.toLowerCase().indexOf(searchTerm) != -1);
            } else {
              this.getCarts(searchTerm).then(carts => this.carts = carts);
            }
          },
          500
        );
      }
    },

    getCarts: function(searchTerm) {
      return http.get('specimen-lists', {name: searchTerm}).then(
        (carts) => {
          const myDefCart = '$$$$user_' + this.$ui.currentUser.id;
          carts = carts.filter(cart => cart.name != myDefCart)
          carts.forEach(cart => cart.displayName = this.getDisplayName(cart));
          return carts;
        }
      );
    },

    getDisplayName: function(cart) {
      let displayName = cart.name;
      if (cart.name.indexOf('$$$$user_') == 0) {
        displayName = cart.owner.firstName || '';
        if (displayName) {
          displayName += ' ';
        }

        displayName += cart.owner.lastName + '\'s Default Cart';
      }

      return displayName;
    },

    addToMyDefaultCart: function() {
      const cart = {
        id: 0,
        name: '$$$$user_' + this.$ui.currentUser.id,
        owner: this.$ui.currentUser
      };

      cart.displayName = this.getDisplayName(cart);
      this.addToCart(null, cart);
    },

    addToCart: function(event, cart) {
      if (event) {
        this.$refs.cartOptions.toggle(event);
      }

      if (!this.specimens || this.specimens.length == 0) {
        alertsSvc.error('Select at least one specimen to add to cart');
        return;
      }

      cartsSvc.addToCart(cart, this.specimens).then(
        ({count}) => {
          if (count == 0) {
            alertsSvc.info('No specimens added to the cart: ' + cart.displayName);
          } else if (count == 1) {
            alertsSvc.success('One specimen added to the cart: ' + cart.displayName);
          } else {
            alertsSvc.success(count + ' specimens added to the cart: ' + cart.displayName);
          }
        }
      );
    },

    viewDefaultCart: function() {
      routerSvc.ngGoto('specimen-lists/0/specimens');
    },

    createNewCart: function(event) {
      this.$refs.cartOptions.toggle(event);
      if (!this.specimens || this.specimens.length == 0) {
        alertsSvc.error('Select at least one specimen to add to cart');
        return;
      }

      itemsSvc.ngSetItems('specimens', this.specimens);
      routerSvc.ngGoto('specimen-lists/-1/addedit');
    },

    viewCarts: function() {
      routerSvc.ngGoto('specimen-lists');
    }
  }
}
</script>

<style scoped>
.os-carts .os-input-text {
  padding: 0.5rem 1rem;
}

.os-carts ul {
  margin: 0.5rem -1.25rem;
  padding: 0;
  list-style: none;
  border-bottom: 1px solid #ddd;
}

.os-carts ul.carts {
  max-height: 200px;
  overflow: scroll;
}

.os-carts ul:last-child {
  margin-bottom: 0rem;
  border-bottom: 0px;
}

.os-carts ul li a {
  padding: 0.5rem 1rem;
  text-decoration: none;
  display: inline-block;
  width: 100%;
  color: #212529;
}

.os-carts ul li a:hover:not(.no-click) {
  background: #e9ecef;
}

.os-carts ul li a.no-click {
  cursor: initial;
}
</style>

<style>
.os-carts .p-overlaypanel-content {
  padding: 0.5rem 1.25rem;
}
</style>
