<template>
  <div class="os-root">
    <os-navbar class="no-login-navbar" :no-login="true" @single-logout="logout">
      <template #default="slotProps" v-if="navBarButtons">
        <span v-if="!slotProps.authenticated">
          <os-plugin-views page="no-login" view="navbar" />

          <os-button primary :label="$t('login.sign_up')" @click="navToSignUp()" v-if="allowSignup" />
        </span>
      </template>
    </os-navbar>

    <os-app-body />

    <iframe :src="logoutUrl" style="height: 0; width: 0; display:none;" v-if="logoutUrl"></iframe>
  </div>
</template>

<script>
import routerSvc  from '@/common/services/Router.js';

export default {
  name: 'NoLoginApp',

  props: ['navBarButtons'],

  data() {
    return {
      allowSignup: false,

      logoutUrl: null
    }
  },

  created() {
    this.allowSignup = this.$ui.global.appProps.user_sign_up;
    if (window.osLogoutUrl) {
      this._setLogoutUrl(window.osLogoutUrl);
    }
  },

  unmounted() {
    //
    // Look for _setLogoutUrl method
    // we remove the timers when the view is unmounted / destroyed
    // for example when users move from NoLoginApp (core) to NoLoginApp (catalog view)
    //
    if (this.timers && this.timers.length > 0) {
      this.timers.forEach(timer => clearTimeout(timer));
      this.timers = [];
    }
  },

  methods: {
    navToSignUp: function() {
      routerSvc.goto('UserSignUp');
    },

    logout: function(logoutUrl) {
      this._setLogoutUrl(logoutUrl);
    },

    _setLogoutUrl: function(logoutUrl) {
      window.osLogoutUrl = logoutUrl;

      const timers = this.timers = [];
      timers.push(setTimeout(
        () => {
          this.logoutUrl = logoutUrl;
          timers.push(this._clearLogoutUrl());
        },
        500 /* This timer allows the UI to transition from App or NoLoginApp view, if any */
      ));
    },

    _clearLogoutUrl: function() {
      /* This allows to remove the iframe from the view, if any */
      return setTimeout(() => window.osLogoutUrl = this.logoutUrl = null, 10000);
    }
  }
}
</script>

<style scoped>
.os-root {
  height: 100%;
}

.no-login-navbar :deep(button.btn) {
  border: none;
  color: #fff;
  padding: 0.25rem 0.5rem;
  font-weight: bold;
  font-size: 0.8rem;
  min-width: 7rem;
  border-radius: 0.25rem;
  cursor: pointer;
  height: 1.75rem;
  border-radius: 1rem;
}
</style>
