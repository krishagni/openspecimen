
<template>
  <div v-if="!inputValue">
    <FileUpload ref="uploader" mode="basic" name="file" :auto="auto != false" :url="url" :tabindex="tabOrder"
      :disabled="disabled" @before-send="addHeaders" @upload="onUpload" @error="onError" />
  </div>
  <div v-else class="os-selected-file">
    <span class="filename">{{inputValue.filename}}</span>
    <Button left-icon="times" :disabled="disabled" @click="removeFile"/>
  </div>
</template>

<script>
import FileUpload from 'primevue/fileupload';

import Button from '@/common/components/Button.vue';

import alertSvc from '@/common/services/Alerts.js';

export default {
  props: ['name', 'url', 'modelValue', 'headers', 'auto', 'tabOrder', 'disabled'],

  components: {
    Button,
    FileUpload
  },

  computed: {
    inputValue: {
      get() {
        return this.modelValue;
      },

      set(value) {
        this.$emit('update:modelValue', value);
      }
    }
  },

  methods: {
    upload: function() {
      if (!this.$refs.uploader.hasFiles) {
        alertSvc.error({code: 'common.file_not_selected'});
        return;
      }

      return new Promise((resolve, reject) => {
        this.$refs.uploader.upload();
        this.resolve = resolve;
        this.reject = reject;
      });
    },

    hasFiles: function() {
      return this.$refs.uploader.hasFiles;
    },

    addHeaders: function({xhr}) {
      if (this.headers) {
        Object.keys(this.headers).forEach((name) => xhr.setRequestHeader(name, this.headers[name]));
      }
    },

    onUpload: function({xhr}) {
      const response = JSON.parse(xhr.responseText);
      this.$emit('update:modelValue', response);

      if (this.resolve) {
        this.resolve(response);
        this.resolve = undefined;
      }

      if (this.auto != false) {
        alertSvc.success({code: 'common.file_uploaded'});
      }
    },

    onError: function({xhr}) {
      try {
        const errors = JSON.parse(xhr.responseText);
        if (this.reject) {
          this.reject(errors);
          this.reject = undefined;
        }

        if (errors instanceof Array) {
          let msg = errors.map(err => err.message + ' (' + err.code + ')').join(',');
          alertSvc.error(msg);
        } else if (errors) {
          alertSvc.error(errors);
        } else {
          alertSvc.error({code: 'common.file_upload_error'});
        }
      } catch {
        alertSvc.error(xhr.status + ': ' + xhr.responseText);
      }
    },

    removeFile: function() {
      this.$emit('update:modelValue', null);
    },

    getDisplayValue: function() {
      if (this.inputValue) {
        return this.inputValue.filename;
      }

      return null;
    }
  }
}
</script>

<style scoped>
.os-selected-file {
  display: flex;
}
  
.os-selected-file .filename {
  flex: 1 1 auto;
  padding: 0.5rem 0.75rem;
  border: 1px solid;
  border-color: #ced4da;
  border-radius: 4px;
  border-top-right-radius: 0px;
  border-bottom-right-radius: 0px;
  border-right: 0px;
}

.os-selected-file button.btn {
  width: 2.357rem;
  display: inline-flex;
  padding: 0.5rem 0;
  align-items: center;
  justify-content: center;
  margin-right: 0px!important;
  border: 1px solid;
  border-color: #007bff;
  border-top-left-radius: 0px;
  border-bottom-left-radius: 0px;
  background: #fff;
}

.os-selected-file button.btn:hover {
  background: #0069d9;
  color: #ffffff;
  border-color: #0069d9;
}

</style>
