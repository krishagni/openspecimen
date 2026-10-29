package com.krishagni.catissueplus.core.de.services.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

class FormFileUploadTracker {
	private static final LogUtil logger = LogUtil.getLogger(FormFileUploadTracker.class);

	private static final String OWNERSHIP_DIR = ".ownership";

	public void add(File uploadDir, Long userId, String fileId) {
		if (!isValidFileName(fileId)) {
			throw new IllegalArgumentException("Invalid form file ID: " + fileId);
		}

		try {
			File userDir = getUserDir(uploadDir, userId);
			Files.createDirectories(userDir.toPath());
			Files.createFile(Utility.getFile(userDir, fileId).toPath());
		} catch (IOException e) {
			throw OpenSpecimenException.serverError(e);
		}
	}

	public boolean isOwner(File uploadDir, Long userId, String fileId) {
		return isValidFileName(fileId) && Utility.getFile(getUserDir(uploadDir, userId), fileId).isFile();
	}

	public File getUpload(File uploadDir, String fileId) {
		return isValidFileName(fileId) ? Utility.getFile(uploadDir, fileId) : null;
	}

	public void deleteOlderThan(File uploadDir, long expiryTime) {
		File ownershipDir = getOwnershipDir(uploadDir);
		if (!ownershipDir.isDirectory()) {
			return;
		}

		try (Stream<Path> userDirs = Files.list(ownershipDir.toPath())) {
			userDirs.filter(Files::isDirectory).forEach(userDir -> deleteOlderThan(userDir, expiryTime));
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		}
	}

	private void deleteOlderThan(Path userDir, long expiryTime) {
		try (Stream<Path> markers = Files.list(userDir)) {
			markers
				.filter(Files::isRegularFile)
				.filter(marker -> marker.toFile().lastModified() < expiryTime)
				.forEach(this::delete);
		} catch (Exception e) {
			logger.error("Error deleting ownership markers of user: " + userDir, e);
		}
	}

	private void delete(Path marker) {
		try {
			Files.deleteIfExists(marker);
		} catch (Exception e) {
			logger.error("Error deleting form file ownership marker: " + marker, e);
		}
	}

	private File getUserDir(File uploadDir, Long userId) {
		return Utility.getFile(getOwnershipDir(uploadDir), userId.toString());
	}

	private File getOwnershipDir(File uploadDir) {
		return Utility.getFile(uploadDir, OWNERSHIP_DIR);
	}

	private boolean isValidFileName(String filename) {
		return filename != null &&
			!filename.isEmpty() &&
			!filename.equals(".") &&
			!filename.equals("..") &&
			filename.indexOf('/') == -1 &&
			filename.indexOf('\\') == -1;
	}
}
