<template>
  <Chart class="os-chart" :type="type" :data="data" :options="options" />
</template>

<script>

import Chart from 'primevue/chart';

export default {
  props: ['type', 'data', 'options'],

  components: {
    Chart
  }
}

</script>

<style scoped>
.os-chart {
  height: 100%;
}
</style>

