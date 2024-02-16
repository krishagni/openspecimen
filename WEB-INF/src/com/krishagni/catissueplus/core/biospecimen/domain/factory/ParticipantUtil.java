package com.krishagni.catissueplus.core.biospecimen.domain.factory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.ConfigParams;
import com.krishagni.catissueplus.core.biospecimen.WorkflowUtil;
import com.krishagni.catissueplus.core.biospecimen.domain.CpWorkflowConfig;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.events.PmiDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.OpenSpecimenAppCtxProvider;
import com.krishagni.catissueplus.core.common.errors.ErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.service.MpiGenerator;
import com.krishagni.catissueplus.core.common.service.impl.DefaultMpiGenerator;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.RegexValidator;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.common.util.Validator;

public class ParticipantUtil {
	public static boolean ensureUniqueUid(DaoFactory daoFactory, String uid, OpenSpecimenException ose) {
		if (StringUtils.isBlank(uid)) {
			return true;
		}
		
		if (!daoFactory.getParticipantDao().isUidUnique(uid)) {
			ose.addError(ParticipantErrorCode.DUP_UID, uid);
			return false;
		}
		
		return true;
	}
	
	public static boolean ensureUniqueEmpi(DaoFactory daoFactory, String empi, OpenSpecimenException ose) {
		if (StringUtils.isBlank(empi)) {
			return true;
		}
		
		if (daoFactory.getParticipantDao().getByEmpi(empi) != null) {
			ose.addError(ParticipantErrorCode.DUP_EMPI, empi);
			return false;
		}
		
		return true;
	}

	public static boolean ensureUniquePmis(DaoFactory daoFactory, List<PmiDetail> pmis, Participant participant, OpenSpecimenException ose) {
		List<Long> participantIds = daoFactory.getParticipantDao().getParticipantIdsByPmis(pmis);
		if (CollectionUtils.isEmpty(participantIds)) { 
			// no one own these pmis yet
			return true;
		}
		
		if (participant.getId() == null) { // create mode
			ose.addError(ParticipantErrorCode.DUP_MRN);
			return false;
		} else {
			for (Long participantId : participantIds) {
				if (!participant.getId().equals(participantId)) {
					ose.addError(ParticipantErrorCode.DUP_MRN);
					return false;
				}
			}			
		}
		
		return true;
	}
	
	public static boolean isValidMpi(String empi, OpenSpecimenException ose) {
		return isValidInput(
				empi, 
				ConfigParams.MPI_PATTERN, 
				ConfigParams.MPI_VALIDATOR, 
				ParticipantErrorCode.INVALID_MPI, 
				ose);
	}
	
	public static boolean isValidUid(String uid, OpenSpecimenException ose) {
		return isValidInput(
				uid, 
				ConfigParams.PARTICIPANT_UID_PATTERN, 
				ConfigParams.PARTICIPANT_UID_VALIDATOR, 
				ParticipantErrorCode.INVALID_UID, 
				ose);
	}
	
	public static MpiGenerator getMpiGenerator() {
		String mpiFormat = getMpiCfgProp(ConfigParams.MPI_FORMAT);
		if (StringUtils.isNotBlank(mpiFormat)) {
			return new DefaultMpiGenerator(mpiFormat);
		} 
		
		String mpiGeneratorBean = getMpiCfgProp(ConfigParams.MPI_GENERATOR);
		if (StringUtils.isNotBlank(mpiGeneratorBean)) {
	  		return (MpiGenerator)OpenSpecimenAppCtxProvider.getAppCtx().getBean(mpiGeneratorBean);
		}
		
		return null;
	}

	public static void ensureLockedFieldsAreUntouched(Participant existing, Participant newParticipant) {
		if (existing.getSource().equals(newParticipant.getSource())) {
			//
			// No locked fields check when both existing and new participant objects
			// are from same data source
			//
			return;
		}

		List<String> diff = Utility.diff(existing, newParticipant, getLockedFields(existing.getSource()));
		if (!diff.isEmpty()) {
			String errFields = diff.stream().map(f -> PART_FIELD_PREFIX + f).collect(Collectors.joining(", "));
			throw OpenSpecimenException.userError(ParticipantErrorCode.LF_UPDATE_NOT_ALLOWED, errFields);
		}
	}

	public static List<String> getLockedFields(String source) {
		if (StringUtils.isBlank(source)) {
			return Collections.emptyList();
		}

		List<String> lockedFields = Collections.emptyList();
		CpWorkflowConfig.Workflow workflow = WorkflowUtil.getInstance().getSysWorkflow(LOCKED_FIELDS);
		if (workflow != null && workflow.getData() != null) {
			Map<String, Object> wfData = (Map<String, Object>)workflow.getData().get("participant");
			if (wfData != null && wfData.get(source) instanceof List) {
				lockedFields = (List<String>)wfData.get(source);
			}
		}

		return lockedFields.stream()
			.filter(field -> field.startsWith(PART_FIELD_PREFIX))
			.map(field -> field.substring(PART_FIELD_PREFIX.length()))
			.collect(Collectors.toList());
	}

	private static boolean isValidInput(String input, String patternCfg, String validatorCfg, ErrorCode error, OpenSpecimenException ose) {
		String pattern = ConfigUtil.getInstance().getStrSetting(ConfigParams.MODULE, patternCfg, null);
		
		if (StringUtils.isNotBlank(pattern)) {
			if (!RegexValidator.validate(pattern, input)) {
				ose.addError(error, input);
				return false;
			}
			
			return true;
		}
		
		String validatorName = ConfigUtil.getInstance().getStrSetting(ConfigParams.MODULE, validatorCfg, null);
		if (StringUtils.isBlank(validatorName)) {
			return true;
		}
		
		Validator validator = OpenSpecimenAppCtxProvider.getBean(validatorName);
		return validator.validate(input, ose);		
	}
	
	private static String getMpiCfgProp(String property) {
		return ConfigUtil.getInstance().getStrSetting(ConfigParams.MODULE, property, null);
	}

	private static String LOCKED_FIELDS = "locked-fields";

	private static String PART_FIELD_PREFIX = "cpr.participant.";

}
