<template>
  <Toast />
  <router-view/>
</template>

<script>
import Toast from "primevue/toast";

export default {
  name: 'App',

  components: {
    Toast
  },

  setup() {
  }
}
</script>

<style>
html {
  height: 100%;
}

body {
  margin: 0px;
  height: calc(100% - 15px);
  overflow: hidden;
}

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

body .p-component,
body .p-component .p-inputtext {
  font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
  font-size: 14px;
  line-height: 1.42857143;
  color: #333333;
  background-color: #fff;
}

#nav {
  padding: 30px;
}

#nav a {
  font-weight: bold;
  color: #2c3e50;
}

#nav a.router-link-exact-active {
  color: #42b983;
}

a {
  color: #337ab7;
  text-decoration: none;
}

.os-hide {
  display: none;
}

.os-table {
  width: 100%;
  display: table;
  border-collapse: collapse;
}

.os-table.muted-header thead th {
  background: #f5f5f5;
  color: #707070;
  font-size: 12px;
  border-bottom: 1px solid #ddd;
  text-align: left;
  padding: 10px 15px;
}

.os-table tbody td {
  border-bottom: 1px solid #ddd;
  text-align: left;
  padding: 10px 15px;
}

.os-border {
  border: 1px solid #ddd;
}

</style>
