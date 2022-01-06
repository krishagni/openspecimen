
<template>
  <div class="os-navbar">
    <div class="items">
      <div class="logo">
        <a href="http://www.openspecimen.org" target="_blank" relopener="noopener">
          <img :src="osLogo">
        </a>
        <a class="deploy-logo" :href="deploySiteUrl" v-if="deploySiteLogo" target="_blank" rel="noopener">
          <img :src="deploySiteLogo">
        </a>
        <div class="deploy-env">
          <span>{{deployEnv}}</span>
        </div>
      </div>

      <div class="search">
        <os-search />
      </div>

      <div class="buttons">
        <os-new-stuff />

        <os-user-feedback />

        <os-about />

        <os-notifs-overlay />

        <div class="user-profile" v-os-tooltip.bottom="'User Profile'">
          <button @click="toggleProfileMenu">
            <os-username-avatar :name="username" />
          </button>

          <os-overlay ref="userProfileMenu" @click="toggleProfileMenu">
            <ul class="user-profile-options">
              <li>
                <router-link :to="{name: 'UserOverview', params: {userId: $ui.currentUser.id}}">
                  <span>{{username}}</span>
                </router-link>
              </li>
              <li class="divider">
                <os-divider />
              </li>
              <li>
                <a v-if="ssoLogout" :href="ssoLogoutUrl">Log Out</a>
                <a v-else :href="$ui.ngServer + '#/?logout=true'">Log Out</a>
              </li>
            </ul>
          </os-overlay>
        </div>
      </div>
    </div>
  </div>
</template>

<script>

import osLogo from '@/assets/images/os_logo.png';
import http from '@/common/services/HttpClient.js';
import settingsSvc from '@/common/services/Setting.js';
import util from '@/common/services/Util.js';

import Search        from '@/common/components/Search';
import NewStuff      from '@/common/components/NewStuff';
import Feedback      from '@/common/components/Feedback';
import About         from '@/common/components/About';
import NotifsOverlay from '@/common/components/NotifsOverlay';

export default {
  components: {
    'os-search': Search,
    'os-new-stuff': NewStuff,
    'os-user-feedback': Feedback,
    'os-about': About,
    'os-notifs-overlay': NotifsOverlay
  },

  data() {
    return {
      osLogo: osLogo,

      ssoLogout: false,

      ssoLogoutUrl: ''
    }
  },

  async created() {
    const samlEnabled = await settingsSvc.getSetting('auth', 'saml_enable');
    const sloEnabled  = await settingsSvc.getSetting('auth', 'single_logout');
    this.ssoLogout = util.isTrue(samlEnabled[0].value) && util.isTrue(sloEnabled[0].value);
    if (this.ssoLogout) {
      const appUrl = await settingsSvc.getSetting('common', 'app_url');
      this.ssoLogoutUrl = appUrl[0].value;
      if (!this.ssoLogoutUrl.endsWith("/")) {
        this.ssoLogoutUrl += '/';
      }

      this.ssoLogoutUrl += 'saml/logout';
    }
  },

  computed: {
    siteAssets: function() {
      return (this.$ui && this.$ui.global && this.$ui.global.siteAssets) || {};
    },

    deployEnv: function() {
      if (this.$ui && this.$ui.global && this.$ui.global.appProps.deploy_env) {
        return this.$ui.global.appProps.deploy_env.toUpperCase();
      }
      return 'UNKNOWN';
    },

    deploySiteUrl: function() {
      return this.siteAssets.siteUrl || '';
    },

    deploySiteLogo: function() {
      let logoUrl = this.siteAssets.siteLogo || '';
      if (logoUrl && logoUrl.length > 9) {
        logoUrl = http.getUrl(logoUrl.substring(9));
      }

      return logoUrl;
    },

    username: function() {
      return this.$filters.username(this.$ui.currentUser);
    }
  },

  methods: {
    toggleProfileMenu: function(event) {
      this.$refs.userProfileMenu.toggle(event);
    }
  }
}
</script>

<style scoped>

.os-navbar {
  display: block;
  height: 40px;
  width: 100%;
  background: #205081;
  border-bottom: 1px solid #2e3d54;
  color: #fff;
  font-weight: bold;
}

.os-navbar .items {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: row;
}

.os-navbar .items .logo {
  display: flex;
  flex: 1;
  flex-direction: row;
  padding: 0.15rem;
}

.os-navbar .items .logo img {
  height: 2rem;
}

.os-navbar .items .logo .deploy-logo {
  margin-left: 0.4rem;
}

.os-navbar .items .logo .deploy-env {
  font-size: 0.6rem;
  color: #fff;
  background-color: #c33;
  padding: 0.125rem 0.25rem;
  border-radius: 0.25rem;
  display: inline-block;
  height: 1.125rem;
  margin-left: 0.4rem;
  margin-top: 0.2rem;
}

.os-navbar .items .search {
  display: flex;
  flex: 2;
}

.os-navbar .items .buttons {
  display: flex;
  flex: 1;
  justify-content: flex-end;
  align-items: center;
  margin-right: 0.67rem;
}

.buttons :deep(button) {
  background: transparent;
  border: none;
  color: #fff;
  font-weight: bold;
  font-size: 1.2rem;
  cursor: pointer;
  margin: 0rem 0.5rem;
}

.user-profile {
  display: inline-block;
}

.user-profile button {
  padding-top: 0rem;
}

.user-profile-options {
  margin: -1.25rem;
  list-style: none;
  padding: 0.5rem 0rem;
}

.user-profile-options li a {
  display: inline-block;
  padding: 0.75rem 1rem;
  transition: box-shadow 0.15s;
  text-decoration: none;
  color: inherit;
  width: 100%;
}

.user-profile-options li:not(.divider):hover {
  background: #e9ecef;
}

.user-profile-options li.divider {
  padding: 0.25rem 0rem;
  margin: -1rem 0rem;
}

</style>
