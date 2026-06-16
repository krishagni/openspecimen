
<template>
  <a :href="url" :class="linkClass" :target="target" rel="noopener">
    <icon v-if="leftIcon" :name="leftIcon" :class="leftIconClass" :size="iconSize" />
    <span v-if="label">{{label}}</span>
    <span v-else-if="$slots.default"><slot /></span>
    <icon v-if="rightIcon" :name="rightIcon" :class="rightIconClass" :size="iconSize" />
  </a>
</template>

<script>
import Icon from '@/common/components/Icon.vue';

export default {
  props: ['leftIcon', 'rightIcon', 'label', 'url', 'newTab', 'size', 'no-outline'],

  components: {
    'icon': Icon
  },

  computed: {
    linkClass: function() {
      let kls = this.size == 'small' ? 'button-link btn-xs' : 'button-link';

      if (this.$attrs.type) {
        kls += ' ' + this.$attrs.type;
      } else {
        const types = ['primary', 'secondary', 'danger', 'text', 'success'];
        for (const type of types) {
          if (Object.prototype.hasOwnProperty.call(this.$attrs, type)) {
            kls += ' ' + type;
          }
        }
      }

      if (this.noOutline) {
        kls += ' no-outline';
      }

      if (!this.label && (this.leftIcon || this.rightIcon)) {
        kls += ' icon-btn';
      }

      return kls;
    },

    iconSize: function() {
      return this.size == 'small' ? '85%' : '';
    },

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
a.button-link:not(.icon-btn) {
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
a.button-link:not(.icon-btn):hover {
  text-decoration: none;
  background: #ddd;
  border: 1px solid #a5a5a5;
}

a.button-link.btn-xs {
  padding: 0px 6px;
}

a.button-link.primary {
  color: #fff;
  background-color: #337ab7;
  border-color: #2e6da4;
}

a.button-link.primary:hover,
a.button-link.primary:focus {
  color: #fff;
  background-color: #286090;
  border-color: #204d74;
}

a.button-link.secondary {
  color: #fff;
  background: #888;
  border-color: #666;
}

a.button-link.secondary:hover,
a.button-link.secondary:focus {
  color: #fff;
  background: #777;
  border-color: #555;
}

a.button-link.success {
  color: #fff;
  background: #5cb85c;
  border-color: #4cae4c;
}

a.button-link.success:hover,
a.button-link.success:focus {
  color: #fff;
  background: #449d44;
  border-color: #398439;
}

a.button-link.danger {
  color: #fff;
  background-color: #d9534f;
  border-color: #d43f3a;
}

a.button-link.danger:hover,
a.button-link.danger:focus {
  color: #fff;
  background-color: #c9302c;
  border-color: #ac2925;
}

a.button-link.text {
  background: transparent;
  border: 1px solid transparent;
  color: #428bca;
}

a.button-link.text:hover {
  color: #2a6496;
  text-decoration: none;
  border: 1px solid #428bca;
}

a.button-link.no-outline,
a.button-link.icon-btn.no-outline {
  border: none;
  background: transparent;
}

a.button-link.icon-btn {
  color: #666666;
  background: transparent;
  border: 1px solid #ccc;
  border-radius: 50%;
  height: 1.75rem;
  width: 1.75rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
  text-decoration: none;
  vertical-align: initial;
  cursor: pointer;
}

a.button-link.icon-btn:hover {
  color: #666666;
  background: #ddd;
  border-color: #a5a5a5;
  text-decoration: none;
}
</style>
