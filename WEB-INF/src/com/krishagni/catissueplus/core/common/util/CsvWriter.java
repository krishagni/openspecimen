package com.krishagni.catissueplus.core.common.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public interface CsvWriter extends Closeable {
	public void writeAll(List<String[]> allLines);
	
	public void writeNext(String[] nextLine);
	
	public void flush();
}
