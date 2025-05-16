package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationListener;

import com.krishagni.catissueplus.core.biospecimen.ConfigParams;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolSavedEvent;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolSummary;
import com.krishagni.catissueplus.core.common.TransactionCache;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.EmailUtil;

public class CollectionProtocolChangeListener implements ApplicationListener<CollectionProtocolSavedEvent> {

	@Override
	public void onApplicationEvent(CollectionProtocolSavedEvent event) {
		boolean enabled = ConfigUtil.getInstance().getBoolSetting(ConfigParams.MODULE, ConfigParams.CP_VERSIONING_ENABLED, false);
		if (!enabled) {
			return;
		}

		CollectionProtocol cp = event.getEventData();
		if (cp.isDraftMode()) {
			return;
		}

		cp.setDraftMode(true);

		Map<Long, CollectionProtocolDetail> modifiedCps = TransactionCache.getInstance().get("modifiedCps");
		if (modifiedCps == null) {
			final Map<Long, CollectionProtocolDetail> modifiedCpsList = modifiedCps = new HashMap<>();
			TransactionCache.getInstance().put("modifiedCps", modifiedCps,
				(status) -> {
					if (status == 0) {
						modifiedCpsList.values().forEach(this::notifyCpMovedToDraft);
					}
				}
			);
		}

		if (!modifiedCps.containsKey(cp.getId())) {
			CollectionProtocolDetail detail = new CollectionProtocolDetail();
			CollectionProtocolSummary.copy(cp, detail);
			detail.setCoordinators(UserSummary.from(cp.getCoordinators()));
			modifiedCps.put(cp.getId(), detail);
		}
	}

	private void notifyCpMovedToDraft(CollectionProtocolDetail cp) {
		UserSummary user = UserSummary.from(AuthUtil.getCurrentUser());
		Map<String, Object> emailProps = new HashMap<>();
		emailProps.put("cp", cp);
		emailProps.put("user", user);
		emailProps.put("$subject", new Object[] { cp.getShortTitle() });

		List<UserSummary> users = new ArrayList<>();
		users.add(user);
		users.add(cp.getPrincipalInvestigator());
		users.addAll(cp.getCoordinators());

		for (UserSummary rcpt : users) {
			emailProps.put("rcpt", rcpt);
			EmailUtil.getInstance().sendEmail(NOTIFY_DRAFT_CP, new String[] { rcpt.getEmailAddress() }, new File[0], emailProps);
		}
	}

	private static final String NOTIFY_DRAFT_CP = "cp_moved_to_draft";
}
