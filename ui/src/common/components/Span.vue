
<template>
  <span :class="$attrs['md-type'] && 'md-type'">
    <a v-if="link" :href="link" target="_blank" rel="noopener">
      <span>{{displayText}}</span>
    </a>
    <span v-else>{{displayText}}</span>
  </span>
</template>

<script>
export default {
  props: ['modelValue', 'displayType', 'href', 'form', 'context'],

  computed: {
    inputValue: {
      get() {
        return this.modelValue;
      },

      set(value) {
        this.$emit('update:modelValue', value);
      }
    },

    displayText: function() {
      if (this.displayType == 'storage-position') {
        let result = this.inputValue;

        if (this.inputValue && typeof this.inputValue == 'object') {
          let position = this.inputValue;
          result = position.name;
          if (position.mode == 'TWO_D') {
            result += ' (' + position.positionY + ', ' + position.positionX + ')';
          } else if (position.mode == 'LINEAR') {
            result += ' (' + position.position + ')';
          }
        }

        return result || 'Not Stored';
      }

      return this.inputValue;
    },

    link: function() {
      if (typeof this.href == 'function') {
        let ctx = this.form || this.context || {};
        let link = this.href(ctx.formData || ctx);
        if (link.indexOf('#') == 0) {
          link = this.$ui.ngServer + link;
        }

        return link;
      } else if (this.displayType == 'storage-position') {
        if (this.inputValue && typeof this.inputValue == 'object' && +this.inputValue.id > 0) {
          return this.$ui.ngServer + '#/containers/' + this.inputValue.id + '/locations';
        }
      }

      return this.href;
    }
  }
}
</script>

<style scoped>

.md-type {
  display: inline-block;
  padding: 2px 0px;
}

</style>
