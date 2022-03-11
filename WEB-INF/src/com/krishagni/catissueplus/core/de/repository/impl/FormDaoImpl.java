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
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.sql.JoinType;
import org.hibernate.type.BlobType;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;
import org.hibernate.type.TimestampType;

import com.krishagni.catissueplus.core.administrative.repository.FormListCriteria;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolSummary;
import com.krishagni.catissueplus.core.biospecimen.repository.impl.BiospecimenDaoHelper;
import com.krishagni.catissueplus.core.common.OrderByNotNullProperty;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
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
		return getById(id, "deletedOn is null");
	}

	@Override
	public Form getFormById(Long formId) {
		return (Form) getCurrentSession().createCriteria(Form.class)
			.add(Restrictions.eq("id", formId))
			.add(Restrictions.isNull("deletedOn"))
			.uniqueResult();
	}

	@Override
	public Form getFormByName(String name) {
		return (Form) getCurrentSession().createCriteria(Form.class)
			.add(Restrictions.eq("name", name))
			.add(Restrictions.isNull("deletedOn"))
			.uniqueResult();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Form> getFormsByIds(Collection<Long> formIds) {
		return getCurrentSession().createCriteria(Form.class)
			.add(Restrictions.in("id", formIds))
			.add(Restrictions.isNull("deletedOn"))
			.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Form> getFormsByName(Collection<String> formNames) {
		return getCurrentSession().createCriteria(Form.class)
			.add(Restrictions.in("name", formNames))
			.add(Restrictions.isNull("deletedOn"))
			.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Form> getForms(FormListCriteria crit) {
		return (List<Form>) getCurrentSession().createCriteria(Form.class, "form")
			.add(Subqueries.propertyIn("form.id", getListFormIdsQuery(crit)))
			.addOrder(OrderByNotNullProperty.desc("form.updateTime", "form.creationTime"))
			.setFirstResult(crit.startAt())
			.setMaxResults(crit.maxResults())
			.list();
	}

	@Override
	public Long getFormsCount(FormListCriteria crit) {
		Number count = (Number) getCurrentSession().createCriteria(Form.class, "f")
			.add(Subqueries.propertyIn("f.id", getListFormIdsQuery(crit)))
			.setProjection(Projections.rowCount())
			.uniqueResult();
		return count.longValue();
	}

	@Override
	public List<FormSummary> getEntityForms(FormListCriteria crit) {
		List<Object[]> rows = (List<Object[]>) getListFormsQuery(crit)
			.setProjection(
				Projections.projectionList()
					.add(Projections.property("form.id"))
					.add(Projections.property("form.name"))
					.add(Projections.property("form.caption"))
					.add(Projections.property("fc.entityType"))
					.add(Projections.property("fc.sysForm"))
					.add(Projections.property("fc.multiRecord"))
					.add(Projections.property("fc.identifier"))
					.add(Projections.property("fc.cpId"))
			)
			.getExecutableCriteria(getCurrentSession())
			.list();

		List<FormSummary> result = new ArrayList<>();
		for (Object[] row : rows) {
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

	@SuppressWarnings("unchecked")
	@Override
	public Map<Long, Integer> getAssociationsCount(Collection<Long> formIds) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_ASSOCIATIONS_CNT)
			.setParameterList("formIds", formIds)
			.list();

		Map<Long, Integer> result = new HashMap<>();
		for (Object[] row : rows) {
			result.put((Long) row[0], (Integer) row[1]);
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean isSystemForm(Long formId) {
		List<FormContextBean> result = sessionFactory.getCurrentSession()
				.getNamedQuery(GET_SYSTEM_FORM_CTXTS)
				.setLong("formId", formId)
				.setMaxResults(1)
				.list();

		return !result.isEmpty();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Date getUpdateTime(Long formId) {
		return (Date) getCurrentSession().getNamedQuery(GET_FORM_UPDATE_TIME)
				.setParameter("formId", formId)
				.uniqueResult();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<FormSummary> getQueryForms() {
		Query query = sessionFactory.getCurrentSession().getNamedQuery(GET_QUERY_FORMS);
		List<Object[]> rows = query.list();

		List<FormSummary> forms = new ArrayList<FormSummary>();
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
	
	@SuppressWarnings("unchecked")
	@Override
	public List<FormContextDetail> getFormContexts(Long formId) {
		Query query = sessionFactory.getCurrentSession().getNamedQuery(GET_FORM_CTXTS);
		List<Object[]> rows = query.setLong("formId", formId).list();
		
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

			CollectionProtocolSummary cp = new CollectionProtocolSummary();
			cp.setId((Long)row[++idx]);
			cp.setShortTitle((String)row[++idx]);
			cp.setTitle((String)row[++idx]);
			formCtxt.setCollectionProtocol(cp);
			
			formCtxts.add(formCtxt);
		}
		
		return formCtxts;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public FormContextBean getQueryFormContext(Long formId) {
		Query query = sessionFactory.getCurrentSession().getNamedQuery(GET_QUERY_FORM_CONTEXT);
		List<Object> queryFormContext = query.setLong("formId", formId).setString("entityType", "Query").list();
		return (FormContextBean)(queryFormContext.size() == 1 ? queryFormContext.get(0) : null);
	}

	@Override
	public List<FormContextBean> getFormContexts(Collection<Long> cpIds, String entityType) {
		return sessionFactory.getCurrentSession()
			.createCriteria(FormContextBean.class)
			.add(Restrictions.in("cpId", cpIds))
			.add(Restrictions.eq("entityType", entityType))
			.add(Restrictions.isNull("deletedOn"))
			.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<FormCtxtSummary> getCprForms(Long cprId) {
		Query query = sessionFactory.getCurrentSession().getNamedQuery(GET_CPR_FORMS);
		query.setLong("cprId", cprId);		
		return getEntityForms(query.list());
	}

	public List<FormCtxtSummary> getParticipantForms(Long cprId) {
		Query query = sessionFactory.getCurrentSession().getNamedQuery(GET_PARTICIPANT_FORMS);
		query.setLong("cprId", cprId);
		return getEntityForms(query.list());
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<FormCtxtSummary> getSpecimenForms(Long specimenId) {
		Query query = sessionFactory.getCurrentSession().getNamedQuery(GET_SPECIMEN_FORMS);
		query.setLong("specimenId", specimenId);		
		return getEntityForms(query.list());
	}
	
    @SuppressWarnings("unchecked")
    @Override
    public List<FormCtxtSummary> getSpecimenEventForms(Long specimenId) {
        Query query = sessionFactory.getCurrentSession().getNamedQuery(GET_SPECIMEN_EVENT_FORMS);
        query.setLong("specimenId", specimenId);
        return getEntityForms(query.list());
    }
	
	@SuppressWarnings("unchecked")
	@Override
	public List<FormCtxtSummary> getScgForms(Long scgId) {
		Query query = sessionFactory.getCurrentSession().getNamedQuery(GET_SCG_FORMS);
		query.setLong("scgId", scgId);		
		return getEntityForms(query.list());
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<FormCtxtSummary> getFormContexts(Long cpId, String entityType) {
		List<FormContextBean> result = sessionFactory.getCurrentSession()
			.getNamedQuery(GET_FORM_CTXTS_BY_ENTITY)
			.setLong("cpId", cpId)
			.setString("entityType", entityType)
			.list();
				
		return FormCtxtSummary.from(result);
	}
			
	@SuppressWarnings("unchecked")
	@Override
	public List<FormRecordSummary> getFormRecords(Long formCtxtId, Long objectId) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_FORM_RECORDS)
			.setParameter("formCtxtId", formCtxtId)
			.setParameter("objectId", objectId)
			.list();

		List<FormRecordSummary> formRecords = new ArrayList<>();
		for (Object[] row : rows) {
			int idx = 0;
			FormRecordSummary record = new FormRecordSummary();
			record.setId((Long)row[idx++]);
			record.setRecordId((Long)row[idx++]);
			record.setUpdateTime((Date)row[idx++]);
			record.setSysForm((boolean)row[idx++]);
			
			UserSummary user = new UserSummary();
			user.setId((Long)row[idx++]);
			user.setFirstName((String)row[idx++]);
			user.setLastName((String)row[idx++]);
			record.setUser(user);
			
			formRecords.add(record);
		}
		
		return formRecords;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public FormSummary getFormByContext(Long formCtxtId) {
		List<Object[]> rows = sessionFactory.getCurrentSession()
			.getNamedQuery(GET_FORM_BY_CTXT)
			.setLong("formCtxtId", formCtxtId)
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
		
	@SuppressWarnings("unchecked")
	@Override
	public FormContextBean getFormContext(Long formId, Long cpId, String entity) {
		return getFormContext(formId, cpId, Collections.singletonList(entity));
	}

	@Override
	public FormContextBean getFormContext(Long formId, Long cpId, List<String> entities) {
		List<FormContextBean> fcs = getCurrentSession().getNamedQuery(GET_FORM_CTXT)
			.setParameter("formId", formId)
			.setParameter("cpId", cpId)
			.setParameterList("entityTypes", entities)
			.list();
		return fcs != null && !fcs.isEmpty() ? fcs.iterator().next() : null;
	}

	@Override
	public FormContextBean getFormContext(boolean cpBased, String entityType, Long entityId, Long formId) {
		entityId = entityId == null ? -1L : entityId;

		Criteria query = getCurrentSession().createCriteria(FormContextBean.class)
			.add(Restrictions.eq("entityType", entityType))
			.add(Restrictions.isNull("deletedOn"));

		if (cpBased) {
			query.add(Restrictions.or(Restrictions.eq("cpId", -1L), Restrictions.eq("cpId", entityId)));
		} else {
			query.add(Restrictions.eq("entityId", entityId));
		}

		if (formId != null) {
			query.add(Restrictions.eq("containerId", formId));
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

	@SuppressWarnings("unchecked")
	@Override
	public List<FormContextBean> getFormContextsById(List<Long> formContextIds) {
		List<FormContextBean> formContexts = new ArrayList<FormContextBean>();
		
		int i = 0;
		int numIds = formContextIds.size();
		while (i < numIds) {
			List<Long> params = formContextIds.subList(i, i + 500 > numIds ? numIds : i + 500);
			i += 500;
			
			formContexts.addAll(
				sessionFactory.getCurrentSession()
					.getNamedQuery(GET_FORM_CTXTS_BY_ID)
					.setParameterList("ids", params)
					.list());					
		}
		
		return formContexts;
	}

	// Returns deleted form contexts for a specific input CP. Not fallback to all.
	@Override
	public FormContextBean getAbsFormContext(Long formId, Long cpId, String entity) {
		Session session = getCurrentSession();
		session.disableFilter("activeForms");

		List<FormContextBean> fcs = session.createCriteria(FormContextBean.class, "fc")
			.add(Restrictions.eq("fc.containerId", formId))
			.add(Restrictions.eq("fc.cpId", cpId))
			.add(Restrictions.eq("fc.entityType", entity))
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
		List<Object[]> rows = getCurrentSession()
			.getNamedQuery(GET_FORM_NAME_CTXT_ID)
			.setParameter("cpId", cpId)
			.setParameter("entityType", entityType)
			.setParameter("entityId", entityId, LongType.INSTANCE)
			.list();

		if (CollectionUtils.isEmpty(rows)) {
			return null;
		}

		Object[] row = rows.iterator().next();
		return Pair.make((String)row[0], (Long)row[1]);
	}
	
	@Override
	public void saveOrUpdateRecordEntry(FormRecordEntryBean recordEntry) {
		sessionFactory.getCurrentSession().saveOrUpdate(recordEntry);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<FormRecordEntryBean> getRecordEntries(Long formCtxtId, Long objectId) {
		return getCurrentSession().getNamedQuery(GET_RECORD_ENTRIES)
			.setParameter("formCtxtId", formCtxtId)
			.setParameter("objectId", objectId)
			.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public FormRecordEntryBean getRecordEntry(Long recordId) {
		return (FormRecordEntryBean) getCurrentSession().getNamedQuery(GET_RE_BY_ID)
			.setParameter("recordId", recordId)
			.uniqueResult();
	}

	@SuppressWarnings("unchecked")
	@Override
	public FormRecordEntryBean getRecordEntry(Long formCtxtId, Long objectId, Long recordId) {
		return (FormRecordEntryBean) getCurrentSession().getNamedQuery(GET_RECORD_ENTRY)
			.setParameter("formCtxtId", formCtxtId)
			.setParameter("objectId", objectId)
			.setParameter("recordId", recordId)
			.uniqueResult();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public FormRecordEntryBean getRecordEntry(Long formId, Long recordId) {
		return (FormRecordEntryBean) getCurrentSession().getNamedQuery(GET_REC_BY_FORM_N_REC_ID)
			.setParameter("formId", formId)
			.setParameter("recordId", recordId)
			.uniqueResult();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<Long, Pair<Long, Long>> getLatestRecordIds(Long formId, String entityType, List<Long> objectIds) {
		List<Object[]> rows = sessionFactory.getCurrentSession()
				.getNamedQuery(GET_LATEST_RECORD_IDS)
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
		if (entityType.equals("Participant") ) {
			objCp = getObjectIdForParticipant(dataHookingInformation);
		} else if (entityType.equals("Specimen") || entityType.equals("SpecimenEvent")) {
			objCp = getObjectIdForSpecimen(entityType, dataHookingInformation);
		} else if (entityType.equals("SpecimenCollectionGroup")) {
			objCp = getObjectIdForSCG(dataHookingInformation);
		}
		
		return objCp;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Long getFormCtxtId(Long containerId, String entityType, Long cpId) {
        Long formCtxtId = null;

        Query query = sessionFactory.getCurrentSession().getNamedQuery(GET_FORM_CTX_ID);
		query.setLong("containerId", containerId);
		query.setString("entityType", entityType);
        query.setLong("cpId", cpId);

        List<Object[]> rows = query.list();

        if (rows == null || rows.isEmpty()) {
            return null;
        }

        for (Object[] row : rows) {
            formCtxtId = (Long) row[0];
            if (cpId.equals((Long) row[1])) {
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
		Query query = sessionFactory.getCurrentSession().getNamedQuery(GET_FORM_IDS);
		query.setLong("cpId", cpId).setParameterList("entityTypes", entityTypes);
		
		List<Long> formIds = new ArrayList<Long>();
		for (Object id : query.list()) {
			formIds.add((Long)id);
		}
		
		return formIds;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Long getRecordsCount(Long formCtxtId, Long objectId) {
		Query query = sessionFactory.getCurrentSession().getNamedQuery(GET_RECORD_CNT);
		query.setLong("formCtxtId", formCtxtId).setLong("objectId", objectId);
		List<Long> result = query.list();
		return result.iterator().next();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Map<Long, List<FormRecordSummary>> getFormRecords(Long objectId, String entityType, Long formId) {
		Query query = null;
		if (formId == null) {
			query = sessionFactory.getCurrentSession()
				.getNamedQuery(GET_RECS_BY_TYPE_AND_OBJECT);					
		} else {
			query = sessionFactory.getCurrentSession()
				.getNamedQuery(GET_RECS)
				.setLong("formId", formId);
		}
		
		List<Object[]> rows = query.setString("entityType", entityType)
				.setLong("objectId", objectId)
				.list();
		
		Map<Long, List<FormRecordSummary>> result = new HashMap<Long, List<FormRecordSummary>>();
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

			List<FormRecordSummary> recs = result.computeIfAbsent(form, (k) -> new ArrayList<>());
			recs.add(record);
		}
				
		return result;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<DependentEntityDetail> getDependentEntities(Long formId) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_DEPENDENT_ENTITIES)
				.setLong("formId", formId)
				.list();
		
		return getDependentEntities(rows);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public String getFormChangeLogDigest(String file) {
		List<Object> rows = getCurrentSession().createSQLQuery(GET_CHANGE_LOG_DIGEST_SQL)
				.setString("filename", file)
				.list();
		
		if (rows == null || rows.isEmpty()) {
			return null;
		}		
		return (String)rows.iterator().next();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object[] getLatestFormChangeLog(String file) {
		List<Object[]> rows = getCurrentSession().createSQLQuery(GET_LATEST_CHANGE_LOG_SQL)
				.addScalar("filename", StringType.INSTANCE) //fl.filename, fl.form_id, fl.md5_digest, fl.executed_on
				.addScalar("form_id", LongType.INSTANCE)
				.addScalar("md5_digest", StringType.INSTANCE)
				.addScalar("executed_on", TimestampType.INSTANCE)
				.setParameter("filename", file)
				.list();

		if (rows == null || rows.isEmpty()) {
			return null;
		}

		return rows.iterator().next();
	}

	@Override
	public void insertFormChangeLog(String file, String digest, Long formId) {
		sessionFactory.getCurrentSession()
				.createSQLQuery(INSERT_CHANGE_LOG_SQL)
				.setString("filename", file)
				.setString("digest", digest)
				.setLong("formId", formId)
				.setTimestamp("executedOn", Calendar.getInstance().getTime())
				.executeUpdate();
	}
	
	@Override
	public void deleteFormContexts(Collection<Long> formIds) {
		getCurrentSession()
			.createSQLQuery(SOFT_DELETE_FORM_CONTEXTS_SQL)
			.setTimestamp("deletedOn", Calendar.getInstance().getTime())
			.setParameterList("formIds", formIds)
			.executeUpdate(); 
	}

	@Override
	public void deleteRecords(Long formCtxtId, Collection<Long> recordIds) {
		getCurrentSession().createSQLQuery(SOFT_DELETE_RECS_SQL)
			.setLong("formCtxtId", formCtxtId)
			.setParameterList("recordIds", recordIds)
			.executeUpdate();
	}

	@Override
	public int deleteRecords(Long cpId, List<String> entityTypes, Long objectId) {
		return getCurrentSession().getNamedQuery(SOFT_DELETE_ENTITY_RECS)
			.setParameter("cpId", cpId)
			.setParameterList("entityTypes", entityTypes)
			.setParameter("objectId", objectId)
			.executeUpdate();
	}

	@Override
	public int undeleteRecords(Long cpId, List<String> entityTypes, Long objectId) {
		return getCurrentSession().getNamedQuery(UNDO_DELETE_ENTITY_RECS)
			.setParameter("cpId", cpId)
			.setParameterList("entityTypes", entityTypes)
			.setParameter("objectId", objectId)
			.executeUpdate();
	}

	@Override
	public int deleteFormContexts(Long cpId, List<String> entityTypes) {
		return getCurrentSession().getNamedQuery(SOFT_DELETE_CP_FORMS)
			.setParameter("deleteTime", Calendar.getInstance().getTime())
			.setParameter("cpId", cpId)
			.setParameterList("entityTypes", entityTypes)
			.executeUpdate();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<Long, List<Long>> getRecordIds(Long formCtxtId, Collection<Long> objectIds) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_RECORD_IDS)
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
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_ENTITY_FORM_RECORD_IDS)
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
				return visitRecord;
			});
	}

	@SuppressWarnings("unchecked")
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
				return spmnRecord;
			});
	}

	@Override
	public int moveRegistrationRecords(Long cpId, Long srcFormCtxtId, Long tgtFormCtxtId) {
		String query = MV_REG_FORM_RECORDS;
		if (isOracle()) {
			query += "Ora";
		}

		return getCurrentSession().getNamedQuery(query)
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

		return getCurrentSession().getNamedQuery(query)
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

		return getCurrentSession().getNamedQuery(query)
			.setParameter("cpId", cpId)
			.setParameter("srcFcId", srcFormCtxtId)
			.setParameter("tgtFcId", tgtFormCtxtId)
			.executeUpdate();
	}

	@Override
	public List<FormRevisionDetail> getFormRevisions(Long formId) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_FORM_REVISIONS)
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
			Blob xmlBlob = (Blob) getCurrentSession()
				.createSQLQuery(
					"select " +
						"xml " +
					"from " +
						"dyextn_containers_aud " +
					"where " +
						"identifier = :formId and rev = :rev"
				)
				.setParameter("formId", formId)
				.setParameter("rev", revId)
				.addScalar("xml", BlobType.INSTANCE)
				.uniqueResult();
			if (xmlBlob == null) {
				return null;
			}

			return Container.fromXml(new String(xmlBlob.getBytes(1, (int) xmlBlob.length())));
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(CommonErrorCode.SQL_EXCEPTION, e.getMessage());
		}
	}

	@Override
	public List<FormContextRevisionDetail> getFormContextRevisions(Long formId) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_FORM_CTXT_REVS)
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

	private DetachedCriteria getListFormIdsQuery(FormListCriteria crit) {
		return getListFormsQuery(crit).setProjection(Projections.distinct(Projections.property("form.id")));
	}

	private DetachedCriteria getListFormsQuery(FormListCriteria crit) {
		DetachedCriteria query = DetachedCriteria.forClass(Form.class, "form")
			.createAlias("form.associations", "fc", JoinType.LEFT_OUTER_JOIN, Restrictions.isNull("fc.deletedOn"))
			.add(Restrictions.isNull("form.deletedOn"));

		if (StringUtils.isNotBlank(crit.query())) {
			query.add(Restrictions.ilike("form.caption", crit.query(), MatchMode.ANYWHERE));
		}

		if (crit.userId() != null) {
			Junction userOrCp = Restrictions.disjunction();

			query.createAlias("form.createdBy", "createdBy");
			userOrCp.add(Restrictions.eq("createdBy.id", crit.userId()));

			if (CollectionUtils.isNotEmpty(crit.siteCps())) {
				userOrCp.add(Restrictions.eq("fc.cpId", -1L))
					.add(
						Subqueries.propertyIn(
							"fc.cpId",
							BiospecimenDaoHelper.getInstance().getCpIdsFilter(crit.siteCps())
						)
					);
			}

			query.add(userOrCp);
		}

		if (CollectionUtils.isNotEmpty(crit.cpIds())) {
			query.add(
				Restrictions.or(
					Restrictions.eq("fc.cpId", -1L),
					Restrictions.in("fc.cpId", crit.cpIds())
				)
			);
		}

		if (CollectionUtils.isNotEmpty(crit.entityIds())) {
			query.add(
				Restrictions.or(
					Restrictions.eq("fc.entityId", -1L),
					Restrictions.in("fc.entityId", crit.entityIds())
				)
			);
		}

		if (CollectionUtils.isNotEmpty(crit.names())) {
			query.add(Restrictions.in("form.name", crit.names()));
		}

		if (CollectionUtils.isEmpty(crit.entityTypes())) {
			query.add(
				Restrictions.or(
					Restrictions.isNull("fc.entityType"), // when form is not yet associated
					Restrictions.ne("fc.entityType", "Query")
				)
			);
		} else {
			query.add(Restrictions.in("fc.entityType", crit.entityTypes()));
		}

		if (crit.excludeSysForm() != null) {
			DetachedCriteria sysForms = DetachedCriteria.forClass(FormContextBean.class, "ifc")
				.add(Restrictions.eq("ifc.sysForm", true))
				.add(Restrictions.isNull("ifc.deletedOn"))
				.setProjection(Projections.distinct(Projections.property("ifc.containerId")));
			if (crit.excludeSysForm()) {
				query.add(Subqueries.propertyNotIn("form.id", sysForms));
			} else {
				query.add(Subqueries.propertyIn("form.id", sysForms));
			}
		}

		return query;
	}

	private String getCpIdsSql(Collection<SiteCpPair> siteCps) {
		return AccessCtrlMgr.getInstance().getAllowedCpIdsSql(siteCps);
	}

	@SuppressWarnings("unchecked")
	private ObjectCpDetail getObjectIdForParticipant(Map<String, Object> dataHookingInformation) {
		ObjectCpDetail objCp = new ObjectCpDetail();
		String cpTitle = (String) dataHookingInformation.get("collectionProtocol");
		String ppId = (String) dataHookingInformation.get("ppi");

		Query query = sessionFactory.getCurrentSession().getNamedQuery(GET_PARTICIPANT_OBJ_ID);
		query.setString("ppId", ppId);
		query.setString("cpTitle", cpTitle);

		List<Object[]> objs = query.list();
		if (objs == null || objs.isEmpty()) {
			return null;
		}

		Object[] row = objs.get(0);
		objCp.setObjectId((Long) row[0]);
		objCp.setCpId((Long) row[1]);
		return objCp;
	}
	
	@SuppressWarnings("unchecked")
	private ObjectCpDetail getObjectIdForSpecimen(String entityType, Map<String, Object> dataHookingInformation) {
		ObjectCpDetail objCp = new ObjectCpDetail();
		String id = null, label = null, barcode = null;

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

		Criteria query = sessionFactory
				.getCurrentSession()
				.createCriteria(Specimen.class)
				.createAlias("specimenCollectionGroup", "scg")
				.createAlias("scg.collectionProtocolRegistration", "cpr")
				.createAlias("cpr.collectionProtocol", "cp")
				.setProjection(
						Projections.projectionList()
								.add(Projections.property("id"))
								.add(Projections.property("cp.id")));

		if (id != null) {
			Long specimenId = Long.parseLong(id);
			query.add(Restrictions.eq("id", specimenId));
		} else if (label != null) {
			query.add(Restrictions.eq("label", label));
		} else if (barcode != null) {
			query.add(Restrictions.eq("barcode", barcode));
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

	@SuppressWarnings("unchecked")
	private ObjectCpDetail getObjectIdForSCG(Map<String, Object> dataHookingInformation) {
		ObjectCpDetail objCp = new ObjectCpDetail();
		String id = (String) dataHookingInformation.get("scgId");
		String name = (String) dataHookingInformation.get("scgName");
		String barcode = (String) dataHookingInformation.get("scgBarcode");

		Criteria query = sessionFactory
				.getCurrentSession()
				.createCriteria(Visit.class)
				.createAlias("collectionProtocolRegistration", "cpr")
				.createAlias("cpr.collectionProtocol", "cp")
				.setProjection(
						Projections.projectionList()
								.add(Projections.property("id"))
								.add(Projections.property("cp.id")));

		if (id != null) {
			Long scgId = Long.parseLong(id);
			query.add(Restrictions.eq("id", scgId));
		} else if (name != null) {
			query.add(Restrictions.eq("name", name));
		} else if (barcode != null) {
			query.add(Restrictions.eq("barcode", barcode));
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
		List<DependentEntityDetail> dependentEntities = new ArrayList<DependentEntityDetail>();
		
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

		String sql = getCurrentSession().getNamedQuery(queryName).getQueryString();
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

		Query query = getCurrentSession().createSQLQuery(sql)
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

		return ((List<Object[]>)query.list()).stream().map(rowMapper).collect(Collectors.toList());
	}

	private static final String FQN = FormContextBean.class.getName();
	
	private static final String GET_QUERY_FORMS = FQN + ".getQueryForms";

	private static final String GET_ASSOCIATIONS_CNT = FQN + ".getAssociationsCount";
	
	private static final String GET_FORM_CTXTS = FQN + ".getFormContexts";
	
	private static final String GET_FORM_CTXT = FQN + ".getFormContext";
	
	private static final String GET_SYSTEM_FORM_CTXTS = FQN + ".getSysFormContexts";

	private static final String GET_FORM_CTXTS_BY_ID = FQN + ".getFormContextsById";
	
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
