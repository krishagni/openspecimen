package com.krishagni.catissueplus.core.common.service.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;

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
import com.krishagni.catissueplus.core.exporter.domain.ExportJob;
import com.krishagni.catissueplus.core.exporter.services.ExportService;

public class PrintRuleConfigServiceImpl implements PrintRuleConfigService, InitializingBean {
	private DaoFactory daoFactory;

	private PrintRuleConfigFactory printRuleConfigFactory;

	private LabelPrintRuleFactoryRegistrar labelPrintRuleFactoryRegistrar;

	private ExportService exportSvc;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setPrintRuleConfigFactory(PrintRuleConfigFactory printRuleConfigFactory) {
		this.printRuleConfigFactory = printRuleConfigFactory;
	}

	public void setLabelPrintRuleFactoryRegistrar(LabelPrintRuleFactoryRegistrar labelPrintRuleFactoryRegistrar) {
		this.labelPrintRuleFactoryRegistrar = labelPrintRuleFactoryRegistrar;
	}

	public void setExportSvc(ExportService exportSvc) {
		this.exportSvc = exportSvc;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<PrintRuleConfigDetail>> getPrintRuleConfigs(RequestEvent<PrintRuleConfigsListCriteria> req) {
		try {
			AccessCtrlMgr.getInstance().ensureReadPrintRuleRights();

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
			AccessCtrlMgr.getInstance().ensureReadPrintRuleRights();

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
			AccessCtrlMgr.getInstance().ensureCreatePrintRuleRights();

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
			AccessCtrlMgr.getInstance().ensureUpdatePrintRuleRights();

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
			AccessCtrlMgr.getInstance().ensureDeletePrintRuleRights();

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
		AccessCtrlMgr.getInstance().ensureReadPrintRuleRights();

		List<FileEntry> cmdFiles = new ArrayList<>();
		try (Stream<Path> files = getFiles(req.getPayload())) {
			files.forEach(
				filePath -> {
					File file = filePath.toFile();
					int i = 0;
					for (FileEntry e : cmdFiles) {
						if (e.getMtime() < file.lastModified()) {
							break;
						} else if (e.getMtime() == file.lastModified()) {
							if (e.getName().compareTo(file.getName()) < 0) {
								break;
							}
						}

						++i;
					}

					if (i <= 499) {
						cmdFiles.add(i, FileEntry.from(file));
						if (cmdFiles.size() > 500) {
							cmdFiles.remove(500);
						}
					}
				}
			);

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
			AccessCtrlMgr.getInstance().ensureReadPrintRuleRights();

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

	@Override
	@PlusTransactional
	public ResponseEvent<Integer> clearCommandFiles(RequestEvent<Long> req) {
		AccessCtrlMgr.getInstance().ensureUpdatePrintRuleRights();

		AtomicInteger count = new AtomicInteger(0);
		try (Stream<Path> files = getFiles(req.getPayload())) {
			files.forEach(
				file -> {
					file.toFile().delete();
					count.incrementAndGet();
				}
			);

			return ResponseEvent.response(count.get());
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		exportSvc.registerObjectsGenerator("containerPrintRule", () -> getPrintRulesGenerator("CONTAINER"));
		exportSvc.registerObjectsGenerator("orderItemPrintRule", () -> getPrintRulesGenerator("ORDER_ITEM"));
		exportSvc.registerObjectsGenerator("specimenPrintRule", () -> getPrintRulesGenerator("SPECIMEN"));
		exportSvc.registerObjectsGenerator("visitPrintRule", () -> getPrintRulesGenerator("VISIT"));
	}

	private Stream<Path> getFiles(Long ruleId)
	throws IOException  {
		if (ruleId == null) {
			throw  OpenSpecimenException.userError(PrintRuleConfigErrorCode.ID_REQ);
		}

		PrintRuleConfig printRuleConfig = daoFactory.getPrintRuleConfigDao().getById(ruleId);
		if (printRuleConfig == null) {
			throw  OpenSpecimenException.userError(PrintRuleConfigErrorCode.NOT_FOUND, ruleId);
		}

		LabelPrintRule rule = printRuleConfig.getRule();
		if (StringUtils.isBlank(rule.getCmdFilesDir()) || rule.getCmdFilesDir().equals("*")) {
			return Stream.empty();
		}

		Path path = Paths.get(rule.getCmdFilesDir());
		if (!path.toFile().getCanonicalPath().equals(path.toFile().getAbsolutePath())) {
			throw  OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "Print labels directory path is not absolute: " + rule.getCmdFilesDir());
		}

		String prefix = labelPrintRuleFactoryRegistrar.getFactory(printRuleConfig.getObjectType()).getItemType() + "_" + ruleId;
		return Files.find(
			path,
			1,
			(file, attrs) -> {
				if (!attrs.isRegularFile()) {
					return false;
				}

				String filename = file.toFile().getName();
				return filename.startsWith(prefix) && filename.endsWith(rule.getFileExtn());
			}
		);
	}

	private void deleteRule(PrintRuleConfig rule) {
		rule.delete();
		EventPublisher.getInstance().publish(PrintRuleEvent.DELETED, rule);
	}

	private Function<ExportJob, List<? extends Object>> getPrintRulesGenerator(String objectType) {
		return new Function<>() {
			private boolean paramsInited;

			private boolean endOfRules;

			private PrintRuleConfigsListCriteria crit;

			@Override
			public List<PrintRuleConfigDetail> apply(ExportJob job) {
				initParams();

				if (endOfRules) {
					return Collections.emptyList();
				}

				List<PrintRuleConfig> dbRules = daoFactory.getPrintRuleConfigDao().getPrintRules(crit);
				crit.startAt(crit.startAt() + dbRules.size());
				endOfRules = dbRules.size() < crit.maxResults();
				return PrintRuleConfigDetail.from(dbRules);
			}

			private void initParams() {
				if (paramsInited) {
					return;
				}

				endOfRules = !AccessCtrlMgr.getInstance().hasPrintRuleEximRights();
				crit = new PrintRuleConfigsListCriteria().objectType(objectType).startAt(0).maxResults(100);
				paramsInited = true;
			}
		};
	}
}