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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tools.xor.util.ClassUtil;


public class ExpressionFactory {
	private static final String FUNCTION_NAME_PAT = "^(\\w+)\\s*\\(.*"; // ^(\w+)\s*\( - Get the name of the filter function

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

	public static AbstractFunctionExpression getExpression(String expression) {
		Class clazz = FUNCTION_MAP.get(getFilterFunction(expression).toUpperCase());
		AbstractFunctionExpression result;
		try {
			result = (AbstractFunctionExpression) clazz.newInstance();
		} catch (Exception e) {
			throw ClassUtil.wrapRun(e);
		}
		result.setExpression(expression);

		return result;
	}	

	public static String getFilterFunction(String expression) {
		Pattern pattern = Pattern.compile(FUNCTION_NAME_PAT); 
		Matcher matcher = pattern.matcher(expression);
		matcher.find();

		if(matcher.matches())
			return matcher.group(1);
		else
			throw new RuntimeException("Cannot find the filter function");

	}			
}
