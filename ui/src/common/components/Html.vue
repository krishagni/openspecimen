<template>
  <span v-html="sanitizedContent" />
</template>

<script>

import util from "@/common/services/Util.js";

export default {
  props: ["content"],

  computed: {
    sanitizedContent: function() {
      return util.sanitizeHtml(this.content);
    }
  }
}
</script>
