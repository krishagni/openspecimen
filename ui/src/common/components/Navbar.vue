
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
        <span> </span>
      </div>

      <div class="buttons">
        <os-new-stuff />

        <os-user-feedback />

        <os-about />

        <div class="notifs">
          <button @click="toggleNotifOverlay">
            <os-icon name="bell" v-os-badge.danger="unreadNotifCount" v-if="unreadNotifCount > 0" />
            <os-icon name="bell" v-else />
          </button>

          <os-overlay class="os-notifs-overlay" ref="notifsOverlay"
            @click="toggleNotifOverlay" @show="notifsDisplayed" @hide="notifsHidden">
            <div class="content">
              <os-user-notifs />
            </div>
          </os-overlay>
        </div>

        <div class="user-profile">
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
                <a v-if="ssoLogout" href="saml/logout">Log Out</a>
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
import notifSvc from '@/common/services/Notif.js';

import NewStuff from '@/common/components/NewStuff';
import Feedback from '@/common/components/Feedback';
import About    from '@/common/components/About';
import NotificationsList from '@/common/components/NotificationsList';

export default {
  components: {
    'os-new-stuff': NewStuff,
    'os-user-feedback': Feedback,
    'os-about': About,
    'os-user-notifs': NotificationsList
  },

  data() {
    return {
      osLogo: osLogo,

      ssoLogout: false,

      unreadNotifCount: 0,
    }
  },

  mounted() {
    let samlEnabled = settingsSvc.getSetting('auth', 'saml_enable');
    let sloEnabled  = settingsSvc.getSetting('auth', 'single_logout');
    Promise.all([samlEnabled, sloEnabled]).then(
      (resp) => {
        this.ssoLogout = (resp[0][0].value == true || resp[0][0].value == 'true') &&
          (resp[1][0].value == true || resp[1][0].value == 'true');
      }
    );

    let self = this;
    let timeout;
    function getUnreadNotifCount() {
      if (timeout) {
        clearTimeout(timeout);
      }

      if (self.notifsOpen) {
        timeout = setTimeout(getUnreadNotifCount, 10000);
        return;
      }

      notifSvc.getUnreadCount().then(
        (result) => {
          self.unreadNotifCount = result.count;
          timeout = setTimeout(getUnreadNotifCount, 10000);
        }
      );
    }

    getUnreadNotifCount();
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
    },

    toggleNotifOverlay: function(event) {
      this.$refs.notifsOverlay.toggle(event);
    },

    notifsDisplayed: function() {
      this.notifsOpen = true;
      if (this.unreadNotifCount > 0) {
        this.notifOpenTime = new Date();
      }
    },

    notifsHidden: function() {
      this.notifsOpen = false;
      if (this.notifOpenTime) {
        notifSvc.markAsRead(this.notifOpenTime).then(() => this.unreadNotifCount = 0);
        this.notifOpenTime = null;
      }
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
  justify-content: right;
  padding: 0.2rem 1rem;
}

.buttons button {
  background: transparent;
  border: none;
  color: #fff;
  font-weight: bold;
  font-size: 1.2rem;
  padding: 0.5rem 1.25rem 0rem 1.25rem;
  cursor: pointer;
}

.help-footer {
  padding: 0.75rem 1rem;
  text-align: center;
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

<style>
.os-notifs-overlay {
  max-height: calc(100% - 100px);
  width: 35%;
  overflow: auto;
}
</style>
