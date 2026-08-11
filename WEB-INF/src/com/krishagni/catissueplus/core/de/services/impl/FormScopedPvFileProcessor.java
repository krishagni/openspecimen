package com.krishagni.catissueplus.core.de.services.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.PvAttribute;
import com.krishagni.catissueplus.core.administrative.domain.factory.PvErrorCode;
import com.krishagni.catissueplus.core.administrative.events.PermissibleValueDetails;
import com.krishagni.catissueplus.core.administrative.events.PvAttributeDetail;
import com.krishagni.catissueplus.core.administrative.repository.PermissibleValueDao;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.PermissibleValueService;
import com.krishagni.catissueplus.core.common.util.CsvFileReader;
import com.krishagni.catissueplus.core.common.util.CsvFileWriter;
import com.krishagni.catissueplus.core.common.util.CsvWriter;
import com.krishagni.catissueplus.core.common.util.DbUtil;
import com.krishagni.catissueplus.core.common.util.SessionUtil;
import com.krishagni.catissueplus.core.de.services.FormDefinitionFileProcessor;
import com.krishagni.catissueplus.core.de.services.FormDefinitionFileProcessors;
import com.krishagni.catissueplus.core.de.ui.PvControl;

import edu.common.dynamicextensions.domain.nui.Container;
import edu.common.dynamicextensions.domain.nui.Control;
import edu.common.dynamicextensions.napi.FormEventsListener;
import edu.common.dynamicextensions.napi.FormEventsNotifier;

public class FormScopedPvFileProcessor implements FormDefinitionFileProcessor, FormEventsListener, InitializingBean, DisposableBean {
	private static final int PV_BATCH_SIZE = 1000;

	private DaoFactory daoFactory;

	private PermissibleValueService pvSvc;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setPvSvc(PermissibleValueService pvSvc) {
		this.pvSvc = pvSvc;
	}

	@Override
	public void afterPropertiesSet() {
		FormDefinitionFileProcessors.getInstance().addProcessor(this);
		FormEventsNotifier.getInstance().addListener(this);
	}

	@Override
	public void destroy() {
		FormDefinitionFileProcessors.getInstance().removeProcessor(this);
		FormEventsNotifier.getInstance().removeListener(this);
	}

	@Override
	public void exportForm(Container form, String outputDir) {
		List<PvAttribute> attributes = DbUtil.newTxn(
			() -> daoFactory.getPermissibleValueDao().getFormAttributes(form.getId())
		);
		if (attributes == null || attributes.isEmpty()) {
			return;
		}

		File pvsDir = new File(outputDir, "pvs");
		if (!pvsDir.exists() && !pvsDir.mkdirs()) {
			throw new RuntimeException("Unable to create PV export directory: " + pvsDir);
		}

		for (PvAttribute attribute : attributes) {
			exportAttribute(pvsDir, attribute);
		}
	}

	@Override
	public boolean importForm(Container form, String inputDir) {
		File pvsDir = new File(inputDir, "pvs");
		if (!pvsDir.isDirectory()) {
			return false;
		}

		Map<String, List<PvControl>> controlsByAttribute = new LinkedHashMap<>();
		for (Control control : form.getAllControls()) {
			if (control instanceof PvControl pvCtrl) {
				if (StringUtils.isNotBlank(pvCtrl.getFormPvOptionsFile())) {
					List<PvControl> pvCtrls = controlsByAttribute.computeIfAbsent(pvCtrl.getAttribute(), key -> new ArrayList<>());
					pvCtrls.add(pvCtrl);
				}
			}
		}

		boolean changed = false;
		for (Map.Entry<String, List<PvControl>> entry : controlsByAttribute.entrySet()) {
			PvControl sourceControl = entry.getValue().get(0);

			String targetAttribute = getOrCreateAttribute(form, sourceControl);
			File csvFile = new File(pvsDir, sourceControl.getFormPvOptionsFile());
			importPvs(targetAttribute, csvFile);
			for (PvControl control : entry.getValue()) {
				control.setAttribute(targetAttribute);
				control.setFormPvCaption(null);
				control.setFormPvOptionsFile(null);
				changed = true;
			}
		}

		return changed;
	}

	@Override
	@PlusTransactional
	public void validateForm(Container form) {
		PermissibleValueDao pvDao = daoFactory.getPermissibleValueDao();
		for (Control control : form.getAllControls()) {
			if (!(control instanceof PvControl)) {
				continue;
			}

			//
			// A form-scoped attribute can be shared by multiple controls in its owning form, but it
			// cannot be referenced by another form. This guards form XML and API-based updates that
			// bypass the Form Designer's filtered attribute list.
			//
			PvAttribute attribute = pvDao.getAttribute(((PvControl) control).getAttribute());
			if (attribute != null && attribute.isFormScoped() && !Objects.equals(form.getId(), attribute.getFormId())) {
				throw OpenSpecimenException.userError(
					PvErrorCode.FORM_ATTR_FORM_MISMATCH,
					attribute.getPublicId(),
					attribute.getFormId(),
					form.getId()
				);
			}
		}
	}

	@Override
	public void onCreate(Container form) {
	}

	@Override
	public void preUpdate(Container form) {
	}

	@Override
	public void onUpdate(Container form) {
	}

	@Override
	@PlusTransactional
	public void onDelete(Container form) {
		daoFactory.getPermissibleValueDao().deleteFormAttributes(form.getId());
	}

	private void exportAttribute(File pvsDir, PvAttribute attribute) {
		File csvFile = new File(pvsDir, getOptionsFile(attribute.getPublicId()));
		CsvWriter writer = null;
		try {
			writer = CsvFileWriter.createCsvFileWriter(csvFile);
			writer.writeNext(new String[] {"Value", "Activity Status"});
			exportPvs(attribute, writer);
		} finally {
			IOUtils.closeQuietly(writer);
		}
	}

	private void exportPvs(PvAttribute attribute, CsvWriter writer) {
		boolean done = false;
		Long lastId = null;
		while (!done) {
			Long batchLastId = lastId;
			List<PermissibleValue> pvs = DbUtil.newTxn(
				() -> daoFactory.getPermissibleValueDao().getPvs(attribute.getPublicId(), batchLastId, PV_BATCH_SIZE)
			);

			if (pvs == null || pvs.isEmpty()) {
				done = true;
				continue;
			}

			for (PermissibleValue pv : pvs) {
				writer.writeNext(new String[] {pv.getValue(), pv.getActivityStatus()});
			}

			lastId = pvs.get(pvs.size() - 1).getId();
			done = pvs.size() < PV_BATCH_SIZE;
		}
	}

	private void importPvs(String attribute, File file) {
		try (CsvFileReader reader = CsvFileReader.createCsvFileReader(file.getAbsolutePath(), true)) {
			List<PermissibleValueDetails> pvs = new ArrayList<>(PV_BATCH_SIZE);
			while (reader.next()) {
				PermissibleValueDetails pv = new PermissibleValueDetails();
				pv.setAttribute(attribute);
				pv.setValue(reader.getColumn("Value"));
				pv.setActivityStatus(reader.getColumn("Activity Status"));
				pvs.add(pv);
				if (pvs.size() == PV_BATCH_SIZE) {
					createPvs(pvs);
					pvs = new ArrayList<>(PV_BATCH_SIZE);
				}
			}

			createPvs(pvs);
		}
	}

	private String getOrCreateAttribute(Container form, PvControl sourceControl) {
		PvAttribute sourceAttribute = daoFactory.getPermissibleValueDao().getAttribute(sourceControl.getAttribute());
		if (sourceAttribute != null) {
			if (!sourceAttribute.isFormScoped() || sourceAttribute.getFormId().equals(form.getId())) {
				return sourceAttribute.getPublicId();
			}
		}

		// The source attribute is unavailable or belongs to another form. Create an independent
		// attribute so that imported copies and cloned forms do not share form-scoped PVs.
		PvAttributeDetail input = new PvAttributeDetail();
		input.setFormId(form.getId());
		input.setName(sourceControl.getFormPvCaption());
		PvAttributeDetail target = ResponseEvent.unwrap(pvSvc.createAttribute(RequestEvent.wrap(input)));
		SessionUtil.getInstance().clearSession();
		return target.getAttribute();
	}

	private void createPvs(List<PermissibleValueDetails> pvs) {
		if (pvs.isEmpty()) {
			return;
		}

		PermissibleValueDao pvDao = daoFactory.getPermissibleValueDao();
		for (PermissibleValueDetails pv : pvs) {
			PermissibleValue existing = pvDao.getByValue(pv.getAttribute(), pv.getValue());
			if (existing != null) {
				if (StringUtils.equals(pv.getActivityStatus(), "Closed") &&
					!StringUtils.equals(existing.getActivityStatus(), "Closed")) {
					pv.setId(existing.getId());
					ResponseEvent.unwrap(pvSvc.updatePermissibleValue(RequestEvent.wrap(pv)));
				}
			} else {
				ResponseEvent.unwrap(pvSvc.createPermissibleValue(RequestEvent.wrap(pv)));
			}
		}

		SessionUtil.getInstance().clearSession();
	}

	private String getOptionsFile(String attribute) {
		return PvControl.getFormPvOptionsFile(attribute);
	}
}
