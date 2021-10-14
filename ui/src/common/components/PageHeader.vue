
<template>
  <div class="os-page-header">
    <sidebar class="os-sidenav" v-model:visible="showNav" :showCloseIcon="false" :dismissable="true">
      <div class="header">
        <div class="nav-button">
          <button @click="showNav = false">
            <os-icon name="bars" size="24px" />
          </button>
        </div>

        <div class="title">
          <h3>Navigate To</h3>
        </div>
      </div>

      <div class="content-wrapper">
        <div class="content">
          <Menu :model="items" :popup="false" />
        </div>
      </div>
    </sidebar>

    <div class="nav-button">
      <button @click="showNav = true">
        <os-icon name="bars" size="24px" />
      </button>
    </div>

    <div class="content-wrapper">
      <slot name="breadcrumb"></slot>
      <div class="content">
        <div class="left">
          <slot></slot>
        </div>
        <div class="right">
          <slot name="right"></slot>
        </div>
      </div>
    </div>
  </div>
</template>

<script>

import Menu from 'primevue/menu';
import Sidebar from 'primevue/sidebar';

import Icon from './Icon.vue'

import authSvc from '@/common/services/Authorization.js';
import homePageSvc from '@/common/services/HomePageService.js';
import routerSvc from '@/common/services/Router.js';

export default {
  inject: ['ui'],

  components: {
    Menu,
    'sidebar': Sidebar,
    'os-icon': Icon
  },

  data() {
    return {
      showNav: false
    }
  },

  computed: {
    items: function() {
      let items = homePageSvc.getCards()
        .filter(card => {
          if (typeof card.showIf == 'function') {
            return card.showIf();
          }

          return !card.showIf || authSvc.isAllowed(card.showIf);
        })
        .map(card => {
          let item = {label: card.title};
          if (typeof card.href == 'string') {
            item.url = this.ui.ngServer + card.href;
          } else if (typeof card.href == 'function') {
            item.command = () => {
              Promise.resolve(card.href()).then(
                link => {
                  if (card.newTab) {
                    window.open(link, '_blank');
                  } else {
                    routerSvc.ngGoto(link);
                  }
                }
              );
            }
          }

          return item;
        });

      items.splice(0, 0, {label: 'Home', url: this.ui.ngServer + '#/home'});
      return items;
    }
  }
}

</script>

<style scoped>
  .os-page-header {
    padding: 10px 15px 0px;
    border-bottom: 1px solid #ddd;
  }

  .os-page-header:after {
    content: ' ';
    display: block;
    clear: both;
  }

  .os-page-header .nav-button {
    float: left;
    width: 36px;
    margin-top: 7px;
    margin-left: -5px;
    margin-right: 7px;
  }

  .os-page-header .nav-button button {
    color: #666;
    background: transparent;
    border: none;
    outline: none;
    cursor: pointer;
  }

  .os-page-header .content-wrapper {
    float: left;
    padding-left: 30px;
    width: calc(100% - 65px);
  }

  .os-page-header .content-wrapper .content {
    margin-top: 7px;
  }

  .os-page-header .content-wrapper .content:after {
    content: ' ';
    display: block;
    clear: both;
  }

  .os-page-header .content-wrapper .content .left {
    float: left;
    width: 65%;
  }

  .os-page-header .content-wrapper .content .right {
    float: right;
    width: 20%;
    text-align: right;
  }

  .os-page-header :deep(h3) {
    margin-top: 0px;
    margin-bottom: 10px;
    font-size: 24px;
    font-family: inherit;
    font-weight: 500;
    line-height: 1.1;
    color: inherit;
  }

  .os-page-header :deep(.os-breadcrumb + .content) {
    margin-top: 0px;
  }
</style>

<style>
  .os-sidenav .p-sidebar-header,
  .os-sidenav .p-sidebar-content {
    padding: 0rem;
  }

  .os-sidenav .header {
    padding: 10px 15px 0px;
    border-bottom: 1px solid #ddd;
  }

  .os-sidenav .header:after {
    content: ' ';
    display: block;
    clear: both;
  }

  .os-sidenav .header .nav-button {
    float: left;
    width: 36px;
    margin-top: 7px;
    margin-left: -5px;
    margin-right: 7px;
  }

  .os-sidenav .header .nav-button button {
    color: #666;
    background: transparent;
    border: none;
    outline: none;
    cursor: pointer;
  }

  .os-sidenav .header .title {
    float: left;
    padding-left: 30px;
    width: calc(100% - 65px);
    margin-top: 7px;
  }

  .os-sidenav .header .title h3 {
    margin-top: 0px;
    margin-bottom: 10px;
    font-size: 24px;
    font-family: inherit;
    font-weight: 500;
    line-height: 1.1;
    color: inherit;
  }

  .os-sidenav .p-sidebar-content {
    height: 100%;
    overflow: hidden;
  }

  .os-sidenav .content-wrapper {
    position: relative;
    height: calc(100% - 54px);
    overflow: hidden;
  }

  .os-sidenav .content-wrapper .content {
    position: absolute;
    top: 0px;
    bottom: 0px;
    left: 0px;
    right: 0px;
    overflow-y: auto;
  }

  .os-sidenav .content-wrapper .content .p-menu {
    margin: 0px;
    padding: 0px;
    border: 0px;
    width: 100%;
  }

  .os-sidenav .content-wrapper .content .p-menu .p-menuitem .p-menuitem-link {
    padding: 0.75rem;
  }

  .os-sidenav .content-wrapper .content .p-menu .p-menuitem .p-menuitem-link .p-menuitem-text {
    color: #666666;
    font-size: 1rem;
    line-height: inherit;
  }
</style>
