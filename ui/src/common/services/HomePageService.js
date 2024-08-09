
import authSvc from '@/common/services/Authorization.js';
import http from '@/common/services/HttpClient.js';

class HomePage {
  pageCards = [];

  sorted = false;

  widgetRules = {};

  favorites = null;

  favoritesListeners = [];

  registerCards(cards) {
    cards.forEach(card => this.registerCard(card));
  }

  registerCard(card) {
    this.pageCards.push(card);
    this.sorted = false;
  }

  getCards() {
    if (this.hidePageCards) {
      return [];
    }

    if (this.sorted || this.pageCards.length == 0) {
      return this.pageCards;
    }
    
    this.pageCards.sort((c1, c2) => c1.title.localeCompare(c2.title));
    this.sorted = true;
    return this.pageCards;
  }

  hideCards() {
    this.hidePageCards = true;
  }

  showCards() {
    this.hidePageCards = false;
  }

  registerWidget(name, showIf) {
    this.widgetRules[name] = showIf;
  }

  registerWidgets(widgets) {
    for (let {name, showIf} of widgets || []) {
      this.widgetRules[name] = showIf;
    }
  }

  getWidgets() {
    const result = [];
    for (let name of Object.keys(this.widgetRules)) {
      if (authSvc.isAllowed(this.widgetRules[name])) {
        result.push(name);
      }
    }

    return result;
  }

  getAllWidgets() {
    return Object.keys(this.widgetRules);
  }

  saveUserWidgets(widgets) {
    return http.put('users/current-user-ui-state', {widgets});
  }

  async getFavorites() {
    if (this.favorites) {
      return this.favorites;
    }

    return http.get('user-favorites').then(favorites => this._setFavorites(favorites));
  }

  addFavorite(favorite) {
    return http.post('user-favorites', favorite).then(favorites => this._setFavorites(favorites));
  }

  deleteFavorite(favoriteId) {
    return http.delete('user-favorites/' + favoriteId).then(favorites => this._setFavorites(favorites));
  }

  registerFavoritesListener(callback) {
    if (callback) {
      this.favoritesListeners.push(callback);
    }
  }

  _setFavorites(favorites) {
    if (!this.favorites) {
      this.favorites = [];
    }

    this.favorites.length = 0;
    Array.prototype.push.apply(this.favorites, favorites);

    for (let listener of this.favoritesListeners) {
      try {
        listener(this.favorites);
      } catch (e) {
        console.log(e);
      }
    }

    return this.favorites;
  }
}

export default new HomePage();
