<template>
  <FormCard ref="loginForm" :schema="loginSchema" :data="ctx" :logo="osLogo">
    <template #title v-if="showHeader">
      <h3 class="header">Sign in to continue to OpenSpecimen</h3>
    </template>

    <template #primary-action>
      <os-button primary label="Sign In" @click="login" />
    </template>
  
    <template #secondary-actions v-if="ctx.forgotPasswordEnabled || ctx.otpAuthEnabled">
      <os-button text label="Forgot Password?" @click="gotoForgotPassword" v-if="ctx.forgotPasswordEnabled" />

      <os-button text label="Reset OTP Secret Code?" @click="gotoResetOtpSecret" v-if="ctx.otpAuthEnabled" />
    </template>
  </FormCard>
</template>

<script>
import loginSvc    from '@/common/services/Login.js';
import loginSchema from '@/users/schemas/login.js';
import osLogo      from '@/assets/images/os_dna_logo.png';
import routerSvc   from '@/common/services/Router.js';

import FormCard    from './FormCard.vue';

export default {
  props: ['show-header'],

  inject: ['ui'],

  components: {
    FormCard
  },

  data() {
    const {global: {appProps}} = this.ui;
    return {
      osLogo,

      loginSchema: loginSchema.layout,

      ctx: {
        loginDetail: {
          domainName: appProps.default_domain
        },

        getDomains: async () => this.domains,

        otpAuthEnabled: false,

        forgotPasswordEnabled: appProps.forgot_password,

        samlDomainSelected: false
      }
    };
  },

  mounted() {
    this._getDomains().then(
      domains => {
        this.domains = domains
        this._toggleSamlDomainSelected();
      }
    );

    if (this.$osSvc.userOtpSvc) {
      this.$osSvc.userOtpSvc.isFeatureEnabled().then(status => this.ctx.otpAuthEnabled = status);
    }
  },

  computed: {
  },

  watch: {
    'ctx.loginDetail.domainName': function() {
      this._toggleSamlDomainSelected();
      if (this.ctx.samlDomainSelected) {
        loginSvc.gotoIdp();
      }
    }
  },

  methods: {
    login: function() {
      if (!this.$refs.loginForm.validate()) {
        return;
      }

      if (this.ctx.samlDomainSelected) {
        loginSvc.gotoIdp();
        return;
      }

      const {loginDetail} = this.ctx;
      loginSvc.login(loginDetail).then(
        () => {
          routerSvc.goto('HomePage');
        }
      );
    },

    gotoForgotPassword: function() {
      routerSvc.goto('UserForgotPassword');
    },

    gotoResetOtpSecret: function() {
      routerSvc.goto('UserResetOtpSecretCode');
    },

    _toggleSamlDomainSelected: function() {
      const {loginDetail: {domainName}} = this.ctx;
      const domain = (this.domains || []).find(d => d.name == domainName);
      this.ctx.samlDomainSelected = (domain && domain.type == 'saml') || false;
    },

    _getDomains: function() {
      return loginSvc.getAuthDomains();
    }
  }
}
</script>
