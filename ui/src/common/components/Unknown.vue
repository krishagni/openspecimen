<template>
  <span>Unknown component: {{type}}</span>
</template>

<script>
export default {
  props: ['type'],

  methods: {
    getDisplayValue: function() {
      return "Unknown Component"
    }
  }
}
</script>
