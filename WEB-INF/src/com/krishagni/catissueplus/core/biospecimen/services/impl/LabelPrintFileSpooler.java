package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.domain.LabelPrintJobItem;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;

public class LabelPrintFileSpooler implements InitializingBean {
    private static final LogUtil logger = LogUtil.getLogger(LabelPrintFileSpooler.class);

    private final BlockingQueue<Long> queuedJobs = new LinkedBlockingQueue<>();

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
                        if (jobId != null) {
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
            logger.info("Generating print label files for the job ID = " + jobId);
            boolean hasItems = true;
            int startAt = 0, maxItems = 10, count = 0;
            while (hasItems) {
                List<LabelPrintJobItem> printItems = daoFactory.getLabelPrintJobDao().getPrintJobItems(jobId, startAt, maxItems);
                startAt += maxItems;
                if (printItems.size() < maxItems) {
                    hasItems = false;
                }

                count += generateLabelFiles(printItems);
            }

            logger.info("Generated " + count + " print label files for the job ID = " + jobId);
        } catch (Exception e) {
            logger.error("Error generating print label files for job ID = " + jobId, e);
        }
    }

    private int generateLabelFiles(List<LabelPrintJobItem> printItems)
    throws Exception {
        int count = 0;
        String defaultDir = null;
        for (LabelPrintJobItem item : printItems) {
            if (StringUtils.isBlank(item.getContent()) || (item.getCreateFile() != null && !Boolean.TRUE.equals(item.getCreateFile()))) {
                continue;
            }

            List<Map<String, String>> labels = new ObjectMapper().readValue(item.getContent(), new TypeReference<>() {});
            for (Map<String, String> label : labels) {
                String dir = label.get("dir");
                if (StringUtils.isBlank(dir) || dir.trim().equals("*")) {
                    if (StringUtils.isBlank(defaultDir)) {
                        defaultDir = new File(ConfigUtil.getInstance().getDataDir(), "print-labels").getCanonicalPath();
                    }

                    dir = defaultDir;
                }

                File output = new File(dir, label.get("file"));
                FileUtils.write(output, label.get("content"), Charset.defaultCharset());
                ++count;
            }
        }

        return count;
    }
}
