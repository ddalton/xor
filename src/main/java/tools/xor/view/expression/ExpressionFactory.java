/**
 * XOR, empowering Model Driven Architecture in J2EE applications
 *
 * Copyright (c) 2012, Dilip Dalton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations 
 * under the License.
 */

package tools.xor.view.expression;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tools.xor.util.ClassUtil;


public class ExpressionFactory {
	private static final String FUNCTION_NAME_PAT = "^(\\w+)\\s*\\(.*"; // Get the name of the filter function
	private static final String FIELD_NAME_PAT = "\\[\\s*([^\\s]*)\\s*\\]+"; // Get the name of the field name enclosed in [entity.id]

	private static final Map<String, Class> FUNCTION_MAP  = new HashMap<String, Class>() {
		{
			put("ILIKE",   IlikeFunctionExpression.class);
			put("IN",      InFunctionExpression.class);
			put("EQUAL",   EqualFunctionExpression.class);
			put("GE",      GeFunctionExpression.class);
			put("GT",      GtFunctionExpression.class);
			put("LE",      LeFunctionExpression.class);
			put("LT",      LtFunctionExpression.class);			
			put("ASC",     AscFunctionExpression.class);
			put("DESC",    DescFunctionExpression.class);
			put("BETWEEN", BetweenFunctionExpression.class);
		}
	};

	public static AbstractFunctionExpression getFunctionExpression(String expression) {
		String functionName = getFilterFunction(expression);

		AbstractFunctionExpression result = null;
		if(functionName == null) {
			result = new LiteralExpression();
		} else {
			Class clazz = FUNCTION_MAP.get(functionName.toUpperCase());
			try {
				if (clazz != null) {
					result = (AbstractFunctionExpression)clazz.newInstance();
				}
			}
			catch (Exception e) {
				throw ClassUtil.wrapRun(e);
			}
		}

		if(result != null) {
			result.setExpression(expression);
		}

		return result;
	}	

	public static String getFilterFunction(String expression) {
		final Pattern pattern = Pattern.compile(FUNCTION_NAME_PAT);
		final Matcher matcher = pattern.matcher(expression);
		matcher.find();

		if(matcher.matches()) {
			return matcher.group(1);
		}

		return null;
	}

	public static List<String> extractFields(String input) {
		final Pattern pattern = Pattern.compile(FIELD_NAME_PAT);
		final Matcher matcher = pattern.matcher(input);

		List<String> result = new LinkedList<>();
		while (matcher.find()) {
			System.out.println("Full match: " + matcher.group(0));
			for (int i = 1; i <= matcher.groupCount(); i++) {
				System.out.println("Group " + i + ": " + matcher.group(i));
				result.add(matcher.group(i));
			}
		}

		return result;
	}
}
