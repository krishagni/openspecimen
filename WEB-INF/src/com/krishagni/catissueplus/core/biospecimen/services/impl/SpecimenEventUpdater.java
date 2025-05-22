package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hibernate.type.StandardBasicTypes;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenErrorCode;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenEventDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.importer.events.ImportObjectDetail;
import com.krishagni.catissueplus.core.importer.services.ObjectImporter;

public class SpecimenEventUpdater implements ObjectImporter<SpecimenEventDetail, SpecimenEventDetail> {

	private SessionFactory sessionFactory;

	private DaoFactory daoFactory;

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenEventDetail> importObject(RequestEvent<ImportObjectDetail<SpecimenEventDetail>> req) {
		try {
			ImportObjectDetail<SpecimenEventDetail> importObj = req.getPayload();
			String eventName = importObj.getObjectName();

			SpecimenEventDetail event = importObj.getObject();
			if (event.getId() == null) {
				return ResponseEvent.userError(SpecimenErrorCode.EVT_ID_REQ);
			}

			switch (eventName) {
				case "specimenDisposalEvent" -> updateDisposeEvent(event);
				case "specimenReturnEvent" -> updateReturnEvent(event);
				case "specimenReservedEvent", "specimenReservationCancelEvent" -> updateReservedEvent(event);
				case "specimenTransferEvent" -> updateTransferEvent(event);
				case "containerTransferEvent" -> updateContainerTransferEvent(event);
			}

			return ResponseEvent.response(event);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	private void updateDisposeEvent(SpecimenEventDetail event) {
		Map<String, Object> params = new HashMap<>();

		if (event.isAttrModified("reason")) {
			params.put("REASON", event.getReason());
		}

		if (event.isAttrModified("comments")) {
			params.put("COMMENTS", event.getComments());
		}

		Long userId = getUserId(event.getUser());
		if (userId != null) {
			params.put("USER_ID", userId);
		}

		if (event.getTime() != null) {
			params.put("EVENT_TIMESTAMP", event.getTime());
		}

		if (params.isEmpty()) {
			return;
		}

		updateEventParams(DISPOSAL_EVENT_TABLE, EVENT_ID_COL, event.getId(), params);
	}

	private void updateReturnEvent(SpecimenEventDetail event) {
		Map<String, Object> params = new HashMap<>();

		if (event.isAttrModified("comments")) {
			params.put("RETURN_COMMENTS", event.getComments());
		}

		Long userId = getUserId(event.getUser());
		if (userId != null) {
			params.put("RETURNED_BY", userId);
		}

		if (event.getTime() != null) {
			params.put("RETURN_DATE", event.getTime());
		}

		if (params.isEmpty()) {
			return;
		}

		updateEventParams(RETURN_EVENT_TABLE, EVENT_ID_COL, event.getId(), params);
	}

	private void updateReservedEvent(SpecimenEventDetail event) {
		Map<String, Object> params = new HashMap<>();

		if (event.isAttrModified("comments")) {
			params.put("COMMENTS", event.getComments());
		}

		Long userId = getUserId(event.getUser());
		if (userId != null) {
			params.put("USER_ID", userId);
		}

		if (event.getTime() != null) {
			params.put("EVENT_TIME", event.getTime());
		}

		if (params.isEmpty()) {
			return;
		}

		updateEventParams(RESERVED_EVENT_TABLE, EVENT_ID_COL, event.getId(), params);
	}

	private void updateTransferEvent(SpecimenEventDetail event) {
		Map<String, Object> params = new HashMap<>();

		Long userId = getUserId(event.getUser());
		if (userId != null) {
			params.put("USER_ID", userId);
		}

		if (event.isAttrModified("comments")) {
			params.put("COMMENTS", event.getComments());
		}

		if (event.getTime() != null) {
			params.put("EVENT_TIMESTAMP", event.getTime());
		}

		if (params.isEmpty()) {
			return;
		}

		updateEventParams(TRANSFER_EVENT_TABLE, EVENT_ID_COL, event.getId(), params);
	}

	private void updateContainerTransferEvent(SpecimenEventDetail event) {
		Long containerEventId = (Long) sessionFactory.getCurrentSession()
			.createNativeQuery(GET_CONTAINER_TRANSFER_EVENT_ID_SQL)
			.addScalar("event_id", StandardBasicTypes.LONG)
			.setParameter("eventId", event.getId())
			.uniqueResult();

		Map<String, Object> params = new HashMap<>();

		Long userId = getUserId(event.getUser());
		if (userId != null) {
			params.put("USER_ID", userId);
		}

		if (event.isAttrModified("reason")) {
			params.put("REASON", event.getReason());
		}

		if (event.getTime() != null) {
			params.put("TRANSFER_TIME", event.getTime());
		}

		if (params.isEmpty()) {
			return;
		}

		updateEventParams(CONT_TRANSFER_EVENT_TABLE, EVENT_ID_COL, containerEventId, params);
	}

	private void updateEventParams(String eventTable, String eventIdCol, Long eventId, Map<String, Object> params) {
		String setter = params.keySet().stream()
			.map(k -> k + " = :" + k)
			.collect(Collectors.joining(","));

		String updateSql = String.format(UPDATE_EVENT_SQL, eventTable, setter, eventIdCol);
		Query<?> query = sessionFactory.getCurrentSession().createNativeQuery(updateSql).setParameter("eventId", eventId);
		params.forEach(query::setParameter);
		query.executeUpdate();
	}

//	private Specimen getDisposedSpecimen(Long eventId) {
//		return getSpecimen(DISPOSAL_EVENT_NAME, eventId, DISPOSAL_EVENT_TABLE);
//	}
//
//	private Specimen getReturnedSpecimen(Long eventId) {
//		return getSpecimen(RETURN_EVENT_NAME, eventId, RETURN_EVENT_TABLE);
//	}
//
//	private Specimen getReservedSpecimen(Long eventId, boolean cancelledEvent) {
//		return getSpecimen(cancelledEvent ? CANCEL_RESERVATION_EVENT_NAME : RESERVED_EVENT_NAME, eventId, RESERVED_EVENT_TABLE);
//	}
//
//	private Specimen getTransferredSpecimen(Long eventId) {
//		return getSpecimen(TRANSFER_EVENT_NAME, eventId, TRANSFER_EVENT_TABLE);
//	}

//	private Specimen getSpecimen(String formName, Long eventId, String eventTable) {
//		return getSpecimen(formName, eventId, eventTable, EVENT_ID_COL);
//	}

//	private Specimen getSpecimen(String formName, Long eventId, String eventTable, String eventIdColumn) {
//		String sql = String.format(GET_SPMN_ID_SQL, eventTable, eventIdColumn);
//		List<Long> rows = sessionFactory.getCurrentSession().createNativeQuery(sql, Long.class)
//			.setParameter("eventId", eventId)
//			.setParameter("formName", formName)
//			.list();
//		if (rows == null || rows.isEmpty()) {
//			throw OpenSpecimenException.userError(SpecimenErrorCode.INV_EVT_ID, eventId);
//		}
//
//		Long spmnId = rows.get(0);
//		Specimen spmn = daoFactory.getSpecimenDao().getById(spmnId);
//		if (spmn == null) {
//			throw OpenSpecimenException.userError(SpecimenErrorCode.NOT_FOUND, spmnId);
//		}
//
//		AccessCtrlMgr.getInstance().ensureCreateOrUpdateSpecimenRights(spmn);
//		return spmn;
//	}

	private Long getUserId(UserSummary inputUser) {
		if (inputUser == null || StringUtils.isBlank(inputUser.getEmailAddress())) {
			return null;
		}

		User user = daoFactory.getUserDao().getUserByEmailAddress(inputUser.getEmailAddress());
		if (user == null) {
			throw OpenSpecimenException.userError(UserErrorCode.NOT_FOUND, inputUser.getEmailAddress());
		}

		return user.getId();
	}

//	private static final String GET_SPMN_ID_SQL =
//		"select " +
//		"  re.object_id " +
//		"from " +
//		"  %s evt " +
//		"  inner join catissue_form_record_entry re on re.record_id = evt.%s " +
//		"  inner join catissue_form_context fc on fc.identifier = re.form_ctxt_id " +
//		"  inner join dyextn_containers f on f.identifier = fc.container_id " +
//		"where " +
//		"  f.name = :formName and " +
//		"  re.record_id = :eventId and " +
//		"  fc.entity_type = 'SpecimenEvent'";

	private static final String GET_CONTAINER_TRANSFER_EVENT_ID_SQL =
		"select" +
		"  event_id " +
		"from " +
		"  os_cont_transfer_evt_spmns " +
		"where " +
		"  identifier = :eventId";

	private static final String UPDATE_EVENT_SQL = "update %s set %s where %s = :eventId";

	private static final String DISPOSAL_EVENT_TABLE = "catissue_disposal_event_param";

//	private static final String DISPOSAL_EVENT_NAME = "SpecimenDisposalEvent";

	private static final String RETURN_EVENT_TABLE = "os_order_items";

//	private static final String RETURN_EVENT_NAME = "SpecimenReturnEvent";

	private static final String RESERVED_EVENT_TABLE = "os_spmn_reserved_events";

//	private static final String RESERVED_EVENT_NAME = "SpecimenReservedEvent";
//
//	private static final String CANCEL_RESERVATION_EVENT_NAME = "SpecimenReservationCancelledEvent";

	private static final String TRANSFER_EVENT_TABLE = "catissue_transfer_event_param";

//	private static final String TRANSFER_EVENT_NAME = "SpecimenTransferEvent";

	private static final String CONT_TRANSFER_EVENT_TABLE = "os_container_transfer_events";

//	private static final String CONT_TRANSFER_SPMN_EVENT_TABLE = "os_cont_transfer_evt_spmns";
//
//	private static final String CONT_TRANSFER_EVENT_NAME = "ContainerTransferEvent";

	private static final String EVENT_ID_COL = "IDENTIFIER";
}
