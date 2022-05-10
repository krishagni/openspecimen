<template>
  <div class="bar" :style="colorCss" v-if="colorCss != null">
    <div class="label">
      <span>{{utilisation}}%</span>
    </div>
  </div>
  <span v-else>
    <span>-</span>
  </span>
</template>

<script>

export default {
  props: ['utilisation'],

  computed: {
    colorCss: function() {
      if (this.utilisation == null || this.utilisation == undefined || this.utilisation < 0) {
        return null;
      }

      const used = this.utilisation + '%';
      return {background: 'linear-gradient(90deg, #d9534f, #d9534f ' + used + ', #5cb85c ' + used + ', #5cb85c 100%)'};
    }
  }
}
</script>

<style scoped>
.bar {
  width: 100%;
  height: 1.25rem;
  position: relative;
  border-radius: 0.25rem;
}

.bar .label {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  text-align: center;
  color: #fff;
  font-weight: bold;
}
</style>
