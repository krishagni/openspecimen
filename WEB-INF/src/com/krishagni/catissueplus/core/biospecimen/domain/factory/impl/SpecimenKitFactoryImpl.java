package com.krishagni.catissueplus.core.biospecimen.domain.factory.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.factory.SiteErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenKit;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CprErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenKitFactory;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenInfo;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenKitDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListCriteria;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.Status;

public class SpecimenKitFactoryImpl implements SpecimenKitFactory {

    private DaoFactory daoFactory;

    public void setDaoFactory(DaoFactory daoFactory) {
        this.daoFactory = daoFactory;
    }

    @Override
    public SpecimenKit createSpecimenKit(SpecimenKitDetail detail) {
        OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
        return createSpecimenKit(detail, getSpecimens(detail, ose), ose);
    }

    public SpecimenKit createSpecimenKit(SpecimenKitDetail detail, List<Specimen> specimens) {
        return createSpecimenKit(detail, specimens, null);
    }

    private SpecimenKit createSpecimenKit(SpecimenKitDetail detail, List<Specimen> specimens, OpenSpecimenException ose) {
        if (ose == null) {
            ose = new OpenSpecimenException(ErrorType.USER_ERROR);
        }

        SpecimenKit kit = new SpecimenKit();
        kit.setSpecimens(new HashSet<>(specimens));

        setCollectionProtocol(detail, kit, ose);
        setSendingSite(detail, kit, ose);
        setReceivingSite(detail, kit, ose);
        setSendingDate(detail, kit, ose);
        setSender(detail, kit, ose);
        setComments(detail, kit, ose);
        setActivityStatus(detail, kit, ose);

        ose.checkAndThrow();
        return kit;
    }

    private List<Specimen> getSpecimens(SpecimenKitDetail detail, OpenSpecimenException ose) {
        List<Long> ids = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(detail.getSpecimens())) {
            ids = detail.getSpecimens().stream().map(SpecimenInfo::getId).collect(Collectors.toList());
        }

        if (CollectionUtils.isEmpty(ids)) {
            ose.addError(SpecimenKitErrorCode.SPECIMENS_REQUIRED);
            return null;
        }

        SpecimenListCriteria crit = new SpecimenListCriteria().ids(ids);
        List<Specimen> specimens = daoFactory.getSpecimenDao().getSpecimens(crit);
        if (specimens.size() != ids.size()) {
            ose.addError(SpecimenKitErrorCode.INVALID_SPECIMENS);
        }

        return specimens;
    }

    private void setCollectionProtocol(SpecimenKitDetail detail, SpecimenKit kit, OpenSpecimenException ose) {
        Long cpId = detail.getCpId();
        String title = detail.getCpTitle();
        String shortTitle = detail.getCpShortTitle();

        CollectionProtocol cp = null;
        if (cpId != null) {
            cp = daoFactory.getCollectionProtocolDao().getById(detail.getCpId());
        } else if (StringUtils.isNotBlank(title)) {
            cp = daoFactory.getCollectionProtocolDao().getCollectionProtocol(title);
        } else if (StringUtils.isNotBlank(shortTitle)) {
            cp = daoFactory.getCollectionProtocolDao().getCpByShortTitle(shortTitle);
        } else {
            ose.addError(CprErrorCode.CP_REQUIRED);
            return;
        }

        if (cp == null) {
            ose.addError(CpErrorCode.NOT_FOUND);
            return;
        }

        kit.setCollectionProtocol(cp);
    }

    private void setSendingSite(SpecimenKitDetail detail, SpecimenKit kit, OpenSpecimenException ose) {
        String sendingSite = detail.getSendingSite();
        Site site = getSite(kit, sendingSite, SpecimenKitErrorCode.SEND_SITE_REQUIRED, SpecimenKitErrorCode.INVALID_SEND_SITE,  ose);
        kit.setSendingSite(site);
    }

    private void setReceivingSite(SpecimenKitDetail detail, SpecimenKit kit, OpenSpecimenException ose) {
        String receivingSite = detail.getReceivingSite();
        Site site = getSite(kit, receivingSite, SpecimenKitErrorCode.RECV_SITE_REQUIRED, SpecimenKitErrorCode.INVALID_RECV_SITE, ose);
        kit.setReceivingSite(site);
    }

    private Site getSite(SpecimenKit kit, String siteName, ErrorCode reqError, ErrorCode invError, OpenSpecimenException ose) {
        if (StringUtils.isBlank(siteName)) {
            ose.addError(reqError);
            return null;
        }

        Site site = daoFactory.getSiteDao().getSiteByName(siteName);
        if (site == null) {
            ose.addError(SiteErrorCode.NOT_FOUND);
            return null;
        }

        CollectionProtocol cp = kit.getCollectionProtocol();
        if (cp != null && !cp.getRepositories().contains(site)) {
            ose.addError(invError, siteName);
            return null;
        }

        return site;
    }

    private void setSendingDate(SpecimenKitDetail detail, SpecimenKit kit, OpenSpecimenException ose) {
        kit.setSendingDate(detail.getSendingDate());
    }

    private void setSender(SpecimenKitDetail detail, SpecimenKit kit, OpenSpecimenException ose) {
        UserSummary senderDetail = detail.getSender();

        User sender = AuthUtil.getCurrentUser();
        if (senderDetail.getId() != null) {
            sender = daoFactory.getUserDao().getById(senderDetail.getId());
        } else if (StringUtils.isNotBlank(senderDetail.getEmailAddress())) {
            sender = daoFactory.getUserDao().getUserByEmailAddress(senderDetail.getEmailAddress());
        } else if (StringUtils.isNotBlank(senderDetail.getLoginName()) && StringUtils.isNotBlank(senderDetail.getDomain())) {
            sender = daoFactory.getUserDao().getUser(senderDetail.getLoginName(), senderDetail.getDomain());
        }

        if (sender == null) {
            ose.addError(UserErrorCode.NOT_FOUND);
            return;
        }

        kit.setSender(sender);
    }

    private void setComments(SpecimenKitDetail detail, SpecimenKit kit, OpenSpecimenException ose) {
        kit.setComments(detail.getComments());
    }

    private void setActivityStatus(SpecimenKitDetail detail, SpecimenKit kit, OpenSpecimenException ose) {
        String status = detail.getActivityStatus();

        if (StringUtils.isBlank(status)) {
            kit.setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
        } else if (Status.isValidActivityStatus(status)) {
            kit.setActivityStatus(status);
        } else {
            ose.addError(ActivityStatusErrorCode.INVALID);
        }
    }
}
