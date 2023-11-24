<template>
  <div class="os-progress-bar">
    <div class="bar" :style="bar.style" v-for="(bar, $index) of bars" :key="$index">
      <span>{{bar.value}}</span>
    </div>
  </div>
</template>

<script>

export default {
  props: ['total', 'parts'],

  computed: {
    bars: function() {
      return (this.parts || []).map(
        (part) => {
          const style = Object.assign({width: (part.value / this.total) * 100 + "%"}, part.style || {});
          return { style, value: part.value }
        }
      ).filter(bar => bar.value > 0);
    }
  }
}

</script>

<style scoped>
.os-progress-bar {
  display: flex;
  flex-direction: row;
  height: 1.25rem;
  line-height: 0;
  font-size: .75rem;
  font-weight: bold;
  background-color: #e9ecef;
  border-radius: .25rem;
  overflow: hidden;
}

.os-progress-bar .bar {
  display: flex;
  flex-direction: column;
  text-align: center;
  justify-content: center;
  color: #fff;
  white-space: nowrap;
  background-color: #007bff;
  transition: width 0.6s ease; 
  overflow: hidden;
}
</style>
