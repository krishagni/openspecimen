package com.krishagni.catissueplus.core.de.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.de.repository.DaoFactory;
import com.krishagni.catissueplus.core.de.services.FormContextProcessor;
import com.krishagni.catissueplus.core.de.services.FormService;

import edu.common.dynamicextensions.domain.nui.Container;
import edu.common.dynamicextensions.napi.FormEventsListener;
import edu.common.dynamicextensions.napi.FormEventsNotifier;
import krishagni.catissueplus.beans.FormContextBean;

//
// Used by DeObject. Any further use of this object needs to be carefully
// reviewed, as lot of internal details of this object are based on how
// DeObject works.
//

public class FormInfoCache implements FormContextProcessor, FormEventsListener, InitializingBean {
	private FormService formSvc;

	private DaoFactory daoFactory;

	public void setFormSvc(FormService formSvc) {
		this.formSvc = formSvc;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	//
	// Key is CP ID. For non CP specific, key is -1L
	//
	private final Map<Long, ContextInfo> contextInfoMap = new HashMap<>();

	public FormInfoCache() {

	}

	@Override
	public void afterPropertiesSet() throws Exception {
		FormEventsNotifier.getInstance().addListener(this);
		getFormService().addFormContextProc("*", this);
	}

	public String getFormName(Long cpId, String entityType) {
		return getFormName(true, entityType, cpId);
	}

	public String getFormName(boolean cpBased, String entityType, Long entityId) {
		Long cpId = cpBased ? entityId : -1L;
		String entityTypeKey = cpBased ? entityType : entityType + "-" + entityId;

		ContextInfo ctxtInfo = getCpContextInfo(cpId);
		if (!ctxtInfo.hasFormName(entityTypeKey)) {
			synchronized (ctxtInfo) {
				String formName = null;
				Long formCtxtId = null;

				Pair<String, Long> formInfo = getDaoFactory().getFormDao().getFormNameContext(cpId, entityType, cpBased ? null : entityId);
				if (formInfo != null) {
					formName = formInfo.first();
					formCtxtId = formInfo.second();
				}

				ctxtInfo.addFormName(entityTypeKey, formName);
				ctxtInfo.addFormContext(entityTypeKey, formName, formCtxtId);
			}
		}

		return ctxtInfo.getFormName(entityTypeKey);
	}

	public Long getFormContext(Long cpId, String entityType, String formName) {
		return getFormContext(true, entityType, cpId, formName);
	}

	public Long getFormContext(boolean cpBased, String entityType, Long entityId, String formName) {
		Long cpId = cpBased ? entityId : -1L;
		String entityTypeKey = cpBased ? entityType : entityType + "-" + entityId;

		ContextInfo ctxtInfo = getCpContextInfo(cpId);
		if (!ctxtInfo.hasFormContext(entityType, formName)) {
			Container form = getForm(formName);
			synchronized (ctxtInfo) {
				FormContextBean formCtx = getDaoFactory().getFormDao().getFormContext(cpBased, entityType, entityId, form.getId());
				if (formCtx != null) {
					ctxtInfo.addFormContext(entityTypeKey, formName, formCtx.getIdentifier());
				}
			}
		}

		return ctxtInfo.getFormContext(entityTypeKey, formName);
	}

	public Container getForm(String formName) {
		return Container.getContainer(formName);
	}

	public Map<String, Object> getFormInfo(Long cpId, String entity) {
		return getFormInfo(true, entity, cpId);
	}

	public Map<String, Object> getFormInfo(boolean cpBased, String entityType, Long entityId) {
		String formName = getFormName(cpBased, entityType, entityId);
		if (StringUtils.isBlank(formName) && entityId != -1L) {
			entityId = -1L;
			formName = getFormName(cpBased, entityType, entityId);
		}

		if (StringUtils.isBlank(formName)) {
			return null;
		}

		Map<String, Object> formInfo = new HashMap<>();
		formInfo.put("formId", getForm(formName).getId());
		formInfo.put("formCtxtId", getFormContext(cpBased, entityType, entityId, formName));
		formInfo.put("formName", formName);
		return formInfo;
	}

	@Override
	public void onSaveOrUpdate(FormContextBean formCtxt) {
		removeCpFormContext(formCtxt);
	}

	@Override
	public void onRemove(FormContextBean formCtxt) {
		removeCpFormContext(formCtxt);
	}

	@Override
	public void onCreate(Container container) {
		// no-op
	}

	@Override
	public void preUpdate(Container form) {
		// no-op
	}

	@Override
	public void onUpdate(Container container) {
		// no-op
	}

	@Override
	public synchronized void onDelete(Container container) {
		for (ContextInfo ctxtInfo : contextInfoMap.values()) {
			ctxtInfo.removeForm(container.getName());
		}
	}

	public void removeCpFormContext(FormContextBean formCtxt) {
		ContextInfo contextInfo = getCpContextInfo(formCtxt.getCpId());
		if (contextInfo == null) {
			return;
		}

		synchronized (contextInfo) {
			String entityTypeKey = formCtxt.getEntityType();
			if (formCtxt.getEntityId() != null) {
				entityTypeKey += "-" + formCtxt.getEntityId();
			}

			contextInfo.removeFormName(entityTypeKey);
			contextInfo.removeFormContext(formCtxt.getIdentifier());
		}
	}

	private FormService getFormService() {
		return formSvc;
	}

	private DaoFactory getDaoFactory() {
		return daoFactory;
	}

	private ContextInfo getCpContextInfo(Long cpId) {
		if (!contextInfoMap.containsKey(cpId)) {
			synchronized (contextInfoMap) {
				if (!contextInfoMap.containsKey(cpId)) {
					contextInfoMap.put(cpId, new ContextInfo());
				}
			}
		}

		return contextInfoMap.get(cpId);
	}

	private static class ContextInfo {
		//
		// Key is entity. e.g. ParticipantExtension, SpecimenExtension etc
		// Value is name of form representing the extension
		//
		private Map<String, String> entityForms = new HashMap<>();

		//
		// Key is entity#form_name. e.g. ParticipantExtension#ParticipantCustomFields
		// Value is form context ID
		//
		private Map<String, Long> formContexts = new HashMap<>();

		public Map<String, String> getEntityForms() {
			return entityForms;
		}

		public void setEntityForms(Map<String, String> entityForms) {
			this.entityForms = entityForms;
		}

		public Map<String, Long> getFormContexts() {
			return formContexts;
		}

		public void setFormContexts(Map<String, Long> formContexts) {
			this.formContexts = formContexts;
		}

		public boolean hasFormName(String entityType) {
			return entityForms.containsKey(entityType);
		}

		public String getFormName(String entityType) {
			return entityForms.get(entityType);
		}

		public void addFormName(String entityType, String formName) {
			entityForms.put(entityType, formName);
		}

		public void removeFormName(String entityType) {
			entityForms.remove(entityType);
		}

		public void removeForm(String formName) {
			List<String> entities = new ArrayList<>();
			Iterator<Map.Entry<String, String>> iter = entityForms.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, String> entityFormName = iter.next();
				if (entityFormName.getValue() != null && entityFormName.getValue().equals(formName)) {
					entities.add(entityFormName.getKey() + "#" + formName);
					iter.remove();
				}
			}

			for (String entity : entities) {
				formContexts.remove(entity);
			}
		}

		public boolean hasFormContext(String entityType, String formName) {
			return formContexts.containsKey(getFormCtxtKey(entityType, formName));
		}

		public Long getFormContext(String entityType, String formName) {
			return formContexts.get(getFormCtxtKey(entityType, formName));
		}

		public void addFormContext(String entityType, String formName, Long ctxtId) {
			formContexts.put(getFormCtxtKey(entityType, formName), ctxtId);
		}

		private String getFormCtxtKey(String entityType, String formName) {
			return entityType + "#" + formName;
		}

		private void removeFormContext(Long formCtxtId) {
			Iterator<Map.Entry<String, Long>> iter = formContexts.entrySet().iterator();
			while (iter.hasNext()) {
				Long cachedId = iter.next().getValue();
				if (cachedId == null || cachedId.equals(formCtxtId)) {
					iter.remove();
				}
			}
		}
	}
}

