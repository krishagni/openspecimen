
<template>
  <span v-html="note"></span>
</template>

<script>

export default {
  props: ['note'],

  methods: {
    getDisplayValue() {
      return this.note;
    }
  }
}

</script>
