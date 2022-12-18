package com.krishagni.catissueplus.core.administrative.services.impl;

import java.io.File;
import java.util.Collections;
import java.util.UUID;

import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.events.ContainerDefragDetail;
import com.krishagni.catissueplus.core.administrative.services.ContainerDefragmenter;
import com.krishagni.catissueplus.core.administrative.services.ContainerReport;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.events.ExportedFileDetail;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

public class ContainerDefragReport extends AbstractContainerReport implements ContainerReport {
	@Override
	public String getName() {
		return "container_defrag_report";
	}

	@Override
	public ExportedFileDetail generate(StorageContainer container, Object... params) {
		String uuid = UUID.randomUUID().toString();
		File file = generateReport(container, (ContainerDefragDetail) params[0], uuid);

		String filename = getFilename(container);
		String zipFilename = getZipFileId(filename, uuid);
		String entryName = filename + ".csv";
		Pair<String, String> zipEntry = Pair.make(file.getAbsolutePath(), entryName);
		File zipFile = new File(ConfigUtil.getInstance().getReportsDir(), zipFilename + ".zip");
		Utility.zipFilesWithNames(Collections.singletonList(zipEntry), zipFile.getAbsolutePath());
		file.delete();
		return new ExportedFileDetail(zipFilename, zipFile);
	}

	@Override
	public String getFilenamePrefix() {
		return "defrag_report";
	}

	private File generateReport(StorageContainer container, ContainerDefragDetail input, String fileId) {
		File rptFile = new File(ConfigUtil.getInstance().getReportsDir(), fileId + ".csv");
		ContainerDefragmenter defragmenter = new DefaultContainerDefragmenter(rptFile, input.isAliquotsInSameContainer());
		defragmenter.defragment(container);
		return rptFile;
	}
}
