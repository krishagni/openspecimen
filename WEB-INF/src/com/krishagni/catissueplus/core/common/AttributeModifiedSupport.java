package com.krishagni.catissueplus.core.common;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AttributeModifiedSupport implements Serializable {
	private static final long serialVersionUID = -6538288756745006122L;

	private Set<String> modifiedAttrs = new HashSet<>();

	private String opComments;
	
	public void attrModified(String attr) {
		modifiedAttrs.add(attr);
	}
	
	public boolean isAttrModified(String attr) {
		return modifiedAttrs.contains(attr);
	}

	public Set<String> modifiedAttrs() {
		return Collections.unmodifiableSet(modifiedAttrs);
	}

	public int modifiedAttrsCount() {
		return modifiedAttrs.size();
	}

	public boolean areTheOnlyModifiedAttrs(String ...attrs) {
		if (attrs == null) {
			return modifiedAttrs.isEmpty();
		}

		int modified = 0;
		for (String attr : attrs) {
			modified += (isAttrModified(attr) ? 1 : 0);
		}

		return modified == modifiedAttrs.size();
	}

	public String getOpComments() {
		return opComments;
	}

	public void setOpComments(String opComments) {
		this.opComments = opComments;
	}
}
