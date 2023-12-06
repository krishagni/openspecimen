package com.krishagni.catissueplus.core.common.util;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;

public class ExpressionUtil {
	private static ExpressionUtil instance = new ExpressionUtil();

	private SpelExpressionParser parser = new SpelExpressionParser();

	private Map<String, Method> methods = new HashMap<>();

	public static ExpressionUtil getInstance() {
		return instance;
	}

	@SuppressWarnings("unchecked")
	public <T> T evaluate(String exprStr, Map<String, Object> variables) {
		try {
			StandardEvaluationContext ctxt = new StandardEvaluationContext();
			addMethods(ctxt);
			ctxt.setVariables(variables);
			ctxt.setVariable("collFns", new CollectionFunctions(variables));

			SpelExpression expr = parse(exprStr);
			return (T) expr.getValue(ctxt);
		} catch (Exception e) {
			String error   = (e instanceof  NullPointerException ? "Null object access" : ExceptionUtils.getRootCauseMessage(e));
			throw OpenSpecimenException.userError(CommonErrorCode.EVAL_EXPR_ERROR, exprStr, error);
		}
	}

	private SpelExpression parse(String exprStr) {
		return parser.parseRaw(exprStr);
	}

	private void addMethods(StandardEvaluationContext ctxt) {
		if (methods.isEmpty()) {
			methods = getMethods();
		}

		methods.forEach(ctxt::registerFunction);
	}

	private Map<String, Method> getMethods() {
		try {
			Map<String, Method> methods = new HashMap<>();
			methods.put("containsAny", CollectionUtils.class.getDeclaredMethod("containsAny", Collection.class, Collection.class));
			methods.put("yearsBetween", Utility.class.getDeclaredMethod("yearsBetween", Date.class, Date.class));
			methods.put("monthsBetween", Utility.class.getDeclaredMethod("monthsBetween", Date.class, Date.class));
			methods.put("daysBetween", Utility.class.getDeclaredMethod("daysBetween", Date.class, Date.class));
			methods.put("cmp", Utility.class.getDeclaredMethod("cmp", Date.class, Date.class));
			methods.put("formatDate", Utility.class.getDeclaredMethod("format", Date.class, String.class));
			methods.put("currentTime", Utility.class.getDeclaredMethod("currentTime"));
			return methods;
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		}
	}

	private static class CollectionFunctions {
		private Map<String, Object> ctxt;

		CollectionFunctions(Map<String, Object> ctxt) {
			this.ctxt = ctxt;
		}

		public boolean isEmpty(Collection<?> coll) {
			return coll == null || coll.isEmpty();
		}

		public boolean isNotEmpty(Collection<?> coll) {
			return !isEmpty(coll);
		}

		public boolean forEvery(Collection<?> coll, String elementName, String expression) {
			return match(coll, elementName, expression, true);
		}

		public boolean forSome(Collection<?> coll, String elementName, String expression) {
			return match(coll, elementName, expression, false);
		}

		public boolean forEvery(Iterable<?> coll, String elementName, String expression) {
			return match(coll, elementName, expression, true);
		}

		public boolean forSome(Iterable<?> coll, String elementName, String expression) {
			return match(coll, elementName, expression, false);
		}

		public String join(Collection<String> coll) {
			return Utility.nullSafeStream(coll).filter(StringUtils::isNotBlank).collect(Collectors.joining(", "));
		}

		private boolean match(Iterable<?> coll, String elementName, String expression, boolean allMatch) {
			Map<String, Object> localCtxt = new HashMap<>();
			if (ctxt != null) {
				localCtxt.putAll(ctxt);
			}

			if (coll == null) {
				return false;
			}

			boolean emptyCollection = true;
			boolean truth = allMatch;
			for (Object element : coll) {
				emptyCollection = false;
				localCtxt.put(elementName, element);
				truth = ExpressionUtil.getInstance().evaluate(expression, localCtxt);
				if ((allMatch && !truth) || (!allMatch && truth)) {
					break;
				}
			}

			if (emptyCollection) {
				return false;
			}

			return truth;
		}
	}
}
