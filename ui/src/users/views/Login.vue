<template>
  <div class="login-container">
    <div class="title">
      <h3>Sign in to continue to OpenSpecimen</h3>
    </div>

    <div class="login-box">
      <img class="logo" :src="osLogo" />

      <os-form class="login-form" ref="loginForm" :schema="loginSchema" :data="ctx">
        <div class="actions">
          <div class="primary">
            <os-button class="sign-in-btn" primary label="Sign In" @click="login" />
          </div>

          <div class="secondary" v-if="ctx.forgotPasswordEnabled || ctx.otpAuthEnabled">
            <os-button text label="Forgot Password?" @click="gotoForgotPassword" v-if="ctx.forgotPasswordEnabled" />

            <os-button text label="Reset OTP Secret Code?" @click="gotoResetOtpSecret" v-if="ctx.otpAuthEnabled" />
          </div>
        </div>
      </os-form>
    </div>
  </div>
</template>

<script>
import loginSvc    from '@/common/services/Login.js';
import loginSchema from '@/users/schemas/login.js';
import osLogo      from '@/assets/images/os_dna_logo.png';
import routerSvc   from '@/common/services/Router.js';

export default {
  inject: ['ui'],

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

<style scoped>
.login-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #205081, #2d67a3);
}

.login-box {
  padding: 0.5rem;
  background: white;
  border-radius: 0.5rem;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
  min-width: 420px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.title h3 {
  color: #fff;
  font-size: 24px;
  font-weight: 500;
  line-height: 1.1;
  margin-top: 1.25rem;
}

.login-form {
  width: 90%;
  max-width: 90%;
}

.login-form :deep(.row:last-child) {
  margin-top: 1rem;
}

.login-form :deep(.row .field) {
  padding: 0.5rem 0rem;
}

.login-form :deep(.p-float-label label) {
  line-height: 1.1
}

.login-form :deep(.p-divider) {
  display: none;
}

.login-form :deep(.os-input-text .p-float-label .p-inputtext),
.login-form :deep(.os-input-password .p-float-label .p-inputtext),
.login-form :deep(.os-dropdown .p-float-label .p-dropdown) {
  border: 1px solid #1f1f1f;
  border-radius: 4px;
  padding: 13px 15px;
}

.login-form :deep(.os-input-text .p-float-label label),
.login-form :deep(.os-input-password .p-float-label label),
.login-form :deep(.os-dropdown .p-float-label label) {
  padding: 0px 8px;
  left: 8px;
  color: #444746;
}

.login-form :deep(.os-input-text .p-float-label .p-inputtext:not(:enabled:focus) ~ label),
.login-form :deep(.os-dropdown .p-float-label .p-dropdown ~ label) {
  color: #444746;
}

.login-form :deep(.os-input-text .p-float-label .p-inputtext:enabled:focus ~ label),
.login-form :deep(.os-input-password .p-float-label .p-password.p-inputwrapper-focus ~ label),
.login-form :deep(.os-dropdown .p-float-label .p-dropdown.p-inputwrapper-focus ~ label) {
  color: #007bff;
}

.login-form :deep(.os-dropdown .p-float-label .p-dropdown.p-inputwrapper-filled .p-inputtext) {
  padding: 0;
}

.login-form :deep(.p-float-label:has(input:focus) label), 
.login-form :deep(.p-float-label:has(input.p-filled) label),
.login-form :deep(.p-float-label:has(input:-webkit-autofill) label),
.login-form :deep(.p-float-label:has(textarea:focus) label),
.login-form :deep(.p-float-label:has(textarea.p-filled) label),
.login-form :deep(.p-float-label:has(.p-inputwrapper-focus) label),
.login-form :deep(.p-float-label:has(.p-inputwrapper-filled) label) {
  top: 2px;
  z-index: 1;
  background: #fff;
  width: max-content;
}

.login-form :deep(.p-dropdown-trigger) {
  width: auto;
}

.login-form :deep(.os-dropdown .p-float-label .p-dropdown.p-inputwrapper-focus),
.login-form :deep(.os-input-text .p-float-label .p-inputtext:enabled:focus),
.login-form :deep(.os-input-password .p-float-label .p-inputtext:enabled:focus) {
  border: 1px solid #007bff;
}

.logo {
  height: 100px;
  width: 100px;
  border-radius: 50%;
}

.login-form .actions {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.login-form .actions > .primary {
  width: 100%;
  margin-bottom: 1rem;
}

.login-form .actions > .primary .btn {
  width: 100%;
}

.login-form .actions > .secondary {
  display: flex;
  justify-content: center;
  align-items: center;
}

.login-form .actions > .secondary .btn:last-child {
  margin-right: 0;
}
</style>
