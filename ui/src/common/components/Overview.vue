
<template>
  <ul class="os-key-values os-two-cols">
    <li class="item" v-for="(field, idx) of ctx.fields.simple" :key="idx">
      <strong class="key key-sm">{{field.caption}}</strong>
      <span class="value value-md">{{field.value}}</span>
    </li>
  </ul>

  <template v-for="(field, idx) of ctx.fields.textArea" :key="idx">
    <Section>
      <template #title>
        <span>{{field.caption}}</span>
      </template>
      <template #content>
        <span>{{field.value}}</span>
      </template>
    </Section>
  </template>
</template>

<script>
import { reactive, watchEffect } from 'vue';

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

    watchEffect(() => {
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
