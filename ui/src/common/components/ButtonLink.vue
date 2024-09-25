
<template>
  <a :href="url" class="button-link" :target="target" rel="noopener">
    <icon v-if="leftIcon" :name="leftIcon" :class="leftIconClass" />
    <span>{{label}}</span>
    <icon v-if="rightIcon" :name="rightIcon" :class="rightIconClass" />
  </a>
</template>

<script>
import Icon from '@/common/components/Icon.vue';

export default {
  props: ['leftIcon', 'rightIcon', 'label', 'url', 'newTab'],

  components: {
    'icon': Icon
  },

  computed: {
    leftIconClass: function() {
      return this.label ? 'pad-right' : '';
    },

    rightIconClass: function() {
      return this.label ?  'pad-left' : '';
    },
 
    target: function() {
      return this.newTab == 'true' || this.newTab == true ? '_blank' : '_self';
    }
  }
}

</script>

<style scoped>
a.btn,
.os-page-toolbar a.button-link {
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

  border-radius: 1.125rem;
  height: 2.25rem;
  padding: 7px 16px;
}

a.btn:hover,
.os-page-toolbar a.button-link:hover {
  text-decoration: none;
  background: #ddd;
  border: 1px solid #a5a5a5;
}
</style>
