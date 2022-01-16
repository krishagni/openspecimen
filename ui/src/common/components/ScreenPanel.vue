
<template>
  <div :class="widthClass" style="height: 100%;">
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
