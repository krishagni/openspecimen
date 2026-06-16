<template>
  <FormCard class="sign-up-form" ref="form" :schema="signUpSchema" :data="ctx" :message="true">
    <template #box-title>
      <h3>Create Your OpenSpecimen Account</h3>
    </template>

    <template #message>
      <os-message type="success" v-if="ctx.showMessage">
        <span v-if="ctx.approved">
          Your membership request has been approved. Click the "Sign In" button in the email sent to you to log in.
        </span>
        <span v-else>
          Your membership request has been received. Please wait for an approval email from OpenSpecimen Administrator.
        </span>
      </os-message>
      <span class="sign-in-message" v-else>
        Already have an OpenSpecimen ID? <a :href="signInUrl">Sign In here</a>
      </span>
    </template>

    <template #primary-action v-if="!ctx.showMessage">
      <os-button primary label="Sign Up" @click="signUp" />
    </template>

    <template #secondary-actions v-else>
      <a class="primary-link" :href="signInUrl">Sign In</a>
    </template>
  </FormCard>
</template>

<script>
import loginSvc  from '@/common/services/Login.js';
import routerSvc from '@/common/services/Router.js';

import signUpSchema from '@/users/schemas/sign-up.js';

import FormCard from './FormCard.vue';

export default {
  components: {
    FormCard
  },

  data() {
    return {
      ctx: {
        userDetail: {},

        getDomains: async () => this.domains
      }
    };
  },

  mounted() {
    this._getDomains().then(domains => this.domains = domains.filter(domain => domain.allowLogins));
  },

  computed: {
    signInUrl: function() {
      return routerSvc.getUrl('UserLogin');
    },

    signUpSchema: function() {
      return this.ctx.showMessage ? {rows: []} : signUpSchema.layout;
    }
  },

  methods: {
    signUp: function() {
      if (!this.$refs.form.validate()) {
        return;
      }

      loginSvc.signUp(this.ctx.userDetail).then(
        user => {
          this.ctx.showMessage = true;
          this.ctx.approved = (user.activityStatus == 'Active');
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
.sign-up-form :deep(.login-box) {
  width: 600px;
}

.sign-up-form :deep(.box-title) {
  text-align: center;
  border-bottom: 1px solid #ddd;
}

.sign-up-form :deep(.box-title h3) {
  color: #666;
  font-size: 1.5rem;
  margin: 0.5rem auto;
}

.sign-in-message {
  display: inline-block;
  text-align: center;
  width: 100%;
  margin: 0.25rem;
}

.primary-link {
  color: #fff;
  background-color: #337ab7;
  border: 1px solid #2e6da4;
  border-radius: 1.125rem;
  display: inline-block;
  height: 2.25rem;
  padding: 7px 16px;
  text-decoration: none;
}

.primary-link:hover,
.primary-link:focus {
  color: #fff;
  background-color: #286090;
  border-color: #204d74;
  text-decoration: none;
}
</style>
