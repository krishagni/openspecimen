
<template>
  <div class="os-mask" v-if="show"> </div>
</template>

<script>
export default {
  data() {
    return {
      show: false
    }
  },

  methods: {
    enable: function() {
      this.show = true;
    },

    disable: function() {
      this.show = false;
    }
  }
}
</script>

<style scoped>

.os-mask:before {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 5000;
  content: ' ';
  display: block;
  background: rgba(269, 269, 269, 0.5);
}

.os-mask:after {
  position: fixed;
  top: 40px;
  left: calc(50% - 35px);
  content: 'Working...';
  display: block;
  z-index: 5500;
  padding: 5px 12px;
  background: #ffc;
  border: 1px solid #ddd;
}
</style>
