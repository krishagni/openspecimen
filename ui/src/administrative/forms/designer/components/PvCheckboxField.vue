<template>
  <PvField :field="field" :preview="preview" :no-label="noLabel"
    :main="main" :sub-form="subForm" display-type="checkbox" />
</template>

<script>
import PvField from './PvField.vue';

export default {
  components: { PvField },
  props: {
    field: Object,
    preview: Boolean,
    noLabel: Boolean,
    main: Object,
    subForm: Object
  }
};
</script>
