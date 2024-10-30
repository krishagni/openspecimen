
<template>
  <Panel class="os-list-group-wrapper">
    <template #header>
      <slot name="header"></slot>
    </template>

    <template #actions>
      <slot name="actions"></slot>
    </template>

    <slot name="empty-list" v-if="!list || list.length == 0" />

    <div class="os-list-group" v-else>
      <a v-for="(item, idx) of list" :key="idx"
        class="item" :class="{active: item == selected}"
        @click="$emit('on-item-select', {item: item, index: idx})">
        <slot :item="item" :index="idx"></slot>
      </a>
    </div>
  </Panel>
</template>

<script>

import Panel from '@/common/components/Panel.vue';

export default {
  props: ['list', 'selected'],

  components: {
    Panel
  }
}

</script>

<style scoped>

.os-list-group-wrapper :deep(.p-panel-content) {
  padding: 0rem;
}

.os-list-group {
  list-style: none;
  padding: 0rem;
  margin: 0rem;
}

.os-list-group .item {
  padding: 1rem;
  display: block;
}

.os-list-group .item:not(:first-child) {
  border-top: 1px solid #ddd;
}

.os-list-group .item:hover {
  background: #f7f7f7;
}

.os-list-group .item.active {
  background-color: #337ab7;
  border-color: #337ab7;
  color: #fff;
  z-index: 2;
}

.os-list-group a.item {
  text-decoration: none;
  cursor: pointer;
  color: inherit;
}

.os-list-group .item.active :deep(.os-badge) {
  color: #337ab7;
  background-color: #fff;
}
</style>
