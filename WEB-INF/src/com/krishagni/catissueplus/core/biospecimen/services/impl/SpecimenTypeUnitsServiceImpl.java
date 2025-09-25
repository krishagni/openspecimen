package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenTypeUnit;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenTypeUnitError;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenTypeUnitDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenTypeUnitsListCriteria;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenTypeUnitsService;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.PvAttributes;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.exporter.domain.ExportJob;
import com.krishagni.catissueplus.core.exporter.services.ExportService;
import com.krishagni.rbac.common.errors.RbacErrorCode;

public class SpecimenTypeUnitsServiceImpl implements SpecimenTypeUnitsService, InitializingBean {
	private static final LogUtil logger = LogUtil.getLogger(SpecimenTypeUnitsServiceImpl.class);

	private DaoFactory daoFactory;

	private ExportService exportSvc;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setExportSvc(ExportService exportSvc) {
		this.exportSvc = exportSvc;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<SpecimenTypeUnitDetail>> getUnits(RequestEvent<SpecimenTypeUnitsListCriteria> req) {
		List<SpecimenTypeUnit> units = daoFactory.getSpecimenTypeUnitDao().getUnits(req.getPayload());
		return ResponseEvent.response(SpecimenTypeUnitDetail.from(units));
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Long> getUnitsCount(RequestEvent<SpecimenTypeUnitsListCriteria> req) {
		return ResponseEvent.response(daoFactory.getSpecimenTypeUnitDao().getUnitsCount(req.getPayload()));
	}

	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenTypeUnitDetail> createUnit(RequestEvent<SpecimenTypeUnitDetail> req) {
		try {
			SpecimenTypeUnit unit = createUnit(req.getPayload());
			ensureCreateOrUpdateUnitRights(unit);

			ensureUniqueUnit(null, unit);
			daoFactory.getSpecimenTypeUnitDao().saveOrUpdate(unit);
			return ResponseEvent.response(SpecimenTypeUnitDetail.from(unit));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenTypeUnitDetail> updateUnit(RequestEvent<SpecimenTypeUnitDetail> req) {
		try {
			SpecimenTypeUnit unit = createUnit(req.getPayload());
			unit.setId(req.getPayload().getId());
			if (unit.getId() == null) {
				return ResponseEvent.userError(SpecimenTypeUnitError.ID_REQ);
			}

			SpecimenTypeUnit existing = daoFactory.getSpecimenTypeUnitDao().getById(unit.getId());
			if (existing == null) {
				return ResponseEvent.userError(SpecimenTypeUnitError.NOT_FOUND, unit.getId());
			}

			ensureCreateOrUpdateUnitRights(existing);
			if (!Objects.equals(existing.getCp(), unit.getCp())) {
				ensureCreateOrUpdateUnitRights(unit);
			}

			ensureUniqueUnit(existing, unit);
			existing.update(unit);
			daoFactory.getSpecimenTypeUnitDao().saveOrUpdate(existing);
			return ResponseEvent.response(SpecimenTypeUnitDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenTypeUnitDetail> deleteUnit(RequestEvent<SpecimenTypeUnitDetail> req) {
		try {
			SpecimenTypeUnitDetail input = req.getPayload();
			if (input.getId() == null) {
				return ResponseEvent.userError(SpecimenTypeUnitError.ID_REQ);
			}

			SpecimenTypeUnit unit = daoFactory.getSpecimenTypeUnitDao().getById(input.getId());
			if (unit == null) {
				return ResponseEvent.userError(SpecimenTypeUnitError.NOT_FOUND, input.getId());
			}

			ensureCreateOrUpdateUnitRights(unit);
			daoFactory.getSpecimenTypeUnitDao().delete(unit);
			return ResponseEvent.response(SpecimenTypeUnitDetail.from(unit));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		exportSvc.registerObjectsGenerator("specimenTypeUnit", this::getUnitsGenerator);
	}

	private SpecimenTypeUnit createUnit(SpecimenTypeUnitDetail input) {
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);

		SpecimenTypeUnit unit = new SpecimenTypeUnit();
		setCp(input, unit, ose);
		setSpecimenClass(input, unit, ose);
		setSpecimenType(input, unit, ose);
		setQuantityUnit(input, unit, ose);
		setConcentrationUnit(input, unit, ose);
		if (unit.getQuantityUnit() == null && unit.getConcentrationUnit() == null) {
			ose.addError(SpecimenTypeUnitError.QTY_OR_CONC_UNIT_REQ);
		}

		ose.checkAndThrow();
		return unit;
	}

	private void setCp(SpecimenTypeUnitDetail input, SpecimenTypeUnit unit, OpenSpecimenException ose) {
		String shortTitle = input.getCpShortTitle();
		CollectionProtocol cp = null;
		if (input.getCpId() != null) {
			if (input.getCpId() > 0L) {
				cp = daoFactory.getCollectionProtocolDao().getById(input.getCpId());
				if (cp == null) {
					ose.addError(CpErrorCode.NOT_FOUND, input.getCpId());
				}
			}
		} else if (StringUtils.isNotBlank(shortTitle)) {
			cp = daoFactory.getCollectionProtocolDao().getCpByShortTitle(shortTitle);
			if (cp == null) {
				ose.addError(CpErrorCode.NOT_FOUND, shortTitle);
			}
		}

		unit.setCp(cp);
	}

	private void setSpecimenClass(SpecimenTypeUnitDetail input, SpecimenTypeUnit unit, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.getSpecimenClass())) {
			ose.addError(SpecimenErrorCode.SPECIMEN_CLASS_REQUIRED);
			return;
		}

		PermissibleValue specimenClass = daoFactory.getPermissibleValueDao().getPv(PvAttributes.SPECIMEN_CLASS, input.getSpecimenClass());
		if (specimenClass == null) {
			ose.addError(SpecimenErrorCode.INVALID_SPECIMEN_CLASS, input.getSpecimenClass());
		}

		unit.setSpecimenClass(specimenClass);
	}

	private void setSpecimenType(SpecimenTypeUnitDetail input, SpecimenTypeUnit unit, OpenSpecimenException ose) {
		PermissibleValue specimenType = null;
		if (StringUtils.isNotBlank(input.getType())) {
			specimenType = daoFactory.getPermissibleValueDao().getPv(PvAttributes.SPECIMEN_CLASS, input.getType(), true);
			if (specimenType == null) {
				ose.addError(SpecimenErrorCode.INVALID_SPECIMEN_TYPE, input.getType());
			}
		}

		if (specimenType != null && unit.getSpecimenClass() != null && !unit.getSpecimenClass().equals(specimenType.getParent())) {
			ose.addError(SpecimenErrorCode.INVALID_SPECIMEN_TYPE, input.getType());
		}

		unit.setType(specimenType);
	}

	private void setQuantityUnit(SpecimenTypeUnitDetail input, SpecimenTypeUnit unit, OpenSpecimenException ose) {
		PermissibleValue qtyUnit = null;
		if (StringUtils.isNotBlank(input.getQuantityUnit())) {
			qtyUnit = daoFactory.getPermissibleValueDao().getPv(PvAttributes.SPECIMEN_UNIT, input.getQuantityUnit());
			if (qtyUnit == null) {
				ose.addError(SpecimenErrorCode.INVALID_UNIT, input.getQuantityUnit());
			}
		}

		unit.setQuantityUnit(qtyUnit);
	}

	private void setConcentrationUnit(SpecimenTypeUnitDetail input, SpecimenTypeUnit unit, OpenSpecimenException ose) {
		PermissibleValue concUnit = null;
		if (StringUtils.isNotBlank(input.getConcentrationUnit())) {
			concUnit = daoFactory.getPermissibleValueDao().getPv(PvAttributes.SPECIMEN_UNIT, input.getConcentrationUnit());
			if (concUnit == null) {
				ose.addError(SpecimenErrorCode.INVALID_UNIT, input.getConcentrationUnit());
			}
		}

		unit.setConcentrationUnit(concUnit);
	}

	private void ensureUniqueUnit(SpecimenTypeUnit existing, SpecimenTypeUnit unit) {
		Long cpId    = unit.getCp() != null ? unit.getCp().getId() : null;
		Long classId = unit.getSpecimenClass().getId();
		Long typeId  = unit.getType() != null ? unit.getType().getId() : null;

		SpecimenTypeUnit dbUnit = daoFactory.getSpecimenTypeUnitDao().getUnit(cpId, classId, typeId);
		if (dbUnit == null) {
			return;
		}

		if (!dbUnit.equals(existing)) {
			String cpShortTitle = unit.getCp() != null ? unit.getCp().getShortTitle() : "All CPs";
			String specimenClass = unit.getSpecimenClass().getValue();
			String type = unit.getType() != null ? unit.getType().getValue() : "All Types";
			throw OpenSpecimenException.userError(SpecimenTypeUnitError.DUP, cpShortTitle, specimenClass, type);
		}
	}

	private void ensureCreateOrUpdateUnitRights(SpecimenTypeUnit unit) {
		if (unit.getCp() == null) {
			AccessCtrlMgr.getInstance().ensureUserIsAdmin();
		} else {
			AccessCtrlMgr.getInstance().ensureUpdateCpRights(unit.getCp());
		}
	}

	private Function<ExportJob, List<? extends Object>> getUnitsGenerator() {
		return new Function<>() {
			private boolean paramsInited = false;

			private boolean endOfUnits = false;

			private SpecimenTypeUnitsListCriteria crit;

			private Long lastId;

			private int startAt;

			@Override
			public List<? extends Object> apply(ExportJob job) {
				initParams(job);
				if (endOfUnits) {
					return Collections.emptyList();
				}

				List<SpecimenTypeUnit> units = daoFactory.getSpecimenTypeUnitDao().getUnits(crit.startAt(startAt).lastId(lastId));
				List<SpecimenTypeUnitDetail> result = SpecimenTypeUnitDetail.from(units);
				result.forEach(unit -> unit.setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus()));

				startAt += result.size();
				endOfUnits = result.size() < crit.maxResults();
				if (!result.isEmpty()) {
					lastId = result.get(result.size() - 1).getId();
				}

				return result;
			}

			private void initParams(ExportJob job) {
				if (paramsInited) {
					return;
				}

				Map<String, String> params = job.getParams();
				if (params == null) {
					params = Collections.emptyMap();
				}

				Long cpId = null;
				String cpIdStr = params.get("cpId");
				if (StringUtils.isNotBlank(cpIdStr)) {
					try {
						cpId = Long.parseLong(cpIdStr);
						if (cpId == -1L) {
							cpId = null;
						}
					} catch (Exception e) {
						logger.error("Invalid CP ID: " + cpIdStr, e);
					}
				}

				if (cpId != null) {
					try {
						AccessCtrlMgr.getInstance().ensureReadCpRights(cpId);
					} catch (OpenSpecimenException ose) {
						if (ose.containsError(RbacErrorCode.ACCESS_DENIED)) {
							endOfUnits = true;
						} else {
							throw ose;
						}
					}
				} else if (!AuthUtil.isAdmin()) {
					endOfUnits = true;
				}

				crit = new SpecimenTypeUnitsListCriteria()
					.cpId(cpId)
					.startAt(0)
					.maxResults(100)
					.asc(true);
				paramsInited = true;
			}
		};
	}
}