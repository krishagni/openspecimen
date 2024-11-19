package com.krishagni.catissueplus.core.upgrade;

import java.util.List;

import org.hibernate.SessionFactory;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.OpenSpecimenAppCtxProvider;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

public class SanitiseParticipantPhoneNumbers implements CustomTaskChange {

	@Override
	@PlusTransactional
	public void execute(Database database) throws CustomChangeException {
		try {
			SessionFactory sessionFactory = OpenSpecimenAppCtxProvider.getBean("sessionFactory");

			DaoFactory daoFactory = OpenSpecimenAppCtxProvider.getBean("biospecimenDaoFactory");
			User sysUser = daoFactory.getUserDao().getSystemUser();
			AuthUtil.setCurrentUser(sysUser);

			boolean endOfParticipants = false;
			long lastId = 0L;
			while (!endOfParticipants) {
				List<Participant> participants = sessionFactory.getCurrentSession().createQuery(GET_PARTICIPANTS_HQL, Participant.class)
					.setParameter("participantId", lastId)
					.setMaxResults(100)
					.list();
				for (Participant p : participants) {
					p.setPhoneNumber(Utility.normalizePhoneNumber(p.getPhoneNumber()));
					lastId = p.getId();
				}

				endOfParticipants = (participants.size() < 100);
			}
		} catch (Exception e) {
			throw new CustomChangeException("Error normalising participant phone numbers.", e);
		} finally {
			AuthUtil.clearCurrentUser();
		}
	}

	@Override
	public String getConfirmationMessage() {
		return "Normalised participant phone numbers.";
	}

	@Override
	public void setUp() throws SetupException {

	}

	@Override
	public void setFileOpener(ResourceAccessor resourceAccessor) {

	}

	@Override
	public ValidationErrors validate(Database database) {
		return null;
	}

	private static final String GET_PARTICIPANTS_HQL =
		"from " +
		"  com.krishagni.catissueplus.core.biospecimen.domain.Participant p " +
		"where" +
		"  length(p.phoneNumber) > 0 and " +
		"  p.id > :participantId " +
		"order by" +
		"  p.id asc";
}
