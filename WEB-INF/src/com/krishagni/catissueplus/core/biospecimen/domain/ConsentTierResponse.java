package com.krishagni.catissueplus.core.biospecimen.domain;

import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.springframework.beans.BeanUtils;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;

@Audited
@AuditTable(value="CAT_CONSENT_TIER_RESPONSE_AUD")
public class ConsentTierResponse extends BaseEntity {

	private static final String ENTITY_NAME = "consent_response";

	private PermissibleValue response;

	private CpConsentTier consentTier;

	private CollectionProtocolRegistration cpr;
	
	public static String getEntityName() {
		return ENTITY_NAME;
	}

	public PermissibleValue getResponse() {
		return response;
	}

	public void setResponse(PermissibleValue response) {
		this.response = response;
	}

	public CpConsentTier getConsentTier() {
		return consentTier;
	}

	public void setConsentTier(CpConsentTier consentTier) {
		this.consentTier = consentTier;
	}

	public CollectionProtocolRegistration getCpr() {
		return cpr;
	}

	public void setCpr(CollectionProtocolRegistration cpr) {
		this.cpr = cpr;
	}

	public String getStatement() {
		return getConsentTier().getStatement().getStatement();
	}

	public String getStatementCode() {
		return getConsentTier().getStatement().getCode();
	}

	public ConsentTierResponse copy() {
		ConsentTierResponse copy = new ConsentTierResponse();
		BeanUtils.copyProperties(this, copy);
		return copy;
	}
}