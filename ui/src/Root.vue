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
}

.os-key-values:after {
  content: '';
  display: block;
  clear: both;
}

.os-key-values .item {
  margin: 2px 0px 0px 0px;
  position: relative;
  margin-left: 150px;
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
