package com.krishagni.catissueplus.core.common.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.activation.FileTypeMap;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.springframework.mail.javamail.ConfigurableMimeFileTypeMap;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.JavaScriptUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseExtensionEntity;
import com.krishagni.catissueplus.core.common.OpenSpecimenAppCtxProvider;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.PdfUtil;
import com.krishagni.catissueplus.core.common.domain.IntervalUnit;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;
import com.krishagni.catissueplus.core.exporter.services.impl.ExporterContextHolder;
import com.krishagni.catissueplus.core.init.AppProperties;

import au.com.bytecode.opencsv.CSVWriter;

public class Utility {
	private static SecretKey secretKey = null;

	private static String algorithm = null;

	private static FileTypeMap fileTypesMap = null;

	private static volatile TikaConfig tikaConfig = null;

	private static final ThreadLocal<ObjectMapper> om = new ThreadLocal<ObjectMapper>() {
		@Override
		protected ObjectMapper initialValue() {
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			return mapper;
		}
	};

	public static String getDisabledValue(String value, int maxLength) {
		if (StringUtils.isBlank(value)) {
			return value;
		}

		if (maxLength < 14) {
			throw new IllegalArgumentException("Max length should be at least 14 characters");
		}

		int valueMaxLength = maxLength - 14;
		if (value.length() > valueMaxLength) {
			value = value.substring(0, valueMaxLength);
		}

		return value + "_" + Calendar.getInstance().getTimeInMillis();
	}

	public static String stripTs(String value) {
		if (StringUtils.isBlank(value)) {
			return value;
		}

		int ucIdx = value.lastIndexOf("_");
		try {
			Long.parseLong(value.substring(ucIdx + 1));
			return value.substring(0, ucIdx);
		} catch (NumberFormatException nfe) {

		}

		return value;
	}

	public static Long numberToLong(Object number) {
		if (number == null) {
			return null;
		}

		if (!(number instanceof Number)) {
			throw new IllegalArgumentException("Input object is not a number");
		}

		return ((Number) number).longValue();
	}

	public static boolean isEmptyOrSuperset(Set<?> leftOperand, Set<?> rightOperand) {
		if (CollectionUtils.isEmpty(leftOperand)) {
			return true;
		}

		return leftOperand.containsAll(rightOperand);
	}

	public static String getInputStreamDigest(InputStream in) throws IOException {
		return DigestUtils.sha512Hex(getInputStreamBytes(in));
	}

	public static String getDigest(String input) {
		return DigestUtils.sha512Hex(input);
	}

	public static String getDigest(byte[] input) {
		return DigestUtils.sha512Hex(input);
	}

	public static String getResourceDigest(String resource)
	throws IOException {
		InputStream in = null;
		try {
			in = getResourceInputStream(resource);
			return getInputStreamDigest(in);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	public static byte[] getInputStreamBytes(InputStream in)
	throws IOException {
		ByteArrayOutputStream bout = null;
		try {
			bout = new ByteArrayOutputStream();
			IOUtils.copy(in, bout);
			return bout.toByteArray();
		} finally {
			IOUtils.closeQuietly(bout);
		}
	}

	public static InputStream getResourceInputStream(String path) {
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
	}

	public static List<String> csvToStringList(String value) {
		return csvToStringList(value, true);
	}

	public static List<String> csvToStringList(String value, boolean ignoreEmptyElements) {
		return csvToStringList(value, ignoreEmptyElements, ',');
	}

	public static List<String> csvToStringList(String value, boolean ignoreEmptyElements, char separator) {
		if (StringUtils.isBlank(value)) {
			return Collections.emptyList();
		}

		CsvReader reader = null;
		try {
			reader = CsvFileReader.createCsvFileReader(new StringReader(value), false, separator);

			List<String> result = new ArrayList<>();
			while (reader.next()) {
				String[] row = reader.getRow();
				result.addAll(Stream.of(row).map(String::trim).collect(Collectors.toList()));
			}

			if (ignoreEmptyElements) {
				result = result.stream().filter(s -> !s.isEmpty()).collect(Collectors.toList());
			}

			return result;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception e) {

				}
			}
		}
	}

	public static String stringListToCsv(Collection<String> elements) {
		return stringListToCsv(elements.toArray(new String[0]), true);
	}

	public static String stringListToCsv(Collection<String> elements, boolean quotechar) {
		return stringListToCsv(elements.toArray(new String[0]), quotechar);
	}

	public static String stringListToCsv(Collection<String> elements, boolean quotechar, char fieldSeparator) {
		return stringListToCsv(elements.toArray(new String[0]), quotechar, fieldSeparator);
	}

	public static String stringListToCsv(Collection<String> elements, boolean quotechar, char fieldSeparator, String lineEnding) {
		return stringListToCsv(elements.toArray(new String[0]), quotechar, fieldSeparator, lineEnding);
	}

	public static String stringListToCsv(String[] elements) {
		return stringListToCsv(elements, true);
	}

	public static String stringListToCsv(String[] elements, boolean quotechar) {
		return stringListToCsv(elements, quotechar, CSVWriter.DEFAULT_SEPARATOR);
	}

	public static String stringListToCsv(String[] elements, boolean quotechar, char fieldSeparator) {
		return stringListToCsv(elements, quotechar, fieldSeparator, null);
	}

	public static String stringListToCsv(String[] elements, boolean quotechar, char fieldSeparator, String lineEnding) {
		StringWriter writer = new StringWriter();
		CsvWriter csvWriter = null;
		try {
			if (quotechar) {
				csvWriter = CsvFileWriter.createCsvFileWriter(writer, fieldSeparator, CSVWriter.DEFAULT_QUOTE_CHARACTER, lineEnding);
			} else {
				csvWriter = CsvFileWriter.createCsvFileWriter(writer, fieldSeparator, CSVWriter.NO_QUOTE_CHARACTER, lineEnding);
			}
			csvWriter.writeNext(elements);
			csvWriter.flush();
			return writer.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (csvWriter != null) {
				try {
					csvWriter.close();
				} catch (Exception e) {
				}
			}
		}
	}

	public static void writeKeyValuesToCsv(OutputStream out, Map<String, String> keyValues) {
		StringWriter strWriter = new StringWriter();
		CsvWriter csvWriter = null;

		try {
			csvWriter = CsvFileWriter.createCsvFileWriter(strWriter);
			for (Map.Entry<String, String> keyValue : keyValues.entrySet()) {
				csvWriter.writeNext(new String[]{keyValue.getKey(), keyValue.getValue()});
			}
			csvWriter.flush();
			out.write(strWriter.toString().getBytes());
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			IOUtils.closeQuietly(strWriter);
			IOUtils.closeQuietly(csvWriter);
		}
	}

	public static String getQuotedString(String input) {
		return getQuotedString(input, '"');
	}

	public static String getQuotedString(String input, char quoteChar) {
		return getQuotedString(input, quoteChar, quoteChar);
	}

	public static String getQuotedString(String input, char quoteChar, char escapeChar) {
		if (input == null || input.isEmpty()) {
			return input;
		}

		StringBuilder result = new StringBuilder();
		result.append(quoteChar);

		if (input.indexOf(quoteChar) == -1 && input.indexOf(escapeChar) == -1) {
			result.append(input);
		} else {
			for (int i = 0; i < input.length(); ++i) {
				char nextChar = input.charAt(i);
				if (escapeChar != 0 && (nextChar == quoteChar || nextChar == escapeChar)) {
					result.append(escapeChar).append(nextChar);
				} else {
					result.append(nextChar);
				}
			}
		}

		result.append(quoteChar);
		return result.toString();
	}

	public static long getTimezoneOffset() {
		Calendar cal = Calendar.getInstance();
		return -1 * cal.get(Calendar.ZONE_OFFSET);
	}

	public static void sendToClient(HttpServletResponse httpResp, String filename, File file) {
		sendToClient(httpResp, filename, file, false);
	}

	public static void sendToClient(HttpServletResponse httpResp, String filename, File file, boolean deleteOnSend) {
		try {
			sendToClient(httpResp, filename, getContentType(file), file);
		} finally {
			if (deleteOnSend && file != null) {
				file.delete();
			}
		}
	}

	public static void sendToClient(HttpServletResponse httpResp, String filename, String contentType, File file) {
		InputStream in = null;
		try {
			in = new FileInputStream(file);
			sendToClient(httpResp, filename, contentType, in);
		} catch (IOException e) {
			throw new RuntimeException("Error sending file", e);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	public static void sendToClient(HttpServletResponse httpResp, String filename, String contentType, InputStream in) {
		try {
			if (StringUtils.isNotBlank(contentType)) {
				httpResp.setContentType(contentType);
			}

			if (StringUtils.isNotBlank(filename)) {
				httpResp.setHeader("Content-Disposition", "attachment;filename=\"" + filename + "\"");
			}

			IOUtils.copy(in, httpResp.getOutputStream());
		} catch (IOException e) {
			throw new RuntimeException("Error sending file", e);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	public static void downloadFile(InputStream in, String dir, String filename) {
		File dirFile = new File(dir);
		if (!dirFile.exists()) {
			dirFile.mkdirs();
		}

		File outFile = new File(dirFile, filename);
		OutputStream out = null;
		try {
			out = new FileOutputStream(outFile);
			IOUtils.copy(in, out);
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		} finally {
			IOUtils.closeQuietly(out);
		}
	}

	public static void inflateZip(InputStream in, String destination) {
		int ALLOWED_ENTRIES = 100;
		int MAX_INFLATED_SIZE = 1024 * 1024 * 1024;
		int MAX_COMPRESSION_RATIO = 10;

		int totalEntries = 0;
		int totalSize = 0;

		ZipInputStream zipIn = null;
		try {
			File destinationDir = new File(destination);
			zipIn = new ZipInputStream(in);
			while (true) {
				try {
					ZipEntry zipEntry = zipIn.getNextEntry();
					if (zipEntry == null) {
						break;
					}

					++totalEntries;
					if (totalEntries > ALLOWED_ENTRIES) {
						FileUtils.deleteDirectory(destinationDir);
						throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "The count of entries in the input ZIP file exceeds the allowed limit. Bailing out assuming the input is ZIP bomb attack.");
					}

					if (zipEntry.isDirectory()) {
						continue;
					}

					Path entryPath = destinationDir.toPath().resolve(zipEntry.getName());
					if (!entryPath.normalize().startsWith(destinationDir.toPath())) {
						FileUtils.deleteDirectory(destinationDir);
						throw new IOException("Zip entry contains path traversal");
					}

					if (entryPath.getParent() != null) {
						Files.createDirectories(entryPath.getParent());
					}

					try (FileOutputStream fout = new FileOutputStream(entryPath.toFile())) {
						byte[] buffer = new byte[8 * 1024]; // 8 KB
						int entrySize = 0;

						int nBytes = -1;
						while ((nBytes = zipIn.read(buffer)) > 0) {
							fout.write(buffer, 0, nBytes);
							entrySize += nBytes;
							totalSize += nBytes;

							long compressionRatio = entrySize / zipEntry.getCompressedSize();
							if (compressionRatio > MAX_COMPRESSION_RATIO) {
								FileUtils.deleteDirectory(destinationDir);
								throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "Compression ratio of the one or more ZIP file entries exceed the allowed limit. Bailing out assuming the input is ZIP bomb attack.");
							}

							if (totalSize > MAX_INFLATED_SIZE) {
								FileUtils.deleteDirectory(destinationDir);
								throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "Uncompressed file exceeds the max allowed limit. Bailing out assuming the input is ZIP bomb attack.");
							}
						}
					}
				} finally {
					zipIn.closeEntry();
				}
			}
		} catch (IOException e) {
			throw OpenSpecimenException.serverError(e);
		} finally {
			IOUtils.closeQuietly(zipIn);
		}
	}

	public static String getContentType(String filename) {
		if (fileTypesMap == null) {
			synchronized (Utility.class) {
				fileTypesMap = new ConfigurableMimeFileTypeMap();
			}
		}

		return fileTypesMap.getContentType(filename);
	}

	public static String getContentType(File file) {
		return getContentType(file.getName());
	}

	public static String getContentType(InputStream in) {
		MediaType mediaType = getMediaType(in);
		return mediaType != null ? mediaType.toString() : null;
	}

	public static String getFileType(String contentType) {
		try {
			return getTikaConfig().getMimeRepository().forName(contentType).getExtension();
		} catch (Exception e) {
			throw OpenSpecimenException.userError(CommonErrorCode.FILE_TYPE_DETECT_FAILED, e.getLocalizedMessage());
		}
	}

	public static String ensureFileUploadAllowed(File file) {
		try (FileInputStream fin = new FileInputStream(file)) {
			return ensureFileUploadAllowed(file.getName(), fin);
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		}
	}

	public static String ensureFileUploadAllowed(String filename, InputStream in) {
		final String contentType = getContentType(in);
		String detectedFileType = getFileType(contentType).substring(1).toLowerCase();
		String allowedTypesStr = ConfigUtil.getInstance().getStrSetting("common", "allowed_file_types", "");
		Set<String> allowedTypes = Arrays.stream(allowedTypesStr.split(","))
			.map(type -> type.trim().toLowerCase())
			.collect(Collectors.toSet());
		if (!allowedTypes.contains(detectedFileType)) {
			throw OpenSpecimenException.userError(
				CommonErrorCode.INVALID_INPUT,
				"File with content type '" + contentType + "' is not allowed.");
		}

		String fileType = FilenameUtils.getExtension(filename);
		if (StringUtils.isBlank(fileType)) {
			throw OpenSpecimenException.userError(
				CommonErrorCode.INVALID_INPUT,
				"File without extensions are not allowed. Filename:  " + filename);
		}

		if (!allowedTypes.contains(fileType.toLowerCase())) {
			throw OpenSpecimenException.userError(
				CommonErrorCode.INVALID_INPUT,
				"File with extension '" + fileType + "' is not allowed.");
		}

		return contentType;
	}

	public static String getFileText(File file) {
		FileInputStream in = null;
		try {
			in = new FileInputStream(file);
			String contentType = getContentType(file);
			return getString(in, contentType);
		} catch (Exception e) {
			throw new RuntimeException("Error getting file text", e);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	public static String getString(InputStream in, String contentType) {
		String fileText = null;
		try {
			if (StringUtils.isBlank(contentType) || !contentType.equals("application/pdf")) {
				fileText = IOUtils.toString(in, Charset.defaultCharset());
			} else {
				fileText = PdfUtil.getInstance().getText(in);
			}
			return fileText;
		} catch (IOException e) {
			throw new RuntimeException("Error getting file text", e);
		}
	}

	public static String getDateString(Date date) {
		return getDateString(date, false);
	}

	public static String getDateString(Date date, boolean dateOnly) {
		SimpleDateFormat sdf = new SimpleDateFormat(ConfigUtil.getInstance().getDateFmt());
		if (dateOnly) {
			return sdf.format(date);
		}

		TimeZone timeZone = AuthUtil.getUserTimeZone();
		if (timeZone != null) {
			sdf.setTimeZone(timeZone);
		}

		return sdf.format(date);
	}

	public static Date parseDateString(String dateStr) {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat(ConfigUtil.getInstance().getDateFmt());
			TimeZone timeZone = AuthUtil.getUserTimeZone();
			if (timeZone != null) {
				sdf.setTimeZone(timeZone);
			}

			return sdf.parse(dateStr);
		} catch (ParseException pe) {
			throw OpenSpecimenException.serverError(pe);
		}
	}

	public static String getDateTimeString(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat(ConfigUtil.getInstance().getDateTimeFmt());
		TimeZone timeZone = AuthUtil.getUserTimeZone();
		if (timeZone != null) {
			sdf.setTimeZone(timeZone);
		}

		return sdf.format(date);
	}

	public static Date parseDateTimeString(String dateTimeStr) {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat(ConfigUtil.getInstance().getDateTimeFmt());
			TimeZone timeZone = AuthUtil.getUserTimeZone();
			if (timeZone != null) {
				sdf.setTimeZone(timeZone);
			}

			return sdf.parse(dateTimeStr);
		} catch (ParseException pe) {
			throw OpenSpecimenException.serverError(pe);
		}
	}

	public static String format(Date date, String format) {
		return date != null ? new SimpleDateFormat(format).format(date) : null;
	}

	public static Integer getYear(Date date) {
		if (date == null) {
			return null;
		}

		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal.get(Calendar.YEAR);
	}

	public static Date chopSeconds(Date date) {
		if (date == null) {
			return null;
		}

		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}

	@SuppressWarnings("unchecked")
	public static <T> T collect(Collection<?> collection, String propertyName, boolean returnSet) {
		Stream<Object> output = nullSafeStream(collection)
			.map(object -> {
				try {
					return PropertyUtils.getProperty(object, propertyName);
				} catch (Exception e) {
					throw OpenSpecimenException.serverError(e);
				}
			});
		return returnSet ? (T) output.collect(Collectors.toSet()) : (T) output.collect(Collectors.toList());
	}

	public static <T> T collect(Collection<?> collection, String propertyName) {
		return collect(collection, propertyName, false);
	}

	public static <T> Set<T> subtract(Collection<T> coll1, Collection<T> coll2) {
		return coll1.stream().filter(element -> !coll2.contains(element)).collect(Collectors.toSet());
	}

	public static <T> List<T> nullSafe(List<T> iterable) {
		return iterable == null ? Collections.emptyList() : iterable;
	}

	public static <T> Stream<T> nullSafeStream(Collection<T> collection) {
		return collection != null ? collection.stream() : Stream.empty();
	}

	public static <T, K, V> Map<K, V> toLinkedMap(Collection<T> collection, Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
		Map<K, V> map = new LinkedHashMap<>();
		if (CollectionUtils.isEmpty(collection)) {
			return map;
		}

		for (T element : collection) {
			map.put(keyMapper.apply(element), valueMapper.apply(element));
		}

		return map;
	}

	public static <T, K, V> Collector<T, ?, Map<K, V>> toLinkedMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
		return Collectors.toMap(
			keyMapper,
			valueMapper,
			(u, v) -> {
				throw new IllegalStateException(String.format("Duplicate key %s", u));
			},
			LinkedHashMap::new
		);
	}

	public static <T> boolean isEmptyOrSameAs(Collection<T> collection, T element) {
		int size = collection.size();
		if (size > 1) {
			return false;
		}

		return size == 0 || collection.contains(element);
	}

	public static Integer getAge(Date birthDate) {
		return yearsBetween(birthDate, null);
	}

	public static Integer yearsBetween(Date start, Date end) {
		return getPeriodBetween(ChronoUnit.YEARS, start, end);
	}

	public static Integer monthsBetween(Date start, Date end) {
		return getPeriodBetween(ChronoUnit.MONTHS, start, end);
	}

	public static Integer daysBetween(Date start, Date end) {
		return getPeriodBetween(ChronoUnit.DAYS, start, end);
	}

	public static Date currentTime() {
		return Calendar.getInstance().getTime();
	}

	public static int cmp(Date d1, Date d2) {
		return cmp(d1, d2, false);
	}

	public static int cmp(Date d1, Date d2, boolean onlyDate) {
		if (d1 == d2) {
			return 0;
		} else if (d1 == null) {
			return -1;
		} else if (d2 == null) {
			return 1;
		} else if (onlyDate) {
			return chopTime(d1).compareTo(chopTime(d2));
		} else {
			return d1.compareTo(d2);
		}
	}

	public static boolean isEmpty(Map<?, ?> map) {
		return map == null || map.isEmpty();
	}

	public static Date chopTime(Date date) {
		if (date == null) {
			return null;
		}

		return DateUtils.truncate(date, Calendar.DATE);
	}

	public static Date getEndOfDay(Date date) {
		if (date == null) {
			return null;
		}

		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		cal.set(Calendar.MILLISECOND, 999);
		return cal.getTime();
	}

	public static char getFieldSeparator() {
		return ConfigUtil.getInstance().getCharSetting("common", "field_separator", CSVWriter.DEFAULT_SEPARATOR);
	}

	public static String encrypt(String value) {
		return encrypt(null, value);
	}

	public static String encrypt(Connection conn, String value) {
		try {
			Cipher cipher = Cipher.getInstance(getAlgorithm(conn));
			cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(conn));
			byte[] encryptedValue = cipher.doFinal(value.getBytes("UTF-8"));
			return Base64.getEncoder().encodeToString(encryptedValue);
		} catch (Exception e) {
			throw new RuntimeException("Error encrypting the value: " + e.getMessage(), e);
		}
	}

	public static String decrypt(String value) {
		try {
			Cipher cipher = Cipher.getInstance(getAlgorithm());
			cipher.init(Cipher.DECRYPT_MODE, getSecretKey());
			byte[] decodedValue = Base64.getDecoder().decode(value.getBytes());
			return new String(cipher.doFinal(decodedValue));
		} catch (Exception e) {
			throw new RuntimeException("Error decrypting the value: " + e.getMessage(), e);
		}
	}

	public static String stripWs(String value) {
		if (value == null) {
			return null;
		}

		return value.replaceAll("\\s{2,}", "_").trim();
	}

	public static String getHeader(HttpServletRequest httpReq, String name) {
		return stripWs(httpReq.getHeader(name));
	}

	public static String cleanPath(String path) {
		return stripWs(FilenameUtils.getName(path));
	}

	public static String escapeXss(String input) {
		return JavaScriptUtils.javaScriptEscape(HtmlUtils.htmlEscape(stripWs(input)));
	}

	public static boolean isQuoted(String input) {
		if (StringUtils.isBlank(input) || input.length() < 2) {
			return false;
		}

		return (input.charAt(0) == '"' && input.charAt(input.length() - 1) == '"') ||
				(input.charAt(0) == '\'' && input.charAt(input.length() - 1) == '\'');
	}

	public static Map<String, Object> toMap(InputStream in) {
		try {
			return om.get().readValue(in, new TypeReference<HashMap<String, Object>>() {});
		} catch (Exception e) {
			throw new RuntimeException("Error parsing input stream JSON to map:", e);
		}
	}

	public static Map<String, Object> jsonToMap(String json) {
		try {
			if (StringUtils.isBlank(json)) {
				return Collections.emptyMap();
			}

			return om.get().readValue(json, new TypeReference<HashMap<String, Object>>() {});
		} catch (Exception e) {
			throw new RuntimeException("Error parsing JSON to map:\n" + json, e);
		}
	}

	public static <T> Map<String, Object> objectToMap(T object) {
		try {
			return om.get().convertValue(object, new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			throw new RuntimeException("Error converting the input object to Map", e);
		}
	}

	public static <T> String objectToJson(T object) {
		try {
			return om.get().writeValueAsString(object);
		} catch (Exception e) {
			throw new RuntimeException("Error converting the input object to JSON string", e);
		}
	}

	public static String mapToJson(Map<String, ?> map) {
		return mapToJson(map, false);
	}

	public static String mapToJson(Map<String, ?> map, boolean ignoreNull) {
		try {
			if (map == null) {
				map = Collections.emptyMap();
			}

			if (ignoreNull) {
				Map<String, Object> newMap = new HashMap<>();
				for (Map.Entry<String, ?> kv : map.entrySet()) {
					if (kv.getValue() != null) {
						newMap.put(kv.getKey(), kv.getValue());
					}
				}

				map = newMap;
			}

			return om.get().writeValueAsString(map);
		} catch (IOException e) {
			throw new RuntimeException("Error converting to JSON string", e);
		}
	}

	public static <T> T jsonToObject(String json, Class<T> klass) {
		try {
			if (StringUtils.isBlank(json)) {
				return null;
			}

			return om.get().readValue(json, klass);
		}  catch (Exception e) {
			throw new RuntimeException("Error converting to object of type: " + klass.getName(), e);
		}
	}

	public static <T> T mapToObject(Map<String, Object> map, Class<T> klass) {
		try {
			if (map == null) {
				map = Collections.emptyMap();
			}

			return om.get().convertValue(map, klass);
		}  catch (Exception e) {
			throw new RuntimeException("Error converting to object of type: " + klass.getName(), e);
		}
	}

	public static List<String> diff(BaseEntity obj1, BaseEntity obj2, List<String> fields) {
		return fields.stream().filter(field -> {
			if (!field.startsWith("extensionDetail.attrsMap.")) {
				return !equals(obj1, obj2, field);
			} /*else {
				field = field.substring("extensionDetail.attrsMap.".length());
				return !equals(map1.get(field), map2.get(field));
			}*/

			return false;
		}).collect(Collectors.toList());
	}

	public static List<String> equals(Object obj1, Object obj2, List<String> fields) {
		return fields.stream().filter(field -> !equals(obj1, obj2, field)).collect(Collectors.toList());
	}

	public static String sanitizeFilename(String filename) {
		return filename.replaceAll("[^\\w.]+", "_").replaceAll("__+", "_");
	}

	public static File zipFiles(List<File> files, File zipFile) {
		return zipFiles(files.stream().map(File::getAbsolutePath).collect(Collectors.toList()), zipFile.getAbsolutePath());
	}

	public static File zipFiles(List<String> files, String zipFilePath) {
		return zipFilesWithNames(files.stream().map((f) -> Pair.make(f, "")).collect(Collectors.toList()), zipFilePath);
	}

	// files => [{filePath, name}]
	public static File zipFilesWithNames(List<Pair<String, String>> files, String zipFilePath) {
		ZipOutputStream zout = null;
		FileOutputStream fout = null;
		File result = null;

		try {
			result = new File(zipFilePath);
			fout = new FileOutputStream(result);
			zout = new ZipOutputStream(fout);

			for (Pair<String, String> fd : files) {
				InputStream in = null;
				try {
					File file = new File(fd.first());
					in = new FileInputStream(file);

					String entryName = StringUtils.isBlank(fd.second()) ? file.getName() : fd.second();
					zout.putNextEntry(new ZipEntry(entryName));
					IOUtils.copy(in, zout);
				} finally {
					zout.closeEntry();
					IOUtils.closeQuietly(in);
				}
			}

			zout.finish();
			zout.flush();
			return result;
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		} finally {
			IOUtils.closeQuietly(fout);
			IOUtils.closeQuietly(zout);
		}
	}

	public static File zipInputStreams(List<Pair<InputStream, String>> files, String zipFilePath) {
		ZipOutputStream zout = null;
		FileOutputStream fout = null;
		File result = null;

		try {
			result = new File(zipFilePath);
			fout = new FileOutputStream(result);
			zout = new ZipOutputStream(fout);

			for (Pair<InputStream, String> fd : files) {
				try {
					zout.putNextEntry(new ZipEntry(fd.second()));
					IOUtils.copy(fd.first(), zout);
				} finally {
					zout.closeEntry();
					IOUtils.closeQuietly(fd.first());
				}
			}

			zout.finish();
			zout.flush();
			return result;
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		} finally {
			IOUtils.closeQuietly(fout);
			IOUtils.closeQuietly(zout);
		}
	}

	public static File gzip(File srcFile, File destFile) {
		FileInputStream fin = null;
		FileOutputStream fout = null;
		GZIPOutputStream gout = null;

		try {
			fin = new FileInputStream(srcFile);
			fout = new FileOutputStream(destFile);
			gout = new GZIPOutputStream(fout);

			int bytesRead = 0;
			byte[] bytes = new byte[4096];
			while ((bytesRead = fin.read(bytes)) != -1) {
				gout.write(bytes, 0, bytesRead);
			}

			return destFile;
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		} finally {
			IOUtils.closeQuietly(gout);
			IOUtils.closeQuietly(fout);
			IOUtils.closeQuietly(fin);
		}
	}

	public static File writeToCsv(String filePath, List<Map<String, String>> rows) {
		List<String> columns = rows.stream()
			.map(Map::keySet)
			.flatMap(Set::stream)
			.distinct()
			.collect(Collectors.toList());
		return writeToCsv(filePath, columns, rows);
	}

	public static File writeToCsv(String filePath, List<String> columns, List<Map<String, String>> rows) {
		FileOutputStream out = null;
		CsvFileWriter csvWriter = null;

		try {
			File result = new File(filePath);
			out = new FileOutputStream(result);
			csvWriter = CsvFileWriter.createCsvFileWriter(out);

			csvWriter.writeNext(columns.toArray(new String[0]));
			for (Map<String, String> row : rows) {
				csvWriter.writeNext(columns.stream().map(row::get).toArray(String[]::new));
			}

			csvWriter.flush();
			return result;
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		} finally {
			IOUtils.closeQuietly(out);
			IOUtils.closeQuietly(csvWriter);
		}
	}

	public static boolean isValidEmail(String emailId) {
		try {
			if (StringUtils.isBlank(emailId)) {
				return false;
			}

			new InternetAddress(emailId).validate();
			return true;
		} catch (AddressException ae) {
			return false;
		}
	}

	public static String getErrorMessage(Throwable t) {
		String error = t instanceof NullPointerException ? ExceptionUtils.getStackTrace(t) : ExceptionUtils.getMessage(t);
		if (StringUtils.isBlank(error)) {
			error = t.getClass().getName();
		}

		return error;
	}

	public static Object getProperty(Object object, String propName) {
		try {
			return PropertyUtils.getProperty(object, propName);
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		}
	}

	public static void setProperty(Object object, String propName, Object value) {
		try {
			PropertyUtils.setProperty(object, propName, value);
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		}
	}

	public static String getRemoteAddress(HttpServletRequest httpReq) {
		Enumeration<String> headers = httpReq.getHeaderNames();

		String result = null;
		while (headers.hasMoreElements()) {
			String header = headers.nextElement();
			if (header != null && !header.toLowerCase().equals("x-forwarded-for")) {
				continue;
			}

			String value = httpReq.getHeader(header);
			if (StringUtils.isNotBlank(value)) {
				String[] parts = value.split(",");
				result = parts[0].trim();
				break;
			}
		}

		if (StringUtils.isBlank(result)) {
			result = httpReq.getRemoteAddr();
		}

		return result;
	}

	public static File tarGzip(File sourceDir, File destDir) {
		return tarGzip(sourceDir, destDir, null);
	}

	public static File tarGzip(File sourceDir, File destDir, Predicate<File> filter) {
		if (!sourceDir.isDirectory()) {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "Source directory path is required");
		}

		String name = sourceDir.getName() + ".tar.gz";
		Path targetPath = Paths.get(destDir.getAbsolutePath(), name);
		try (
			OutputStream out = Files.newOutputStream(targetPath);
			BufferedOutputStream bout = new BufferedOutputStream(out);
			GzipCompressorOutputStream gzOut = new GzipCompressorOutputStream(bout);
			TarArchiveOutputStream tarOut = new TarArchiveOutputStream(gzOut);
		) {
			tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
			tarOut.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
			Path sourceDirPath = sourceDir.toPath();
			Files.walkFileTree(sourceDirPath, new SimpleFileVisitor<Path>() {
				public FileVisitResult visitFile(Path file, BasicFileAttributes fattrs) {
					if (fattrs.isSymbolicLink()) {
						return FileVisitResult.CONTINUE;
					}

					if (filter != null && !filter.test(file.toFile())) {
						return FileVisitResult.CONTINUE;
					}

					Path filePath = sourceDirPath.relativize(file);
					try {
						TarArchiveEntry tarEntry = new TarArchiveEntry(file.toFile(), filePath.toString());
						tarOut.putArchiveEntry(tarEntry);
						Files.copy(file, tarOut);
						tarOut.closeArchiveEntry();
					} catch (IOException ie) {
						throw new RuntimeException("Error creating tar.gz file: " + ie.getMessage(), ie);
					}

					return FileVisitResult.CONTINUE;
				}
			});

			return targetPath.toFile();
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		}
	}

	public static String byteCountToDisplaySize(long inputSize) {
		BigDecimal size = BigDecimal.valueOf(inputSize);
		final String displaySize;

		if (inputSize / FileUtils.ONE_EB > 0) {
			displaySize = divide(size, FileUtils.ONE_EB, " EB");
		} else if (inputSize / FileUtils.ONE_PB > 0) {
			displaySize = divide(size, FileUtils.ONE_PB, " PB");
		} else if (inputSize / FileUtils.ONE_TB > 0) {
			displaySize = divide(size, FileUtils.ONE_TB, " PB");
		} else if (inputSize / FileUtils.ONE_GB > 0) {
			displaySize = divide(size, FileUtils.ONE_GB, " GB");
		} else if (inputSize / FileUtils.ONE_MB > 0) {
			displaySize = divide(size, FileUtils.ONE_MB, " MB");
		} else if (inputSize / FileUtils.ONE_KB > 0) {
			displaySize = divide(size, FileUtils.ONE_KB, " KB");
		} else {
			displaySize = String.valueOf(size) + " bytes";
		}

		return displaySize;
	}

	private static Map<String, Object> getExtnAttrValues(BaseExtensionEntity obj) {
		if (obj.getExtension() != null) {
			return obj.getExtension().getAttrValues();
		}

		return Collections.emptyMap();
	}

	public static String normalizePhoneNumber(String phoneNumber) {
		if (StringUtils.isNotBlank(phoneNumber)) {
			// remove - (, ), -, whitespaces
			phoneNumber = phoneNumber.replaceAll("[-|\\s|)|(]+", "");
		}

		return phoneNumber;
	}

	private static boolean equals(Object obj1, Object obj2, String fieldName) {
		try {
			Object val1 = PropertyUtils.getProperty(obj1, fieldName);
			Object val2 = PropertyUtils.getProperty(obj2, fieldName);
			return equals(val1, val2);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static boolean equals(Object val1, Object val2) {
		if (val1 instanceof Collection || val2 instanceof Collection) {
			return equalCollections((Collection) val1, (Collection) val2);
		} else if (val1 instanceof Map || val2 instanceof Map) {
			return equalMaps((Map) val1, (Map) val2);
		} else if (val1 instanceof Date) {
			return DateUtils.isSameDay((Date) val1, (Date) val2);
		} else if (val1 == val2) {
			return true;
		} else if ((val1 != null && val2 == null) || val1 == null) {
			return false;
		} else if (val1 instanceof BaseEntity) {
			return ((BaseEntity) val1).sameAs(val2);
		} else {
			//
			// if-else is hack for DE date fields in bulk import
			//
			if (val1 instanceof String && val2 instanceof Long) {
				val2 = val2.toString();
			} else if (val1 instanceof Long && val2 instanceof String) {
				val1 = val1.toString();
			}

			return val1.equals(val2);
		}
	}

	private static boolean equalCollections(Collection<?> coll1, Collection<?> coll2) {
		if (coll1 == coll2) {
			return true;
		} else if (coll1 == null) {
			return coll2.isEmpty();
		} else if (coll2 == null) {
			return coll1.isEmpty();
		} else if (coll1.isEmpty() && coll2.isEmpty()) {
			return true;
		} else if (coll1.size() != coll2.size()) {
			return false;
		}

		return containsAll(coll1, coll2);
	}

	private static boolean equalMaps(Map<?, ?> map1, Map<?, ?> map2) {
		if (map1 == map2) {
			return true;
		} else if (map1 == null) {
			return map2.isEmpty();
		} else if (map2 == null) {
			return map1.isEmpty();
		} else if (map1.isEmpty() && map2.isEmpty()) {
			return true;
		} else if (map1.size() != map2.size()) {
			return false;
		}

		for (Map.Entry<?, ?> e : map1.entrySet()) {
			if (!equals(e.getValue(), map2.get(e.getKey()))) {
				return false;
			}
		}

		return true;
	}

	private static boolean containsAll(Collection<?> coll1, Collection<?> coll2) {
		for (Object e : coll2) {
			if (!contains(coll1, e)) {
				return false;
			}
		}

		return true;
	}

	private static boolean contains(Collection<?> coll, Object obj) {
		if (obj == null) {
			for (Object e : coll) {
				if (e == null) {
					return true;
				}
			}
		} else {
			for (Object e : coll) {
				if (equals(e, obj)) {
					return true;
				}
			}
		}

		return false;
	}

	public static boolean isValidDateFormat(String format) {
		boolean isValid = true;
		try {
			new SimpleDateFormat(format);
		} catch (IllegalArgumentException e) {
			isValid = false;
		}

		return isValid;
	}

	public static Integer getNoOfDays(Integer interval, IntervalUnit intervalUnit) {
		if (interval == null) {
			return null;
		}

		Integer noOfDays = null;
		switch (intervalUnit) {
			case DAYS:
				noOfDays = interval;
				break;

			case WEEKS:
				noOfDays = interval * 7;
				break;

			case MONTHS:
				noOfDays = interval * 30;
				break;

			case YEARS:
				noOfDays = interval * 365;
				break;
		}

		return noOfDays;
	}

	public static <T> Stream<T> stream(Collection<T> coll) {
		return Optional.ofNullable(coll).map(Collection::stream).orElse(Stream.empty());
	}

	public static <T> String join(Collection<T> coll, Function<T, String> mapper, String delimiter) {
		return Utility.nullSafeStream(coll).map(mapper).collect(Collectors.joining(delimiter));
	}

	public static boolean isExportOp() {
		return ExporterContextHolder.getInstance().isExportOp();
	}

	public static boolean isLinuxLike() {
		return !SystemUtils.IS_OS_WINDOWS;
	}

	public static boolean isRootUser() {
		InputStream in = null;
		try {
			if (!isLinuxLike()) {
				return false;
			}

			Process process = Runtime.getRuntime().exec("id -u");
			process.waitFor();
			in = process.getInputStream();
			String userId = IOUtils.toString(in, Charset.defaultCharset());
			return userId != null && userId.trim().equals("0");
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	public static String getNodeName() {
		Properties props = AppProperties.getInstance().getProperties();
		return props.getProperty("node.name", "Unknown");
	}

	public static boolean isOracle() {
		return getDbType().equals("oracle");
	}

	public static boolean isMySQL() {
		return getDbType().equals("mysql");
	}

	private static Integer getPeriodBetween(ChronoUnit unit, Date from, Date to) {
		if (from == null) {
			return null;
		}

		//
		// to ensure "from" date is of type java.util.Date and not java.sql.Date
		// java.sql.Date does not have toInstant() method
		//
		from = new Date(from.getTime());

		LocalDate startDt = LocalDate.from(from.toInstant().atZone(ZoneId.systemDefault()));
		LocalDate endDt = LocalDate.now();
		if (to != null) {
			to = new Date(to.getTime());
			endDt = LocalDate.from(to.toInstant().atZone(ZoneId.systemDefault()));
		}

		return Math.toIntExact(unit.between(startDt, endDt));
	}

	private static SecretKey getSecretKey() {
		return getSecretKey(null);
	}

	private static SecretKey getSecretKey(Connection conn) {
		if (secretKey != null) {
			return secretKey;
		}

		synchronized (Utility.class) {
			if (secretKey != null) {
				return secretKey;
			}


			File dataDir = new File(getDataDir(conn));
			try {
				String canonicalPath = dataDir.getCanonicalPath();
				String absolutePath  = dataDir.getAbsolutePath();
				if (!StringUtils.equals(canonicalPath, absolutePath)) {
					throw OpenSpecimenException.userError(
						CommonErrorCode.INVALID_INPUT,
						String.format("Data directory has path traversal. Always use absolute path. Canonical: %s; Absolute: %s;", canonicalPath, absolutePath));
				}
			} catch (IOException ioe) {
				throw new RuntimeException("Error checking whether data directory path is absolute or not", ioe);
			}

			Path secretKeyPath = dataDir.toPath().resolve("secret.key");
			File file = secretKeyPath.toFile();
			InputStream fin = null;
			try {
				if (!file.exists()) {
					fin = getResourceInputStream("/com/krishagni/catissueplus/core/secret.key");
				} else {
					fin = new FileInputStream(file);
				}

				List<String> secretSpecs = IOUtils.readLines(fin, Charset.defaultCharset());
				secretKey = new SecretKeySpec(secretSpecs.get(0).getBytes(), secretSpecs.get(1));
				algorithm = secretSpecs.get(1);
			} catch (Exception e) {
				throw OpenSpecimenException.serverError(e);
			}
		}

		return secretKey;
	}

	private static String getAlgorithm() {
		return getAlgorithm(null);
	}

	private static String getAlgorithm(Connection conn) {
		SecretKey key = getSecretKey(conn);
		if (key != null) {
			return algorithm;
		}

		return null;
	}

	private static String getDataDir(Connection conn) {
		if (conn != null) {
			try (
				PreparedStatement p = conn.prepareStatement(GET_DATA_DIR);
				ResultSet rs = p.executeQuery();
			) {
				String dataDir = null;
				if (rs.next()) {
					dataDir = rs.getString(1);
				}

				if (StringUtils.isBlank(dataDir)) {
					Properties appProps = OpenSpecimenAppCtxProvider.getBean(APP_PROPS);
					dataDir = appProps.getProperty("app.data_dir");
					if (StringUtils.isBlank(dataDir)) {
						dataDir = ".";
					}
				}

				return dataDir;
			} catch (Exception e) {
				throw new RuntimeException("Error obtaining data directory: " + e.getMessage(), e);
			}
		} else {
			ConfigurationService cfgSvc = OpenSpecimenAppCtxProvider.getBean(CFG_SVC_BEAN);
			return cfgSvc.getDataDir();
		}
	}

	private static String divide(BigDecimal dividend, long divisor, String unit) {
		return dividend.divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP) + unit;
	}

	private static String getDbType() {
		return AppProperties.getInstance().getProperties()
			.getProperty("database.type", "mysql").toLowerCase();
	}

	private static MediaType getMediaType(InputStream in) {
		try {
			Metadata metadata = new Metadata();
			return getTikaConfig().getDetector().detect(TikaInputStream.get(in), metadata);
		} catch (Exception e) {
			throw OpenSpecimenException.userError(CommonErrorCode.CONTENT_DETECT_FAILED, e.getLocalizedMessage());
		}
	}

	private static TikaConfig getTikaConfig() {
		try {
			if (tikaConfig == null) {
				synchronized (Utility.class) {
					if (tikaConfig == null) {
						tikaConfig = new TikaConfig();
					}
				}
			}

			return tikaConfig;
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(CommonErrorCode.SERVER_ERROR, "Error initialising the configuration for content detection");
		}
	}

	private static final String GET_DATA_DIR =
		"select " +
			"s.value " +
		"from " +
			"os_cfg_settings s " +
			"inner join os_cfg_props p on p.identifier = s.property_id " +
			"inner join os_modules m on m.identifier = p.module_id " +
		"where " +
			"m.name = 'common' and " +
			"p.name = 'data_dir' and " +
			"s.activity_status = 'Active'";

	private static final String APP_PROPS    = "appProps";

	private static final String CFG_SVC_BEAN = "cfgSvc";
}
