
<template>
  <div class="os-side-menu">
    <div class="menu-items">
      <slot></slot>
    </div>
  </div>
</template>

<script>
export default {
  setup() {
    return { }
  }
}
</script>

<style scoped>
.os-side-menu {
  position: absolute;
  top: 0px;
  bottom: 0px;
  left: 0px;
  width: 54px;
  border-right: 1px solid #ddd;
  transition: width 0.3s ease;
  background: #fff;
  z-index: 1000;
}

.os-side-menu:hover {
  width: 225px;
}

.os-side-menu :deep(ul) {
  list-type: none;
  margin: 0px;
  padding: 0px;
}

.os-side-menu :deep(ul li) {
  position: relative;
  border-top: 1px solid #ddd;
}

.os-side-menu :deep(ul li:first-child) {
  border-top: none;
}

.os-side-menu :deep(ul li:last-child) {
  border-bottom: 1px solid #fff;
}

.os-side-menu :deep(ul li:after) {
  border-top: 1px solid #ddd;
  content: '';
  display: block;
}

.os-side-menu :deep(ul li a) {
  display: flex;
  color: #232323;
  height: 3rem;
  width: 100%;
  padding: 15px 5px;
  cursor: pointer;
  text-decoration: none;
  transition: background-color 0.3s ease, color 0.3s ease;
}

.os-side-menu :deep(ul li a .os-icon-wrapper) {
  width: 40px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.os-side-menu :deep(ul li a:hover) {
  background: #e0e0e0;
}

.os-side-menu :deep(ul li a.router-link-active) {
  color: #428bca;
}

.os-side-menu :deep(ul li a.router-link-active:before) {
  content: ' ';
  display: block;
  position: absolute;
  border-left: 4px solid #428bca;
  top: 0px;
  bottom: 1px;
  left: 0px;
}

.os-side-menu :deep(ul li:first-child a.router-link-active:before) {
  top: 2px;
}

.os-side-menu:hover :deep(ul li a .os-icon-wrapper) {
  margin-right: 1rem;
}

.os-side-menu :deep(ul li a .label) {
  white-space: nowrap;
  display: none;
  transition: visibility 0s, opacity 0.3s ease;
}

.os-side-menu:hover :deep(ul li a .label) {
  display: inline;
}
</style>
