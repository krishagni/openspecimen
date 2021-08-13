
<template>
  <div class="os-section os-line-section">
    <strong class="title">{{title}}</strong>
    <pre class="content">{{content}}</pre>
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

.os-line-section > .title:after {
  content: '';
  display: block;
  margin-top: -10px;
  margin-bottom: 20px;
  border: 0;
  border-top: 1px solid ;
  border-color: #E6E6E6;
}

.os-line-section > .title {
  background: #ffffff;
}

</style>
