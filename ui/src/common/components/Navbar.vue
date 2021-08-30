
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
        <span> </span>
      </div>
    </div>
  </div>
</template>

<script>

import osLogo from '@/assets/images/os_logo.png';
import http from '@/common/services/HttpClient.js';

export default {
  data() {
    return {
      osLogo: osLogo
    };
  },

  computed: {
    siteAssets: function() {
      return (this.$ui && this.$ui.global && this.$ui.global.siteAssets) || {};
    },

    deployEnv: function() {
      if (this.$ui && this.$ui.global && this.$ui.global.appProps.deploy_env) {
        return this.$ui.global.appProps.deploy_env.toUpperCase();
      }
      return null;
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
}

.os-navbar .items:after {
  content: '';
  display: inline-block;
  clear: both;
}

.os-navbar .items .logo {
  float: left;
  width: 25%;
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
  position: absolute;
  top: 0.5rem;
}

.os-navbar .items .search {
  float: left;
  width: 50%;
}

.os-navbar .items .buttons {
  float: left;
  width: 25%;
}

</style>
