
<template>
  <span :class="$attrs['md-type'] && 'md-type'">
    <span v-if="component">
      <component :is="component" v-model="inputValue" v-bind="$attrs" />
    </span>

    <span v-else-if="effDisplayType == 'signature' && inputValue">
      <img :src="imageUrl">
    </span>

    <span v-else-if="effDisplayType == 'fileUpload' && inputValue">
      <a :href="fileUrl" target="_blank" rel="noopener">
        {{inputValue.filename}}
      </a>
    </span>

    <span v-else-if="effDisplayType == 'subform' && inputValue">
      <os-table-span v-model="displayText" />
    </span>

    <span class="value-text" v-else>
      <a v-if="link" :href="link" :target="hrefTarget || '_blank'" rel="noopener">
        <span>{{displayText}}</span>
      </a>
      <span v-else>{{displayText}}</span>
    </span>
  </span>
</template>

<script>
import exprUtil from '@/common/services/ExpressionUtil.js';
import http from '@/common/services/HttpClient.js';
import routerSvc from '@/common/services/Router.js';
import util from '@/common/services/Util.js';

export default {
  props: ['modelValue', 'displayType', 'displayTypeExpr', 'href', 'hrefTarget', 'form', 'context'],

  created() {
    this.customField = this.$attrs.name && this.$attrs.name.indexOf('.extensionDetail.attrsMap.');
  },

  computed: {
    inputValue: {
      get() {
        return this.modelValue;
      },

      set(value) {
        this.$emit('update:modelValue', value);
      }
    },

    effDisplayType: function() {
      if (this.displayType) {
        return this.displayType;
      } else if (this.displayTypeExpr) {
        return exprUtil.eval(this.form || this.context || {}, this.displayTypeExpr);
      }

      return undefined;
    },

    displayText: function() {
      return util.getFieldDisplayValue(
        this.form || this.context || {},
        this.$attrs,
        this.inputValue,
        this.effDisplayType
      );
    },

    link: function() {
      if (typeof this.href == 'function') {
        let ctx = this.form || this.context || {};
        let link = this.href(ctx.formData || ctx);
        if (link && link.indexOf('#') == 0) {
          link = this.$ui.ngServer + link;
        }

        return link;
      } else if (this.effDisplayType == 'storage-position') {
        if (this.inputValue && typeof this.inputValue == 'object' && +this.inputValue.id > 0) {
          return routerSvc.getUrl('ContainerDetail.Locations', {containerId: this.inputValue.id});
        }
      }

      return this.href;
    },

    imageUrl: function() {
      return http.getUrl('form-files/' + this.inputValue);
    },

    fileUrl: function() {
      let file = this.inputValue;
      return http.getUrl('form-files/' + file.fileId +
        '?contentType=' + file.contentType + '&filename=' + file.filename
      );
    },

    component: function() {
      const dt = this.displayType;
      if (!dt) {
        return null;
      }

      if (dt.indexOf('component:') == 0) {
        const component = dt.substring('component:'.length).trim();
        return component.indexOf('os-') == 0 ? component : 'os-' + component;
      } else if (dt == 'specimen-description') {
        return 'os-' + dt;
      }

      return null;
    }
  },

  methods: {
    getDisplayValue: function() {
      return this.displayText;
    }
  }
}
</script>

<style scoped>

.md-type {
  display: inline-block;
  padding: 2px 0px;
}

.value-text {
  white-space: pre;
}

</style>
