<template>
  <div class="os-root">
    <os-navbar class="no-login-navbar" :no-login="true">
      <template #default="slotProps" v-if="navBarButtons">
        <span v-if="!slotProps.authenticated">
          <os-plugin-views page="no-login" view="navbar" />

          <os-button primary :label="$t('login.sign_up')" @click="navToSignUp()" v-if="allowSignup" />
        </span>
      </template>
    </os-navbar>

    <os-app-body />
  </div>
</template>

<script>
import routerSvc  from '@/common/services/Router.js';

export default {
  name: 'NoLoginApp',

  props: ['navBarButtons'],

  data() {
    return {
      allowSignup: false
    }
  },

  methods: {
    navToSignUp: function() {
      routerSvc.goto('UserSignUp');
    }
  },

  created() {
    this.allowSignup = this.$ui.global.appProps.user_sign_up;
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
