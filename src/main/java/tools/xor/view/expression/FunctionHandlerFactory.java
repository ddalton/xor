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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.FunctionType;
import tools.xor.util.ClassUtil;


public class FunctionHandlerFactory
{
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	private static final Map<String, Class> FUNCTION_MAP  = new HashMap<String, Class>() {
		{
			put(FunctionHandler.ILIKE,   IlikeFunctionHandler.class);
			put(FunctionHandler.IN,      InFunctionHandler.class);
			put(FunctionHandler.EQUAL,   EqualFunctionHandler.class);
			put(FunctionHandler.GE,      GeFunctionHandler.class);
			put(FunctionHandler.GT,      GtFunctionHandler.class);
			put(FunctionHandler.LE,      LeFunctionHandler.class);
			put(FunctionHandler.LT,      LtFunctionHandler.class);
			put(FunctionHandler.NE,      NeFunctionHandler.class);
			put(FunctionHandler.BETWEEN, BetweenFunctionHandler.class);
			put(FunctionHandler.NULL,    NullFunctionHandler.class);
			put(FunctionHandler.NOTNULL, NotNullFunctionHandler.class);
		}
	};

	private static final Map<FunctionType, Class> HANDLER_MAP  = new HashMap<FunctionType, Class>() {
		{
			put(FunctionType.FREESTYLE,  FreestyleHandler.class);
			put(FunctionType.ASC,        AscHandler.class);
			put(FunctionType.DESC,       DescHandler.class);
			put(FunctionType.ALIAS,      AliasHandler.class);
			put(FunctionType.SKIP,       SkipHandler.class);
			put(FunctionType.INCLUDE,    IncludeHandler.class);
		}
	};

	public static FunctionHandler getFunctionHandler (FunctionType type, String name) {

		FunctionHandler result = null;
		Class clazz = null;

		if(type == FunctionType.COMPARISON) {
			if(name == null) {
				throw new IllegalArgumentException("A comparison function should have a name.");
			}
			clazz = FUNCTION_MAP.get(name.toUpperCase());
		} else {
			clazz = HANDLER_MAP.get(type);
		}

		try {
			if (clazz != null) {
				result = (FunctionHandler)clazz.newInstance();
			} else {
				logger.error("Unable to find Filter handler for type: " + type + " and name: " + name);
			}
		}
		catch (Exception e) {
			throw ClassUtil.wrapRun(e);
		}

		return result;
	}
}
