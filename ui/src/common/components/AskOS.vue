<template>
  <div class="os-ask-os" v-if="signedIn">
    <os-button type="success" :label="$t('common.buttons.ask_os')" left-icon="comments" @click="askOs" />
  </div>
</template>

<script>
export default {
  computed: {
    signedIn: function() {
      return this.$ui.currentUser && this.$ui.currentUser.id > 0;
    }
  },

  methods: {
    askOs: function() {
      window.open('https://helpdesk.openspecimen.org/', '_blank');
    }
  }
}
</script>

<style scoped>
.os-ask-os button.btn.success {
  font-size: 0.8rem;
  width: 7rem;
}
</style>
