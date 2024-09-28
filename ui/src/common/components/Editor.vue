<template>
  <os-message type="info" v-if="!initialised">
    <span>Initialising the editor...</span>
  </os-message>

  <div :id="id"></div>
</template>

<script>

import tinymce from 'tinymce/tinymce';

import 'tinymce/themes/silver/theme';

import 'tinymce/plugins/image';
import 'tinymce/plugins/importcss';
import 'tinymce/plugins/link';
import 'tinymce/plugins/lists';
import 'tinymce/plugins/textcolor';
import 'tinymce/plugins/table';
import 'tinymce/plugins/fullscreen';

import 'tinymce/skins/ui/oxide/skin.min.css';
import 'tinymce/icons/default';

export default {
  props: ['modelValue'],

  data() {
    return {
      id: null,

      initialised: false
    }
  },

  created() {
    this.id = Date.now() + '_' + Math.ceil(Math.random(100000));
  },

  mounted() {
    const opts = {
      selector: '#' + this.id,
      skin_url   : 'tinymce/skins',
      content_css: 'tinymce/skins',
      init_instance_callback : this._initEditor,
      convert_urls: false,
      height: 250,
      menubar: '',
      branding: false,
      elementpath: false,
      plugins: 'image, link, lists, textcolor, table, fullscreen',
      toolbar1: 'undo redo | formatselect | fontselect | fontsizeselect | removeformat | fullscreen',
      toolbar2: 'bold italic underline | insertfile link image | forecolor backcolor | ' +
                'bullist numlist | table | outdent indent | ' +
                'alignleft aligncenter alignright alignjustify '
    };
    setTimeout(() => { tinymce.init(opts); this.initialised = true }, 1000);
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
    getDisplayValue: function() {
      return this.modelValue;
    },

    _initEditor: function(editor) {
      editor.on('Change', () => {
        if (editor.getContent() !== this.inputValue) {
          this.inputValue = editor.getContent();
        }
      });

      editor.setContent(this.inputValue || '');
    }
  }
}
</script>

<style>
.tox.tox-tinymce {
  background: #ffffff;
  border: 1px solid #ced4da;
  transition: background-color 0.15s, border-color 0.15s, box-shadow 0.15s;
  border-radius: 4px;
  outline-color: transparent;
}

.tox .tox-statusbar {
  display: none!important;
}
</style>
