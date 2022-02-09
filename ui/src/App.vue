<template>
  <div class="os-root">
    <os-navbar />

    <div class="os-user-impersonate-warn" v-if="ui.currentUser && ui.currentUser.impersonated">
      <span>You are viewing {{$filters.username(ui.currentUser)}}'s account.
      <a @click="returnToMyAccount">Return back to your account.</a></span>
    </div>

    <div class="os-app-body">
      <Toast class="os-app-toast" />
      <router-view/>
    </div>
  </div>
</template>

<script>
import Toast from "primevue/toast";

import Navbar from '@/common/components/Navbar.vue';

import userSvc from '@/administrative/services/User.js';
import routerSvc from '@/common/services/Router.js';

export default {
  name: 'App',

  inject: ['ui'],

  components: {
    Toast,
    'os-navbar': Navbar
  },

  setup() {
  },

  mounted() {
  },

  methods: {
    returnToMyAccount: function() {
      userSvc.unpersonate().then(() => routerSvc.ngGoto(''));
    }
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

a:focus, a:hover {
  color: #23527c;
  text-decoration: underline;
  cursor: pointer;
}

.os-app-toast {
  z-index: 1200!important;
}

.os-app-toast .p-toast-message {
  margin: 0rem;
}

.os-hide {
  display: none!important;
}

.os-table {
  width: 100%;
  display: table;
  border-collapse: collapse;
}

.os-table.os-table-hover tbody tr:hover {
  background-color: #f7f7f7;
  cursor: pointer;
}

.os-table.muted-header thead th {
  background: #f5f5f5;
  color: #707070;
  font-size: 12px;
}

.os-table thead th,
.os-table tbody td {
  padding: 10px 15px;
  vertical-align: top;
  text-align: left;
}

.os-table thead th {
  border-bottom: 2px solid #ddd;
}

.os-table tbody td {
  border-top: 1px solid #ddd;
}

.os-table.os-table-borderless thead th,
.os-table.os-table-borderless tbody td {
  border: 0;
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

.os-key-values .item .value img {
  width: 100%;
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

.os-root {
  height: 100%;
}

.os-app-body {
  position: relative;
  height: calc(100% - 40px);
  height: 100%;
}

.os-col-1 {
  width: 8.33%;
}

.os-col-2 {
  width: 16.67%;
}

.os-col-3 {
  width: 25%;
}

.os-col-4 {
  width: 33.33%;
}

.os-col-5 {
  width: 41.67%;
}

.os-col-6 {
  width: 50%;
}

.os-col-7 {
  width: 58.33%;
}

.os-col-8 {
  width: 66.67%;
}

.os-col-9 {
  width: 75%;
}

.os-col-10 {
  width: 83.33%;
}

.os-col-11 {
  width: 91.67%;
}

.os-col-12 {
  width: 100%;
}

.os-form-footer {
  padding: 0.5rem;
}

.os-form-footer button {
  margin-right: 0.5rem;
}

.p-tooltip.p-component {
  background: transparent;
}
</style>

<style scoped>

.os-user-impersonate-warn {
  position: absolute;
  left: 0;
  right: 0;
  width: 40%;
  margin: auto;
  z-index: 10;
  background: #c33;
  padding: 0.25rem 0.5rem;
  color: #fff;
  border-bottom-left-radius: 0.25rem;
  border-bottom-right-radius: 0.25rem;
}

.os-user-impersonate-warn a {
  color: #fff;
  text-decoration-line: underline;
}
</style>
