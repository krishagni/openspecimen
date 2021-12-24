package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;


import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CprErrorCode;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolRegistrationDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantRegistrationsList;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.CollectionProtocolRegistrationService;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.de.services.impl.ExtensionsUtil;
import com.krishagni.catissueplus.core.importer.events.ImportObjectDetail;
import com.krishagni.catissueplus.core.importer.services.ObjectImporter;

public class MultiCprImporter implements ObjectImporter<ParticipantRegistrationsList, ParticipantRegistrationsList> {

	private static final LogUtil logger = LogUtil.getLogger(MultiCprImporter.class);

	private CollectionProtocolRegistrationService cprSvc;

	private DaoFactory daoFactory;

	public void setCprSvc(CollectionProtocolRegistrationService cprSvc) {
		this.cprSvc = cprSvc;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public ResponseEvent<ParticipantRegistrationsList> importObject(RequestEvent<ImportObjectDetail<ParticipantRegistrationsList>> req) {
		try {
			ImportObjectDetail<ParticipantRegistrationsList> detail = req.getPayload();

			ParticipantRegistrationsList regsList = detail.getObject();
			regsList.setForceDelete(true);

			ParticipantDetail participant = regsList.getParticipant();
			if (participant == null) {
				participant = new ParticipantDetail();
				regsList.setParticipant(participant);
			} else {
				ExtensionsUtil.initFileFields(detail.getUploadedFilesDir(), participant.getExtensionDetail());
			}

			if (StringUtils.isBlank(participant.getSource())) {
				participant.setSource(Participant.DEF_SOURCE);
			}

			if (detail.isCreate()) {
				if (regsList.getRegistrations() == null || regsList.getRegistrations().isEmpty()) {
					return ResponseEvent.userError(CprErrorCode.CP_REQUIRED);
				}

				return cprSvc.createRegistrations(RequestEvent.wrap(regsList));
			} else if ("UPDATE".equals(detail.getType())) {
				return cprSvc.updateRegistrations(RequestEvent.wrap(regsList));
			} else {
				ParticipantRegistrationsList toUpdate = null;
				ParticipantRegistrationsList toInsert = null;

				for (CollectionProtocolRegistrationDetail cpr : regsList.getRegistrations()) {
					boolean update = false;
					if (cpr.getId() != null) {
						update = true;
					} else {
						CollectionProtocolRegistration dbReg = getRegistration(cpr.getCpShortTitle(), cpr.getPpid());
						if (dbReg != null) {
							cpr.setId(dbReg.getId());
							update = true;
						}
					}

					if (update) {
						if (toUpdate == null) {
							toUpdate = new ParticipantRegistrationsList();
							toUpdate.setParticipant(regsList.getParticipant());
							toUpdate.setRegistrations(new ArrayList<>());
						}

						toUpdate.getRegistrations().add(cpr);
					} else {
						if (toInsert == null) {
							toInsert = new ParticipantRegistrationsList();
							toInsert.setParticipant(regsList.getParticipant());
							toInsert.setRegistrations(new ArrayList<>());
						}

						toInsert.getRegistrations().add(cpr);
					}
				}

				ResponseEvent<ParticipantRegistrationsList> resp = null;
				if (toUpdate != null) {
					resp = cprSvc.updateRegistrations(RequestEvent.wrap(toUpdate));
				}

				if (resp != null && !resp.isSuccessful()) {
					return resp;
				}

				if (toInsert != null) {
					if (resp != null) {
						ParticipantDetail savedParticipant = resp.getPayload().getParticipant();
						toInsert.setParticipant(savedParticipant);
					}

					resp = cprSvc.createRegistrations(RequestEvent.wrap(toInsert));
				}

				return resp;
			}
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	private CollectionProtocolRegistration getRegistration(String cpShortTitle, String ppid) {
		if (StringUtils.isBlank(cpShortTitle) || StringUtils.isBlank(ppid)) {
			return null;
		}

		return daoFactory.getCprDao().getCprByCpShortTitleAndPpid(cpShortTitle, ppid);
	}
}
