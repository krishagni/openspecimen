package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.EmailUtil;
import com.krishagni.catissueplus.core.common.util.Status;

@Configurable
public class CpCoordinatorsReportTask implements ScheduledTask {
	private final static String CP_COORD_REPORT = "cp_coord_report";

	@Autowired
	private DaoFactory daoFactory;

	@Override
	@PlusTransactional
	public void doJob(ScheduledJobRun job) {
		List<Long> cpIds = daoFactory.getCollectionProtocolDao().getAllCpIds();
		notifyPis(cpIds);
	}

	private void notifyPis(List<Long> cpIds) {
		for (int i = 0; i < cpIds.size(); i += 25) {
			List<CollectionProtocol> cps = getByIds(cpIds.subList(i, Math.min(i + 25, cpIds.size())));
			for (CollectionProtocol cp : cps) {
				if (Status.isClosedStatus(cp.getActivityStatus())) {
					continue;
				}

				notifyPi(cp);
			}
		}
	}

	private List<CollectionProtocol> getByIds(List<Long> cpIds) {
		return daoFactory.getCollectionProtocolDao().getByIds(cpIds);
	}

	private void notifyPi(CollectionProtocol cp) {
		if (cp.getCoordinators() == null || cp.getCoordinators().isEmpty()) {
			return;
		}

		String piEmail = cp.getPrincipalInvestigator().getEmailAddress();
		Map<String, Object> mailProps = getMailProps(cp);
		EmailUtil.getInstance().sendEmail(CP_COORD_REPORT, new String[] { piEmail }, null, mailProps);
	}

	private Map<String, Object> getMailProps(CollectionProtocol cp) {
		String adminEmail = getConfig("email", "admin_email_id", "");
		String institute  = getConfig("common", "hosting_institute", "{{common:hosting_institute}}");

		Map<String, Object> props = new HashMap<>();
		props.put("cpShortTitle", cp.getShortTitle());
		props.put("$subject", new String[] { cp.getShortTitle() });
		props.put("pi", cp.getPrincipalInvestigator().formattedName());
		props.put("coordinators", cp.getCoordinators().stream().map(User::formattedName).collect(Collectors.joining(", ")));
		props.put("helpdeskEmail", adminEmail);
		props.put("contactEmail", adminEmail);
		props.put("hostingInstitute", institute);
		return props;
	}

	private String getConfig(String module, String attr, String defValue) {
		return ConfigUtil.getInstance().getStrSetting(module, attr, defValue);
	}
}