<template>
  <os-message type="info" v-if="!initialised">
    <span>Initialising the editor...</span>
  </os-message>

  <div :id="id"></div>
</template>

<script>
import tinymce from 'tinymce/tinymce';

export default {
  props: ['modelValue'],

  data() {
    return {
      id: null,

      initialised: false
    }
  },

  created() {
    this.id = 'editor_' + Date.now() + '_' + Math.ceil(Math.random(100000));
  },

  mounted() {
    const opts = {
      selector: '#' + this.id,
      skin: false,
      content_css: false,
      init_instance_callback : this._initEditor,
      convert_urls: false,
      height: 250,
      menubar: '',
      branding: false,
      elementpath: false,
      plugins: 'image, link, lists, table, fullscreen',
      toolbar1: 'undo redo | blocks | fontfamily | fontsize | removeformat | fullscreen',
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

.tox .tox-promotion {
  display: none!important;
}

.tox .tox-menubar {
  display: none!important;
}

.tox .tox-editor-header {
  box-shadow: none!important;
  border-bottom: 1px solid #ced4da!important;
}

.tox .tox-edit-area::before {
  border: 1px solid #007bff!important;
  box-shadow: 0 0 0 0.2rem rgba(38, 143, 255, 0.5);
}
</style>
