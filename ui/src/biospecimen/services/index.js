import cartSvc        from './SpecimenCart.js';
import consentStmtSvc from './ConsentStatement.js';
import cpSvc          from './CollectionProtocol.js';
import folderSvc      from './SpecimenCartsFolder.js';
import specimenSvc    from './Specimen.js';
import visitSvc       from './Visit.js';

export default {
  install(app) {
    let osSvc = app.config.globalProperties.$osSvc = app.config.globalProperties.$osSvc || {};
    Object.assign(osSvc, {
      cartSvc       : cartSvc,
      cartFolderSvc : folderSvc,
      cpSvc         : cpSvc,
      consentStmtSvc: consentStmtSvc,
      specimenSvc   : specimenSvc,
      visitSvc      : visitSvc
    });
  }
}
