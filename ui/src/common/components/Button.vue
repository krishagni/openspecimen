
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
      return this.size == 'small' ? 'btn btn-xs' : 'btn';
    },

    iconSize: function() {
      return this.size == 'small' ? '0.6rem' : '';
    },

    leftIconClass: function() {
      return this.label ? 'pad-right' : '';
    },

    rightIconClass: function() {
      return this.label ?  'pad-left' : '';
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

.inline-button.btn {
  background: #fff;
  padding: 0.5rem 0.75rem;
  border: 1px solid;
  border-color: #007bff;
  border-radius: 4px;
}

.inline-button.btn:hover {
  background: #0069d9;
  color: #ffffff;
  border-color: #0069d9;
  cursor: pointer;
}
</style>
