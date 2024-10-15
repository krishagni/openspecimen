export default {
  layout: {
    rows: [
      {
        fields: [
          {
            name: "file",
            type: "fileUpload",
            labelCode: "cps.select_cp_json",
            url: () => window.osSvc.http.getUrl("collection-protocols/definition"),
            headers: () => window.osSvc.http.headers,
            auto: false
          }
        ]
      }
    ]
  }
}
