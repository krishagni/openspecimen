<template>
  <suspense>
    <router-view :key="$route.params.cpId" />
  </suspense>
</template>

<script>
export default {
}
</script>
