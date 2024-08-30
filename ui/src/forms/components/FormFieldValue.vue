
<template>
  <span v-if="!field.value && field.value != 0">
    <span>-</span>
  </span>
  <span v-else>
    <span v-if="field.type == 'fileUpload'">
      <a :href="fileUrl" target="_blank" rel="noopener">
        {{field.value.filename}}
      </a>
    </span>
    <span v-else-if="field.type == 'signature'">
      <img :src="imageUrl">
    </span>
    <span v-else-if="field.type == 'textArea' || field.type == 'stringTextField'">
      <os-html :content="displayValue" />
    </span>
    <span v-else>
      <span><span>{{displayValue}}</span></span>
    </span>
  </span>
</template>

<script>
import http from '@/common/services/HttpClient.js';
import util from '@/common/services/Util.js';

export default {
  props: ['field'],

  computed: {
    fileUrl: function() {
      let file = this.field.value;
      return http.getUrl('form-files/' + file.fileId + 
        '?contentType=' + file.contentType + '&filename=' + file.filename
      );
    },

    imageUrl: function() {
      return http.getUrl('form-files/' + this.field.value);
    },

    displayValue: function() {
      if (this.field.type == 'datePicker') {
        if (this.field.format.indexOf('HH:mm') >= 0) {
          return this.$filters.dateTime(+this.field.value);
        } else {
          return this.$filters.date(+this.field.value);
        }
      } else if (this.field.type == 'booleanCheckbox') {
        return this.$filters.boolValue(this.field.value);
      } else if (this.field.type == 'textArea' || this.field.type == 'stringTextField') {
        return util.linkify(this.field.displayValue || this.field.value);
      } else if (this.field.displayValue) {
        return this.field.displayValue;
      } else if (this.field.value instanceof Array) {
        return this.$filters.arrayJoin(this.field.value);
      } else {
        return this.field.value;
      }
    }
  }
}
</script>

<style scoped>
pre {
  background: transparent;
  border: none;
  font: inherit;
  font-size: inherit;
  padding: 0px;
  margin: 0px;
}
</style>
