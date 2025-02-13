package com.krishagni.catissueplus.core.de.repository.impl;

import java.sql.Blob;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.FlushMode;
import org.hibernate.Session;

import com.krishagni.catissueplus.core.administrative.events.UserGroupSummary;
import com.krishagni.catissueplus.core.administrative.repository.FormListCriteria;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolSummary;
import com.krishagni.catissueplus.core.biospecimen.repository.impl.BiospecimenDaoHelper;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.repository.AbstractCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.Junction;
import com.krishagni.catissueplus.core.common.repository.Query;
import com.krishagni.catissueplus.core.common.repository.SubQuery;
import com.krishagni.catissueplus.core.de.domain.Form;
import com.krishagni.catissueplus.core.de.events.FormContextDetail;
import com.krishagni.catissueplus.core.de.events.FormContextRevisionDetail;
import com.krishagni.catissueplus.core.de.events.FormCtxtSummary;
import com.krishagni.catissueplus.core.de.events.FormRecordSummary;
import com.krishagni.catissueplus.core.de.events.FormRevisionDetail;
import com.krishagni.catissueplus.core.de.events.FormSummary;
import com.krishagni.catissueplus.core.de.events.ObjectCpDetail;
import com.krishagni.catissueplus.core.de.repository.FormDao;

import edu.common.dynamicextensions.domain.nui.Container;
import krishagni.catissueplus.beans.FormContextBean;
import krishagni.catissueplus.beans.FormRecordEntryBean;

public class FormDaoImpl extends AbstractDao<FormContextBean> implements FormDao {

	@Override
	public Class<FormContextBean> getType() {
		return FormContextBean.class;
	}
	
	@Override
	public FormContextBean getById(Long id) {
		Criteria<FormContextBean> query = createCriteria(FormContextBean.class, "fc");
		return query.add(query.eq("fc.identifier", id))
			.add(query.isNull("fc.deletedOn"))
			.uniqueResult();
	}

	@Override
	public Form getFormById(Long formId) {
		Criteria<Form> query = createCriteria(Form.class, "form");
		return query.add(query.eq("form.id", formId))
			.add(query.isNull("form.deletedOn"))
			.uniqueResult();
	}

	@Override
	public Form getFormByName(String name) {
		Criteria<Form> query = createCriteria(Form.class, "form");
		return query.add(query.eq("form.name", name))
			.add(query.isNull("form.deletedOn"))
			.uniqueResult();
	}

	@Override
	public List<Form> getFormsByIds(Collection<Long> formIds) {
		Criteria<Form> query = createCriteria(Form.class, "form");
		return query.add(query.in("form.id", formIds))
			.add(query.isNull("form.deletedOn"))
			.list();
	}

	@Override
	public List<Form> getFormsByName(Collection<String> formNames) {
		Criteria<Form> query = createCriteria(Form.class, "form");
		return query.add(query.in("form.name", formNames))
			.add(query.isNull("form.deletedOn"))
			.list();
	}

	@Override
	public List<Form> getForms(FormListCriteria crit) {
		Criteria<Form> query = createCriteria(Form.class, "form");
		return query.add(query.in("form.id", getListFormIdsQuery(crit, query)))
			.orderBy(query.desc(query.isNotNull("form.updateTime"), "form.updateTime", "form.creationTime"))
			.list(crit.startAt(), crit.maxResults());
	}

	@Override
	public Long getFormsCount(FormListCriteria crit) {
		Criteria<Form> query = createCriteria(Form.class, "form");
		return query.add(query.in("form.id", getListFormIdsQuery(crit, query))).getCount("form.id");
	}

	@Override
	public List<FormSummary> getEntityForms(FormListCriteria crit) {
		Criteria<Object[]> query = createCriteria(Form.class, Object[].class, "form");
		query.leftJoin("form.associations", "fc", () -> query.isNull("fc.deletedOn"))
			.add(query.isNull("form.deletedOn"))
			.addOrder(query.asc("form.caption"));

		if (CollectionUtils.isNotEmpty(crit.cpIds())) {
			query.add(
				query.or(
					query.eq("fc.cpId", -1L),
					query.in("fc.cpId", crit.cpIds())
				)
			);
		}

		if (CollectionUtils.isNotEmpty(crit.entityTypes())) {
			query.add(query.in("fc.entityType", crit.entityTypes()));
		}

		List<Object[]> rows = query.add(query.in("form.id", getListFormIdsQuery(crit, query)))
			.select(
				"form.id", "form.name", "form.caption",
				"fc.entityType", "fc.sysForm", "fc.multiRecord", "fc.identifier", "fc.cpId"
			).list();

		Map<String, List<Object[]>> forms = new LinkedHashMap<>();
		for (Object[] row : rows) {
			Long formId = (Long) row[0];
			String entityType = (String) row[3];
			String key = entityType + ":" + formId;
			List<Object[]> associations = forms.computeIfAbsent(key, (k) -> new ArrayList<>());
			associations.add(row);
		}

		List<FormSummary> result = new ArrayList<>();
		for (Object[] row : rows) {
			Long formId = (Long) row[0];
			String entityType = (String) row[3];
			String key = entityType + ":" + formId;
			if (forms.get(key).size() > 1) {
				//
				// multiple associations for this form - one with CP, another for all current and future
				//
				Long cpId = (Long) row[7];
				if (cpId == null || cpId == -1L) {
					//
					// skip this association
					// we would like to add the CP specific association to the final result list
					//
					continue;
				}
			}

			int idx = -1;
			FormSummary form = new FormSummary();
			form.setFormId((Long) row[++idx]);
			form.setName((String) row[++idx]);
			form.setCaption((String) row[++idx]);
			form.setEntityType((String) row[++idx]);

			Boolean sysForms = (Boolean) row[++idx];
			form.setSysForm(sysForms != null ? sysForms : false);

			Boolean multiRecords = (Boolean) row[++idx];
			form.setMultipleRecords(multiRecords != null ? multiRecords : false);
			form.setFormCtxtId((Long) row[++idx]);
			form.setCpId((Long) row[++idx]);
			result.add(form);
		}

		return result;
	}

	@Override
	public Map<Long, Integer> getAssociationsCount(Collection<Long> formIds) {
		List<Object[]> rows = createNamedQuery(GET_ASSOCIATIONS_CNT, Object[].class)
			.setParameterList("formIds", formIds)
			.list();

		Map<Long, Integer> result = new HashMap<>();
		for (Object[] row : rows) {
			result.put((Long) row[0], (Integer) row[1]);
		}

		return result;
	}

	@Override
	public boolean isSystemForm(Long formId) {
		List<FormContextBean> result = createNamedQuery(GET_SYSTEM_FORM_CTXTS, FormContextBean.class)
			.setParameter("formId", formId)
			.setMaxResults(1)
			.list();
		return !result.isEmpty();
	}

	@Override
	public Date getUpdateTime(Long formId) {
		return createNamedQuery(GET_FORM_UPDATE_TIME, Date.class)
			.setParameter("formId", formId)
			.uniqueResult();
	}

	@Override
	public List<FormSummary> getQueryForms() {
		List<Object[]> rows = createNamedQuery(GET_QUERY_FORMS, Object[].class).list();

		List<FormSummary> forms = new ArrayList<>();
		for (Object[] row : rows) {
			FormSummary form = new FormSummary();
			form.setFormId((Long)row[0]);
			form.setName((String)row[1]);
			form.setCaption((String)row[2]);
			form.setCreationTime((Date)row[3]);
			form.setModificationTime((Date)row[4]);
			form.setAssociations(-1);

			UserSummary user = new UserSummary();
			user.setId((Long)row[5]);
			user.setFirstName((String)row[6]);
			user.setLastName((String)row[7]);
			form.setCreatedBy(user);
			
			forms.add(form);						
		}
				
		return forms;		
	}

	@Override
	public List<FormContextDetail> getFormContexts(Long formId) {
		List<Object[]> rows = createNamedQuery(GET_FORM_CTXTS, Object[].class)
			.setParameter("formId", formId)
			.list();

		List<FormContextDetail> formCtxts = new ArrayList<>();
		for (Object[] row : rows) {
			int idx = -1;
			FormContextDetail formCtxt = new FormContextDetail();
			formCtxt.setFormCtxtId((Long)row[++idx]);
			formCtxt.setFormId((Long)row[++idx]);
			formCtxt.setLevel((String)row[++idx]);
			formCtxt.setEntityId((Long)row[++idx]);
			formCtxt.setMultiRecord((Boolean)row[++idx]);
			formCtxt.setSysForm((Boolean)row[++idx]);
			formCtxt.setNotifEnabled((Boolean)row[++idx]);
			formCtxt.setDataInNotif((Boolean)row[++idx]);

			CollectionProtocolSummary cp = new CollectionProtocolSummary();
			cp.setId((Long)row[++idx]);
			cp.setShortTitle((String)row[++idx]);
			cp.setTitle((String)row[++idx]);
			formCtxt.setCollectionProtocol(cp);
			formCtxt.setInstituteName((String)row[++idx]);
			
			formCtxts.add(formCtxt);
		}
		
		return formCtxts;
	}

	@Override
	public Map<Long, List<UserGroupSummary>> getNotifUsers(Collection<Long> contextIds) {
		List<Object[]> rows = createNamedQuery(GET_NOTIF_USERS, Object[].class)
			.setParameterList("contextIds", contextIds)
			.list();

		Map<Long, List<UserGroupSummary>> result = new HashMap<>();
		for (Object[] row : rows) {
			int idx = -1;
			List<UserGroupSummary> groups = result.computeIfAbsent((Long) row[++idx], (k) -> new ArrayList<>());
			UserGroupSummary group = new UserGroupSummary();
			group.setId((Long) row[++idx]);
			group.setName((String) row[++idx]);
			groups.add(group);
		}

		return result;
	}

	@Override
	public FormContextBean getQueryFormContext(Long formId) {
		List<FormContextBean> ctxts = createNamedQuery(GET_QUERY_FORM_CONTEXT, FormContextBean.class)
			.setParameter("formId", formId)
			.setParameter("entityType", "Query")
			.list();
		return ctxts.size() == 1 ? ctxts.get(0) : null;
	}

	@Override
	public List<FormContextBean> getFormContexts(Collection<Long> cpIds, String entityType) {
		Criteria<FormContextBean> query = createCriteria(FormContextBean.class, "ctxt");
		return query.add(query.in("ctxt.cpId", cpIds))
			.add(query.eq("ctxt.entityType", entityType))
			.add(query.isNull("ctxt.deletedOn"))
			.list();
	}

	@Override
	public List<FormCtxtSummary> getCprForms(Long cprId) {
		Query<Object[]> query = createNamedQuery(GET_CPR_FORMS, Object[].class);
		return getEntityForms(query.setParameter("cprId", cprId).list());
	}

	@Override
	public List<FormCtxtSummary> getParticipantForms(Long cprId) {
		Query<Object[]> query = createNamedQuery(GET_PARTICIPANT_FORMS, Object[].class);
		return getEntityForms(query.setParameter("cprId", cprId).list());
	}

	@Override
	public List<FormCtxtSummary> getSpecimenForms(Long specimenId) {
		Query<Object[]> query = createNamedQuery(GET_SPECIMEN_FORMS, Object[].class);
		return getEntityForms(query.setParameter("specimenId", specimenId).list());
	}

    @Override
    public List<FormCtxtSummary> getSpecimenEventForms(Long specimenId) {
	    Query<Object[]> query = createNamedQuery(GET_SPECIMEN_EVENT_FORMS, Object[].class);
	    return getEntityForms(query.setParameter("specimenId", specimenId).list());
    }

	@Override
	public List<FormCtxtSummary> getScgForms(Long scgId) {
		Query<Object[]> query = createNamedQuery(GET_SCG_FORMS, Object[].class);
		return getEntityForms(query.setParameter("scgId", scgId).list());
	}

	@Override
	public List<FormCtxtSummary> getFormContexts(Long cpId, String entityType) {
		List<FormContextBean> result = createNamedQuery(GET_FORM_CTXTS_BY_ENTITY, FormContextBean.class)
			.setParameter("cpId", cpId)
			.setParameter("entityType", entityType)
			.list();
		return FormCtxtSummary.from(result);
	}

	@Override
	public List<FormRecordSummary> getFormRecords(Long formCtxtId, Long objectId) {
		List<Object[]> rows = createNamedQuery(GET_FORM_RECORDS, Object[].class)
			.setParameter("formCtxtId", formCtxtId)
			.setParameter("objectId", objectId)
			.list();

		List<FormRecordSummary> formRecords = new ArrayList<>();
		for (Object[] row : rows) {
			int idx = -1;
			FormRecordSummary record = new FormRecordSummary();
			record.setId((Long)row[++idx]);
			record.setRecordId((Long)row[++idx]);
			record.setUpdateTime((Date)row[++idx]);
			record.setSysForm((boolean)row[++idx]);
			
			UserSummary user = new UserSummary();
			user.setId((Long)row[++idx]);
			user.setFirstName((String)row[++idx]);
			user.setLastName((String)row[++idx]);
			record.setUser(user);

			record.setFormStatus((String) row[++idx]);
			formRecords.add(record);
		}
		
		return formRecords;
	}

	@Override
	public FormSummary getFormByContext(Long formCtxtId) {
		List<Object[]> rows = createNamedQuery(GET_FORM_BY_CTXT, Object[].class)
			.setParameter("formCtxtId", formCtxtId)
			.list();
		
		if (rows.isEmpty()) {
			return null;
		}
		
		Object[] row = rows.iterator().next();
		FormSummary result = new FormSummary();
		result.setFormId((Long)row[0]);
		result.setCaption((String)row[1]);
		return result;		
	}

	@Override
	public FormContextBean getFormContext(Long formCtxtId) {
		Criteria<FormContextBean> query = createCriteria(FormContextBean.class, "fc");
		return query.add(query.eq("fc.identifier", formCtxtId)).uniqueResult();
	}

	@Override
	public FormContextBean getFormContext(Long formId, Long cpId, String entity) {
		return getFormContext(formId, cpId, Collections.singletonList(entity));
	}

	@Override
	public FormContextBean getFormContext(Long formId, Long cpId, List<String> entities) {
		List<FormContextBean> fcs = createNamedQuery(GET_FORM_CTXT, FormContextBean.class)
			.setParameter("formId", formId)
			.setParameter("cpId", cpId)
			.setParameterList("entityTypes", entities)
			.list();
		return fcs != null && !fcs.isEmpty() ? fcs.iterator().next() : null;
	}

	@Override
	public FormContextBean getFormContext(boolean cpBased, String entityType, Long entityId, Long formId) {
		entityId = entityId == null ? -1L : entityId;

		Criteria<FormContextBean> query = createCriteria(FormContextBean.class, "ctxt");
		query.add(query.eq("ctxt.entityType", entityType))
			.add(query.isNull("ctxt.deletedOn"));

		if (cpBased) {
			query.add(query.or(query.eq("ctxt.cpId", -1L), query.eq("ctxt.cpId", entityId)));
		} else {
			query.add(query.eq("ctxt.entityId", entityId));
		}

		if (formId != null) {
			query.add(query.eq("ctxt.containerId", formId));
		}

		List<FormContextBean> fcs = query.list();
		if (fcs.isEmpty()) {
			return null;
		} else if (!cpBased) {
			return fcs.get(0);
		} else {
			FormContextBean allCp = null;
			for (FormContextBean fc : fcs) {
				if (fc.getCpId().equals(entityId)) {
					return fc;
				} else if (fc.getCpId().equals(-1L)) {
					allCp = fc;
				}
			}

			return allCp;
		}
	}

	@Override
	public List<FormContextBean> getFormContextsById(List<Long> formContextIds) {
		Criteria<FormContextBean> query = createCriteria(FormContextBean.class, "fc");
		query.add(query.isNull("fc.deletedOn"));
		applyIdsFilter(query, "fc.identifier", formContextIds);
		return query.list();
	}

	// Returns deleted form contexts for a specific input CP. Not fallback to all.
	@Override
	public FormContextBean getAbsFormContext(Long formId, Long cpId, String entity) {
		Session session = getCurrentSession();
		session.disableFilter("activeForms");

		Criteria<FormContextBean> query = Criteria.create(session, FormContextBean.class, "fc");
		List<FormContextBean> fcs = query.add(query.eq("fc.containerId", formId))
			.add(query.eq("fc.cpId", cpId))
			.add(query.eq("fc.entityType", entity))
			.list();
		session.enableFilter("activeForms");

		fcs.sort((f1, f2) -> {
			if (f1.getDeletedOn() == null && f2.getDeletedOn() != null) {
				return -1;
			} else if (f1.getDeletedOn() != null && f2.getDeletedOn() == null) {
				return 1;
			} else if (f1.getDeletedOn() != null && f2.getDeletedOn() != null) {
				return f2.getDeletedOn().compareTo(f1.getDeletedOn());
			} else if (f1.getDeletedOn() == null && f2.getDeletedOn() == null) {
				return f2.getIdentifier().compareTo(f1.getIdentifier());
			}

			return 0;
		});

		return !fcs.isEmpty() ? fcs.get(0) : null;
	}

	@Override
	public Pair<String, Long> getFormNameContext(Long cpId, String entityType, Long entityId) {
		List<Object[]> rows = createNamedQuery(GET_FORM_NAME_CTXT_ID, Object[].class)
			.setParameter("entityType", entityType)
			.list();

		for (Object[] row : rows) {
			int idx = -1;
			String name     = (String) row[++idx];
			Long fcId       = (Long) row[++idx];
			Long fcEntityId = (Long) row[++idx];
			Long fcCpId     = (Long) row[++idx];
			if ((entityId == null && Objects.equals(cpId, fcCpId)) || (entityId != null && entityId.equals(fcEntityId))) {
				return Pair.make(name, fcId);
			}
		}

		return null;
	}
	
	@Override
	public void saveOrUpdateRecordEntry(FormRecordEntryBean recordEntry) {
		sessionFactory.getCurrentSession().saveOrUpdate(recordEntry);
	}

	@Override
	public List<FormRecordEntryBean> getRecordEntries(Long formCtxtId, Long objectId) {
		return createNamedQuery(GET_RECORD_ENTRIES, FormRecordEntryBean.class)
			.setParameter("formCtxtId", formCtxtId)
			.setParameter("objectId", objectId)
			.list();
	}

	@Override
	public FormRecordEntryBean getRecordEntry(Long recordId) {
		return createNamedQuery(GET_RE_BY_ID, FormRecordEntryBean.class)
			.setParameter("recordId", recordId)
			.uniqueResult();
	}

	@Override
	public FormRecordEntryBean getRecordEntry(Long formCtxtId, Long objectId, Long recordId) {
		return createNamedQuery(GET_RECORD_ENTRY, FormRecordEntryBean.class)
			.setParameter("formCtxtId", formCtxtId)
			.setParameter("objectId", objectId)
			.setParameter("recordId", recordId)
			.uniqueResult();
	}

	@Override
	public FormRecordEntryBean getRecordEntry(Long formId, Long recordId) {
		return createNamedQuery(GET_REC_BY_FORM_N_REC_ID, FormRecordEntryBean.class)
			.setParameter("formId", formId)
			.setParameter("recordId", recordId)
			.uniqueResult();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<Long, Pair<Long, Long>> getLatestRecordIds(Long formId, String entityType, List<Long> objectIds) {
		List<Object[]> rows = createNamedQuery(GET_LATEST_RECORD_IDS, Object[].class)
				.setParameter("formId", formId)
				.setParameter("entityType", entityType)
				.setParameterList("objectIds", objectIds)
				.list();
		return rows.stream().collect(Collectors.toMap(
			row -> (Long) row[0],
			row -> Pair.make((Long) row[1], (Long) row[2])
		));
	}

	private List<FormCtxtSummary> getEntityForms(List<Object[]> rows) {
		Map<Long, FormCtxtSummary> formsMap = new LinkedHashMap<>();
		
		for (Object[] row : rows) {
			Long cpId = (Long)row[4];
			Long formId = (Long)row[1];

			FormCtxtSummary form = formsMap.get(formId);
			if (form != null && cpId == -1) {
				continue;
			}
			
			form = new FormCtxtSummary();
			form.setFormCtxtId((Long)row[0]);
			form.setFormId(formId);
			form.setFormName((String)row[2]);
			form.setFormCaption((String)row[3]);
			form.setEntityType((String)row[5]);
			form.setCreationTime((Date)row[6]);
			form.setModificationTime((Date)row[7]);
			
			UserSummary user = new UserSummary();
			user.setId((Long)row[8]);
			user.setFirstName((String)row[9]);
			user.setLastName((String)row[10]);
			form.setCreatedBy(user);
			
			form.setMultiRecord((Boolean)row[11]);
			form.setSysForm((Boolean)row[12]);
			form.setNoOfRecords((Integer)row[13]);
			formsMap.put(formId, form);
		}
		
		return new ArrayList<>(formsMap.values());
	}
		
	@Override
	public ObjectCpDetail getObjectCpDetail(Map<String, Object> dataHookingInformation) {
		ObjectCpDetail objCp = null;
		String entityType = (String)dataHookingInformation.get("entityType");
		switch (entityType) {
			case "Participant" -> objCp = getObjectIdForParticipant(dataHookingInformation);
			case "Specimen", "SpecimenEvent" -> objCp = getObjectIdForSpecimen(entityType, dataHookingInformation);
			case "SpecimenCollectionGroup" -> objCp = getObjectIdForSCG(dataHookingInformation);
		}
		
		return objCp;
	}

	@Override
	public Long getFormCtxtId(Long containerId, String entityType, Long cpId) {
        Long formCtxtId = null;

		List<Object[]> rows =  createNamedQuery(GET_FORM_CTX_ID, Object[].class)
			.setParameter("containerId", containerId)
			.setParameter("entityType", entityType)
			.setParameter("cpId", cpId)
			.list();
        if (rows == null || rows.isEmpty()) {
            return null;
        }

        for (Object[] row : rows) {
            formCtxtId = (Long) row[0];
            if (cpId.equals(row[1])) {
                break;
            }
        }

		return formCtxtId;
	}
	
	@Override
	public List<Long> getFormIds(Long cpId, String entityType) {
		return getFormIds(cpId, Collections.singletonList(entityType));
	}
	
	@Override
	public List<Long> getFormIds(Long cpId, List<String> entityTypes) {
		return createNamedQuery(GET_FORM_IDS, Long.class)
			.setParameter("cpId", cpId)
			.setParameterList("entityTypes", entityTypes)
			.list();
	}

	@Override
	public Long getRecordsCount(Long formCtxtId, Long objectId) {
		return createNamedQuery(GET_RECORD_CNT, Long.class)
			.setParameter("formCtxtId", formCtxtId)
			.setParameter("objectId", objectId)
			.uniqueResult();
	}

	@Override
	public Map<Long, List<FormRecordSummary>> getFormRecords(Long objectId, String entityType, Long formId) {
		Query<Object[]> query;
		if (formId == null) {
			query = createNamedQuery(GET_RECS_BY_TYPE_AND_OBJECT, Object[].class);
		} else {
			query = createNamedQuery(GET_RECS, Object[].class)
				.setParameter("formId", formId);
		}
		
		List<Object[]> rows = query.setParameter("entityType", entityType)
			.setParameter("objectId", objectId)
			.list();
		
		Map<Long, List<FormRecordSummary>> result = new HashMap<>();
		for (Object[] row : rows) {
			int idx = 0;
			Long form = (Long)row[idx++];
			
			FormRecordSummary record = new FormRecordSummary();
			record.setFcId((Long)row[idx++]);
			record.setSysForm((boolean)row[idx++]);
			record.setRecordId((Long)row[idx++]);
			record.setUpdateTime((Date)row[idx++]);
			
			UserSummary user = new UserSummary();
			user.setId((Long)row[idx++]);
			user.setFirstName((String)row[idx++]);
			user.setLastName((String)row[idx++]);
			record.setUser(user);

			record.setFormStatus((String) row[idx]);
			List<FormRecordSummary> recs = result.computeIfAbsent(form, (k) -> new ArrayList<>());
			recs.add(record);
		}
				
		return result;
	}

	@Override
	public List<DependentEntityDetail> getDependentEntities(Long formId) {
		List<Object[]> rows = createNamedQuery(GET_DEPENDENT_ENTITIES, Object[].class)
			.setParameter("formId", formId)
			.list();
		return getDependentEntities(rows);
	}

	@Override
	public String getFormChangeLogDigest(String file) {
		List<String> rows = createNativeQuery(GET_CHANGE_LOG_DIGEST_SQL, String.class)
			.addStringScalar("md5_digest")
			.setParameter("filename", file)
			.list();
		return rows != null && !rows.isEmpty() ? rows.iterator().next() : null;
	}

	@Override
	public Object[] getLatestFormChangeLog(String file) {
		List<Object[]> rows = createNativeQuery(GET_LATEST_CHANGE_LOG_SQL, Object[].class)
			.addStringScalar("filename") //fl.filename, fl.form_id, fl.md5_digest, fl.executed_on
			.addLongScalar("form_id")
			.addStringScalar("md5_digest")
			.addTimestampScalar("executed_on")
			.setParameter("filename", file)
			.list();
		return rows != null && !rows.isEmpty() ? rows.iterator().next() : null;
	}

	@Override
	public void insertFormChangeLog(String file, String digest, Long formId) {
		createNativeQuery(INSERT_CHANGE_LOG_SQL)
			.setParameter("filename", file)
			.setParameter("digest", digest)
			.setParameter("formId", formId)
			.setParameter("executedOn", Calendar.getInstance().getTime())
			.executeUpdate();
	}
	
	@Override
	public void deleteFormContexts(Collection<Long> formIds) {
		createNativeQuery(SOFT_DELETE_FORM_CONTEXTS_SQL)
			.setParameter("deletedOn", Calendar.getInstance().getTime())
			.setParameterList("formIds", formIds)
			.executeUpdate(); 
	}

	@Override
	public void deleteRecords(Long formCtxtId, Collection<Long> recordIds) {
		createNativeQuery(SOFT_DELETE_RECS_SQL)
			.setParameter("formCtxtId", formCtxtId)
			.setParameterList("recordIds", recordIds)
			.executeUpdate();
	}

	@Override
	public int deleteRecords(Long cpId, List<String> entityTypes, Long objectId) {
		return createNamedQuery(SOFT_DELETE_ENTITY_RECS)
			.setParameter("cpId", cpId)
			.setParameterList("entityTypes", entityTypes)
			.setParameter("objectId", objectId)
			.executeUpdate();
	}

	@Override
	public int undeleteRecords(Long cpId, List<String> entityTypes, Long objectId) {
		return createNamedQuery(UNDO_DELETE_ENTITY_RECS)
			.setParameter("cpId", cpId)
			.setParameterList("entityTypes", entityTypes)
			.setParameter("objectId", objectId)
			.executeUpdate();
	}

	@Override
	public int deleteFormContexts(Long cpId, List<String> entityTypes) {
		return createNamedQuery(SOFT_DELETE_CP_FORMS)
			.setParameter("deleteTime", Calendar.getInstance().getTime())
			.setParameter("cpId", cpId)
			.setParameterList("entityTypes", entityTypes)
			.executeUpdate();
	}

	@Override
	public Map<Long, List<Long>> getRecordIds(Long formCtxtId, Collection<Long> objectIds) {
		List<Object[]> rows = createNamedQuery(GET_RECORD_IDS, Object[].class)
			.setParameter("formCtxtId", formCtxtId)
			.setParameterList("objectIds", objectIds)
			.list();

		Map<Long, List<Long>> result = new HashMap<>();
		for (Object[] row : rows) {
			List<Long> recordIds = result.computeIfAbsent((Long)row[0], (u) -> new ArrayList<>());
			recordIds.add((Long)row[1]);
		}

		return result;
	}

	@Override
	public Map<String, List<Long>> getEntityFormRecordIds(Collection<String> entityTypes, Long objectId, Collection<String> formNames) {
		getCurrentSession().setHibernateFlushMode(FlushMode.ALWAYS);
		List<Object[]> rows = createNamedQuery(GET_ENTITY_FORM_RECORD_IDS, Object[].class)
			.setParameterList("formNames", formNames)
			.setParameterList("entityTypes", entityTypes)
			.setParameter("objectId", objectId)
			.list();

		Map<String, List<Long>> result = new HashMap<>();
		for (Object[] row : rows) {
			List<Long> recordIds = result.computeIfAbsent((String) row[0], (k) -> new ArrayList<>());
			recordIds.add((Long) row[1]);
		}

		return result;
	}

	@Override
	public List<Map<String, Object>> getRegistrationRecords(Long cpId, Collection<SiteCpPair> siteCps, Long formId, List<String> ppids, int startAt, int maxResults) {
		return getEntityRecords(
			cpId, siteCps, formId, GET_REG_FORM_RECORDS,
			ppids, "ppids", "cpr.protocol_participant_id in (:ppids)", null,
			startAt, maxResults,
			row -> {
				Map<String, Object> regRecord = new HashMap<>();
				regRecord.put("cpId", ((Number) row[0]).longValue());
				regRecord.put("cpShortTitle", row[1]);
				regRecord.put("cprId", ((Number) row[2]).longValue());
				regRecord.put("ppid", row[3]);
				regRecord.put("recordId", ((Number) row[4]).longValue());
				regRecord.put("formStatus", row[5]);
				return regRecord;
			});
	}

	@Override
	public List<Map<String, Object>> getParticipantRecords(Long cpId, Collection<SiteCpPair> siteCps, Long formId, List<String> ppids, int startAt, int maxResults) {
		return getEntityRecords(
			cpId, siteCps, formId, GET_PART_FORM_RECORDS,
			ppids, "ppids", "cpr.protocol_participant_id in (:ppids)", null,
			startAt, maxResults,
			row -> {
				Map<String, Object> partRecord = new HashMap<>();
				partRecord.put("cpId", ((Number) row[0]).longValue());
				partRecord.put("cpShortTitle", row[1]);
				partRecord.put("cprId", ((Number) row[2]).longValue());
				partRecord.put("participantId", ((Number) row[3]).longValue());
				partRecord.put("ppid", row[4]);
				partRecord.put("recordId", ((Number) row[5]).longValue());
				partRecord.put("formStatus", row[6]);
				return partRecord;
			});
	}

	@Override
	public List<Map<String, Object>> getVisitRecords(Long cpId, Collection<SiteCpPair> siteCps, Long formId, List<String> visitNames, int startAt, int maxResults) {
		return getEntityRecords(
			cpId, siteCps, formId, GET_VISIT_FORM_RECORDS,
			visitNames, "visitNames", "v.name in (:visitNames)", null,
			startAt, maxResults,
			row -> {
				Map<String, Object> visitRecord = new HashMap<>();
				visitRecord.put("cpId", ((Number) row[0]).longValue());
				visitRecord.put("cpShortTitle", row[1]);
				visitRecord.put("visitId", ((Number) row[2]).longValue());
				visitRecord.put("visitName", row[3]);
				visitRecord.put("recordId", ((Number) row[4]).longValue());
				visitRecord.put("formStatus", row[5]);
				return visitRecord;
			});
	}

	@Override
	public List<Map<String, Object>> getSpecimenRecords(Long cpId, Collection<SiteCpPair> siteCps, Long formId, String entityType, List<String> spmnLabels, int startAt, int maxResults) {
		return getEntityRecords(
			cpId, siteCps, formId, GET_SPMN_FORM_RECORDS,
			spmnLabels, "spmnLabels", "s.label in (:spmnLabels)",
			Collections.singletonMap("entityType", entityType),
			startAt, maxResults,
			(row) -> {
				Map<String, Object> spmnRecord = new HashMap<>();
				spmnRecord.put("cpId", ((Number) row[0]).longValue());
				spmnRecord.put("cpShortTitle", row[1]);
				spmnRecord.put("spmnId", ((Number) row[2]).longValue());
				spmnRecord.put("spmnLabel", row[3]);
				spmnRecord.put("spmnBarcode", row[4]);
				spmnRecord.put("recordId", ((Number) row[5]).longValue());
				spmnRecord.put("formStatus", row[6]);
				return spmnRecord;
			});
	}

	@Override
	public int moveRegistrationRecords(Long cpId, Long srcFormCtxtId, Long tgtFormCtxtId) {
		String query = MV_REG_FORM_RECORDS;
		if (isOracle()) {
			query += "Ora";
		}

		return createNamedQuery(query)
			.setParameter("cpId", cpId)
			.setParameter("srcFcId", srcFormCtxtId)
			.setParameter("tgtFcId", tgtFormCtxtId)
			.executeUpdate();
	}

	@Override
	public int moveVisitRecords(Long cpId, Long srcFormCtxtId, Long tgtFormCtxtId) {
		String query = MV_VISIT_FORM_RECORDS;
		if (isOracle()) {
			query += "Ora";
		}

		return createNamedQuery(query)
			.setParameter("cpId", cpId)
			.setParameter("srcFcId", srcFormCtxtId)
			.setParameter("tgtFcId", tgtFormCtxtId)
			.executeUpdate();
	}

	@Override
	public int moveSpecimenRecords(Long cpId, Long srcFormCtxtId, Long tgtFormCtxtId) {
		String query = MV_SPMN_FORM_RECORDS;
		if (isOracle()) {
			query += "Ora";
		}

		return createNamedQuery(query)
			.setParameter("cpId", cpId)
			.setParameter("srcFcId", srcFormCtxtId)
			.setParameter("tgtFcId", tgtFormCtxtId)
			.executeUpdate();
	}

	@Override
	public List<FormRevisionDetail> getFormRevisions(Long formId) {
		List<Object[]> rows = createNamedQuery(GET_FORM_REVISIONS, Object[].class)
			.setParameter("formId", formId)
			.list();

		List<FormRevisionDetail> revisions = new ArrayList<>();
		for (Object[] row : rows) {
			int idx = -1;

			FormRevisionDetail revision = new FormRevisionDetail();
			revision.setRev((Long) row[++idx]);
			revision.setRevType((Integer) row[++idx]);

			UserSummary user = new UserSummary();
			user.setId((Long) row[++idx]);
			user.setFirstName((String) row[++idx]);
			user.setLastName((String) row[++idx]);
			user.setEmailAddress((String) row[++idx]);
			revision.setRevBy(user);

			revision.setRevTime((Date) row[++idx]);
			revision.setId((Long) row[++idx]);
			revision.setName((String) row[++idx]);
			revision.setCaption((String) row[++idx]);
			revision.setDeletedOn((Date) row[++idx]);
			revisions.add(revision);
		}

		return revisions;
	}

	@Override
	public Container getFormAtRevision(Long formId, Long revId) {
		try {
			Blob xmlBlob = (Blob) createNativeQuery(
				"select " +
					"xml " +
				"from " +
					"dyextn_containers_aud " +
				"where " +
					"identifier = :formId and rev = :rev"
			)
				.setParameter("formId", formId)
				.setParameter("rev", revId)
				.addBlobScalar("xml")
				.uniqueResult();

			if (xmlBlob == null) {
				return null;
			}

			return Container.fromXml(new String(xmlBlob.getBytes(1, Long.valueOf(xmlBlob.length()).intValue())));
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(CommonErrorCode.SQL_EXCEPTION, e.getMessage());
		}
	}

	@Override
	public List<FormContextRevisionDetail> getFormContextRevisions(Long formId) {
		List<Object[]> rows = createNamedQuery(GET_FORM_CTXT_REVS, Object[].class)
			.setParameter("formId", formId)
			.list();

		List<FormContextRevisionDetail> revisions = new ArrayList<>();
		for (Object[] row : rows) {
			int idx = -1;

			FormContextRevisionDetail rev = new FormContextRevisionDetail();
			rev.setRev((Long) row[++idx]);
			rev.setRevType((Integer) row[++idx]);
			rev.setRevTime((Date) row[++idx]);

			UserSummary user = new UserSummary();
			user.setId((Long) row[++idx]);
			user.setFirstName((String) row[++idx]);
			user.setLastName((String) row[++idx]);
			user.setEmailAddress((String) row[++idx]);
			rev.setRevBy(user);

			rev.setId((Long) row[++idx]);
			rev.setEntityType((String) row[++idx]);

			if (row[++idx] != null) {
				rev.setRevType(2);
			}

			rev.setEntityName((String) row[++idx]);
			revisions.add(rev);
		}

		return revisions;
	}

	private SubQuery<Long> getListFormIdsQuery(FormListCriteria crit, AbstractCriteria<?, ?> mainQuery) {
		SubQuery<Long> query = mainQuery.createSubQuery(Form.class, "form");
		query.leftJoin("form.associations", "fc", () -> query.isNull("fc.deletedOn"))
			.add(query.isNull("form.deletedOn"));

		if (StringUtils.isNotBlank(crit.query())) {
			query.add(query.ilike("form.caption", crit.query()));
		}

		if (crit.userId() != null) {
			Junction userOrCp = query.disjunction();

			query.join("form.createdBy", "createdBy");
			userOrCp.add(query.eq("createdBy.id", crit.userId()));

			if (CollectionUtils.isNotEmpty(crit.siteCps())) {
				userOrCp.add(query.eq("fc.cpId", -1L))
					.add(query.in(
						"fc.cpId",
						BiospecimenDaoHelper.getInstance().getCpIdsFilter(query, crit.siteCps())
					));
			}

			query.add(userOrCp);
		}

		if (CollectionUtils.isNotEmpty(crit.cpIds())) {
			query.add(
				query.or(
					query.eq("fc.cpId", -1L),
					query.in("fc.cpId", crit.cpIds())
				)
			);
		} else if (CollectionUtils.isNotEmpty(crit.cps())) {
			SubQuery<Long> sq = query.createSubQuery(CollectionProtocol.class, Long.class, "cp");
			sq.add(sq.in("cp.shortTitle", crit.cps())).select("cp.id");
			query.add(
				query.or(
					query.eq("fc.cpId", -1L),
					query.in("fc.cpId", sq)
				)
			);
		}

		if (CollectionUtils.isNotEmpty(crit.entityIds())) {
			query.add(
				query.or(
					query.eq("fc.entityId", -1L),
					query.in("fc.entityId", crit.entityIds())
				)
			);
		}

		if (CollectionUtils.isNotEmpty(crit.formIds())) {
			query.add(query.in("form.id", crit.formIds()));
		}

		if (CollectionUtils.isNotEmpty(crit.names())) {
			query.add(query.in("form.name", crit.names()));
		}

		if (CollectionUtils.isEmpty(crit.entityTypes())) {
			query.add(
				query.or(
					query.isNull("fc.entityType"), // when form is not yet associated
					query.ne("fc.entityType", "Query")
				)
			);
		} else {
			query.add(query.in("fc.entityType", crit.entityTypes()));
		}

		if (crit.excludeSysForm() != null) {
			SubQuery<Long> sysForms = query.createSubQuery(FormContextBean.class, "ifc");
			sysForms.add(sysForms.eq("ifc.sysForm", true))
				.add(sysForms.isNull("ifc.deletedOn"))
				.distinct().select("ifc.containerId");
			if (crit.excludeSysForm()) {
				query.add(query.notIn("form.id", sysForms));
			} else {
				query.add(query.in("form.id", sysForms));
			}
		}

		return query.distinct().select("form.id");
	}

	private String getCpIdsSql(Collection<SiteCpPair> siteCps) {
		return AccessCtrlMgr.getInstance().getAllowedCpIdsSql(siteCps);
	}

	private ObjectCpDetail getObjectIdForParticipant(Map<String, Object> dataHookingInformation) {
		ObjectCpDetail objCp = new ObjectCpDetail();
		String cpTitle = (String) dataHookingInformation.get("collectionProtocol");
		String ppId = (String) dataHookingInformation.get("ppi");

		List<Object[]> objs = createNamedQuery(GET_PARTICIPANT_OBJ_ID, Object[].class)
			.setParameter("ppId", ppId)
			.setParameter("cpTitle", cpTitle)
			.list();

		if (objs == null || objs.isEmpty()) {
			return null;
		}

		Object[] row = objs.get(0);
		objCp.setObjectId((Long) row[0]);
		objCp.setCpId((Long) row[1]);
		return objCp;
	}

	private ObjectCpDetail getObjectIdForSpecimen(String entityType, Map<String, Object> dataHookingInformation) {
		ObjectCpDetail objCp = new ObjectCpDetail();
		String id, label, barcode;

		if (entityType.equals("Specimen")) {
			id = (String) dataHookingInformation.get("specimenId");
			label = (String) dataHookingInformation.get("specimenLabel");
			barcode = (String) dataHookingInformation.get("specimenBarcode");
		} else if (entityType.equals("SpecimenEvent")) {
			id = (String) dataHookingInformation.get("specimenIdForEvent");
			label = (String) dataHookingInformation.get("specimenLabelForEvent");
			barcode = (String) dataHookingInformation.get("specimenBarcodeForEvent");
		} else {
			throw new RuntimeException("Unknown entity type: " + entityType);
		}

		Criteria<Object[]> query = createCriteria(Specimen.class, Object[].class, "s");
		query.join("s.visit", "visit")
			.join("visit.registration", "cpr")
			.join("cpr.collectionProtocol", "cp")
			.select("s.id", "cp.id");

		if (id != null) {
			Long specimenId = Long.parseLong(id);
			query.add(query.eq("s.id", specimenId));
		} else if (label != null) {
			query.add(query.eq("s.label", label));
		} else if (barcode != null) {
			query.add(query.eq("s.barcode", barcode));
		} else {
			throw new RuntimeException("Require either Specimen ID, Specimen Label or Specimen Barcode");
		}

		List<Object[]> objs = query.list();
		if (objs == null || objs.isEmpty()) {
			return null;
		}

		Object[] row = objs.get(0);
		objCp.setObjectId((Long) row[0]);
		objCp.setCpId((Long) row[1]);
		return objCp;
	}

	private ObjectCpDetail getObjectIdForSCG(Map<String, Object> dataHookingInformation) {
		ObjectCpDetail objCp = new ObjectCpDetail();
		String id = (String) dataHookingInformation.get("scgId");
		String name = (String) dataHookingInformation.get("scgName");
		String barcode = (String) dataHookingInformation.get("scgBarcode");

		Criteria<Object[]> query = createCriteria(Visit.class, Object[].class, "v");
		query.join("v.registration", "cpr")
			.join("cpr.collectionProtocol", "cp")
			.select("v.id", "cp.id");

		if (id != null) {
			Long scgId = Long.parseLong(id);
			query.add(query.eq("v.id", scgId));
		} else if (name != null) {
			query.add(query.eq("v.name", name));
		} else if (barcode != null) {
			query.add(query.eq("v.barcode", barcode));
		} else {
			throw new RuntimeException("Require either SCG ID, SCG Name or SCG Barcode");
		}

		List<Object[]> objs = query.list();
		if (objs == null || objs.isEmpty()) {
			return null;
		}

		Object[] row = objs.get(0);
		objCp.setObjectId((Long) row[0]);
		objCp.setCpId((Long) row[1]);
		return objCp;
	}
	
	private List<DependentEntityDetail> getDependentEntities(List<Object[]> rows) {
		List<DependentEntityDetail> dependentEntities = new ArrayList<>();
		
		for (Object[] row: rows) {
			String name = (String)row[0];
			int count = ((Integer)row[1]).intValue();
			dependentEntities.add(DependentEntityDetail.from(name, count));
		}
		
		return dependentEntities;
 	}

	private List<Map<String, Object>> getEntityRecords(
		Long cpId, Collection<SiteCpPair> siteCps, Long formId, String queryName,
		List<String> names, String namesVar, String namesCond,
		Map<String, Object> params,
		int startAt, int maxResults,
		Function<Object[], Map<String, Object>> rowMapper) {

		String sql = getCurrentSession().createNamedQuery(queryName).getQueryString();
		if (CollectionUtils.isNotEmpty(names)) {
			int orderByIdx = sql.lastIndexOf("order by");
			sql = sql.substring(0, orderByIdx) + " and " + namesCond + " " + sql.substring(orderByIdx);
		}

		if (cpId != null && cpId != -1L) {
			int orderByIdx = sql.lastIndexOf("order by");
			sql = sql.substring(0, orderByIdx) + " and cp.identifier = " + cpId + " " + sql.substring(orderByIdx);
		}

		if (CollectionUtils.isNotEmpty(siteCps)) {
			int orderByIdx = sql.lastIndexOf("order by");
			sql = sql.substring(0, orderByIdx) + " and cp.identifier in (" + getCpIdsSql(siteCps) + ") " + sql.substring(orderByIdx);
		}

		Query<Object[]> query = createNativeQuery(sql, Object[].class)
			.setParameter("formId", formId)
			.setFirstResult(startAt)
			.setMaxResults(maxResults);

		if (CollectionUtils.isNotEmpty(names)) {
			query.setParameterList(namesVar, names);
		}

		if (params != null) {
			for (Map.Entry<String, Object> param : params.entrySet()) {
				query.setParameter(param.getKey(), param.getValue());
			}
		}

		return query.list().stream().map(rowMapper).collect(Collectors.toList());
	}

	private static final String FQN = FormContextBean.class.getName();
	
	private static final String GET_QUERY_FORMS = FQN + ".getQueryForms";

	private static final String GET_ASSOCIATIONS_CNT = FQN + ".getAssociationsCount";
	
	private static final String GET_FORM_CTXTS = FQN + ".getFormContexts";

	private static final String GET_NOTIF_USERS = FQN + ".getNotifUsers";
	
	private static final String GET_FORM_CTXT = FQN + ".getFormContext";
	
	private static final String GET_SYSTEM_FORM_CTXTS = FQN + ".getSysFormContexts";

	private static final String GET_CPR_FORMS = FQN + ".getCprForms";

	private static final String GET_PARTICIPANT_FORMS = FQN + ".getParticipantForms";
	
	private static final String GET_SPECIMEN_FORMS = FQN + ".getSpecimenForms";
	
	private static final String GET_SPECIMEN_EVENT_FORMS = FQN + ".getSpecimenEventForms";
	
	private static final String GET_SCG_FORMS = FQN + ".getScgForms";
	
	private static final String GET_FORM_CTXTS_BY_ENTITY = FQN + ".getFormContextsByEntity";
	
	private static final String GET_FORM_BY_CTXT = FQN + ".getFormByCtxt";
	
	private static final String GET_FORM_RECORDS = FQN + ".getFormRecords";
	
	private static final String GET_PARTICIPANT_OBJ_ID = FQN + ".getParticipantObjId";

	private static final String GET_FORM_CTX_ID = FQN + ".getFormContextId";

	private static final String GET_FORM_NAME_CTXT_ID = FQN + ".getFormNameContextId";

	private static final String GET_FORM_UPDATE_TIME = FQN + ".getUpdateTime";

	private static final String RE_FQN = FormRecordEntryBean.class.getName();

	private static final String GET_RE_BY_ID = RE_FQN + ".getRecordEntryById";
	
	private static final String GET_RECORD_ENTRY = RE_FQN + ".getRecordEntry";
	
	private static final String GET_RECORD_ENTRIES = RE_FQN + ".getRecordEntries";

	private static final String GET_REC_BY_FORM_N_REC_ID = RE_FQN + ".getRecordEntryByFormAndRecId";

	private static final String GET_LATEST_RECORD_IDS = RE_FQN + ".getLatestRecordIds";

	private static final String GET_RECORD_IDS = RE_FQN + ".getRecordIds";

	private static final String GET_FORM_IDS = FQN + ".getFormIds";
	
	private static final String GET_QUERY_FORM_CONTEXT = FQN + ".getQueryFormCtxtByContainerId";

	private static final String GET_RECORD_CNT = FQN + ".getRecordCount"; 
	
	private static final String GET_RECS_BY_TYPE_AND_OBJECT = FQN  + ".getRecordsByEntityAndObject";

	private static final String GET_RECS = FQN + ".getRecords";
	
	private static final String GET_DEPENDENT_ENTITIES = FQN + ".getDependentEntities";

	private static final String GET_FORM_CTXT_REVS = FQN + ".getFormContextRevisions";
	
	private static final String GET_CHANGE_LOG_DIGEST_SQL =
			"select " +
			"  md5_digest " +
			"from " +
			"  os_import_forms_log fl " +
			"where " +
			"  fl.filename = :filename and fl.executed_on in (" +
			"    select " +
			"      max(executed_on) " +
			"    from " +
			"      os_import_forms_log " +
			"    where " +
			"      filename = :filename )";

	private static final String GET_LATEST_CHANGE_LOG_SQL =
			"select " +
			"  fl.filename, fl.form_id, fl.md5_digest, fl.executed_on " +
			"from " +
			"  os_import_forms_log fl " +
			"where " +
			"  fl.filename = :filename and fl.executed_on in (" +
			"    select " +
			"      max(executed_on) " +
			"    from " +
			"      os_import_forms_log " +
			"    where " +
			"      filename = :filename )";
	
	private static final String INSERT_CHANGE_LOG_SQL =
			"insert into os_import_forms_log " +
			"	(filename, form_id, md5_digest, executed_on) " +
			"values " +
			"   (:filename, :formId, :digest, :executedOn) ";
	
	private static final String SOFT_DELETE_FORM_CONTEXTS_SQL = 
			"update catissue_form_context set deleted_on = :deletedOn where container_id in (:formIds)";

	private static final String SOFT_DELETE_RECS_SQL =
			"update catissue_form_record_entry " +
			"set " +
			"  activity_status = 'CLOSED' " +
			"where " +
			"  form_ctxt_id = :formCtxtId and record_id in (:recordIds)";


	private static final String SOFT_DELETE_ENTITY_RECS = RE_FQN + ".deleteEntityRecords";

	private static final String UNDO_DELETE_ENTITY_RECS = RE_FQN + ".undeleteEntityRecords";

	private static final String SOFT_DELETE_CP_FORMS = FQN + ".deleteCpEntityForms";

	private static final String GET_ENTITY_FORM_RECORD_IDS = RE_FQN + ".getEntityFormRecordIds";

	//
	// used for form records export
	//
	private static final String GET_PART_FORM_RECORDS  = RE_FQN + ".getParticipantRecords";

	private static final String GET_REG_FORM_RECORDS   = RE_FQN + ".getRegistrationRecords";

	private static final String GET_VISIT_FORM_RECORDS = RE_FQN + ".getVisitRecords";

	private static final String GET_SPMN_FORM_RECORDS  = RE_FQN + ".getSpecimenRecords";

	//
	// Used in form records movement
	//
	private static final String MV_REG_FORM_RECORDS   = RE_FQN + ".moveRegistrationRecords";

	private static final String MV_VISIT_FORM_RECORDS = RE_FQN + ".moveVisitRecords";

	private static final String MV_SPMN_FORM_RECORDS  = RE_FQN + ".moveSpecimenRecords";

	//
	// form revisions
	private static final String FORM_FQN = Form.class.getName();

	private static final String GET_FORM_REVISIONS = FORM_FQN + ".getRevisions";

}
