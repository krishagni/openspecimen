
<template>
  <os-html :content="note" />
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
