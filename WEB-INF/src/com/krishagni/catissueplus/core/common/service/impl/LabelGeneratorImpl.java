package com.krishagni.catissueplus.core.common.service.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.LabelTmplTokenRegistrar;
import com.krishagni.catissueplus.core.common.service.LabelGenerator;
import com.krishagni.catissueplus.core.common.util.Utility;

public class LabelGeneratorImpl implements LabelGenerator {
	private LabelTmplTokenRegistrar tokenRegistrar;
	
	//
	// %token1%-%token2%
	//
	private Pattern labelPattern = Pattern.compile("%(.+?)%");

	public LabelTmplTokenRegistrar getTokenRegistrar() {
		return tokenRegistrar;
	}

	public void setTokenRegistrar(LabelTmplTokenRegistrar tokenRegistrar) {
		this.tokenRegistrar = tokenRegistrar;
	}

	@Override
	public boolean isValidLabelTmpl(String labelTmpl) {
		boolean valid = true, haveTokens = false;
		
		Matcher matcher = labelPattern.matcher(labelTmpl);		
		while (matcher.find()) {
			haveTokens = true;

			Pair<String, String[]> tokenArgs = getTokenNameArgs(matcher.group(1));
			LabelTmplToken token = tokenRegistrar.getToken(tokenArgs.first());
			if (token == null || !token.areArgsValid(tokenArgs.second())) {
				valid = false;
				break;
			}			
		}
		
		return valid && haveTokens;
	}	
	
	@Override
	public String generateLabel(String labelTmpl, Object object) { 
		Matcher matcher = labelPattern.matcher(labelTmpl);
		
		StringBuilder result = new StringBuilder();
		int lastIdx = 0;
		boolean nextFreeTextAppend = true;

		while (matcher.find()) {
			Pair<String, String[]> tokenArgs = getTokenNameArgs(matcher.group(1));

			LabelTmplToken token = tokenRegistrar.getToken(tokenArgs.first());
			String replacement = null;
			if (token != null) {
				replacement = token.getReplacement(object, tokenArgs.second());
			}

			if (replacement == null) {
				replacement = matcher.group(0);
			}
			
			if (StringUtils.isNotEmpty(replacement)) {
				if (nextFreeTextAppend) {
					result.append(labelTmpl.substring(lastIdx, matcher.start()));
				}

				if (!replacement.equals(LabelTmplToken.EMPTY_VALUE)) {
					result.append(replacement);
				}

				nextFreeTextAppend = true;
			} else {
				nextFreeTextAppend = false;
			}

			lastIdx = matcher.end();
		}

		if (nextFreeTextAppend) {
			result.append(labelTmpl.substring(lastIdx));
		}

		return result.toString();
	}
	
	@Override
	public boolean validate(String labelTmpl, Object object, String label) {
		Matcher matcher = labelPattern.matcher(labelTmpl);		
		int lastTmplIdx = 0, lastLabelIdx = 0;
		
		while (matcher.find()) {
			String tmplStr = labelTmpl.substring(lastTmplIdx, matcher.start());
			if (!matchesTmplStr(label, lastLabelIdx, tmplStr)) {
				return false;
			}
			
			lastLabelIdx += tmplStr.length();
			
			Pair<String, String[]> tokenArgs = getTokenNameArgs(matcher.group(1));
			LabelTmplToken token = tokenRegistrar.getToken(tokenArgs.first());
			if (token == null) {
				if (!matchesTmplStr(label, lastLabelIdx, matcher.group(0))) {
					return false;
				}
				
				lastLabelIdx += matcher.group(0).length();
			} else {
				int newLabelIdx = token.validate(object, label, lastLabelIdx, tokenArgs.second());
				if (newLabelIdx == lastLabelIdx) {
					return false;
				}
				
				lastLabelIdx = newLabelIdx;
			}
			
			lastTmplIdx = matcher.end();
		}
		
		if (lastTmplIdx == labelTmpl.length()) {
			if (lastLabelIdx == label.length()) {
				return true;
			} else {
				return false;
			}			
		} else {
			String tmplStr = labelTmpl.substring(lastTmplIdx);
			if (!matchesTmplStr(label, lastLabelIdx, tmplStr)) {
				return false;
			}
			
			lastLabelIdx += tmplStr.length();
			return lastLabelIdx == label.length();						
		}
	}
	
	private boolean matchesTmplStr(String label, int lastLabelIdx, String tmplStr) {
		int tmplStrLen = tmplStr.length();
		if (lastLabelIdx >= label.length() || (lastLabelIdx + tmplStrLen) > label.length()) {
			return false;
		}
		
		if (!tmplStr.equals(label.substring(lastLabelIdx, lastLabelIdx + tmplStrLen))) {
			return false;
		}
		
		return true;		
	}

	//
	// input is of type: token(arg1, arg2, ... argn) or token
	//
	//
	private Pair<String, String[]> getTokenNameArgs(String input) {
		input = input.trim();

		int openIdx = input.indexOf('(');
		if (openIdx == -1) {
			return Pair.make(input, null);
		}

		int closeIdx = input.lastIndexOf(')');
		if (closeIdx == -1 || openIdx > closeIdx) {
			return Pair.make(input, null);
		}

		String tokenName = input.substring(0, openIdx);
		String[] args = Utility.csvToStringList(input.substring(openIdx + 1, closeIdx)).toArray(new String[0]);
		return Pair.make(tokenName, args);
	}
}
