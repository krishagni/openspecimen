
<template>
  <div header="header" name="name">
    <slot></slot>
  </div>
</template>

<script>

export default {
  props: ['header', 'name']
}

</script>
