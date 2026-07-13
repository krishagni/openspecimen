<template>
  <span class="os-query-column-text">
    <template v-for="(segment, idx) of segments" :key="idx">
      <a v-if="segment.url" :href="segment.url" target="_blank" rel="noopener">
        <span>{{segment.text}}</span>
      </a>
      <span v-else>{{segment.text}}</span>
    </template>
  </span>
</template>

<script>
export default {
  props: ['params'],

  data() {
    return {
      value: ''
    }
  },

  created() {
    this._updateDisplay(this.params);
  },

  computed: {
    segments: function() {
      return this._getSegments(this.value);
    }
  },

  methods: {
    refresh: function(params) {
      this._updateDisplay(params);
      return true;
    },

    _updateDisplay: function(params) {
      this.value = (params || {}).value;
    },

    _getSegments: function(value) {
      if (value == null || value == undefined) {
        return [];
      }

      const text = value.toString();
      const segments = [];
      const urlRe = /((https?|ftp):\/\/[^\s<>"']+|www\.[^\s<>"']+)/ig;
      let lastIdx = 0;
      let match = null;

      while ((match = urlRe.exec(text)) != null) {
        const candidate = match[0];
        const urlText = this._getUrlText(candidate);
        const url = this._getUrl(urlText);

        if (!url) {
          continue;
        }

        if (match.index > lastIdx) {
          segments.push({text: text.substring(lastIdx, match.index)});
        }

        segments.push({text: urlText, url: url});

        if (candidate.length > urlText.length) {
          segments.push({text: candidate.substring(urlText.length)});
        }

        lastIdx = match.index + candidate.length;
        urlRe.lastIndex = lastIdx;
      }

      if (lastIdx < text.length) {
        segments.push({text: text.substring(lastIdx)});
      }

      return segments.length > 0 ? segments : [{text: text}];
    },

    //
    // remove the trailing punctuations from the detected URL text
    // Click here: www.google.com. turns www.google.com. to www.google.com
    //
    _getUrlText: function(text) {
      return text.replace(/[),.;:!?]+$/g, '');
    },

    _getUrl: function(text) {
      try {
        const startsWithWww = text.indexOf('www.') == 0;
        const url = startsWithWww ? 'http://' + text : text;
        const parsedUrl = new URL(url);
        if (['http:', 'https:', 'ftp:'].indexOf(parsedUrl.protocol) == -1 || !parsedUrl.hostname) {
          return null;
        }

        if (startsWithWww && parsedUrl.hostname.indexOf('.') == -1) {
          return null;
        }

        return url;
      } catch (e) {
        return null;
      }
    }
  }
}
</script>

<style scoped>
.os-query-column-text {
  overflow-wrap: anywhere;
  white-space: pre-wrap;
}
</style>
