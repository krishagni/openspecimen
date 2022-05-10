
export default {
  layout: {
    "rows": [
      {
        "fields": [
          {
            "name": "defrag.aliquotsInSameContainer",
            "label": "Do you want aliquots of the same specimen to be moved together?",
            "type": "radio",
            "options": [
              { "value": true, caption: "Yes" },
              { "value": false, caption: "No" }
            ],
            "optionsPerRow": 2
          }
        ]
      }
    ]
  }
}
