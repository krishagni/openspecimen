
<template>
  <label>
    <slot></slot>
  </label>
</template>

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
</style>
