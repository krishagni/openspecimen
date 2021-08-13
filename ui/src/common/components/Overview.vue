
<template>
  <ul class="os-key-values os-two-cols">
    <li class="item" v-for="(field, idx) of ctx.fields.simple" :key="idx">
      <strong class="key key-sm">{{field.caption}}</strong>
      <span class="value value-md">{{field.value}}</span>
    </li>
  </ul>

  <template v-for="(field, idx) of ctx.fields.textArea" :key="idx">
    <Section :title="field.caption" :content="field.value" />
  </template>
</template>

<script>
import { reactive, watch } from 'vue';

import Section from '@/common/components/Section.vue';

export default {
  props: ['object', 'schema'],

  components: {
    Section
  },

  setup(props) {
    let ctx = reactive({
      fields: {}
    });

    watch(() => {
      let simpleFields = [], textAreaFields = [];
      for (let field of props.schema) {
        let value = props.object[field.name];
        if (value === null || value === undefined || value === '') {
          value = '-';
        } else if (field.options instanceof Array) {
          let option = field.options.find((option) => option.value == value);
          if (option) {
            value = option.caption;
          }
        } else if (field.displayValues) {
          let dispValue = field.displayValues[value];
          if (dispValue) {
            value = dispValue;
          }
        }

        if (field.type == 'textarea') {
          textAreaFields.push({caption: field.label, value: value});
        } else {
          simpleFields.push({caption: field.label, value: value});
        }
      }

      ctx.fields = {
        simple: simpleFields,
        textArea: textAreaFields
      }
    });

    return {
      ctx
    };
  }
}
</script>

<style scoped>

.os-key-values {
  list-style-type: none;
  margin: 0px 0px 20px 0px;
  padding: 0;
}

.os-key-values:after {
  content: '';
  display: block;
  clear: both;
}

.os-key-values .item {
  margin: 2px 0px 0px 0px;
  position: relative;
  padding-left: 150px;
  -webkit-column-break-inside: avoid;
  page-break-inside: avoid;
  break-inside: avoid-column;
  display: table;
}

.os-key-values.bg-col .item {
  padding-left: 300px;
}

.os-key-values.md-col .item {
  padding-left: 250px;
}

.os-one-col > .item {
  width: 100%;
}

.os-key-values.os-two-cols {
  column-count: 2;
  -webkit-perspective: 1;
}

.os-key-values .item .key, .os-key-values .item .value {
  overflow: hidden;
  text-overflow: ellipsis;
  padding: 2px 5px 2px 5px;
  line-height: 24px;
  width: 100%;
  float: left;
}

.os-key-values .item .key {
  color: #707070;
  font-weight: normal;
  clear: left;
  margin-left: -150px;
  width: 150px;
}

.os-key-values.md-col .item .key {
  margin-left: -250px;
  width: 250px;
}

.os-key-values.bg-col .item .key {
  margin-left: -300px;
  width: 300px;
}

.os-key-values.vertical .item {
  padding-left: 0px;
  width: 100%;
}

.os-key-values.vertical .item:after {
  content: ' ';
  display: inline-block;
  border-bottom: 1px solid #ddd;
  width: 100%;
}

.os-key-values.vertical .item:last-child:after {
  width: 0%;
}

.os-key-values.vertical .item .key.key-sm {
  margin-left: 0px;
  width: 100%;
}

.os-key-values.vertical .item .key.key-sm.strong {
  font-weight: 500;
  font-style: italic;
}

@media only screen and (max-width: 768px) {
  .os-key-values.os-two-cols {
    column-count: 1;
    -webkit-perspective: 1;
  }
}
</style>
