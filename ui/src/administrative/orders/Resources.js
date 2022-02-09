
class OrderResources {
  createOpts    = {resource: 'Order', operations: ['Create']};

  updateOpts    = {resource: 'Order', operations: ['Update']};

  deleteOpts    = {resource: 'Order', operations: ['Delete']};

  importOpts    = {resource: 'Order', operations: ['Export Import']};

  dpOpts        = {resource: 'DistributionProtocol', operations: ['Read']};
}

export default new OrderResources();
