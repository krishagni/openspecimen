
<template>
  <Avatar :label="label" :size="size" :shape="shape">
    <slot> </slot>
  </Avatar>
</template>

<script>

import Avatar from 'primevue/avatar';

export default {
  props: ['label', 'size', 'shape'],

  components: {
    Avatar
  }
}

</script>


