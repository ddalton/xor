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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
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

	/**
	 * Are simple properties form the Base view.
	 * Any EntityType properties including required are not part of the view and needs to be
	 * explicitly specified. The reason for this restriction is that all the required properties
	 * are not known before hand due to inheritance of EntityType. This is needed when
	 * creating an entity based on Base view.
	 *
	 * @param aggregateType whose base view we are configuring
	 * @return set of properties depicting the base view
	 */
	public static Set<String> enumerateBase(Type aggregateType) {
		Set<String> paths = basePaths.get(aggregateType);

		boolean includeEmbedded = false;
		if (ApplicationConfiguration.config().containsKey(Constants.Config.INCLUDE_EMBEDDED)
			&& ApplicationConfiguration.config().getBoolean(Constants.Config.INCLUDE_EMBEDDED)) {
			includeEmbedded = true;
		}
		
		if(!basePaths.containsKey(aggregateType)) {
			paths  = new HashSet<String>();
			for(Property property: aggregateType.getProperties()) {
				if(isPartofBase(property)) {
					paths.add(property.getName());
				} else if ( property.getType() instanceof EntityType && ((EntityType)property.getType()).isEmbedded()) {
					if(includeEmbedded) {
						List embeddedPaths = new ArrayList();
						for (String embeddedPath : property.expand(new HashSet<Type>())) {
							if (!embeddedPath.startsWith(Constants.XOR.IDREF)) {
								Property embeddedProperty = aggregateType.getProperty(embeddedPath);
								if(isPartofBase(embeddedProperty)) {
									embeddedPaths.add(embeddedPath);
								}
							}
						}
						paths.addAll(embeddedPaths);
					}
				}
			}
			basePaths.put(aggregateType, paths);
		}
		
		return Collections.unmodifiableSet(paths);
	}

	private static boolean isPartofBase(Property property) {
		return isSimpleProperty(property) || !property.isNullable();
	}

	/**
	 * The fields that are needed to identify an entity.
	 * There fields might not be sufficient to initialize an entity. For example, required fields.
	 * @param aggregateType type whose minimum fields needed to identify an entity
	 * @return minimum fields that constitute a reference
	 */
	public static Set<String> enumerateRef(Type aggregateType) {
		Set<String> paths = new HashSet<>();

		if(aggregateType instanceof EntityType) {
			EntityType entityType = (EntityType) aggregateType;
			paths.add(entityType.getIdentifierProperty().getName());

			if(entityType.getNaturalKey() != null) {
				paths.addAll(entityType.getExpandedNaturalKey());
			}
		}

		return Collections.unmodifiableSet(paths);
	}

	/**
	 * Lists are the required properties of simple types.
	 * The user will need to explicitly define the types of EntityType, especially if it contains
	 * subtypes.
	 *
	 * @param aggregateType whose required simple properties are enumerated
	 * @return the result
	 */
	public static Set<String> enumerateRequiredSimple(Type aggregateType) {
		Set<String> paths = new HashSet<>(enumerateRef(aggregateType));

		if (aggregateType instanceof EntityType) {
			for (Property property : aggregateType.getProperties()) {
				if (isSimpleProperty(property) && !property.isNullable()) {
					paths.add(property.getName());
				}
				else if (!property.isNullable()) {
					logger.warn(
						"Not adding an required property that refers to an entity type, manually set the scope for: "
							+ aggregateType.getName() + "#" + property.getName());
				}
			}
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
