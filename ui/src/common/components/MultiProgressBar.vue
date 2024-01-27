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

  data() {
    return {
      bars: []
    }
  },

  created() {
    this._computeWidth(true);
    setTimeout(() => this._computeWidth(), 10);
  },

  watch: {
    'parts': function() {
      this._computeWidth();
    }
  },

  methods: {
    _computeWidth: function(initZero) {
      this.bars = (this.parts || []).map(
        (part) => {
          const width = (initZero ? 0 : (part.value * 100 / this.total)) + '%'
          const style = Object.assign({width}, part.style || {});
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
  transition: width 2s ease;
  overflow: hidden;
}
</style>
