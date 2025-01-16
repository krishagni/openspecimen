
<template>
  <div ref="menu" class="os-tab-menu">
    <slot></slot>
  </div>
</template>

<style scoped>
.os-tab-menu :deep(ul) {
  list-style: none;
  margin: 0px;
  padding: 0px;
  width: 100%;
  margin-bottom: 10px;
  border-bottom: 1px solid #ddd;
}

.os-tab-menu :deep(ul li) {
  display: inline-block;
  padding: 0.75rem 1rem;
}

.os-tab-menu :deep(ul li:has(> a.router-link-active)) {
  border-bottom: 4px solid #428bca;
}

.os-tab-menu :deep(ul li a) {
  color: initial;
  text-decoration: none;
  padding: 0.75rem 1rem;
}

.os-tab-menu :deep(ul li a.router-link-active) {
  color: #428bca;
}

.os-tab-menu + :deep(div) {
  height: calc(100% - 45px);
}
</style>
