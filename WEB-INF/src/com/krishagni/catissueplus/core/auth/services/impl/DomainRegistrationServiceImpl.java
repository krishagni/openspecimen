
package com.krishagni.catissueplus.core.auth.services.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.auth.domain.AuthDomain;
import com.krishagni.catissueplus.core.auth.domain.AuthDomainSavedEvent;
import com.krishagni.catissueplus.core.auth.domain.factory.AuthProviderErrorCode;
import com.krishagni.catissueplus.core.auth.domain.factory.DomainRegistrationFactory;
import com.krishagni.catissueplus.core.auth.events.AuthDomainDetail;
import com.krishagni.catissueplus.core.auth.events.AuthDomainSummary;
import com.krishagni.catissueplus.core.auth.events.ListAuthDomainCriteria;
import com.krishagni.catissueplus.core.auth.services.DomainRegistrationService;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.OpenSpecimenAppCtxProvider;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

public class DomainRegistrationServiceImpl implements DomainRegistrationService {

	private DaoFactory daoFactory;

	private DomainRegistrationFactory domainRegFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setDomainRegFactory(DomainRegistrationFactory domainRegFactory) {
		this.domainRegFactory = domainRegFactory;
	}

	@Override
	@PlusTransactional	
	public ResponseEvent<List<AuthDomainSummary>> getDomains(RequestEvent<ListAuthDomainCriteria> req) {
		List<AuthDomain> authDomains = daoFactory.getAuthDao().getAuthDomains(req.getPayload().maxResults());
		return ResponseEvent.response(AuthDomainSummary.from(authDomains));
	}

	@Override
	@PlusTransactional
	public ResponseEvent<AuthDomainDetail> getDomain(RequestEvent<AuthDomainSummary> req) {
		try {
			AccessCtrlMgr.getInstance().ensureUserIsAdmin();

			AuthDomain domain = getDomain(req.getPayload().getId(), req.getPayload().getName());
			return ResponseEvent.response(AuthDomainDetail.from(domain));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<AuthDomainDetail> registerDomain(RequestEvent<AuthDomainDetail> req) {
		try {
			AccessCtrlMgr.getInstance().ensureUserIsAdmin();

			AuthDomain authDomain = domainRegFactory.createDomain(req.getPayload());
			ensureUniqueDomainName(null, authDomain);
			daoFactory.getAuthDao().saveOrUpdate(authDomain);

			OpenSpecimenAppCtxProvider.getAppCtx().publishEvent(new AuthDomainSavedEvent(authDomain));
			return ResponseEvent.response(AuthDomainDetail.from(authDomain));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<AuthDomainDetail> updateDomain(RequestEvent<AuthDomainDetail> req) {
		try {
			AccessCtrlMgr.getInstance().ensureUserIsAdmin();

			AuthDomainDetail detail = req.getPayload();
			AuthDomain existing = getDomain(detail.getId(), detail.getName());

			AuthDomain authDomain = domainRegFactory.createDomain(detail);
			ensureUniqueDomainName(existing, authDomain);
			existing.update(authDomain);

			OpenSpecimenAppCtxProvider.getAppCtx().publishEvent(new AuthDomainSavedEvent(existing));
			return  ResponseEvent.response(AuthDomainDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<AuthDomainDetail> deleteDomain(RequestEvent<AuthDomainSummary> req) {
		try {
			AccessCtrlMgr.getInstance().ensureUserIsAdmin();

			AuthDomainSummary input = req.getPayload();
			AuthDomain existing = getDomain(input.getId(), input.getName());
			existing.delete();

			OpenSpecimenAppCtxProvider.getAppCtx().publishEvent(new AuthDomainSavedEvent(existing));
			return ResponseEvent.response(AuthDomainDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	private void ensureUniqueDomainName(AuthDomain existing, AuthDomain newDomain) {
		if (existing != null && existing.getName().equals(newDomain.getName())) {
			return;
		}

		if (!daoFactory.getAuthDao().isUniqueAuthDomainName(newDomain.getName())) {
			throw OpenSpecimenException.userError(AuthProviderErrorCode.DUP_DOMAIN_NAME);
		}
	}

	private AuthDomain getDomain(Long id, String name) {
		Object key = null;
		AuthDomain existing = null;
		if (id != null) {
			key = id;
			existing = daoFactory.getAuthDao().getById(id);
		} else if (StringUtils.isNotBlank(name)) {
			key = name;
			existing = daoFactory.getAuthDao().getAuthDomainByName(name);
		}

		if (existing == null) {
			throw OpenSpecimenException.userError(AuthProviderErrorCode.DOMAIN_NOT_FOUND, key);
		}

		return existing;
	}
}
