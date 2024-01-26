
<template>
  <ul class="os-breadcrumb">
    <li class="ellipsis" v-if="items.length > 5">
      <span>...</span>
    </li>
    <li v-for="(item, idx) of items" :key="idx" :class="{collapsed: (items.length - idx) > 5}">
      <a :href="item.url" :target="item.target">
        <span>{{item.label}}</span>
      </a>
    </li>
  </ul>
</template>

<script>
  export default {
    props: ['items']
  }
</script>

<style scoped>
  ul.os-breadcrumb {
    list-style: none;
    margin: 0px;
    padding-left: 0px;
  }

  ul.os-breadcrumb > li {
    display: inline-block;
  }

  ul.os-breadcrumb > li:not(.ellipsis) + li:before {
    content: '/\00a0';
    display: inline-block;
    color: #ccc;
    padding: 0 5px;
  }

  ul.os-breadcrumb a {
    color: #428bca;
  }

  ul.os-breadcrumb a:hover {
    color: #2a6496;
    text-decoration: underline;
  }

  ul.os-breadcrumb li.ellipsis {
    cursor: pointer;
  }

  ul.os-breadcrumb li.collapsed {
    display: none;
  }

  ul.os-breadcrumb:hover li.ellipsis {
    display: none;
  }

  ul.os-breadcrumb:hover li.collapsed {
    display: inline-block;
  }
</style>
