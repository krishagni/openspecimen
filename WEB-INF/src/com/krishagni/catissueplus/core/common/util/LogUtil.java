package com.krishagni.catissueplus.core.common.util;

import java.net.URL;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;

public class LogUtil {
    private Logger logger;

    private LogUtil(Logger logger) {
        this.logger = logger;
    }

    public static LogUtil getLogger(Class<?> klass) {
        return new LogUtil(LogManager.getLogger(klass));
    }

    public static void setInfoLevel() {
        Configurator.setRootLevel(Level.INFO);
    }

    public static void configure(URL url) {
        try {
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            context.setConfigLocation(url.toURI());
        } catch (Exception e) {
            String error = Utility.getErrorMessage(e);
            throw new RuntimeException("Error configuring the logger. Error = " + error, e);
        }
    }

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public void debug(String message) {
        logger.debug(message);
    }

    public void debug(String message, Throwable t) {
        logger.debug(message, t);
    }

    public void debug(String message, Object... params) {
        logger.debug(message, params);
    }

    public void info(String message) {
        logger.info(message);
    }

    public void info(String message, Throwable t) {
        logger.info(message, t);
    }

    public void info(String message, Object... params) {
        logger.info(message, params);
    }

    public void warn(String message) {
        logger.warn(message);
    }

    public void warn(String message, Throwable t) {
        logger.warn(message, t);
    }

    public void warn(String message, Object... params) {
        logger.warn(message, params);
    }

    public void error(String message) {
        logger.error(message);
    }

    public void error(String message, Throwable t) {
        logger.error(message, t);
    }

    public void error(String message, Object... params) {
        logger.error(message, params);
    }
}
