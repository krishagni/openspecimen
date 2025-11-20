export default {
  layout: {
    rows: [
      {
        fields: [ { name: "specimen.receivedEvent.receivedQuality" } ]
      },

      {
        fields: [ {
          name: "specimen.receivedEvent.newLabel",
          type: "text",
          labelCode: "specimens.new_label",
          showWhen: "!wasReceived && allowSpmnRelabeling && specimen.receivedEvent.receivedQuality && specimen.receivedEvent.receivedQuality != 'To be Received'"
        } ]
      },

      {
        fields: [ { name: "specimen.receivedEvent.user" } ]
      },

      {
        fields: [ { name: "specimen.receivedEvent.time" } ]
      },

      {
        fields: [ { name: "specimen.receivedEvent.comments" } ]
      }
    ]
  }
}
