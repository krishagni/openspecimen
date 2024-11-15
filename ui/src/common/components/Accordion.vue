<template>
  <Accordion class="os-accordion" :lazy="lazy" v-model:activeIndex="activeTab"
    @tab-open="onTabOpen($event)" @tab-close="onTabClose($event)">
    <AccordionTab v-for="(tab, idx) of tabs" :key="idx">
      <template #header v-if="tab.children && tab.children.header">
        <component :is="tab.children.header" />
      </template>
      <component class="os-accordion-tab" :is="tab.children.content" v-if="tab.children && tab.children.content"/>
    </AccordionTab>
  </Accordion>
</template>

<script>

import Accordion  from 'primevue/accordion';
import AccordionTab from 'primevue/accordiontab';

export default {
  props: ['lazy'],

  emits: ['tab-opened', 'tab-closed'],

  components: {
    Accordion,
    AccordionTab
  },

  data() {
    return {
      activeTab: -1,

      previousTab: -1
    }
  },

  computed: {
    tabs: function() {
      return this.$slots.default().reduce(
        (tabs, slot) => {
          Array.prototype.push.apply(tabs, slot.children || []);
          return tabs;
        },

        []
      );
    },
  },

  methods: {
    onTabOpen: function({index}) {
      const tab = this.tabs[index];
      this.$emit('tab-opened', {accordion: this, index, tab});
      this.previousTab = index;
    },

    onTabClose: function({index}) {
      const tab = this.tabs[index];
      this.$emit('tab-closed', {accordion: this, index, tab});
      this.previousTab = index;
    },

    activate: function(index) {
      setTimeout(() => this.previousTab = this.activeTab = index, 5);
    }
  }
}
</script>

<style scoped>
.os-accordion :deep(.p-accordion-header-link) {
  color: initial;
  text-decoration: none;
  cursor: pointer;
}

.os-accordion {
  height: 100%;
  overflow-y: auto;
}

.os-accordion :deep(.p-accordion-tab) {
  display: flex;
  flex-direction: column;
  max-height: 100%;
}

.os-accordion :deep(.p-accordion-tab .p-toggleable-content) {
  flex: 1;
  overflow-y: auto;
}
</style>
