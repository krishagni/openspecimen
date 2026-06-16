<template>
  <FormCard ref="form" :schema="{rows: []}" :data="{}" :message="true">
    <template #box-title>
      <h3>Login Error</h3>
    </template>

    <template #message>
      <os-message type="error">
        <span>{{message}}</span>
      </os-message>
    </template>

    <template #secondary-actions>
      <a :href="signInUrl">Return to Sign In Page</a>
    </template>
  </FormCard>
</template>

<script>
import FormCard from './FormCard.vue';

import routerSvc from '@/common/services/Router.js';

export default {
  props: ['message'],

  components: {
    FormCard
  },

  data() {
    return {
    };
  },

  computed: {
    signInUrl: function() {
      return routerSvc.getUrl('UserLogin');
    }
  }
}
</script>
