package com.krishagni.catissueplus.core.common.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;


import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;

import au.com.bytecode.opencsv.CSVReader;

public class CsvMapReader {

	public static Map<String, String> getMap(String resourcePath) {
		if(StringUtils.isBlank(resourcePath)) {
			return Collections.emptyMap();
		}

		InputStream in = null;
		try {
			if (resourcePath.startsWith("classpath:")) {
				in = Utility.getResourceInputStream(resourcePath.substring(10));
			} else {
				in = new FileInputStream(resourcePath);
			}

			if (in == null) {
				throw OpenSpecimenException.userError(CommonErrorCode.FILE_NOT_FOUND, resourcePath);
			}

			return getMap(in);
		} catch (FileNotFoundException fe) {
			throw OpenSpecimenException.userError(CommonErrorCode.FILE_NOT_FOUND, resourcePath);
		} catch (Exception e) {
			logger.error("Error reading values from csv file: " + resourcePath, e);
			throw OpenSpecimenException.serverError(e);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	private static Map<String, String> getMap(InputStream in) {		
		Map<String, String> result = new HashMap<String, String>();
		CSVReader csvReader = null;
		try {
			csvReader = new CSVReader(new InputStreamReader(in));
			
			String[] columns = null;			
			while (true) {
				columns = csvReader.readNext();
				if (columns == null || columns.length != 2) {
					break;
				}
				
				if (columns[0].startsWith("#")) {
					continue;
				}
				
				result.put(columns[0], columns[1]);
			}
		} catch (Exception e) {
			logger.error("Error reading csv file", e);
			throw OpenSpecimenException.serverError(e);
		} finally {
			IOUtils.closeQuietly(csvReader);
		}

		return result;
	}
	
	private static final LogUtil logger = LogUtil.getLogger(CsvMapReader.class);
}
