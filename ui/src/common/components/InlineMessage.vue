
<template>
  <div class="os-inline-message">
    <InlineMessage :severity="severity">
      <slot></slot>
    </InlineMessage>
  </div>
</template>

<script>

import InlineMessage from 'primevue/inlinemessage';

export default {
  props: ['severity'],

  components: {
    InlineMessage
  }
}

</script>

<style scoped>
.os-inline-message {
  padding: 0.5rem 0rem;
}

.os-inline-message :deep(.p-inline-message) {
  width: 100%;
  justify-content: start;
}
</style>
