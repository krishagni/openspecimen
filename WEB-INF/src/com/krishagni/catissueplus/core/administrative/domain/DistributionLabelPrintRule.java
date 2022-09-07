package com.krishagni.catissueplus.core.administrative.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;
import com.krishagni.catissueplus.core.common.util.Utility;

public class DistributionLabelPrintRule extends LabelPrintRule {
	private List<DistributionProtocol> dps = new ArrayList<>();

	public List<DistributionProtocol> getDps() {
		return dps;
	}

	public void setDps(List<DistributionProtocol> dps) {
		this.dps = dps;
	}

	public boolean isApplicableFor(DistributionOrderItem item, User user, String ipAddr) {
		if (!super.isApplicableFor(user, ipAddr)) {
			return false;
		}

		return CollectionUtils.isEmpty(dps) || dps.stream().anyMatch(dp -> item.getOrder().getDistributionProtocol().equals(dp));
	}

	@Override
	protected Map<String, Object> getDefMap(boolean ufn) {
		Map<String, Object> ruleDef = new HashMap<>();
		ruleDef.put("dps", getDps(ufn));
		return ruleDef;
	}

	public String toString() {
		return super.toString() + ", dp = " + getDps(true);
	}

	private List<String> getDps(boolean ufn) {
		Function<DistributionProtocol, String> dpMapper = ufn ? DistributionProtocol::getShortTitle : (dp) -> dp.getId().toString();
		return Utility.nullSafeStream(dps).map(dpMapper).collect(Collectors.toList());
	}
}
