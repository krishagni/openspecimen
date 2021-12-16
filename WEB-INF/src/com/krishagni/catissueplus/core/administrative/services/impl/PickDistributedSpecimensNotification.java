package com.krishagni.catissueplus.core.administrative.services.impl;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;


import com.krishagni.catissueplus.core.administrative.domain.DistributionOrder;
import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.util.EmailUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

@Configurable
public class PickDistributedSpecimensNotification implements ScheduledTask {
	private static final LogUtil logger = LogUtil.getLogger(PickDistributedSpecimensNotification.class);

	private static final String PICK_DIST_SPMNS_TMPL = "order_pick_spmns";

	@Autowired
	private DaoFactory daoFactory;

	@Override
	@PlusTransactional
	public void doJob(ScheduledJobRun jobRun)
    throws Exception {
		try {
			int startAt = 0, maxOrders = 25;
			Date distSince = getDistributedSince();

			boolean endOfOrders = false;
			while (!endOfOrders) {
				List<DistributionOrder> orders = getUnpickedOrders(distSince, startAt, maxOrders);
				startAt += orders.size();
				endOfOrders = (orders.size() < maxOrders);

				orders.forEach(this::notifyRequestor);
			}
		} catch (Exception e) {
			logger.error("Error notifying the requestors", e);
			throw e;
		}
	}

	private Date getDistributedSince() {
		Calendar cal = Calendar.getInstance();
		cal.setTime(Utility.chopTime(cal.getTime()));
		cal.add(Calendar.DATE, -1);
		return cal.getTime();
	}

	private List<DistributionOrder> getUnpickedOrders(Date distSince, int startAt, int maxOrders) {
		return daoFactory.getDistributionOrderDao().getUnpickedOrders(distSince, startAt, maxOrders);
	}

	private void notifyRequestor(DistributionOrder order) {
		if (order.getDistributionProtocol().areEmailNotifsDisabled()) {
			return;
		}

		Map<String, Object> props = new HashMap<>();
		props.put("order", order);
		props.put("requestor", order.getRequester());
		props.put("ccAdmin", false);
		props.put("$subject", new Object[] { order.getId() });

		Set<String> to = new HashSet<>();
		to.add(order.getRequester().getEmailAddress());
		to.add(order.getDistributor().getEmailAddress());
		to.add(order.getDistributionProtocol().getPrincipalInvestigator().getEmailAddress());
		to.addAll(getEmailIds(order.getDistributionProtocol().getCoordinators()));
		if (order.getSite() != null) {
			to.addAll(getEmailIds(order.getSite().getCoordinators()));
		}

		EmailUtil.getInstance().sendEmail(PICK_DIST_SPMNS_TMPL, to.toArray(new String[0]), null, props);
	}

	private Set<String> getEmailIds(Collection<User> users) {
		return users.stream().map(User::getEmailAddress).collect(Collectors.toSet());
	}
}
