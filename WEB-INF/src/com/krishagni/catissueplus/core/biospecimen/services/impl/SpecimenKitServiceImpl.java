package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenKit;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenKitFactory;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.impl.SpecimenKitErrorCode;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenKitDetail;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenKitSummary;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenKitListCriteria;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenKitService;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.ObjectAccessor;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.Filter;
import com.krishagni.catissueplus.core.de.domain.SavedQuery;
import com.krishagni.catissueplus.core.de.events.ExecuteQueryEventOp;
import com.krishagni.catissueplus.core.de.events.QueryDataExportResult;
import com.krishagni.catissueplus.core.de.services.QueryService;
import com.krishagni.catissueplus.core.de.services.SavedQueryErrorCode;
import edu.common.dynamicextensions.query.WideRowMode;

public class SpecimenKitServiceImpl implements SpecimenKitService, ObjectAccessor {
	private static final String KIT_QUERY_REPORT_SETTING = "specimen_kit_export_report";

	private DaoFactory daoFactory;

	private SpecimenKitFactory specimenKitFactory;

	private QueryService querySvc;

	private com.krishagni.catissueplus.core.de.repository.DaoFactory deDaoFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setSpecimenKitFactory(SpecimenKitFactory specimenKitFactory) {
		this.specimenKitFactory = specimenKitFactory;
	}

	public void setQuerySvc(QueryService querySvc) {
		this.querySvc = querySvc;
	}

	public void setDeDaoFactory(com.krishagni.catissueplus.core.de.repository.DaoFactory deDaoFactory) {
		this.deDaoFactory = deDaoFactory;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<SpecimenKitSummary>> getSpecimenKits(RequestEvent<SpecimenKitListCriteria> req) {
		try {
			return ResponseEvent.response(daoFactory.getSpecimenKitDao().getSpecimenKits(req.getPayload()));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenKitDetail> getSpecimenKit(RequestEvent<Long> req) {
		try {
			return ResponseEvent.response(SpecimenKitDetail.from(getKit(req.getPayload())));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenKitDetail> createSpecimenKit(RequestEvent<SpecimenKitDetail> req) {
		try {
			SpecimenKit kit = specimenKitFactory.createSpecimenKit(req.getPayload());
			daoFactory.getSpecimenKitDao().saveOrUpdate(kit);
			return ResponseEvent.response(SpecimenKitDetail.from(kit));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenKitDetail> updateSpecimenKit(RequestEvent<SpecimenKitDetail> req) {
		try {
			SpecimenKitDetail detail = req.getPayload();
			SpecimenKit existing = getKit(detail.getId());

			SpecimenKit kit = specimenKitFactory.createSpecimenKit(detail);
			existing.update(kit);

			daoFactory.getSpecimenKitDao().saveOrUpdate(existing);
			return ResponseEvent.response(SpecimenKitDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<QueryDataExportResult> exportReport(RequestEvent<Long> req) {
		SpecimenKit kit = getKit(req.getPayload());

		Integer queryId = ConfigUtil.getInstance().getIntSetting("common", KIT_QUERY_REPORT_SETTING, -1);
		if (queryId == -1) {
			return ResponseEvent.userError(SpecimenKitErrorCode.RPT_TMPL_NOT_CONF);
		}

		SavedQuery query = deDaoFactory.getSavedQueryDao().getQuery(queryId.longValue());
		if (query == null) {
			return ResponseEvent.userError(SavedQueryErrorCode.NOT_FOUND, queryId);
		}

		return new ResponseEvent<>(exportKitReport(kit, query));
	}

	@Override
	public SpecimenKit createSpecimenKit(SpecimenKitDetail kitDetail, List<Specimen> specimens) {
		SpecimenKit kit = specimenKitFactory.createSpecimenKit(kitDetail, specimens);
		daoFactory.getSpecimenKitDao().saveOrUpdate(kit);
		return kit;
	}

	@Override
	public String getObjectName() {
		return SpecimenKit.getEntityName();
	}

	@Override
	@PlusTransactional
	public Map<String, Object> resolveUrl(String key, Object value) {
		if (key.equals("id")) {
			value = Long.valueOf(value.toString());
		}

		return daoFactory.getSpecimenKitDao().getCpIds(key, value);
	}

	@Override
	public String getAuditTable() {
		return "OS_SPECIMEN_KITS_AUD";
	}

	@Override
	public void ensureReadAllowed(Long id) {
		//TODO: handle access control for kit
	}

	private SpecimenKit getKit(Long kitId) {
		SpecimenKit kit = daoFactory.getSpecimenKitDao().getById(kitId);

		if (kit == null) {
			throw OpenSpecimenException.userError(SpecimenKitErrorCode.NOT_FOUND, kitId);
		}

		return kit;
	}

	private QueryDataExportResult exportKitReport(final SpecimenKit kit, SavedQuery query) {
		Filter filter = new Filter();
		filter.setField("SpecimenKit.id");
		filter.setOp(Filter.Op.EQ);
		filter.setValues(new String[] { kit.getId().toString() });

		ExecuteQueryEventOp execReportOp = new ExecuteQueryEventOp();
		execReportOp.setDrivingForm("Participant");
		execReportOp.setAql(query.getAql(new Filter[] { filter }));
		execReportOp.setWideRowMode(WideRowMode.DEEP.name());
		execReportOp.setRunType("Export");

		return querySvc.exportQueryData(execReportOp, new QueryService.ExportProcessor() {
			@Override
			public String filename() {
				return "specimen_kit_" + kit.getId() + "_" + UUID.randomUUID().toString();
			}

			@Override
			public void headers(OutputStream out) {
				Map<String, String> headers = new LinkedHashMap<String, String>() {{
					put(getMessage("kit_id"),           kit.getId().toString());
					put(getMessage("kit_sender"),       kit.getSender().formattedName());
					put(getMessage("kit_sending_date"), Utility.getDateString(kit.getSendingDate()));
					put(getMessage("kit_sending_site"), kit.getSendingSite().getName());
					put(getMessage("kit_recv_site"),    kit.getReceivingSite().getName());
					put(getMessage("kit_comments"),     kit.getComments());

					put("", ""); // blank line
				}};

				Utility.writeKeyValuesToCsv(out, headers);
			}
		});
	}

	private String getMessage(String code) {
		return MessageUtil.getInstance().getMessage(code);
	}
}
