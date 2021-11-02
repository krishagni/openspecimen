<template>
  <Dialog v-model:visible="showDialog" :style="style" position="top" :modal="true">
    <template #header>
      <slot name="header"></slot>
    </template>

    <div :class="contentClass">
      <slot name="content"> </slot>
    </div>

    <template #footer v-if="!!$slots.footer">
      <slot name="footer"></slot>
    </template>
  </Dialog>
</template>

<script>
import Dialog from 'primevue/dialog';

export default {
  props: ['size', 'contentClass'],

  components: {
    Dialog,
  },

  data() {
    return {
      showDialog: false
    }
  },

  computed: {
    style: function() {
      if (this.size == 'md') {
        return {width: '60vw'};
      } else if (this.size == 'lg') { 
        return {width: '75vw'};
      } else {
        return {width: '50vw'};
      }
    }
  },

  methods: {
    open: function() {
      this.showDialog = true;
    },

    close: function() {
      this.showDialog = false;
    }
  }
}
</script>

<style>

.p-dialog .p-dialog-header {
  height: 44px;
  background-color: rgb(237, 239, 240);
  font-size: 18px;
  color: rgb(35, 35, 35);
  padding: 10px 10px 10px 20px;
}

.p-dialog .p-dialog-footer {
  height: 44px;
  background-color: rgb(237, 239, 240);
  text-align: right;
  padding: 0px;
  border-top: 0px;
}

</style>
