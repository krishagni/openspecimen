package com.krishagni.catissueplus.core.administrative.services.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.services.ContainerReport;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.CsvWriter;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

public abstract class AbstractContainerReport implements ContainerReport  {

	protected DaoFactory daoFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	protected void exportContainerSummary(StorageContainer container, CsvWriter writer) {
		writer.writeNext(new String[] { message(CONTAINER_DETAILS) });
		writer.writeNext(new String[] { message(CONTAINER_NAME), container.getName() });
		writer.writeNext(new String[] { message(CONTAINER_HIERARCHY), container.getStringifiedAncestors() });

		writer.writeNext(new String[] { message(CONTAINER_RESTRICTIONS) });

		List<String> cps = new ArrayList<>();
		cps.add(message(CONTAINER_PROTOCOL));
		if (container.getCompAllowedCps().isEmpty()) {
			cps.add(message(ALL));
		} else {
			for (CollectionProtocol cp : container.getCompAllowedCps()) {
				cps.add(cp.getTitle());
			}
		}
		writer.writeNext(cps.toArray(new String[0]));

		List<String> types = new ArrayList<>();
		types.add(message(CONTAINER_SPECIMEN_TYPES));
		if (container.getCompAllowedSpecimenClasses().isEmpty() &&
			container.getCompAllowedSpecimenTypes().isEmpty()) {
			types.add(message(ALL));
		} else {
			for (PermissibleValue specimenClass : container.getCompAllowedSpecimenClasses()) {
				types.add(specimenClass.getValue());
			}

			for (PermissibleValue type : container.getCompAllowedSpecimenTypes()) {
				types.add(type.getValue());
			}
		}
		writer.writeNext(types.toArray(new String[0]));

		writer.writeNext(new String[] { message(EXPORTED_BY), AuthUtil.getCurrentUser().formattedName() });
		writer.writeNext(new String[] { message(EXPORTED_ON), Utility.getDateTimeString(Calendar.getInstance().getTime()) });
		writer.writeNext(new String[0]);
	}

	protected String message(String key, Object... params) {
		return MessageUtil.getInstance().getMessage(key, params);
	}

	// <zip>_<uuid>_<userId>_<name>
	protected String getZipFileId(StorageContainer container, String uuid) {
		return getFileId(container, "zip", uuid);
	}

	// <csv>_<uuid>_<userId>_<name>
	protected String getCsvFileId(StorageContainer container, String uuid) {
		return getFileId(container, "csv", uuid);
	}

	private String getFileId(StorageContainer container, String type, String uuid) {
		return String.join(
			"_",
			type,
			uuid,
			AuthUtil.getCurrentUser().getId().toString(),
			container.getName().replaceAll("[^a-zA-Z0-9\\.\\-]", "_"));
	}

	protected static final String CONTAINER_DETAILS        = "storage_container_details";

	protected static final String CONTAINER_NAME           = "storage_container_name";

	protected static final String CONTAINER_HIERARCHY      = "storage_container_hierarchy";

	protected static final String CONTAINER_RESTRICTIONS   = "storage_container_restrictions";

	protected static final String CONTAINER_PROTOCOL       = "storage_container_restricted_protocols";

	protected static final String CONTAINER_SPECIMEN_TYPES = "storage_container_specimen_types";

	protected static final String ALL                      = "common_all";

	protected static final String EXPORTED_BY              = "common_exported_by";

	protected static final String EXPORTED_ON              = "common_exported_on";

	protected static final String CONTAINER_TYPE           = "container_type";

	protected static final String STORES_SPMN              = "container_stores_specimen";

	protected static final String CELL_ROW                 = "container_position_row";

	protected static final String CELL_COL                 = "container_position_column";

	protected static final String CELL_POS                 = "container_position_position";

	protected static final String YES                      = "common_yes";

	protected static final String NO                       = "common_no";
}
