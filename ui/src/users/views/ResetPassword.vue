<template>
  <FormCard ref="form" :schema="ctx.showMessage ? {rows: []} : resetPasswordSchema" :data="ctx"
    :message="ctx.showMessage">
    <template #box-title>
      <h3>Reset Password</h3>
    </template>

    <template #message>
      <os-message type="success">
        <span>Password reset successful</span>
      </os-message>
    </template>

    <template #primary-action v-if="!ctx.showMessage">
      <os-button primary label="Reset Password" @click="resetPassword" />
    </template>

    <template #secondary-actions>
      <a :href="signInUrl" v-if="ctx.showMessage">Return to Sign In Page</a>
      <a :href="signInUrl" v-else>Cancel</a>
    </template>
  </FormCard>
</template>

<script>
import loginSvc  from '@/common/services/Login.js';
import routerSvc from '@/common/services/Router.js';
import util      from '@/common/services/Util.js';

import resetPasswordSchema from '@/users/schemas/reset-password.js';

import FormCard from './FormCard.vue';

export default {
  props: ['reset-token'],

  components: {
    FormCard
  },

  data() {
    return {
      ctx: {
        userDetail: {}
      }
    };
  },

  mounted() {
    loginSvc.getPasswordSettings().then(passwordSettings => this.ctx.passwordSettings = passwordSettings);
  },

  computed: {
    signInUrl: function() {
      return routerSvc.getUrl('UserLogin');
    },

    resetPasswordSchema: function() {
      const {passwordSettings, showMessage} = this.ctx;
      if (showMessage) {
        return {rows: []};
      }

      const {layout} = resetPasswordSchema;
      if (!passwordSettings || !passwordSettings.pattern) {
        return layout;
      }

      const rows = [];
      for (let row of layout.rows) {
        const fields = [];
        for (let field of row.fields) {
          const newField = util.clone(field);
          if (newField.name == 'userDetail.newPassword') {
            newField.validations.pattern = { expr: passwordSettings.pattern, message: passwordSettings.desc }
          }

          fields.push(newField);
        }

        rows.push({fields});
      }

      return {rows};
    }
  },

  methods: {
    resetPassword: function() {
      if (!this.$refs.form.validate()) {
        return;
      }

      const {loginName, newPassword} = this.ctx.userDetail;
      const detail = {loginName, newPassword, resetPasswordToken: this.resetToken};
      loginSvc.resetPassword(detail).then(
        resp => {
          this.ctx.showMessage = resp;
        }
      );
    }
  }
}
</script>
