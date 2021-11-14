
<template>
  <span class="os-tag">
    <Tag :value="value" :severity="type" :rounded="rounded"></Tag>
  </span>
</template>

<script>
import Tag from 'primevue/tag';

export default {
  props: ['value', 'type', 'rounded'],

  components: {
    Tag
  }
}
</script>
