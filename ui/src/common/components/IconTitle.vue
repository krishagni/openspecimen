
<template>
  <div class="os-icon-title">
    <os-icon class="icon" :name="icon" />
    <span class="title">{{title}}</span>
  </div>
</template>

<script>

export default {
  props: ['icon', 'title']
}

</script>

<style scoped>

.os-icon-title .icon,
.os-icon-title .title {
  padding: 0rem 0.25rem;
}

</style>
