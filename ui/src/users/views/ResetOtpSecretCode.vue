<template>
  <FormCard ref="form" :schema="resetOtpSecretCodeSchema" :data="ctx" :message="ctx.showMessage">
    <template #box-title>
      <h3>Reset OTP Secret Code</h3>
    </template>

    <template #message>
      <os-message type="success" v-if="!token">
        <span>An email with the link to reset OTP secret code has been sent to your registered email address.</span>
      </os-message>

      <os-message type="success" v-else-if="ctx.resetSuccessful">
        <span>Your OTP secret code reset is successful. The new OTP secret code has been sent to your email ID.</span>
      </os-message>

      <os-message type="error" v-else-if="ctx.resetFailed">
        <span>Link to reset OTP secret code is either invalid or may have expired. We'll be happy to send you another link when you're ready.</span>
      </os-message>

      <os-message type="info" v-else>
        <span>Resetting OTP secret code. Please wait for a few moments...</span>
      </os-message>
    </template>

    <template #primary-action v-if="!ctx.showMessage">
      <os-button primary label="Email Reset OTP Secret Code Link" @click="emailResetOtpSecretCodeLink" />
    </template>

    <template #secondary-actions>
      <os-button text label="Return to Sign In Page" @click="gotoSignIn" />
    </template>
  </FormCard>
</template>

<script>
import loginSvc  from '@/common/services/Login.js';
import routerSvc from '@/common/services/Router.js';

import resetOtpSecretCodeSchema from '@/users/schemas/reset-otp-secret-code.js';

import FormCard from './FormCard.vue';

export default {
  props: ['token'],

  components: {
    FormCard
  },

  data() {
    return {
      ctx: {
        userDetail: {},

        showMessage: false,

        resetSuccessful: false,

        resetFailed: false,

        getDomains: async () => this.domains
      }
    };
  },

  mounted() {
    if (this.token) {
      this.ctx.showMessage = true;
      this.ctx.resetSuccessful = this.ctx.resetFailed = false;
      loginSvc.resetOtpSecretCode(this.token)
        .then(() => { this.ctx.resetSuccessful = true; })
        .catch(() => { this.ctx.resetFailed = true; })

      return;
    }
        
   
    this._getDomains().then(domains => { this.domains = domains });
  },

  computed: {
    resetOtpSecretCodeSchema: function() {
      return this.ctx.showMessage ? {rows: []} : resetOtpSecretCodeSchema.layout;
    }
  },

  methods: {
    emailResetOtpSecretCodeLink: function() {
      if (!this.$refs.form.validate()) {
        return;
      }

      loginSvc.generateOtpSecretCodeToken(this.ctx.userDetail).then(() => this.ctx.showMessage = true);
    },

    gotoSignIn: function() {
      routerSvc.goto('UserLogin');
    },

    _getDomains: function() {
      return loginSvc.getAuthDomains();
    }
  }
}
</script>
