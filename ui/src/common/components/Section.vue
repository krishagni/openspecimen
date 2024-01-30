
<template>
  <div class="os-section os-line-section">
    <strong class="title"><slot name="title"> </slot></strong>
    <pre class="content"><slot name="content"> </slot></pre>
  </div>
</template>

<script>
export default {
  props: ['title', 'content']
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

</style>
