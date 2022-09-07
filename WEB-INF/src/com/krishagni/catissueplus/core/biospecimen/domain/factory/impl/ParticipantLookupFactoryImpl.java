package com.krishagni.catissueplus.core.biospecimen.domain.factory.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;


import com.krishagni.catissueplus.core.biospecimen.ConfigParams;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ParticipantErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ParticipantLookupFactory;
import com.krishagni.catissueplus.core.biospecimen.matching.ParticipantLookupLogic;
import com.krishagni.catissueplus.core.common.OpenSpecimenAppCtxProvider;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;

public class ParticipantLookupFactoryImpl implements ParticipantLookupFactory, InitializingBean {
	private static final LogUtil logger = LogUtil.getLogger(ParticipantLookupFactoryImpl.class);

	private ParticipantLookupLogic defaultParticipantLookupFlow;

	private ParticipantLookupLogic participantLookupLogic;

	private ConfigurationService cfgSvc;

	public void setDefaultParticipantLookupFlow(ParticipantLookupLogic defaultParticipantLookupFlow) {
		this.defaultParticipantLookupFlow = defaultParticipantLookupFlow;
	}

	public void setParticipantLookupLogic(ParticipantLookupLogic participantLookupLogic) {
		this.participantLookupLogic = participantLookupLogic;
	}

	public void setCfgSvc(ConfigurationService cfgSvc) {
		this.cfgSvc = cfgSvc;
	}

	@Override
	public ParticipantLookupLogic getLookupLogic() {
		if (participantLookupLogic == null) {
			initParticipantLookupFlow(ConfigUtil.getInstance().getStrSetting(ConfigParams.MODULE, ConfigParams.PARTICIPANT_LOOKUP_FLOW));
		}

		return participantLookupLogic;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		cfgSvc.registerChangeListener(
			ConfigParams.MODULE,
			(name, value) -> {
				if (StringUtils.isBlank(name) || ConfigParams.PARTICIPANT_LOOKUP_FLOW.equals(name)) {
					participantLookupLogic = null;
				}
			}
		);
	}

	private void initParticipantLookupFlow(String lookupFlow) {
		if (StringUtils.isBlank(lookupFlow)) {
			participantLookupLogic = defaultParticipantLookupFlow;
			return;
		}

		ParticipantLookupLogic result = null;
		try {
			lookupFlow = lookupFlow.trim();
			if (lookupFlow.startsWith("bean:")) {
				result = OpenSpecimenAppCtxProvider.getBean(lookupFlow.substring("bean:".length()).trim());
			} else {
				String className = lookupFlow;
				if (lookupFlow.startsWith("class:")) {
					className = lookupFlow.substring("class:".length()).trim();
				}


				Class<ParticipantLookupLogic> klass = (Class<ParticipantLookupLogic>) Class.forName(className);
				result = BeanUtils.instantiate(klass);
			}
		} catch (Exception e) {
			logger.info("Invalid participant lookup flow configuration setting: " + lookupFlow, e);
		}

		if (result == null) {
			throw OpenSpecimenException.userError(ParticipantErrorCode.INVALID_LOOKUP_FLOW, lookupFlow);
		}

		participantLookupLogic = result;
	}
}
