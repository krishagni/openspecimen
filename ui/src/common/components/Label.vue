
<template>
  <label>
    <slot></slot>
    <span class="required-indicator" v-if="requiredTooltip" v-os-tooltip="requiredTooltip">
      <span>*</span>
    </span>
  </label>
</template>

<script>
export default {
  props: ['requiredTooltip']
}
</script>

<style scoped>
label {
  width: 100%;
  margin-bottom: 5px;
  display: inline-block;
  font-weight: 700;
}

label.underlined {
  border-bottom: 2px solid #ddd;
  padding: 8px 0px;
}

label .required-indicator {
  display: inline-block;
  padding: 0.25rem;
  color: red;
  cursor: help;
}
</style>
