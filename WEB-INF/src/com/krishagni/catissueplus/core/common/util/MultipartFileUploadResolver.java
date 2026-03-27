package com.krishagni.catissueplus.core.common.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class MultipartFileUploadResolver extends StandardServletMultipartResolver {
	private long maxUploadSize = -1;

	public void setMaxUploadSize(long maxUploadSize) {
		this.maxUploadSize = maxUploadSize;
	}

	@Override
	public MultipartHttpServletRequest resolveMultipart(HttpServletRequest request) throws MultipartException {
		MultipartHttpServletRequest multipartReq = super.resolveMultipart(request);

		MultiValueMap<String, MultipartFile> validatedFiles = new LinkedMultiValueMap<>();
		List<Path> tempFiles = new ArrayList<>();

		multipartReq.getFileNames().forEachRemaining(
			filename ->
				multipartReq.getFiles(filename).forEach(
					file -> validatedFiles.add(filename, validateFile(file, tempFiles))
				)
		);

		request.setAttribute("os.validatedMultipartTempFiles", tempFiles);
		return new ValidatingMultipartHttpServletRequest(multipartReq, validatedFiles);
	}

	@Override
	public void cleanupMultipart(MultipartHttpServletRequest request) {
		Object tempFiles = request.getAttribute("os.validatedMultipartTempFiles");
		if (tempFiles instanceof List) {
			for (Object path : (List<?>) tempFiles) {
				if (path instanceof Path) {
					try {
						Files.deleteIfExists((Path) path);
					} catch (IOException e) {
						// best-effort cleanup
					}
				}
			}
		}
		super.cleanupMultipart(request);
	}

	private ValidatedMultipartFile validateFile(MultipartFile file, List<Path> tempFiles) {
		try {
			if (maxUploadSize > 0 && file.getSize() > maxUploadSize) {
				throw OpenSpecimenException.userError(
					CommonErrorCode.INVALID_INPUT,
					"File size exceeds the max allowed limit (" + maxUploadSize + " bytes)."
				);
			}

			Path tmpDir = ConfigUtil.getInstance().getTempDir().toPath();
			Path tmpFile = Files.createTempFile(tmpDir, "os-upload-", ".tmp");
			tempFiles.add(tmpFile);

			try (InputStream in = file.getInputStream()) {
				Files.copy(in, tmpFile, StandardCopyOption.REPLACE_EXISTING);
			}

			try (InputStream in = Files.newInputStream(tmpFile)) {
				String contentType = Utility.ensureFileUploadAllowed(file.getOriginalFilename(), in);
				return new ValidatedMultipartFile(file, tmpFile, contentType);
			}
		} catch (IOException e) {
			throw OpenSpecimenException.serverError(e);
		}
	}

	private static class ValidatingMultipartHttpServletRequest extends HttpServletRequestWrapper
		implements MultipartHttpServletRequest {

		private final MultipartHttpServletRequest delegate;
		private final MultiValueMap<String, MultipartFile> validatedFiles;

		private ValidatingMultipartHttpServletRequest(
			MultipartHttpServletRequest delegate,
			MultiValueMap<String, MultipartFile> validatedFiles
		) {
			super(delegate);
			this.delegate = delegate;
			this.validatedFiles = validatedFiles;
		}

		@Override
		public HttpMethod getRequestMethod() {
			return delegate.getRequestMethod();
		}

		@Override
		public HttpHeaders getRequestHeaders() {
			return delegate.getRequestHeaders();
		}

		@Override
		public HttpHeaders getMultipartHeaders(String paramOrFileName) {
			return delegate.getMultipartHeaders(paramOrFileName);
		}

		@Override
		public Iterator<String> getFileNames() {
			return validatedFiles.keySet().iterator();
		}

		@Override
		public MultipartFile getFile(String name) {
			List<MultipartFile> files = validatedFiles.get(name);
			return (files == null || files.isEmpty()) ? null : files.get(0);
		}

		@Override
		public List<MultipartFile> getFiles(String name) {
			List<MultipartFile> files = validatedFiles.get(name);
			return files != null ? files : Collections.emptyList();
		}

		@Override
		public Map<String, MultipartFile> getFileMap() {
			Map<String, MultipartFile> fileMap = new LinkedHashMap<>();
			for (Map.Entry<String, List<MultipartFile>> entry : validatedFiles.entrySet()) {
				if (!entry.getValue().isEmpty()) {
					fileMap.put(entry.getKey(), entry.getValue().get(0));
				}
			}
			return fileMap;
		}

		@Override
		public MultiValueMap<String, MultipartFile> getMultiFileMap() {
			return validatedFiles;
		}

		@Override
		public String getMultipartContentType(String paramOrFileName) {
			MultipartFile file = getFile(paramOrFileName);
			return file != null ? file.getContentType() : delegate.getMultipartContentType(paramOrFileName);
		}
	}

	private static class ValidatedMultipartFile implements MultipartFile {
		private final MultipartFile delegate;
		private final Path tempFile;
		private final String contentType;

		private ValidatedMultipartFile(MultipartFile delegate, Path tempFile, String contentType) {
			this.delegate = delegate;
			this.tempFile = tempFile;
			this.contentType = contentType;
		}

		@Override
		public String getName() {
			return delegate.getName();
		}

		@Override
		public String getOriginalFilename() {
			return delegate.getOriginalFilename();
		}

		@Override
		public String getContentType() {
			return contentType;
		}

		@Override
		public boolean isEmpty() {
			return getSize() == 0;
		}

		@Override
		public long getSize() {
			try {
				return Files.size(tempFile);
			} catch (IOException e) {
				return 0;
			}
		}

		@Override
		public byte[] getBytes() {
			try {
				return Files.readAllBytes(tempFile);
			} catch (IOException e) {
				throw OpenSpecimenException.serverError(e);
			}
		}

		@Override
		public InputStream getInputStream() {
			try {
				return Files.newInputStream(tempFile);
			} catch (IOException e) {
				throw OpenSpecimenException.serverError(e);
			}
		}

		@Override
		public void transferTo(File dest) throws IOException {
			Files.copy(tempFile, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
