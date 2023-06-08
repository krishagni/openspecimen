package com.krishagni.catissueplus.core.biospecimen.label.specimen;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.domain.AbstractLabelTmplToken;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.de.domain.DeObject;
import com.krishagni.catissueplus.core.de.ui.PvControl;

//
// %PV_PROP(field_name, prop_name, [custom_field])%
//
public class PvPropLabelToken extends AbstractLabelTmplToken {

	private DaoFactory daoFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public String getName() {
		return "PV_PROP";
	}

	@Override
	public String getReplacement(Object object) {
		return null;
	}

	@Override
	public String getReplacement(Object object, String ... args) {
		if (args == null || args.length < 2) {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "%PV_PROP(field_name, prop_name, [custom_field]");
		}

		String fieldName = args[0].trim();
		String propName = args[1].trim();
		boolean customField = false;
		if (args.length == 3) {
			customField = Boolean.parseBoolean(args[2].trim());
		}

		BeanWrapper objectWrapper = PropertyAccessorFactory.forBeanPropertyAccess(object);
		PermissibleValue pv = null;
		if (customField) {
			DeObject extn = (DeObject)objectWrapper.getPropertyValue("extension");
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
			pv = (PermissibleValue) objectWrapper.getPropertyValue(fieldName);
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
}
