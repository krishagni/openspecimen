package com.krishagni.catissueplus.core.common.service.impl;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRuleFactoryRegistrar;
import com.krishagni.catissueplus.core.common.domain.PrintRuleConfig;
import com.krishagni.catissueplus.core.common.domain.PrintRuleEvent;
import com.krishagni.catissueplus.core.common.domain.factory.PrintRuleConfigFactory;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.errors.PrintRuleConfigErrorCode;
import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityOp;
import com.krishagni.catissueplus.core.common.events.FileEntry;
import com.krishagni.catissueplus.core.common.events.PrintRuleConfigDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.repository.PrintRuleConfigsListCriteria;
import com.krishagni.catissueplus.core.common.service.PrintRuleConfigService;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.rbac.common.errors.RbacErrorCode;

public class PrintRuleConfigServiceImpl implements PrintRuleConfigService {
	private DaoFactory daoFactory;

	private PrintRuleConfigFactory printRuleConfigFactory;

	private LabelPrintRuleFactoryRegistrar labelPrintRuleFactoryRegistrar;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setPrintRuleConfigFactory(PrintRuleConfigFactory printRuleConfigFactory) {
		this.printRuleConfigFactory = printRuleConfigFactory;
	}

	public void setLabelPrintRuleFactoryRegistrar(LabelPrintRuleFactoryRegistrar labelPrintRuleFactoryRegistrar) {
		this.labelPrintRuleFactoryRegistrar = labelPrintRuleFactoryRegistrar;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<PrintRuleConfigDetail>> getPrintRuleConfigs(RequestEvent<PrintRuleConfigsListCriteria> req) {
		try {
			AccessCtrlMgr.getInstance().ensureUserIsAdmin();

			List<PrintRuleConfig> rules = daoFactory.getPrintRuleConfigDao().getPrintRules(req.getPayload());
			return ResponseEvent.response(PrintRuleConfigDetail.from(rules));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<PrintRuleConfigDetail> getPrintRuleConfig(RequestEvent<Long> req) {
		try {
			AccessCtrlMgr.getInstance().ensureUserIsAdmin();

			PrintRuleConfig rule = daoFactory.getPrintRuleConfigDao().getById(req.getPayload());
			if (rule == null) {
				return ResponseEvent.userError(PrintRuleConfigErrorCode.NOT_FOUND, req.getPayload(), 1);
			}

			return ResponseEvent.response(PrintRuleConfigDetail.from(rule));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<PrintRuleConfigDetail> createPrintRuleConfig(RequestEvent<PrintRuleConfigDetail> req) {
		try {
			AccessCtrlMgr.getInstance().ensureUserIsAdmin();

			PrintRuleConfig rule = printRuleConfigFactory.createPrintRuleConfig(req.getPayload());
			daoFactory.getPrintRuleConfigDao().saveOrUpdate(rule, true);
			EventPublisher.getInstance().publish(PrintRuleEvent.CREATED, rule);
			return ResponseEvent.response(PrintRuleConfigDetail.from(rule));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<PrintRuleConfigDetail> updatePrintRuleConfig(RequestEvent<PrintRuleConfigDetail> req) {
		try {
			AccessCtrlMgr.getInstance().ensureUserIsAdmin();

			PrintRuleConfigDetail detail = req.getPayload();
			if (detail.getId() == null) {
				return ResponseEvent.userError(PrintRuleConfigErrorCode.ID_REQ);
			}

			PrintRuleConfig existing = daoFactory.getPrintRuleConfigDao().getById(detail.getId());
			if (existing == null) {
				return ResponseEvent.userError(PrintRuleConfigErrorCode.NOT_FOUND, detail.getId(), 1);
			}

			PrintRuleConfig rule = printRuleConfigFactory.createPrintRuleConfig(detail);
			existing.update(rule);
			EventPublisher.getInstance().publish(PrintRuleEvent.UPDATED, existing);
			return ResponseEvent.response(PrintRuleConfigDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<PrintRuleConfigDetail>> deletePrintRuleConfigs(RequestEvent<BulkDeleteEntityOp> req) {
		try {
			AccessCtrlMgr.getInstance().ensureUserIsAdmin();

			Set<Long> ruleIds = req.getPayload().getIds();
			if (CollectionUtils.isEmpty(ruleIds)) {
				return ResponseEvent.userError(PrintRuleConfigErrorCode.ID_REQ);
			}

			List<PrintRuleConfig> rules = daoFactory.getPrintRuleConfigDao().getByIds(ruleIds);
			if (ruleIds.size() != rules.size()) {
				rules.forEach(rule -> ruleIds.remove(rule.getId()));
				throw OpenSpecimenException.userError(PrintRuleConfigErrorCode.NOT_FOUND, ruleIds, ruleIds.size());
			}

			rules.forEach(this::deleteRule);
			return ResponseEvent.response(PrintRuleConfigDetail.from(rules));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<FileEntry>> getCommandFiles(RequestEvent<Long> req) {
		try {
			if (!AuthUtil.isAdmin()) {
				return ResponseEvent.userError(RbacErrorCode.ACCESS_DENIED);
			}

			Long ruleId = req.getPayload();
			if (ruleId == null) {
				return ResponseEvent.userError(PrintRuleConfigErrorCode.ID_REQ);
			}

			PrintRuleConfig printRuleConfig = daoFactory.getPrintRuleConfigDao().getById(ruleId);
			if (printRuleConfig == null) {
				return ResponseEvent.userError(PrintRuleConfigErrorCode.NOT_FOUND, ruleId);
			}

			LabelPrintRule rule = printRuleConfig.getRule();
			if (StringUtils.isBlank(rule.getCmdFilesDir()) || rule.getCmdFilesDir().equals("*")) {
				return ResponseEvent.response(Collections.emptyList());
			}

			Path path = Paths.get(rule.getCmdFilesDir());
			if (!path.isAbsolute()) {
				return ResponseEvent.userError(CommonErrorCode.INVALID_INPUT, "Print labels directory path is not absolute: " + rule.getCmdFilesDir());
			}

			String prefix = labelPrintRuleFactoryRegistrar.getFactory(printRuleConfig.getObjectType()).getItemType();
			List<FileEntry> cmdFiles = Arrays.stream(path.toFile().listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return name.startsWith(prefix) && name.endsWith(rule.getFileExtn());
					}
				}))
				.sorted(Comparator.comparing(File::lastModified).reversed())
				.map(FileEntry::from)
				.collect(Collectors.toList());

			return ResponseEvent.response(cmdFiles);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<File> getCommandFile(RequestEvent<Pair<Long, String>> req) {
		try {
			if (!AuthUtil.isAdmin()) {
				return ResponseEvent.userError(RbacErrorCode.ACCESS_DENIED);
			}

			Long ruleId = req.getPayload().first();
			if (ruleId == null) {
				return ResponseEvent.userError(PrintRuleConfigErrorCode.ID_REQ);
			}

			String filename = req.getPayload().second();
			if (StringUtils.isBlank(filename)) {
				return ResponseEvent.userError(CommonErrorCode.INVALID_INPUT, "Filename is required");
			}

			PrintRuleConfig printRuleConfig = daoFactory.getPrintRuleConfigDao().getById(ruleId);
			if (printRuleConfig == null) {
				return ResponseEvent.userError(PrintRuleConfigErrorCode.NOT_FOUND, req.getPayload());
			}

			LabelPrintRule rule = printRuleConfig.getRule();
			if (StringUtils.isBlank(rule.getCmdFilesDir()) || rule.getCmdFilesDir().equals("*")) {
				return ResponseEvent.userError(CommonErrorCode.FILE_NOT_FOUND, filename);
			}

			Path filePath = Paths.get(rule.getCmdFilesDir(), filename);
			File file = filePath.toFile();
			if (!file.exists()) {
				return ResponseEvent.userError(CommonErrorCode.FILE_NOT_FOUND, filename);
			}

			return ResponseEvent.response(file);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	private void deleteRule(PrintRuleConfig rule) {
		rule.delete();
		EventPublisher.getInstance().publish(PrintRuleEvent.DELETED, rule);
	}
}