<template>
  <div>
    <button @click="toggleFavoritesMenu">
      <os-icon name="heart" style="color: orangered;" v-if="currentPageFavorite" />
      <os-icon name="heart" v-else />
    </button>

    <os-overlay ref="favoritesMenu" @click="toggleFavoritesMenu">
      <div class="os-favorite-options">
        <ul class="os-addrm-favorite-link">
          <li v-if="!currentPageFavorite">
            <a @click="showAddFavoriteForm">
              <span v-t="'common.home.add_to_favorites_tooltip'">Add this page to Favorites</span>
            </a>
          </li>
          <li v-else>
            <a @click="confirmRemoveFavorite">
              <span v-t="'common.home.rm_from_favorites_tooltip'">Remove this page to Favorites</span>
            </a>
          </li>
          <li class="divider" v-if="favorites && favorites.length > 0"> </li>
        </ul>

        <ul class="os-favorite-links" v-if="favorites && favorites.length > 0">
          <li v-for="favorite of favorites" :key="favorite.id">
            <a :href="favorite.viewUrl">
              <span>{{favorite.title}}</span>
            </a>
          </li>
        </ul>
      </div>
    </os-overlay>

    <os-dialog ref="addDialog">
      <template #header>
        <span v-t="'common.home.add_to_favorites'">Add to Favorites</span>
      </template>
      <template #content>
        <os-form ref="form" :schema="formSchema" :data="favorite">
          <div>
            <os-button primary :label="$t('common.buttons.add')" @click="add()" />
            <os-button text :label="$t('common.buttons.cancel')" @click="cancel()" />
          </div>
        </os-form>
      </template>
    </os-dialog>

    <os-confirm-delete ref="deleteDialog" :captcha="false" :collect-reason="false">
      <template #message>
        <span v-t="'common.home.rm_from_favorites'"></span>
      </template>
    </os-confirm-delete>
  </div>
</template>

<script>

import homePageSvc from '@/common/services/HomePageService.js';
import routerSvc from '@/common/services/Router.js';

export default {
  data() {
    return {
      currentPageFavorite: false,

      favorite: {},

      favorites: [],

      formSchema: {
        rows: [
          {
            fields: [
              {
                type: "text",
                labelCode: "common.home.favorite_title",
                name: "title",
                validations: {
                  required: {
                    messageCode: "common.home.favorite_title_req"
                  }
                }
              }
            ]
          }
        ]
      }
    };
  },

  created() {
    this._setHeartOnFireIfCurrentPageFavorite();
    homePageSvc.registerFavoritesListener(() => this._setHeartOnFireIfCurrentPageFavorite());
  },

  watch: {
    '$route': function(newVal, oldVal) {
      if (newVal.href != oldVal.href) {
        this._setHeartOnFireIfCurrentPageFavorite();
      }
    }
  },

  methods: {
    toggleFavoritesMenu: function(event) {
      if (!this.favorites || this.favorites.length == 0) {
        this.showAddFavoriteForm();
        return;
      }

      this.$refs.favoritesMenu.toggle(event);
    },

    showAddFavoriteForm: function() {
      this.favorite = {oldView: false};
      this.$refs.addDialog.open();
    },

    add: async function() {
      if (!this.$refs.form.validate()) {
        return;
      }

      this.favorite.viewUrl = routerSvc.getCurrentRoute().href;
      homePageSvc.addFavorite(this.favorite).then(() => this.cancel());
    },

    cancel: function() {
      this.$refs.addDialog.close();
    },

    confirmRemoveFavorite: function() {
      this.$refs.deleteDialog.open().then(
        async (resp) => {
          if (resp != 'proceed') {
            return;
          }

          const href = routerSvc.getCurrentRoute().href;
          const favorite = this.favorites.find(({viewUrl}) => viewUrl == href);
          if (favorite) {
            homePageSvc.deleteFavorite(favorite.id);
          }
        }
      );
    },

    _setHeartOnFireIfCurrentPageFavorite: async function() {
      this.currentPageFavorite = false;

      const href = routerSvc.getCurrentRoute().href;
      const favorites = this.favorites = await homePageSvc.getFavorites();
      for (let favorite of favorites) {
        if (favorite.viewUrl == href && !favorite.oldView) {
          this.currentPageFavorite = true;
          break;
        }
      }
    }
  }
}
</script>

<style scoped>
.os-favorite-options {
  margin: -1.25rem;
}

ul {
  margin: 0rem;
  list-style: none;
  padding: 0rem;
  max-height: 80vh;
  max-width: 350px;
  overflow-y: auto;
}     
        
ul li a {
  display: inline-block;
  padding: 0.75rem 1rem;
  transition: box-shadow 0.15s;
  text-decoration: none;
  color: inherit;
  width: 100%;
}       
          
ul li:not(.divider):hover {
  background: #e9ecef;
}   
          
ul li.divider {
  background: #fff;
  border-bottom: 1px solid #ddd;
}     
</style>
