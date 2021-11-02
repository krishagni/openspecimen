
<template>
  <TabView>
    <TabPanel v-for="(tab, idx) of tabs" :header="tab.props && tab.props.header" :key="idx">
      <template #header v-if="tab.children && tab.children.header">
        <component :is="tab.children.header"></component>
      </template>
      <component :is="tab"></component>
    </TabPanel>
  </TabView>
</template>

<script>

import TabView  from 'primevue/tabview';
import TabPanel from 'primevue/tabpanel';

export default {
  components: {
    TabView,
    TabPanel
  },

  computed: {
    tabs: function() {
      const tabs = [];
      this.$slots.default().forEach(child => {
        tabs.push(child);
      });

      return tabs;
    }
  }
}

</script>
