package com.krishagni.catissueplus.core.common;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class ListenAttributeChangesInterceptor {
	@Pointcut("within(@com.krishagni.catissueplus.core.common.ListenAttributeChanges *)")
	public void annotatedWithListenAttributeChanges() {}
	
	@Pointcut("execution(public * *(..))")
	public void publicMethod() {}

	@Pointcut("publicMethod() && annotatedWithListenAttributeChanges()")
	public void publicMethodInsideListenAttributeChanges() {}	
	
	@Around("publicMethodInsideListenAttributeChanges()")
	public Object startTransaction(final ProceedingJoinPoint pjp) throws Throwable {		
		Object result = pjp.proceed();

		Object obj = pjp.getTarget();
		if (obj instanceof AttributeModifiedSupport) {
			AttributeModifiedSupport ams = (AttributeModifiedSupport)obj;
			String methodName = pjp.getSignature().getName();
			if (methodName.startsWith("set")) {
				String attr = StringUtils.uncapitalize(methodName.substring(3));
				ams.attrModified(attr);
			}
		}
		
		return result;
	}		
}
