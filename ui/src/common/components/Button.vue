
<template>
  <button type="button" :class="buttonClass">
    <icon v-if="leftIcon" :name="leftIcon" :class="leftIconClass" :size="iconSize" />
    <span>{{label}}</span>
    <icon v-if="rightIcon" :name="rightIcon" :class="rightIconClass" :size="iconSize" />
  </button>
</template>

<script>
import Icon from '@/common/components/Icon.vue';

export default {
  props: ['leftIcon', 'rightIcon', 'label', 'size', 'no-outline'],

  components: {
    'icon': Icon
  },

  computed: {
    buttonClass: function() {
      let kls = this.size == 'small' ? 'btn btn-xs' : 'btn';

      if (this.$attrs.type) {
        kls += ' ' + this.$attrs.type;
      } else {
        const types = ['primary', 'secondary', 'danger', 'text'];
        for (const type of types) {
          if (Object.prototype.hasOwnProperty.call(this.$attrs, type)) {
            kls += ' ' + type;
          }
        }
      }

      if (this.noOutline) {
        kls += ' no-outline';
      }

      if (!this.label) {
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
  padding: 4px 10px;
  display: inline-block;
  font-size: inherit;
  line-height: inherit;
  cursor: pointer;
  border-radius: 1.125rem;
}

.os-page-toolbar button:not(.icon-btn),
button:not(.icon-btn) {
  height: 2.25rem;
  padding: 0rem 1rem;
}

.os-page-toolbar button:hover,
button:hover, button.icon-btn:hover {
  background: #ddd;
  border: 1px solid #a5a5a5;
}

button.icon-btn {
  border-radius: 50%;
  font-size: 1rem;
  background: transparent;
  height: 1.75rem;
  width: 1.75rem;
}

.btn.btn-xs {
  padding: 0px 6px;
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

.btn.primary {
  color: #fff;
  background-color: #337ab7;
  border-color: #2e6da4;
}

.btn.primary:hover,
.btn.primary:focus {
  color: #fff;
  background-color: #286090;
  border-color: #204d74;
}

.btn.secondary {
  color: #fff;
  background: #888;
  border-color: #666;
}

.btn.secondary:hover,
.btn.secondary:focus {
  color: #fff;
  background: #777;
  border-color: #555;
}

.btn.danger {
  color: #fff;
  background-color: #d9534f;
  border-color: #d43f3a;
}

.btn.danger:hover,
.btn.danger:focus {
  color: #fff;
  background-color: #c9302c;
  border-color: #ac2925;
}

.btn.text {
  background: transparent;
  border: 0px;
  color: #428bca;
}

.btn.text:hover {
  color: #2a6496;
  text-decoration: none;
  border: 1px solid #428bca;
}

.btn.no-outline {
  border: none;
  background: transparent;
}

.btn[disabled] {
  opacity: 0.6;
}
</style>
