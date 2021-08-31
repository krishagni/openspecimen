
<template>
  <div :class="widthClass">
    <slot></slot>
  </div>
</template>

<script>

export default {
  props: ['width'],

  computed: {
    widthClass: function() {
      let result = 'p-col';
      if (this.width > 0) {
        result += '-' + this.width;
      }

      return result;
    }
  }
}

</script>
