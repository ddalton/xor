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

package tools.xor.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import tools.xor.Property;
import tools.xor.SimpleType;
import tools.xor.Type;
import tools.xor.service.Shape;
import tools.xor.util.DFAtoRE.Expression;
import tools.xor.util.DFAtoRE.LiteralExpression;
import tools.xor.util.DFAtoRE.UnionExpression;

public class AggregatePropertyPaths {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());	

	private static Map<Type, Set<String>> aggregatePaths = new ConcurrentHashMap<Type, Set<String>>();
	private static Map<Type, Set<String>> basePaths = new ConcurrentHashMap<Type, Set<String>>();   

	private static boolean isSimpleProperty(Property property) {
		return  property != null && 
				!property.isMany() && 
				SimpleType.class.isAssignableFrom(property.getType().getClass());
	}
	
	public static Set<String> enumerate(Type aggregateType, Shape shape) {
		Set<String> paths = aggregatePaths.get(aggregateType);

		if(!aggregatePaths.containsKey(aggregateType)) { 
			DFAtoRE dfaRE = new DFAtoRE(aggregateType, shape);
			Map<State, Expression> expressions = dfaRE.getRegEx();

			paths  = new HashSet<String>();
			for(Map.Entry<State, Expression> entry: expressions.entrySet()) {
				Type type = entry.getKey().getType();
				UnionExpression ue = null;
				for(Property childProperty: type.getProperties()) {
					if(isSimpleProperty(childProperty)) {
						if(ue == null) {
							ue = new UnionExpression(new LiteralExpression(childProperty.getName()));
						} else {
							ue.addAlternate(new LiteralExpression(childProperty.getName()));
						}
						/*
						for(String prefix: getExpression(entry.getValue() )) {
							String path = prefix.concat(childProperty.getName());
							paths.add(path);
						}
						*/
					}
				}
				if(ue != null) {
					paths.add(entry.getValue() + ue.toString());
				}
			}

			aggregatePaths.put(aggregateType, paths);
		}

		return Collections.unmodifiableSet(paths);
	}
	
	public static Set<String> enumerateBase(Type aggregateType) {
		Set<String> paths = basePaths.get(aggregateType);
		
		if(!basePaths.containsKey(aggregateType)) {
			paths  = new HashSet<String>();
			for(Property property: aggregateType.getProperties()) {
				if(isSimpleProperty(property)) {
					paths.add(property.getName());
				}
			}
			basePaths.put(aggregateType, paths);
		}
		
		return Collections.unmodifiableSet(paths);
	}

	private static Set<String> getExpression(Expression exp) {
		Set<String> result = new HashSet<String>();

		if(UnionExpression.class.isAssignableFrom(exp.getClass())) {
			UnionExpression unionExp = (UnionExpression) exp;
			for(Expression child: unionExp.getChildren()) {
				String prefix = child.toString();
				if(prefix.equals(LiteralExpression.EMPTY_STRING.toString()))
					prefix = "";
				result.add(prefix);
			}
		} else {
			String prefix = exp.toString();
			if(prefix.equals(LiteralExpression.EMPTY_STRING.toString()))
				prefix = "";
			result.add(prefix);
		}

		return result;
	}
}
