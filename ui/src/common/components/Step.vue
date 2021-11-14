
<template>
  <div class="os-step" v-show="isActive">
    <slot></slot>
  </div>
</template>

<script>
export default {
  props: ['title', 'validate'],

  computed: {
    isActive: function() {
      return this.$parent.isActive(this);
    }
  },

  created() {
    this.$parent.addStep(this);
  }
}
</script>
