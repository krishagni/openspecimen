<template>
  <Dialog v-model:visible="showDialog" :style="style" position="top"
    :modal="true" :autoZIndex="false" :closable="showCloseIcon">
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
  props: ['size', 'contentClass', 'closable'],

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
    },

    showCloseIcon: function() {
      return this.closable == undefined || this.closable == null || this.closable == 'true' || this.closable == true;
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
  height: 52px;
  background-color: rgb(237, 239, 240);
  font-size: 18px;
  color: rgb(35, 35, 35);
  padding: 10px 10px 10px 20px;
}

.p-dialog .p-dialog-footer {
  height: 52px;
  background-color: rgb(237, 239, 240);
  text-align: right;
  padding: 0px;
  border-top: 0px;
}

.p-dialog .p-dialog-footer button {
  margin: 0.5rem 0.5rem 0.5rem 0rem;
}

.p-dialog .p-dialog-header .p-dialog-header-icon:enabled:hover {
  background: #ddd;
  border: 1px solid #a5a5a5;
}
</style>
