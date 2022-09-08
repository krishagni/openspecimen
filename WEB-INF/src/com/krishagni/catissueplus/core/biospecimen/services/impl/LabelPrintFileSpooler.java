package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.InitializingBean;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.domain.LabelPrintFileItem;
import com.krishagni.catissueplus.core.common.util.LogUtil;

public class LabelPrintFileSpooler implements InitializingBean {
    private static final LogUtil logger = LogUtil.getLogger(LabelPrintFileSpooler.class);

    private BlockingQueue<Long> queuedJobs = new LinkedBlockingQueue<>();

    private DaoFactory daoFactory;

    public void setDaoFactory(DaoFactory daoFactory) {
        this.daoFactory = daoFactory;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Executors.newSingleThreadExecutor().submit(
            () -> {
                while (true) {
                    try {
                        Long jobId = queuedJobs.poll(60, TimeUnit.MINUTES);
                        if (jobId == null) {
                            deleteStaleItems();
                        } else {
                            generateLabelFiles(jobId);
                        }
                    } catch (Throwable t) {
                        logger.error("Error encountered in label print files spooler", t);
                    }
                }
            }
        );
    }

    public void queueJob(Long jobId) {
        queuedJobs.add(jobId);
    }

    @PlusTransactional
    private void generateLabelFiles(Long jobId) {
        try {
            boolean hasItems = true;
            int startAt = 0, maxItems = 10;
            while (hasItems) {
                List<LabelPrintFileItem> fileItems = daoFactory.getLabelPrintJobDao().getPrintFileItems(jobId, startAt, maxItems);
                startAt += maxItems;
                if (fileItems.size() < maxItems) {
                    hasItems = false;
                }

                generateLabelFiles(fileItems);
            }
        } catch (Exception e) {
            logger.error("Error generating print label files", e);
        } finally {
            deleteItems(jobId);
        }
    }

    private void generateLabelFiles(List<LabelPrintFileItem> fileItems)
    throws Exception {
        for (LabelPrintFileItem fileItem : fileItems) {
            List<Map<String, String>> labels = new ObjectMapper().readValue(fileItem.getContent(), new TypeReference<>() {});
            for (Map<String, String> label : labels) {
                FileUtils.write(new File(label.get("file")), label.get("content"), Charset.defaultCharset());
            }
        }
    }

    @PlusTransactional
    private void deleteStaleItems() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, -24);
        daoFactory.getLabelPrintJobDao().deletePrintFileItemsOlderThan(cal.getTime());
    }

    private void deleteItems(Long jobId) {
        daoFactory.getLabelPrintJobDao().deletePrintFileItems(jobId);
    }
}
