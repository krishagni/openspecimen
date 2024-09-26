<template>
  <OrderList class="os-order-list" v-model="inputValue" :dataKey="itemKey">
    <template #item="{item}">
      <slot name="item" :item="item" />
    </template>
  </OrderList>
</template>

<script>

import OrderList from 'primevue/orderlist';

export default {
  props: ['modelValue', 'item-key'],

  components: {
    OrderList
  },

  computed: {
    inputValue: {
      get() {
        return this.modelValue;
      },

      set(value) {
        this.$emit('update:modelValue', value);
      }
    }
  }
}
</script>

<style scoped>
.os-order-list :deep(.p-orderlist-item.p-highlight) {
  background-color: #337ab7;
  border-color: #337ab7;
  color: #fff;
}

.os-order-list :deep(.p-orderlist-controls .p-button) {
  background-color: #337ab7;
  border-color: #2e6da4;
  color: #fff;
}

.os-order-list :deep(.p-orderlist-list .p-orderlist-item:not(.p-highlight):hover) {
  background: #f7f7f7;
}
</style>
