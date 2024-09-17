
<template>
  <TabView class="os-tabs" :lazy="lazy" v-model:activeIndex="activeTab" @tab-change="tabChanged($event)">
    <TabPanel v-for="(tab, idx) of tabs" :header="tab.props && tab.props.header" :key="idx">
      <template #header v-if="tab.children && tab.children.header">
        <component :is="tab.children.header"></component>
      </template>
      <component class="os-tab-content" :is="tab"></component>
    </TabPanel>
  </TabView>
</template>

<script>

import TabView  from 'primevue/tabview';
import TabPanel from 'primevue/tabpanel';

export default {
  props: ['lazy'],

  emits: ['tab-changed'],

  components: {
    TabView,
    TabPanel
  },

  data() {
    return {
      activeTab: 0,

      previousTab: 0
    }
  },

  computed: {
    tabs: function() {
      const tabs = [];
      this.$slots.default().forEach(child => {
        tabs.push(child);
      });

      return tabs;
    },
  },

  methods: {
    tabChanged: function({index}) {
      const tab = this.tabs[index];
      this.$emit('tab-changed', { tabPanel: this, activeTab: index, previousTab: this.previousTab, tab });
      this.previousTab = index;
    },

    activate: function(index) {
      setTimeout(() => this.previousTab = this.activeTab = index, 5);
    }
  }
}

</script>

<style scoped>
.os-tabs {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.os-tabs :deep(.p-tabview-panels) {
  flex: 1;
  overflow-y: auto;
}

.os-tabs :deep(.p-tabview-panels .p-tabview-panel) {
  height: 100%;
}

.os-tabs :deep(.os-tab-content) {
  height: 100%;
  overflow-y: auto;
}
</style>
