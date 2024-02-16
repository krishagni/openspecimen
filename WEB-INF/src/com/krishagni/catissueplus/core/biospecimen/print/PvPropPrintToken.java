package com.krishagni.catissueplus.core.biospecimen.print;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.NullValueInNestedPathException;
import org.springframework.beans.PropertyAccessorFactory;

import com.krishagni.catissueplus.core.administrative.domain.DistributionOrderItem;
import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.domain.AbstractLabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.de.domain.DeObject;
import com.krishagni.catissueplus.core.de.ui.PvControl;

public class PvPropPrintToken extends AbstractLabelTmplToken implements LabelTmplToken {

	private String defaultLevel;

	private DaoFactory daoFactory;

	public void setDefaultLevel(String defaultLevel) {
		this.defaultLevel = defaultLevel;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public String getName() {
		return "pv_prop";
	}

	@Override
	public String getReplacement(Object object) {
		return null;
	}

	public String getReplacement(Object object, String ... args) {
		if (args == null || args.length < 2) {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "pv_prop(name, [level], field_name, prop_name, [custom_field])");
		}

		String level = defaultLevel;
		String fieldName = null;
		String propName  = null;
		boolean customField = false;
		if (args.length >= 4) {
			level = args[0].trim();
			fieldName = args[1].trim();
			propName = args[2].trim();
			customField = Boolean.parseBoolean(args[3].trim());
		} else if (args.length == 3) {
			String arg3 = args[2].trim();
			if (arg3.equalsIgnoreCase("true") || arg3.equalsIgnoreCase("false")) {
				customField = Boolean.parseBoolean(arg3);
				fieldName = args[0].trim();
				propName = args[1].trim();
			} else {
				level = args[0].trim();
				fieldName = args[1].trim();
				propName = args[2].trim();
			}
		} else {
			fieldName = args[0].trim();
			propName = args[1].trim();
		}

		object = getObject(object, level);
		if (object == null) {
			return StringUtils.EMPTY;
		}

		BeanWrapper objectWrapper = PropertyAccessorFactory.forBeanPropertyAccess(object);
		PermissibleValue pv = null;
		if (customField) {
			DeObject extn = (DeObject) objectWrapper.getPropertyValue("extension");
			if (extn != null && extn.getAttrs() != null) {
				for (DeObject.Attr attr : extn.getAttrs()) {
					if (attr.getUdn().equals(fieldName) && attr.getCtrlValue().getControl() instanceof PvControl pvCtrl) {
						if (attr.getValue() != null) {
							String valueStr = attr.getValue().toString();
							if (StringUtils.isNumeric(valueStr)) {
								Long pvId = Long.parseLong(valueStr);
								pv = daoFactory.getPermissibleValueDao().getById(pvId);
							} else {
								pv = daoFactory.getPermissibleValueDao().getByValue(pvCtrl.getAttribute(), valueStr);
							}
						}

						break;
					}
				}
			}
		} else {
			try {
				pv = (PermissibleValue) objectWrapper.getPropertyValue(fieldName);
			} catch (NullValueInNestedPathException ignored) {

			}
		}

		if (pv == null) {
			return StringUtils.EMPTY;
		}

		String result = StringUtils.EMPTY;
		if ("concept_code".equals(propName)) {
			result = pv.getConceptCode();
		} else if ("value".equals(propName)) {
			result = pv.getValue();
		}

		if (StringUtils.isBlank(result)) {
			result = pv.getProps().get(propName);
		}

		return StringUtils.isBlank(result) ? StringUtils.EMPTY : result;
	}

	protected Object getObject(Object object, String level) {
		if (object instanceof StorageContainer) {
			return object;
		} else if (object instanceof Visit visit) {
			switch (level) {
				case "visit":
					return visit;
				case "cpr":
					return visit.getRegistration();
				case "cp":
					return visit.getCollectionProtocol();
			}
		} else if (object instanceof Specimen specimen) {
			switch (level) {
				case "specimen":
					return specimen;
				case "parentSpecimen":
					return specimen.getParentSpecimen();
				case "visit":
					return specimen.getVisit();
				case "cpr":
					return specimen.getRegistration();
				case "cp":
					return specimen.getCollectionProtocol();
			}
		} else if (object instanceof DistributionOrderItem orderItem) {
			switch (level) {
				case "orderItem":
					return orderItem;
				case "order":
					return orderItem.getOrder();
				case "specimen":
					return orderItem.getSpecimen();
				case "parentSpecimen":
					return orderItem.getSpecimen().getParentSpecimen();
				case "visit":
					return orderItem.getSpecimen().getVisit();
				case "cpr":
					return orderItem.getSpecimen().getRegistration();
				case "cp":
					return orderItem.getSpecimen().getCollectionProtocol();
			}
		}

		return null;
	}
}
