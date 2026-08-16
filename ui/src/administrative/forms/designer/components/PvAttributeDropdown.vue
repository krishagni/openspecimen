<template>
  <AutoComplete v-model="selectedAttribute" field="name" :suggestions="attributes"
    :dropdown="true" :force-selection="true" @complete="searchAttributes">
    <template #item="slotProps">
      <div>{{ slotProps.item.name }}</div>
      <small>{{ slotProps.item.attribute }}</small>
    </template>
  </AutoComplete>
</template>

<script>
import AutoComplete from 'primevue/autocomplete';

import http from '@/common/services/HttpClient.js';

export default {
  components: {
    AutoComplete
  },

  props: {
    modelValue: String,
    formId: Number
  },

  emits: ['update:modelValue', 'select'],

  data() {
    return {
      attributes: [],

      selectedAttribute: null
    };
  },

  watch: {
    modelValue: {
      immediate: true,

      handler(attribute) {
        this._loadSelectedAttribute(attribute);
      }
    },

    selectedAttribute(attribute) {
      if (!attribute) {
        if (this.modelValue) {
          this.$emit('update:modelValue', null);
          this.$emit('select', null);
        }
        return;
      }

      if (attribute.attribute == this.modelValue) {
        return;
      }

      this.$emit('update:modelValue', attribute.attribute);
      this.$emit('select', attribute);
    }
  },

  mounted() {
    this._loadAttributes();
  },

  methods: {
    //
    // Searches attributes by both caption and internal attribute name.
    //
    searchAttributes(event) {
      this._loadAttributes(event.query.trim());
    },

    //
    // Loads the selected attribute explicitly because it might not be present in the first page.
    //
    async _loadSelectedAttribute(attribute) {
      if (!attribute) {
        this.selectedAttribute = null;
        return;
      }

      if (this.selectedAttribute?.attribute == attribute) {
        return;
      }

      const list = await http.get('permissible-values/attributes', {formId: this.formId, attribute, maxResults: 100});
      this.selectedAttribute = list.find(attr => attr.attribute == attribute) || null;
      this._mergeAttributes(list);
      this.$emit('select', this.selectedAttribute);
    },

    //
    // Loads the first attribute page or matching attributes for the supplied search text.
    //
    async _loadAttributes(query) {
      const params = {formId: this.formId, maxResults: 100};
      if (!query) {
        this._mergeAttributes(await http.get('permissible-values/attributes', params));
        return;
      }

      const [byCaption, byAttribute] = await Promise.all([
        http.get('permissible-values/attributes', {...params, name: query}),
        http.get('permissible-values/attributes', {...params, attribute: query})
      ]);
      this._mergeAttributes([...byCaption, ...byAttribute]);
    },

    //
    // Deduplicates results while retaining the selected attribute for display.
    //
    _mergeAttributes(list) {
      const attributes = new Map();
      if (this.selectedAttribute) {
        attributes.set(this.selectedAttribute.attribute, this.selectedAttribute);
      }

      list.forEach(attr => attributes.set(attr.attribute, attr));
      this.attributes = Array.from(attributes.values());
    }
  }
};
</script>
