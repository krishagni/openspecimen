
package com.krishagni.catissueplus.core.common.service.impl;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.exception.ConstraintViolationException;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.PvAttribute;
import com.krishagni.catissueplus.core.administrative.domain.factory.PvErrorCode;
import com.krishagni.catissueplus.core.administrative.events.ListPvAttributesCriteria;
import com.krishagni.catissueplus.core.administrative.events.ListPvCriteria;
import com.krishagni.catissueplus.core.administrative.events.PermissibleValueDetails;
import com.krishagni.catissueplus.core.administrative.events.PvAttributeDetail;
import com.krishagni.catissueplus.core.administrative.events.PvDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;
import com.krishagni.catissueplus.core.common.service.PermissibleValueService;
import com.krishagni.catissueplus.core.common.util.SessionUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.events.UpdateFormPvAttributesOp;
import com.krishagni.catissueplus.core.de.services.FormService;

import edu.common.dynamicextensions.domain.nui.Container;

public class PermissibleValueServiceImpl implements PermissibleValueService {
	private DaoFactory daoFactory;

	private ConfigurationService cfgSvc;

	private FormService formSvc;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setCfgSvc(ConfigurationService cfgSvc) {
		this.cfgSvc = cfgSvc;
	}

	public void setFormSvc(FormService formSvc) {
		this.formSvc = formSvc;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<PvAttributeDetail>> getAttributes(RequestEvent<ListPvAttributesCriteria> req) {
		try {
			List<PvAttribute> attrs = daoFactory.getPermissibleValueDao().getAttributes(req.getPayload());

			List<String> attrNames = attrs.stream().map(PvAttribute::getPublicId).toList();
			Map<String, Long> pvCounts = daoFactory.getPermissibleValueDao().getPvCounts(attrNames);

			List<PvAttributeDetail> result = PvAttributeDetail.from(attrs);
			result.forEach(attr -> {
				Long pvCount = pvCounts.getOrDefault(attr.getAttribute().toLowerCase(Locale.ROOT), 0L);
				attr.setPvCount(pvCount);
			});
			return ResponseEvent.response(result);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Long> getAttributesCount(RequestEvent<ListPvAttributesCriteria> req) {
		try {
			return ResponseEvent.response(daoFactory.getPermissibleValueDao().getAttributesCount(req.getPayload()));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<PvAttributeDetail> createAttribute(RequestEvent<PvAttributeDetail> req) {
		try {
			PvAttributeDetail input = req.getPayload();
			if (input.getFormId() == null) {
				AccessCtrlMgr.getInstance().ensureUserIsAdmin();
				if (StringUtils.isBlank(input.getAttribute())) {
					throw OpenSpecimenException.userError(PvErrorCode.ATTR_NAME_REQUIRED);
				}
			} else {
				AccessCtrlMgr.getInstance().ensureFormUpdateRights();
			}

			if (StringUtils.isBlank(input.getName())) {
				throw OpenSpecimenException.userError(PvErrorCode.ATTR_CAPTION_REQUIRED);
			}

			String attributeName = input.getAttribute();
			if (StringUtils.isNotBlank(attributeName) && daoFactory.getPermissibleValueDao().getAttribute(attributeName) != null) {
				throw OpenSpecimenException.userError(PvErrorCode.DUP_ATTR, attributeName);
			}

			if (input.getFormId() != null) {
				Container form = Container.getContainer(input.getFormId());
				if (form == null) {
					throw OpenSpecimenException.userError(PvErrorCode.NOT_FOUND, input.getFormId());
				}

				if (StringUtils.isBlank(attributeName)) {
					String baseName = normalizeAttributeName(form.getName() + "_" + input.getName());
					attributeName = getUniqueAttributeName(baseName);
				}
			}

			PvAttribute attribute = new PvAttribute();
			attribute.setPublicId(attributeName);
			attribute.setLongName(input.getName());
			attribute.setDefinition(StringUtils.defaultIfBlank(input.getDefinition(), input.getName()));
			attribute.setVersion((String) cfgSvc.getAppProps().get("build_version"));
			attribute.setLastUpdated(Calendar.getInstance().getTime());
			attribute.setFormId(input.getFormId());
			daoFactory.getPermissibleValueDao().saveAttribute(attribute);

			final String pvAttributeName = attributeName;
			List<PermissibleValueDetails> pvs = new ArrayList<>();
			Utility.nullSafeStream(input.getPvs())
				.filter(pv -> StringUtils.isNotBlank(pv.getValue()))
				.sorted((pv1, pv2) -> pv1.getValue().compareToIgnoreCase(pv2.getValue()))
				.forEach(pv -> {
					pv.setAttribute(pvAttributeName);
					pvs.add(createPv(pv));
				});

			PvAttributeDetail result = PvAttributeDetail.from(attribute);
			result.setPvs(pvs);
			return ResponseEvent.response(result);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<PvAttributeDetail> promoteAttribute(RequestEvent<PvAttributeDetail> req) {
		ResponseEvent<List<PvAttributeDetail>> resp = promoteAttributes(RequestEvent.wrap(List.of(req.getPayload())));
		if (resp.isSuccessful() && resp.getPayload() != null) {
			return ResponseEvent.response(resp.getPayload().get(0));
		}

		return ResponseEvent.error(resp.getError());
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<PvAttributeDetail>> promoteAttributes(RequestEvent<List<PvAttributeDetail>> req) {
		try {
			AccessCtrlMgr.getInstance().ensureUserIsAdmin();
			List<PvAttributeDetail> inputs = req.getPayload();
			if (inputs == null || inputs.isEmpty()) {
				return ResponseEvent.response(Collections.emptyList());
			}

			Map<Long, Map<String, String>> formAttributes = new LinkedHashMap<>();
			Set<String> sourceAttributes = new HashSet<>();
			Set<String> targetAttributes = new HashSet<>();
			for (PvAttributeDetail input : inputs) {
				validatePromotion(input, sourceAttributes, targetAttributes);
				PvAttribute source = daoFactory.getPermissibleValueDao().getAttribute(input.getSourceAttribute());
				formAttributes.computeIfAbsent(source.getFormId(), key -> new LinkedHashMap<>())
					.put(source.getPublicId(), input.getAttribute());
			}

			List<PvAttributeDetail> result = new ArrayList<>();
			for (PvAttributeDetail input : inputs) {
				PvAttribute promoted = daoFactory.getPermissibleValueDao()
					.promoteAttribute(input.getSourceAttribute(), input.getAttribute(), input.getName());
				result.add(PvAttributeDetail.from(promoted));
			}

			for (Map.Entry<Long, Map<String, String>> entry : formAttributes.entrySet()) {
				UpdateFormPvAttributesOp op = new UpdateFormPvAttributesOp();
				op.setFormId(entry.getKey());
				op.setAttributes(entry.getValue());
				ResponseEvent.unwrap(formSvc.updatePvAttributes(RequestEvent.wrap(op)));
			}

			return ResponseEvent.response(result);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<PvDetail>> getPermissibleValues(RequestEvent<ListPvCriteria> req) {
		ListPvCriteria crit = req.getPayload();
		List<PermissibleValue> pvs = daoFactory.getPermissibleValueDao().getPvs(crit);
		return ResponseEvent.response(PvDetail.from(pvs, crit.includeParentValue(), crit.includeProps()));
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Long> getPermissibleValuesCount(RequestEvent<ListPvCriteria> req) {
		return ResponseEvent.response(daoFactory.getPermissibleValueDao().getPvsCount(req.getPayload()));
	}

	@Override
	@PlusTransactional
	public ResponseEvent<PvDetail> getPermissibleValue(RequestEvent<EntityQueryCriteria> req) {
		EntityQueryCriteria crit = req.getPayload();
		PermissibleValue value = null;
		Object key = null;
		if (crit.getId() != null) {
			value = daoFactory.getPermissibleValueDao().getById(crit.getId());
			key = crit.getId();
		} else if (StringUtils.isNotBlank(crit.getName()) && StringUtils.isNotBlank(crit.paramString("attribute"))) {
			value = daoFactory.getPermissibleValueDao().getByValue(crit.paramString("attribute"), crit.getName());
			key = crit.paramString("attribute") + ": " + crit.getName();
		}

		if (key == null) {
			throw OpenSpecimenException.userError(PvErrorCode.VALUE_REQUIRED);
		} else if (value == null) {
			throw OpenSpecimenException.userError(PvErrorCode.NOT_FOUND, key);
		}

		boolean includeProps = Boolean.TRUE.equals(crit.paramBoolean("includeProps"));
		return ResponseEvent.response(PvDetail.from(value, includeProps, includeProps));
	}

	@Override
	@PlusTransactional
	public ResponseEvent<PermissibleValueDetails> createPermissibleValue(RequestEvent<PermissibleValueDetails> req) {
		try {
			PermissibleValueDetails input = req.getPayload();
			ensureAttrEditRights(input.getAttribute());
			return ResponseEvent.response(createPv(input));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<PermissibleValueDetails> updatePermissibleValue(RequestEvent<PermissibleValueDetails> req) {
		try {
			PermissibleValueDetails input = req.getPayload();
			if (StringUtils.isBlank(input.getValue())) {
				throw OpenSpecimenException.userError(PvErrorCode.VALUE_REQUIRED);
			}

			PermissibleValue existing = daoFactory.getPermissibleValueDao().getById(input.getId());
			ensurePvEditRights(existing);

			PermissibleValue duplicate = daoFactory.getPermissibleValueDao().getByValue(existing.getAttribute(), input.getValue());
			if (duplicate != null && !duplicate.equals(existing)) {
				throw OpenSpecimenException.userError(PvErrorCode.DUP_VALUE, input.getValue(), existing.getAttribute());
			}

			existing.setValue(input.getValue());
			existing.setConceptCode(input.getConceptCode());
			existing.setProps(input.getProps());
			existing.setActivityStatus(StringUtils.defaultIfBlank(input.getActivityStatus(), existing.getActivityStatus()));
			daoFactory.getPermissibleValueDao().saveOrUpdate(existing);
			return ResponseEvent.response(PermissibleValueDetails.fromDomain(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<PermissibleValueDetails> deletePermissibleValue(RequestEvent<Long> req) {
		try {
			PermissibleValue existing = daoFactory.getPermissibleValueDao().getById(req.getPayload());
			ensurePvEditRights(existing);

			PermissibleValueDetails result = PermissibleValueDetails.fromDomain(existing);
			try {
				existing.getProps().clear();
				daoFactory.getPermissibleValueDao().delete(existing);
				SessionUtil.getInstance().flush();
			} catch (Throwable t) {
				Throwable cause = t;
				while (cause != null && !(cause instanceof ConstraintViolationException)) {
					cause = cause.getCause();
				}

				if (cause != null) {
					throw OpenSpecimenException.userError(PvErrorCode.IN_USE, existing.getValue(), cause.getMessage());
				}

				throw t;
			}

			return ResponseEvent.response(result);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Throwable t) {
			return ResponseEvent.serverError(t instanceof Exception ? (Exception) t : new RuntimeException(t));
		}
	}

	private PermissibleValueDetails createPv(PermissibleValueDetails input) {
		if (StringUtils.isBlank(input.getAttribute())) {
			throw OpenSpecimenException.userError(PvErrorCode.ATTR_NAME_REQUIRED);
		} else if (StringUtils.isBlank(input.getValue())) {
			throw OpenSpecimenException.userError(PvErrorCode.VALUE_REQUIRED);
		} else if (daoFactory.getPermissibleValueDao().getByValue(input.getAttribute(), input.getValue()) != null) {
			throw OpenSpecimenException.userError(PvErrorCode.DUP_VALUE, input.getValue(), input.getAttribute());
		}

		PermissibleValue pv = new PermissibleValue();
		pv.setAttribute(input.getAttribute());
		pv.setValue(input.getValue());
		pv.setConceptCode(input.getConceptCode());
		pv.setActivityStatus(StringUtils.defaultIfBlank(input.getActivityStatus(), "Active"));
		daoFactory.getPermissibleValueDao().saveOrUpdate(pv, true);
		return PermissibleValueDetails.fromDomain(pv);
	}

	private void ensurePvEditRights(PermissibleValue pv) {
		if (pv == null) {
			throw OpenSpecimenException.userError(PvErrorCode.NOT_FOUND, "null");
		}

		ensureAttrEditRights(pv.getAttribute());
	}

	private void ensureAttrEditRights(String attrName) {
		PvAttribute attribute = daoFactory.getPermissibleValueDao().getAttribute(attrName);
		if (attribute == null || !attribute.isFormScoped()) {
			AccessCtrlMgr.getInstance().ensureUserIsAdmin();
		} else {
			AccessCtrlMgr.getInstance().ensureFormUpdateRights();
		}
	}

	private String normalizeAttributeName(String input) {
		String result = Normalizer.normalize(input, Normalizer.Form.NFD)
			.replaceAll("\\p{M}", "")
			.replaceAll("[^A-Za-z0-9]+", "_")
			.replaceAll("^_+|_+$", "")
			.toLowerCase(Locale.ROOT);
		return StringUtils.defaultIfBlank(result, "form_dropdown");
	}

	private String getUniqueAttributeName(String baseName) {
		String result = baseName;
		int suffix = 2;
		while (daoFactory.getPermissibleValueDao().getAttribute(result) != null) {
			result = baseName + "_" + suffix++;
		}

		return result;
	}

	private void validatePromotion(PvAttributeDetail input, Set<String> sourceAttributes, Set<String> targetAttributes) {
		if (input == null || StringUtils.isBlank(input.getSourceAttribute())) {
			throw OpenSpecimenException.userError(PvErrorCode.SOURCE_ATTR_REQUIRED);
		}

		PvAttribute attribute = daoFactory.getPermissibleValueDao().getAttribute(input.getSourceAttribute());
		if (attribute == null) {
			throw OpenSpecimenException.userError(PvErrorCode.NOT_FOUND, input.getSourceAttribute());
		} else if (!attribute.isFormScoped()) {
			throw OpenSpecimenException.userError(PvErrorCode.FORM_ATTR_REQUIRED, input.getSourceAttribute());
		}

		if (StringUtils.isBlank(input.getAttribute())) {
			throw OpenSpecimenException.userError(PvErrorCode.ATTR_NAME_REQUIRED);
		} else if (StringUtils.isBlank(input.getName())) {
			throw OpenSpecimenException.userError(PvErrorCode.ATTR_CAPTION_REQUIRED);
		} else if (!sourceAttributes.add(input.getSourceAttribute().toLowerCase(Locale.ROOT))) {
			throw OpenSpecimenException.userError(PvErrorCode.DUP_ATTR, input.getSourceAttribute());
		} else if (!targetAttributes.add(input.getAttribute().toLowerCase(Locale.ROOT))) {
			throw OpenSpecimenException.userError(PvErrorCode.DUP_ATTR, input.getAttribute());
		}

		if (daoFactory.getPermissibleValueDao().getAttribute(input.getAttribute()) != null) {
			throw OpenSpecimenException.userError(PvErrorCode.DUP_ATTR, input.getAttribute());
		}
	}
}
