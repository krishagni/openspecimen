<template>
  <div class="os-root">
    <os-navbar class="no-login-navbar" :no-login="true">
      <template #default="slotProps" v-if="navBarButtons">
        <os-button primary :label="$t('login.sign_up')" @click="navToSignUp()" v-if="!slotProps.authenticated" />
      </template>
    </os-navbar>

    <os-app-body />
  </div>
</template>

<script>
import routerSvc from '@/common/services/Router.js';

export default {
  name: 'NoLoginApp',

  props: ['navBarButtons'],

  methods: {
    navToSignUp: function() {
      routerSvc.goto('UserSignUp');
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
  padding: 0.25rem;
  font-weight: bold;
  font-size: 0.8rem;
  width: 7rem;
  border-radius: 0.25rem;
  cursor: pointer;
}
</style>
