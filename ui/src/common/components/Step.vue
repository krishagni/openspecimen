
<template>
  <div class="os-step" v-show="isActive">
    <slot></slot>
  </div>
</template>

<script>
export default {
  props: ['title', 'validate'],

  computed: {
    wizard: function() {
      let parent = this.$parent;
      while (parent) {
        if (typeof parent.addStep == 'function') {
          return parent;
        }

        parent = parent.$parent;
      }

      return null;
    },

    isActive: function() {
      return this.wizard.isActive(this);
    }
  },

  created() {
    this.wizard.addStep(this);
  },

  methods: {
    isLastStep: function() {
      return this.wizard.isLastStep(this);
    },

    next: function() {
      this.wizard.next();
    },

    previous: function() {
      this.wizard.previous();
    }
  }
}
</script>
