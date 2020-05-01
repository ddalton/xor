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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.SimpleType;
import tools.xor.Type;
import tools.xor.service.Shape;
import tools.xor.util.DFAtoRE.Expression;
import tools.xor.util.DFAtoRE.LiteralExpression;
import tools.xor.util.DFAtoRE.UnionExpression;

public class AggregatePropertyPaths {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());	

	private static Map<Type, Set<String>> aggregatePaths = new ConcurrentHashMap<>();
	private static Map<Type, Set<String>> basePaths = new ConcurrentHashMap<>();
	private static Map<Type, Set<String>> migratePaths = new ConcurrentHashMap<>();

	private static boolean isSimpleProperty(Property property) {
		return  property != null && 
				!property.isMany() && 
				SimpleType.class.isAssignableFrom(property.getType().getClass());
	}
	
	public static Set<String> enumerateRegEx (Type aggregateType, Shape shape) {
		Set<String> paths = aggregatePaths.get(aggregateType);

		if(!aggregatePaths.containsKey(aggregateType)) {
			DFAtoRE dfaRE = new DFAtoRE(aggregateType, shape);
			Map<State, Expression> expressions = dfaRE.getRegEx();

			paths  = new HashSet<>();
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
			paths  = new HashSet<>();
			for(Property property: aggregateType.getProperties()) {
				if(isPartofBase(property)) {
					paths.add(property.getName());
				} else if ( property.getType() instanceof EntityType && ((EntityType)property.getType()).isEmbedded()) {
					if(includeEmbedded) {
						List embeddedPaths = getEmbeddedPaths(aggregateType, property);
						paths.addAll(embeddedPaths);
					}
				}
			}
			basePaths.put(aggregateType, paths);
		}
		
		return Collections.unmodifiableSet(paths);
	}

	private static List getEmbeddedPaths(Type aggregateType, Property property) {
		List embeddedPaths = new ArrayList();
		for (String embeddedPath : property.expand(new HashSet<Type>())) {
			if (!embeddedPath.startsWith(Constants.XOR.IDREF)) {
				Property embeddedProperty = aggregateType.getProperty(embeddedPath);
				if(isPartofBase(embeddedProperty)) {
					embeddedPaths.add(embeddedPath);
				}
			}
		}

		return embeddedPaths;
	}

	public static Set<String> enumerateRelationship(Type aggregateType, Property property) {
		/*
		 *  Handle 3 cases
		 *  1. Simple types
		 *     e.g., SELECT u.id, u.EmailAddresses FROM User u
		 *     Here EmailAddresses is a collection of String values
		  *
		 *  2. Embedded types
		 *     e.g., SELECT u.id, u.Address.Street, u.Address.Zipcode FROM User u
		 *     Here Address is an embedded object
		 *
		 *  3. Entity types
		 *     e.g., SELECT u.id, u.Groups.id FROM User u
		 */
		Set<String> paths = new HashSet<>();

		if( !(aggregateType instanceof EntityType) ){
			throw new RuntimeException("Relationships can only be defined on Entities");
		}

		EntityType entityType = (EntityType) aggregateType;
		// If the entityType is an embedded type, that could be problematic
		// But we don't raise an exception as it depends on the persistence provider
		// on whether they store certain types of embedded entities in a separate table
		// In such a case, the persistence provider should provide a way to unique identify
		// through the identifier property

		// First add the id of the Aggregate
		paths.add(entityType.getIdentifierProperty().getName());

		// Case 1 - Collection of simple type values
		Type elementType = ((ExtendedProperty)property).getElementType();
		if( elementType.isDataType()) {
			paths.add(property.getName());
		}

		// Case 2 - Collection of embedded type instances
		else if ( (elementType instanceof EntityType) && ((EntityType)elementType).isEmbedded() ) {
			paths.addAll(getEmbeddedPaths(aggregateType, property));
		}

		// Case 3 - Collection of entities
		else if (elementType instanceof EntityType) {
			paths.add(
				property.getName() + Settings.PATH_DELIMITER
					+ ((EntityType)elementType).getIdentifierProperty().getName());
		}

		return paths;
	}

	/**
	 * The properties needed for migration.
	 * For migration, we include:
	 * 1. all simple properties
	 * 2. single valued embedded relationships.
	 * 3. Natural key of any required entity relationships
	 *
	 * @param aggregateType whose migrate view we are configuring
	 * @return the set of properties needed for migration for that aggregateType
	 */
	public static Set<String> enumerateMigrate (Type aggregateType)
	{
		Set<String> paths = migratePaths.get(aggregateType);

		if (!migratePaths.containsKey(aggregateType)) {
			paths = new HashSet<>();
			for (Property property : aggregateType.getProperties()) {
				if (isPartofBase(property)) {
					paths.addAll(property.expandMigrate(new HashSet<>()));
				}
				else if (property.getType() instanceof EntityType
					&& ((EntityType)property.getType()).isEmbedded() && !property.isMany()) {
					paths.addAll(property.expandMigrate(new HashSet<>()));
				}
			}
			// Make sure to retrieve the source surrogate key value, as this will be saved
			// with the corresponding target surrogate key value to help in building foreign key
			// relationships
			if(((EntityType) aggregateType).getIdentifierProperty() != null) {
				paths.add(((EntityType) aggregateType).getIdentifierProperty().getName());
			}

			migratePaths.put(aggregateType, paths);
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
			EntityType entityType = (EntityType)aggregateType;
			while(entityType != null) {
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
				entityType = entityType.getParentType();
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
