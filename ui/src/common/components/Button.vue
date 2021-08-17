
<template>
  <button type="button" :class="buttonClass">
    <icon v-if="leftIcon" :name="leftIcon" :class="leftIconClass" :size="iconSize"/>
    <span>{{label}}</span>
    <icon v-if="rightIcon" :name="rightIcon" :class="rightIconClass" :size="iconSize"/>
  </button>
</template>

<script>
import Icon from '@/common/components/Icon.vue';

export default {
  props: ['leftIcon', 'rightIcon', 'label', 'size'],

  components: {
    'icon': Icon
  },

  computed: {
    buttonClass: function() {
      if (this.size == 'small') {
        return 'btn btn-xs';
      }

      return 'btn';
    },

    iconSize: function() {
      if (this.size == 'small') {
        return '0.6rem';
      }

      return '';
    },

    leftIconClass: function() {
      if (this.label) {
        return 'pad-right';
      }

      return '';
    },

    rightIconClass: function() {
      if (this.label) {
        return 'pad-left';
      }

      return '';
    }
  }
}

</script>

<style scoped>

.os-page-toolbar button,
button {
  color: #666666;
  background: #f5f5f5;
  border: 1px solid #ccc;
  vertical-align: initial;
  border-radius: 3px;
  padding: 4px 10px;
  display: inline-block;
  font-size: inherit;
  line-height: inherit;
  cursor: pointer;
}

.os-page-toolbar button:hover,
button:hover {
  background: #ddd;
  border: 1px solid #a5a5a5;
}

.btn.btn-xs {
  padding: 0px 4px;
}

</style>
