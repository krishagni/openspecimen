
<template>
  <div class="os-root">
    <os-navbar :no-login="true" />

    <os-app-body />
  </div>
</template>

<script>

import AppBody from '@/common/components/AppBody.vue';
import Navbar  from '@/common/components/Navbar.vue';

export default {
  name: 'NoLoginApp',

  components: {
    'os-app-body': AppBody,
    'os-navbar': Navbar
  }
}
</script>

<style scoped>
#app {  
  font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
  font-size: 14px;
  line-height: 1.42857143;
  color: #333333;
  background-color: #fff;
  height: 100%;
  position: fixed;
  width: 100%;
}

.os-root {
  height: 100%;
}
</style>
