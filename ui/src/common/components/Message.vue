
<template>
  <Message class="os-message" :severity="type" :closable="false">
    <slot></slot>
  </Message>
</template>

<script>

import Message from 'primevue/message';

export default {
  props: ['type'],

  components: {
    Message
  }
}
</script>

<style scoped>
.os-message :deep(.p-message-wrapper) {
  white-space: normal;
}

.os-message :deep(.p-message-text) {
  font-weight: normal;
  font-size: 14px;
}
</style>
