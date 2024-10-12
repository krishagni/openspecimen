
<template>
  <div class="os-section os-line-section">
    <span class="title-wrapper">
      <os-icon name="chevron-circle-right" v-show="collapsed" @click="collapsed=!collapsed"></os-icon>
      <os-icon name="chevron-circle-down" v-show="!collapsed" @click="collapsed=!collapsed"></os-icon>
      <strong class="title"><slot name="title"> </slot></strong>
    </span>
    <pre class="content" v-show="!collapsed"><slot name="content"> </slot></pre>
  </div>
</template>

<script>
export default {
  props: ['title', 'content'],

  data() {
    return {
      collapsed: false
    }
  }
}
</script>

<style scoped>

.os-section {
  margin-bottom: 20px;
}

.os-section pre {
  background: transparent;
  border: none;
  font: inherit;
  font-size: inherit;
  padding: 0px;
  margin-bottom: 0px;
}

.os-line-section > .title {
  background: #ffffff;
}

.os-section .title-wrapper {
  display: flex;
  flex-direction: row;
}

.os-section .title-wrapper :deep(.os-icon-wrapper) {
  color: #707070;
  padding-right: 0.25rem;
  cursor: pointer;
}

.os-section .title-wrapper .title {
  margin-top: -1px;
}


</style>
