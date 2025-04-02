<template>
  <div class="login-container">
    <div class="title">
      <h3>Sign in to continue to OpenSpecimen</h3>
    </div>

    <div class="login-box">
      <img class="logo" :src="osLogo" />

      <os-form class="login-form" ref="loginForm" :schema="loginSchema" :data="ctx">
        <os-button class="sign-in-btn" primary label="Sign In" @click="login" />
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
  data() {
    return {
      osLogo,

      loginSchema: loginSchema.layout,

      ctx: {
        loginDetail: {},

        getDomains: async () => this.domains
      }
    };
  },

  mounted() {
    this._getDomains().then(domains => this.domains = domains);
  },

  computed: {
  },

  watch: {
    'ctx.loginDetail.domainName': function(newVal) {
      const domain = this.domains.find(d => d.name == newVal);
      if (domain && domain.type == 'saml') {
        loginSvc.gotoIdp();
      }
    }
  },

  methods: {
    login: function() {
      if (!this.$refs.loginForm.validate()) {
        return;
      }

      const {loginDetail} = this.ctx;
      loginSvc.login(loginDetail).then(
        () => {
          routerSvc.goto('HomePage');
        }
      );
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
  width: 100%;
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
</style>
