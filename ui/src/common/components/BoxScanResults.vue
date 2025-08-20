<template>
  <div class="scan-results">
    <os-message type="info">
      <span v-t="{ path: 'containers.scan_barcodes_summary', args: infoCtx }"> </span>
    </os-message>

    <os-message type="error" v-if="errorCtx">
      <span v-t="errorCtx"></span>
    </os-message>

    <os-message type="error" v-if="readErrorCtx">
      <span v-t="readErrorCtx" />
    </os-message>
  </div>
</template>

<script>
export default {
  props: ['scan-results', 'not-found-message-code', 'read-error-message-code'],

  computed: {
    infoCtx: function() {
      const {scannedBarcodesCount, readErrorsCount, noTubesCount} = this.scanResults;
      return {scannedBarcodesCount, readErrorsCount, noTubesCount};
    },

    errorCtx: function() {
      const {notFound} = this.scanResults || {};
      if (notFound && notFound.length > 0) {
        const path = this.notFoundMessageCode || 'containers.specimens_not_found_ids';
        const args = {
          ids: notFound.map(({barcode, mode, position, row, column}) =>
            barcode + ' (' + (mode == 'LINEAR' ? position : (row + ', ' + column)) + ')'
          ).join(', ')
        }

        return {path, args};
      }

      return null;
    },

    readErrorCtx: function() {
      const {tubes} = this.scanResults || {};
      const readErrors = (tubes || []).filter(tube => tube.barcode == 'READ_ERROR');
      if (readErrors.length > 0) {
        const path = this.readErrorMessageCode || 'containers.cannot_read_barcodes';
        const args = {
          locations: readErrors.map(
            ({mode, position, row, column}) => mode == 'LINEAR' ? position :  ('(' + row + ', ' + column + ')')
          ).join(', ')
        }

        return {path, args};
      }

      return null;
    }
  }
}
</script>
