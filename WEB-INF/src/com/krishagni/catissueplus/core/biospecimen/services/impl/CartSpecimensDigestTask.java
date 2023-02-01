package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJob;
import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenList;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenListDigestItem;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.util.EmailUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

@Configurable
public class CartSpecimensDigestTask implements ScheduledTask {
	private static final LogUtil logger = LogUtil.getLogger(CartSpecimensDigestTask.class);

	@Autowired
	private DaoFactory daoFactory;

	@Override
	public void doJob(ScheduledJobRun jobRun)
	throws Exception {
		Calendar cal = Calendar.getInstance();
		ScheduledJob job = jobRun.getScheduledJob();
		Date startDate = null, endDate = null;
		switch (job.getRepeatSchedule()) {
			case MINUTELY:
				cal.add(Calendar.MINUTE, -1 * job.getMinutelyInterval());
				reset(cal, false, false);
				startDate = cal.getTime();

				cal.add(Calendar.MINUTE, job.getMinutelyInterval());
				endDate = cal.getTime();
				break;

			case HOURLY:
				cal.add(Calendar.HOUR_OF_DAY, -1);
				reset(cal, false, true);
				startDate = cal.getTime();

				cal.add(Calendar.HOUR_OF_DAY, 1);
				endDate = cal.getTime();
				break;

			case DAILY:
			case ONDEMAND:
				cal.add(Calendar.DAY_OF_MONTH, -1);
				reset(cal, true, true);
				startDate = cal.getTime();

				cal.add(Calendar.DAY_OF_MONTH, 1);
				endDate = cal.getTime();
				break;

			case WEEKLY:
				cal.add(Calendar.DAY_OF_MONTH, -7);
				reset(cal, true, true);
				startDate = cal.getTime();

				cal.add(Calendar.DAY_OF_MONTH, 7);
				endDate = cal.getTime();
				break;

			case MONTHLY:
				cal.add(Calendar.MONTH, -1);
				reset(cal, true, true);
				startDate = cal.getTime();

				cal.add(Calendar.MONTH, 1);
				endDate = cal.getTime();
				break;
		}

		List<Long> cartIds = getDigestEnabledCarts();
		if (cartIds.isEmpty()) {
			return;
		}

		logger.info("Preparing specimens digest email for the carts: " + StringUtils.join(cartIds, ", ") +
			" for the period: " + startDate + " - " + endDate);
		for (Long cartId : cartIds) {
			sendDigest(cartId, startDate, endDate);
		}
	}

	@PlusTransactional
	private List<Long> getDigestEnabledCarts() {
		return daoFactory.getSpecimenListDao().getDigestEnabledLists();
	}

	@PlusTransactional
	private void sendDigest(Long cartId, Date startTime, Date endTime) {
		SpecimenList cart = daoFactory.getSpecimenListDao().getSpecimenList(cartId);
		List<SpecimenListDigestItem> digest = daoFactory.getSpecimenListDao().getListDigest(cartId, startTime, endTime);
		if (digest == null || digest.isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug("No digest for the specimens cart: " + cart.getName() + " for the period " + startTime + " - " + endTime);
			}
			return;
		}

		String startTimeStr = Utility.getDateTimeString(startTime);
		String endTimeStr   = Utility.getDateTimeString(endTime);

		List<User> users = new ArrayList<>(cart.getAllSharedUsers());
		users.add(0, cart.getOwner());
		String[] to = users.stream()
			.filter(user -> user.isActive() && StringUtils.isNotBlank(user.getEmailAddress()))
			.map(User::getEmailAddress)
			.toArray(String[]::new);

		Map<String, Object> props = new HashMap<>();
		props.put("$subject", new String[] {cart.getDisplayName(), startTimeStr, endTimeStr});
		props.put("cart", cart);
		props.put("startTime", startTimeStr);
		props.put("endTime", endTimeStr);
		props.put("digest", digest);
		EmailUtil.getInstance().sendEmail(DIGEST_EMAIL_TMPL, to, null, props);
	}

	private void reset(Calendar cal, boolean hour, boolean minute) {
		if (hour) {
			cal.set(Calendar.HOUR_OF_DAY, 0);
		}

		if (minute) {
			cal.set(Calendar.MINUTE, 0);
		}

		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
	}

	private static final String DIGEST_EMAIL_TMPL = "cart_digest";
}
