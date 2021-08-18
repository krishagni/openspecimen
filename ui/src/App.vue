<template>
  <Toast class="os-app-toast" />
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

.os-app-toast {
  z-index: 1102!important;
}

.os-app-toast .p-toast-message {
  margin: 0rem;
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
}

.os-table thead th,
.os-table tbody td {
  text-align: left;
  padding: 10px 15px;
}

.os-table thead th {
  border-bottom: 2px solid #ddd;
}

.os-table tbody td {
  border-top: 1px solid #ddd;
}

.os-border {
  border: 1px solid #ddd;
}

.os-key-values {
  list-style-type: none;
  margin: 0px 0px 20px 0px;
  padding: 0;
}

.os-key-values:after {
  content: '';
  display: block;
  clear: both;
}

.os-key-values .item {
  margin: 2px 0px 0px 0px;
  position: relative;
  padding-left: 150px;
  -webkit-column-break-inside: avoid;
  page-break-inside: avoid;
  break-inside: avoid-column;
  display: table;
}

.os-key-values.bg-col .item {
  padding-left: 300px;
}

.os-key-values.md-col .item {
  padding-left: 250px;
}

.os-one-col > .item {
  width: 100%;
}

.os-key-values.os-two-cols {
  column-count: 2;
  -webkit-perspective: 1;
}

.os-key-values .item .key, .os-key-values .item .value {
  overflow: hidden;
  text-overflow: ellipsis;
  padding: 2px 5px 2px 5px;
  line-height: 24px;
  width: 100%;
  float: left;
}

.os-key-values .item .key {
  color: #707070;
  font-weight: normal;
  clear: left;
  margin-left: -150px;
  width: 150px;
}

.os-key-values.md-col .item .key {
  margin-left: -250px;
  width: 250px;
}

.os-key-values.bg-col .item .key {
  margin-left: -300px;
  width: 300px;
}

.os-key-values.vertical .item {
  padding-left: 0px;
  width: 100%;
}

.os-key-values.vertical .item:after {
  content: ' ';
  display: inline-block;
  border-bottom: 1px solid #ddd;
  width: 100%;
}

.os-key-values.vertical .item:last-child:after {
  width: 0%;
}

.os-key-values.vertical .item .key.key-sm {
  margin-left: 0px;
  width: 100%;
}

.os-key-values.vertical .item .key.key-sm.strong {
  font-weight: 500;
  font-style: italic;
}

@media only screen and (max-width: 768px) {
  .os-key-values.os-two-cols {
    column-count: 1;
    -webkit-perspective: 1;
  }
}

</style>
