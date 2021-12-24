package com.krishagni.catissueplus.core.biospecimen.services.impl;

import org.apache.commons.lang3.StringUtils;


import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolRegistrationDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.CollectionProtocolRegistrationService;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.de.services.impl.ExtensionsUtil;
import com.krishagni.catissueplus.core.importer.events.ImportObjectDetail;
import com.krishagni.catissueplus.core.importer.services.ObjectImporter;

public class CprImporter implements ObjectImporter<CollectionProtocolRegistrationDetail, CollectionProtocolRegistrationDetail> {

	private static final LogUtil logger = LogUtil.getLogger(CprImporter.class);
	
	private CollectionProtocolRegistrationService cprSvc;

	private DaoFactory daoFactory;
	
	public void setCprSvc(CollectionProtocolRegistrationService cprSvc) {
		this.cprSvc = cprSvc;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public ResponseEvent<CollectionProtocolRegistrationDetail> importObject(RequestEvent<ImportObjectDetail<CollectionProtocolRegistrationDetail>> req) {
		try {
			ImportObjectDetail<CollectionProtocolRegistrationDetail> detail = req.getPayload();

			CollectionProtocolRegistrationDetail cpr = detail.getObject();
			cpr.setForceDelete(true);

			ParticipantDetail participant = cpr.getParticipant();
			if (participant != null) {
				ExtensionsUtil.initFileFields(detail.getUploadedFilesDir(), participant.getExtensionDetail());
			} else {
				participant = new ParticipantDetail();
				cpr.setParticipant(participant);
			}

			if (StringUtils.isBlank(participant.getSource())) {
				participant.setSource(Participant.DEF_SOURCE);
			}

			boolean update = false; // assuming it is create op to start with
			switch (detail.getType()) {
				case "UPSERT":
					if (cpr.getId() != null) {
						update = true;
					} else {
						CollectionProtocolRegistration dbReg = getRegistration(cpr.getCpShortTitle(), cpr.getPpid());
						if (dbReg != null) {
							cpr.setId(dbReg.getId());
							update = true;
						}
					}
					break;

				case "UPDATE":
					update = true;
					break;
			}

			return upsert(cpr, update);
		} catch (Exception e) {
			logger.error("Error importing the participant registration", e);
			return ResponseEvent.serverError(e);
		}
	}

	private CollectionProtocolRegistration getRegistration(String cpShortTitle, String ppid) {
		if (StringUtils.isBlank(cpShortTitle) || StringUtils.isBlank(ppid)) {
			return null;
		}

		return daoFactory.getCprDao().getCprByCpShortTitleAndPpid(cpShortTitle, ppid);
	}

	private ResponseEvent<CollectionProtocolRegistrationDetail> upsert(CollectionProtocolRegistrationDetail cpr, boolean update) {
		if (update) {
			return cprSvc.updateRegistration(RequestEvent.wrap(cpr));
		} else {
			return cprSvc.createRegistration(RequestEvent.wrap(cpr));
		}
	}
}
