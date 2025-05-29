package com.krishagni.catissueplus.core.common.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import au.com.bytecode.opencsv.CSVWriter;

public class CsvFileWriter implements CsvWriter {
	
	private CSVWriter csvWriter;

	public CsvFileWriter(CSVWriter csvWriter) {
		this.csvWriter = csvWriter;
	}
	
	public static CsvFileWriter createCsvFileWriter(OutputStream outputStream) {
		return createCsvFileWriter(new OutputStreamWriter(outputStream));
	}
	
	public static CsvFileWriter createCsvFileWriter(File csvFile) {
		try {
			return createCsvFileWriter(new FileWriter(csvFile));
		} catch (IOException e) {
			throw new CsvException("Error creating CSV file writer", e);
		}
	}
	
	public static CsvFileWriter createCsvFileWriter(Writer writer){
		return createCsvFileWriter(writer, Utility.getFieldSeparator(), CSVWriter.DEFAULT_QUOTE_CHARACTER);
	}
	
	public static CsvFileWriter createCsvFileWriter(Writer writer, char separator, char quotechar) {
		return createCsvFileWriter(writer, separator, quotechar, null);
	}

	public static CsvFileWriter createCsvFileWriter(Writer writer, char separator, char quotechar, String lineEnding) {
		if (StringUtils.isBlank(lineEnding)) {
			lineEnding = getLineEnding();
		}

		CSVWriter csvWriter = new CSVWriter(writer, separator, quotechar, lineEnding);
		return new CsvFileWriter(csvWriter);
	}
	
	@Override
	public void writeAll(List<String[]> allLines) {
		if (allLines == null) {
			return;
		}

		allLines.forEach(this::writeNext);
	}
	
	@Override
	public void writeNext(String[] nextLine) {
		if (nextLine == null) {
			return;
		}

		for (int i = 0; i < nextLine.length; ++i) {
			nextLine[i] = escapeUnsafeChars(StringUtils.trim(nextLine[i]));
		}

		csvWriter.writeNext(nextLine);
	}
	
	@Override
	public void flush() {
		try {
			csvWriter.flush();
		} catch (IOException e) {
			throw new CsvException("Error writing to the CSV file", e);
		}
	}
	
	@Override
	public void close() throws IOException {
		try {
			flush();
			csvWriter.close();
		} catch (IOException e) {
			throw new CsvException("Error closing CSVWriter", e);
		}
	}
	
	private static String getLineEnding() {
		String lineEnding = System.getProperty("line.separator");
		return StringUtils.isNotEmpty(lineEnding) ? lineEnding : DEFAULT_LINE_ENDING;
	}

	private String escapeUnsafeChars(String input) {
		if (input == null || input.isEmpty()) {
			return input;
		}

		//
		// Transform occurrences of \" to \\" in the output CSV
		//
		StringBuilder value = new StringBuilder();
		for (int i = 0; i < input.length(); ++i) {
			if (input.charAt(i) == '\\') {
				if ((i + 1) < input.length() && input.charAt(i + 1) == '"') {
					value.append('\\');
				}
			}

			value.append(input.charAt(i));
		}

		if (UNSAFE_CHARS.indexOf(value.charAt(0)) > -1) {
			value.insert(0, "'");
		} else if (NUMERIC_SIGNS.indexOf(value.charAt(0)) > -1) {
			try {
				new BigDecimal(value.toString());
			} catch (Exception e) {
				value.insert(0, "'");
			}
		}

		return value.toString();
	}
	
	private static final String DEFAULT_LINE_ENDING = "\n";

	private static final String UNSAFE_CHARS = ";=@";

	private static final String NUMERIC_SIGNS = "-+";
}
