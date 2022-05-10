<template>
  <div class="os-click-esc star" @click="toggleStarred">
    <div class="starred" v-if="starred">
      <os-icon name="star" />
    </div>
    <div v-else>
      <os-icon :name="['far', 'star']" />
    </div>
  </div>
</template>

<script>
export default {
  props: ['starred'],

  emits: ['star-toggled'],

  methods: {
    toggleStarred: function() {
      this.$emit('star-toggled');
    }
  }
}
</script>

<style scoped>
.star {
  cursor: pointer;
}

.starred {
  color: #f0ad4e;
}
</style>
