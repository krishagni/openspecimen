<template>
  <span v-if="params">{{params.value}}</span>
</template>

<script>
export default {
  props: ['params'],

  created() {
    console.log(this.params);
  }
}
</script>
