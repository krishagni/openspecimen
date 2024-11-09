export default {
  layout: {
    rows: [
      {
        fields: [
          {
            name: "file",
            type: "fileUpload",
            labelCode: "queries.select_query_json_file",
            url: () => window.osSvc.http.getUrl("saved-queries/definition-file"),
            headers: () => window.osSvc.http.headers,
            auto: false
          }
        ]
      }
    ]
  }
}
