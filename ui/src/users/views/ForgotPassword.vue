<template>
  <FormCard ref="form" :schema="ctx.showMessage ? {rows: []} : forgotPasswordSchema" :data="ctx"
    :message="ctx.showMessage">
    <template #box-title>
      <h3>Forgot Password</h3>
    </template>

    <template #message>
      <os-message type="success">
        <span>An email with the link to reset the password has been sent to your registered email address.</span>
      </os-message>
    </template>

    <template #primary-action v-if="!ctx.showMessage">
      <os-button primary label="Email Reset Password Link" @click="emailResetPasswordLink" />
    </template>

    <template #secondary-actions>
      <a :href="signInUrl">Return to Sign In Page</a>
    </template>
  </FormCard>
</template>

<script>
import loginSvc  from '@/common/services/Login.js';
import routerSvc from '@/common/services/Router.js';

import forgotPasswordSchema from '@/users/schemas/forgot-password.js';

import FormCard from './FormCard.vue';

export default {
  components: {
    FormCard
  },

  data() {
    return {
      forgotPasswordSchema: forgotPasswordSchema.layout,

      ctx: {
        userDetail: {}
      }
    };
  },

  computed: {
    signInUrl: function() {
      return routerSvc.getUrl('UserLogin');
    }
  },

  methods: {
    emailResetPasswordLink: function() {
      if (!this.$refs.form.validate()) {
        return;
      }

      loginSvc.forgotPassword(this.ctx.userDetail).then(
        resp => {
          if (resp) {
            this.ctx.showMessage = true;
          }
        }
      );
    }
  }
}
</script>
