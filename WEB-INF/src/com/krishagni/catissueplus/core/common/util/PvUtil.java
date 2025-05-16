package com.krishagni.catissueplus.core.common.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.TransactionCache;

@Configurable
public class PvUtil {
	private static PvUtil instance = null;

	@Autowired
	private DaoFactory daoFactory;

	public static PvUtil getInstance() {
		if (instance == null || instance.daoFactory == null) {
			instance = new PvUtil();
		}

		return instance;
	}

	public String getAbbr(String attr, String value) {
		return getAbbr(attr, value, null);
	}

	public String getAbbr(String attr, String value, String defVal) {
		PermissibleValue pv = daoFactory.getPermissibleValueDao().getByValue(attr, value);
		return getAbbr(pv, defVal);
	}

	public String getAbbr(PermissibleValue pv, String defVal) {
		if (pv == null) {
			return defVal;
		}

		String abbr = pv.getProps().get(ABBREVIATION);
		if (StringUtils.isBlank(abbr)) {
			abbr = StringUtils.EMPTY;
		}

		return abbr;
	}

	public String getSpecimenUnit(String measure, String spmnClass, String type) {
		String key = spmnClass + ":" + type;
		PermissibleValue pv = getTypeUnits().get(key);
		if (pv == null) {
			List<PermissibleValue> pvs = daoFactory.getPermissibleValueDao()
				.getPvs("specimen_type", spmnClass, Collections.singleton(type), true);
			if (!pvs.isEmpty()) {
				pv = pvs.get(0);
				getTypeUnits().put(key, pv);
			}
		}

		if (pv == null) {
			return "";
		}

		String result = "";
		if ("quantity".equals(measure)) {
			result = pv.getProps().get("quantity_display_unit");
			if (StringUtils.isBlank(result)) {
				result = pv.getParent().getProps().get("quantity_display_unit");
			}

			if (StringUtils.isBlank(result)) {
				result = pv.getProps().get("quantity_unit");
			}

			if (StringUtils.isBlank(result)) {
				result = pv.getParent().getProps().get("quantity_unit");
			}
		} else if ("concentration".equals(measure)) {
			result = pv.getProps().get("concentration_display_unit");
			if (StringUtils.isBlank(result)) {
				result = pv.getParent().getProps().get("concentration_display_unit");
			}

			if (StringUtils.isBlank(result)) {
				result = pv.getProps().get("concentration_unit");
			}

			if (StringUtils.isBlank(result)) {
				result = pv.getParent().getProps().get("concentration_unit");
			}
		} else if (StringUtils.isNotBlank(measure)) {
			result = pv.getProps().get(measure);
			if (StringUtils.isBlank(result)) {
				result = pv.getParent().getProps().get(measure);
			}
		}

		return result;
	}

	private Map<String, PermissibleValue> getTypeUnits() {
		return TransactionCache.getInstance().get("pvTypeUnits", new HashMap<>());
	}

	private static final String ABBREVIATION = "abbreviation";
}