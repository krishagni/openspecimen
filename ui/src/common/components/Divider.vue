
<template>
  <Divider :layout="layout" :align="align" :type="type">
    <slot></slot>
  </Divider>
</template>

<script>

import Divider from 'primevue/divider';

export default {
  props: ['layout', 'align', 'type'],

  components: {
    Divider
  }
}

</script>
