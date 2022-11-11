package com.krishagni.catissueplus.core.common.domain;

import org.apache.commons.lang3.StringUtils;

public abstract class AbstractUniqueIdToken<T> extends AbstractLabelTmplToken {
	protected String name;

	protected String type;

	@Override
	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTypeKey() {
		if (StringUtils.isBlank(type)) {
			return getName();
		}

		return getType() + "_" + getName();
	}

	@Override
	public boolean areArgsValid(String ... args) {
		if (args == null || args.length == 0) {
			// no args
			return true;
		} else if (StringUtils.isBlank(args[0])) {
			// empty arg
			return true;
		} else {
			// one non-empty arg, it has to be a integer
			try {
				return Integer.parseInt(args[0]) >= 0;
			} catch (NumberFormatException e) {
				return false;
			}
		}
	}

	@Override
	public String getReplacement(Object object) {
		return null;
	}

	@Override
	public String getReplacement(Object object, String ... args) {
		Number uniqueId = getUniqueId((T) object, args);
		if (uniqueId == null) {
			return StringUtils.EMPTY;
		} else if (uniqueId.longValue() < 0L) {
			return LabelTmplToken.EMPTY_VALUE;
		}

		return super.formatNumber(uniqueId, args);
	}

	@Override
	public int validate(Object object, String input, int startIdx, String ... args) {
		return super.validateNumber(input, startIdx, args);
	}

	public abstract Number getUniqueId(T object, String ... args);

	protected String getArg(int idx, String... args) {
		return args != null && args.length > idx ? StringUtils.trim(args[idx]) : null;
	}

	protected boolean eqArg(String value, int idx, String... args) {
		return StringUtils.equals(value, getArg(idx, args));
	}
}
