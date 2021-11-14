
class ShipmentResources {
  createOpts    = {resource: 'ShippingAndTracking', operations: ['Create']};

  updateOpts    = {resource: 'ShippingAndTracking', operations: ['Update']};

  deleteOpts    = {resource: 'ShippingAndTracking', operations: ['Delete']};

  importOpts    = {resource: 'ShippingAndTracking', operations: ['Export Import']};
}

export default new ShipmentResources();
