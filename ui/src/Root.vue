<template>
  <router-view/>
</template>

<script>

export default {
  name: 'Root',

  components: {
  },

  setup() {
  },

  mounted() {
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

body .p-component,
body .p-component .p-inputtext {
  font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
  font-size: 14px;
  line-height: 1.42857143;
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
  top: 0;
  left: 0;
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

.os-table tbody tr.error td {
  background: #f8d7da;
  border-top: 1px solid #f5c6cb;
  color: #721c24;
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
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
}

.os-key-values .item {
  display: flex;
  flex-direction: row;
}

.os-one-col > .item {
  flex: 100%;
}

.os-two-cols > .item {
  flex: 50%;
}

.os-key-values .item .key, .os-key-values .item .value {
  padding: 2px 5px 2px 5px;
  line-height: 24px;
  word-break: break-word;
  white-space: break-spaces;
}

.os-key-values img {
  height: 150px;
  width: 300px;
}

.os-key-values .item .key {
  color: #707070;
  font-weight: normal;
  width: 150px;
}

.os-key-values .item .value {
  flex: 1;
}

.os-key-values.md-col .item .key {
  width: 250px;
}

.os-key-values.bg-col .item .key {
  width: 300px;
}

.os-key-values.vertical .item {
  flex: 100%;
  flex-direction: column;
  padding: 0.5rem;
}

.os-key-values.vertical .item:after {
  content: ' ';
  display: inline-block;
  border-bottom: 1px solid #ddd;
}

.os-key-values.vertical .item:last-child:after {
  border-bottom: 0px;
}

.os-key-values.vertical .item .key.key-sm {
}

.os-key-values.vertical .item .key.key-sm.strong {
  font-weight: 500;
  font-style: italic;
}

@media only screen and (max-width: 768px) {
  .os-key-values.os-two-cols > .item {
    flex: 100%;
  }
}

.os-root {
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
