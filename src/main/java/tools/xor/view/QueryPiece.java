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

package tools.xor.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import tools.xor.AbstractBO;
import tools.xor.BusinessObject;
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.OpenType;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.service.AggregateManager;
import tools.xor.service.QueryCapability;
import tools.xor.util.ClassUtil;
import tools.xor.util.Vertex;
import tools.xor.view.QueryTree.QueryKey;

/**
 * Represents a portion of the user's request that can be satisfied by a single query.
 */
public class QueryPiece implements Vertex {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	private        Type                           aggregateType;
	private        Map<String, QueryProperty> viewPropertyByPath = new LinkedHashMap<String, QueryProperty>();
	private        Map<String, ColumnMeta>        augmentedAttributes;
	private        Map<String, String>            attributes = new HashMap<String, String>();
	private        String                         name;
	private        AggregateView                  aggregateView;
	private        boolean                        narrow;
	private        List<Filter>                   filters = new ArrayList<Filter>();
	
	// Related to constructing child Query Branches
	private        List<QueryPiece>                subBranches;  // split according to parallel collections.
	private        QueryPiece                      parent; // branch containing the parent step
	private        QueryPiece                      twig;         // attributes of simple type
	private        boolean                         collection;   // How many collections are under this branch	

	/**
	 * Used for manually creating the child query pieces
	 * @param parent query piece
	 * @param rootName anchored at a property and is the property name
	 * @param rootType type
	 */	
	public QueryPiece(QueryPiece parent, String rootName, Type rootType) {
		this.parent = parent;		
		this.name = rootName;
		this.aggregateType = rootType;
	}
	
	public QueryPiece(QueryKey viewKey, AggregateView contentView) {
		this.aggregateView = contentView;
		this.aggregateType = viewKey.type;
		this.narrow = viewKey.narrow;
		setName(viewKey.viewName);
	}	
	
    // ======================== START buildFlattened QueryTree	
	
	public void buildFlattened() {
		assert(this.aggregateType != null);

		// save the property alias before losing the order
		initAttributes(aggregateView.getAttributeList());

		initProperties();

		if(aggregateView.getChildren() == null || aggregateView.getChildren().size() == 0) {
			// Perform automatic division into multiple queries if necessary
			createBranches();
			consolidateBranches();
			for(QueryPiece child: subBranches) {
				child.setAggregateType(aggregateType);
				child.initProperties();
			}
		} else { // User has explicitly specified the queries needed to populate the view
			this.subBranches = new ArrayList<QueryPiece>();
			for(AggregateView child: aggregateView.getChildren()) {
				QueryPiece childQueryView = new QueryPiece(this, null, this.aggregateType);
				childQueryView.setContentView(child);
				if(aggregateView.isUnion())
					childQueryView.buildFlattened();
				else
					childQueryView.initProperties();
				this.subBranches.add(childQueryView);
			}
		}
	}
	
	private void createBranches() {
		Map<String, QueryPiece> newBranches = new HashMap<String, QueryPiece>();

		for(String attribute: attributes.keySet()) {
			String rootAnchorName = getRootAnchorName(attribute);
			if(rootAnchorName == null) {
				throw new RuntimeException(
					"The query attribute " + attribute + " of type " + aggregateType.getName()
						+ " should specifically refer to a data type and not a data object");
			}

			Property rootProperty = aggregateType.getProperty(rootAnchorName);
			if(rootProperty == null) {
				//logger.warn ("This type does not have a desired property: " + rootAnchorName);
				continue;
			}
			Type rootType = rootProperty.getType();
			if(rootProperty.isMany())
				rootType = ((ExtendedProperty)rootProperty).getElementType();			
			if(rootType.isDataType()) { // Do not create branches for simple types
				if(twig == null)
					twig = new QueryPiece(this, null, null);
				twig.addAttribute(attribute);
				continue;
			}

			QueryPiece branch = newBranches.get(rootAnchorName);
			if(branch == null) {
				// Create a new branch
				branch = new QueryPiece(this, rootAnchorName, rootType);
				if(rootProperty.isMany())
					branch.setHasCollection(true); // The number of collections in this branch is atleast 1 due to the root property.
				// There could be more collections due to the sub branches.
				newBranches.put(rootAnchorName, branch);
			}

			branch.addAttribute(attribute); // add actual attribute
		}

		// Recursively create branches
		for(QueryPiece branch: newBranches.values())
			branch.createBranches();

		this.subBranches = new ArrayList<QueryPiece>(newBranches.values());
	}
	
	private String getRootAnchorName(String attribute) {
		String anchorPath = getAnchorPath();
		if(attribute.length() <= anchorPath.length())
			return null;

		String relativeName = (anchorPath.length() > 0) ? attribute.substring(anchorPath.length() + Settings.PATH_DELIMITER.length()) : attribute;
		return QueryProperty.getRootName(relativeName);
	}
	
	public String getAnchorPath() {
		StringBuilder result = new StringBuilder(this.getParent() == null ? "" : (name == null ? "" : name));
		QueryPiece parentQueryView = getParent();

		while(parentQueryView != null) { 
			if(parentQueryView.getParent() == null)
				break;
			if(parentQueryView.getName() != null)
				result.insert(0, parentQueryView.getName() + Settings.PATH_DELIMITER);
			parentQueryView = parentQueryView.getParent();
		}

		return result.toString();
	}	
	
	private void consolidateBranches() {
		// First go to the leaf
		for(QueryPiece subBranch: subBranches) 
			subBranch.consolidateBranches();

		if(this.subBranches.size() == 0)
			return; // nothing to consolidate

		// If a branch has only one child then consolidate by clearing its child and bringing any parallel branches to the top
		if(this.subBranches.size() == 1) { // flatten by removing child
			// Check if the only child has parallel collections. If so, the child has to be replaced with the parallel collections
			QueryPiece child = this.subBranches.iterator().next();

			this.subBranches.clear();
			if(child.subBranches != null && child.subBranches.size() > 0) { 
				// update the collection count before consolidation
				this.setHasCollection(this.collection || child.hasCollection());
				this.subBranches.addAll(grandChildToChild(this, child));
			}
		}
		else {
			// Move up all the parallel grand child branches
			List<QueryPiece> flattenList = new ArrayList<QueryPiece>();
			nextChild: for(QueryPiece child: subBranches) {
				if(child.getSubBranches() != null) {
					if(child.getSubBranches().size() == 1)
						throw new RuntimeException("A child branch with 1 grandchild should have been consolided");
					if(child.getSubBranches().size() > 1) { // Flattened
						flattenList.addAll(grandChildToChild(this, child));
						continue nextChild;
					}
				}
				flattenList.add(child);
			}
			this.subBranches = flattenList;
			
			// If a branch has many children
			// First check if there are parallel collections	
			Set<QueryPiece> parallelCollectionBranches = new HashSet<QueryPiece>();
			Set<QueryPiece> nonCollectionBranches = new HashSet<QueryPiece>(); // branches that need to be merged
			for(QueryPiece child: subBranches) {
				this.setHasCollection(this.collection || child.hasCollection()); // Mark if it has any collection in its child sub-branches
				if(!child.hasCollection())
					nonCollectionBranches.add(child);
				else
					parallelCollectionBranches.add(child);
			}

			if(parallelCollectionBranches.size() > 1) {
				Iterator<QueryPiece> iter = nonCollectionBranches.iterator();
				// need to consolidate the parallel branches
				while(iter.hasNext())
					for(QueryPiece parallelBranch: parallelCollectionBranches)
						parallelBranch.merge(nonCollectionBranches.iterator().next());		

				subBranches.clear();
				subBranches.addAll(parallelCollectionBranches);
			} else
				subBranches.clear(); // the current branch is sufficient
		}

		if(twig != null)
			for(QueryPiece child: subBranches)
				child.merge(twig);
	}	
	
	private List<QueryPiece> grandChildToChild(QueryPiece branch, QueryPiece child) {
		List<QueryPiece> newChildren = new ArrayList<QueryPiece>();
		
		for(QueryPiece grandChild: child.getSubBranches()) {
			// NOTE: Name is irrelevant in merge. So we don't do anything about updating it.
			grandChild.setAggregateType(child.getAggregateType());
			grandChild.setParent(branch);
			newChildren.add(grandChild); // make the grandChild the child
		}
		
		return newChildren;
	}	
	
	private void merge(QueryPiece graftBranch) {
		if(graftBranch == null)
			return;

		Set<String> uniqueAttributes = new HashSet<String>(this.attributes.keySet());
		uniqueAttributes.addAll(graftBranch.attributes.keySet());
		this.initAttributes(new ArrayList<String>(uniqueAttributes));
	}	
	
    // ======================== END buildFlattened QueryTree
	
	public Map<String, ColumnMeta> getAugmentedAttributes() {
		return augmentedAttributes;
	}

	public QueryProperty getViewProperty(String propertyPath) {
		return viewPropertyByPath.get(propertyPath);
	}	
	
	public Object getQueryValue(Object[] queryResultRow, String path) {
		ColumnMeta columnMeta = getAugmentedAttributes().get(QueryProperty.qualifyProperty(path));
		if(columnMeta != null)
			return queryResultRow[columnMeta.getPosition()];
		else
			return null;
	}	

	public BusinessObject getQueryRoot(Object obj, BusinessObject entity) throws Exception {
		BusinessObject result = null;

		if(ClassUtil.getDimensionCount(obj) == 1) {
			Object[] queryRow = (Object[])obj;

			Object idValue = null;
			if(((EntityType)this.aggregateType).getIdentifierProperty() != null) {
				String idPropertyName = ((EntityType)this.aggregateType).getIdentifierProperty().getName();
				idValue = getQueryValue(queryRow, idPropertyName);
			}

			String entityName = (String) getQueryValue(queryRow, QueryProperty.ENTITYNAME_ATTRIBUTE);
			Type type = entity.getType();
			if(entityName != null) {
				// This is padded with space based on the largest type name if a CASE statement is used
				entityName = entityName.trim();
				type = entity.getObjectCreator().getDAS().getType(entityName);
			}

			// find and create data object
			if(idValue != null) {
				result = ((AbstractBO)entity).getBySurrogateKey(idValue, this.aggregateType);
			}
			if(result == null) {
				if(logger.isDebugEnabled()) {
					logger.debug("Creating instance with id: " + idValue + " and type: " + entity.getType().getName() + ", entityName: |" + entityName + "|");
				}
				result = entity.createDataObject(idValue, type);
			}
		}

		return result;
	}	
	
	public void normalize(BusinessObject root, Object[] queryResultRow) throws Exception {
		Map<Integer, ColumnMeta> metaMap = new HashMap<Integer, ColumnMeta>();
		for(ColumnMeta columnMeta: augmentedAttributes.values()) {
			metaMap.put(columnMeta.getPosition(), columnMeta);
		}

		Map<String, Object> propertyResult = new HashMap<String, Object>();
		for(int i = 0; i < queryResultRow.length; i++) {
			ColumnMeta columnMeta = metaMap.get(i);
			propertyResult.put(columnMeta.getAttributePath(), queryResultRow[i]);
		}

		for(String propertyPath: propertyResult.keySet()) {
			ColumnMeta meta = augmentedAttributes.get(propertyPath);
			if(meta.getViewProperty().isDynamic())
				continue;

			// Set the value and create any intermediate objects if necessary
			root.set(QueryProperty.unqualifyProperty(propertyPath), propertyResult, (EntityType) this.aggregateType);
		}
	}

	
	/**
	 * Get a list of the attribute types in the same order as listed in the AggregateView
	 * @see StoredProcedure
	 * 
	 * @return list of attribute types
	 */
	public List<Type> getAttributeTypes() {
		List<Type> result = new ArrayList<Type>();
		
		for(String attr: aggregateView.getAttributeList()) {
			result.add(aggregateType.getProperty(attr).getType());
		}
		
		return result;
	}
	
	public List<QueryProperty> getAliasedItems() {
		List<String> aliasedPaths = new ArrayList<String>();
		List<QueryProperty> aliasedItems = new ArrayList<QueryProperty>();

		for(Map.Entry<String, QueryProperty> entry: viewPropertyByPath.entrySet()) {
			QueryProperty viewProperty = entry.getValue();
			if(viewProperty.getAlias() == null)
				continue;
			aliasedPaths.add(entry.getKey());
		}
		Collections.sort(aliasedPaths);
		for(String aliasedPath: aliasedPaths)
			aliasedItems.add(viewPropertyByPath.get(aliasedPath));

		return aliasedItems;
	}	
	
	private void initAttributes(List<String> attributeList) {
		if(attributes != null)
			attributes.clear();

		for(int i = 0; i < attributeList.size(); i++)
			addAttribute(attributeList.get(i));
	}
	
	public Set<Parameter> getParameter() {
		View cView = this.aggregateView;
		if(cView == null && parent != null)
			cView = parent.getContentView();

		return (cView.getParameter() == null) ? new HashSet<Parameter>() : new HashSet<Parameter>(cView.getParameter());
	}
	
	public void normalizeFilters(Collection<Filter> filterObjects, QueryCapability queryCapability) {
		// use the entity alias to replace the filter attribute names
		for(Filter filter: filterObjects)
			filter.normalize(getNormalizedNames(queryCapability));		
	}

	private Map<String, String> getNormalizedNames(QueryCapability queryCapability) {
		Map<String, String> result = new HashMap<String, String>();		

		for(Map.Entry<String, ColumnMeta> entry: augmentedAttributes.entrySet()) {
			logger.debug("Key: " + entry.getKey() + ", Value: " + entry.getValue().getQueryString(queryCapability));
			result.put(entry.getKey(), entry.getValue().getQueryString(queryCapability));
		}

		return result;
	}	
	
	public void initProperties() {
		setFilters();
		createProperties();	
		setAliases();		
	}	
	
	private void setFilters() {
		View cView = this.aggregateView;
		if(cView == null && parent != null)
			cView = parent.getContentView();

		if(cView == null || cView.getFilter() == null)
			return;

		// Copy the relevant filters from the content view
		for(Filter filter: cView.getFilter()) {
			Filter narrowedFilter = filter.narrow();
			filters.add(narrowedFilter);	
		}		
	}	
	
	private void createProperties() {
		QueryProperty viewProperty = new QueryProperty(true, aggregateType);
		viewPropertyByPath.put(QueryProperty.ROOT_PROPERTY_NAME, viewProperty);

		for(Map.Entry<String, String> attribute: attributes.entrySet()) {
			addProperty(attribute.getKey(), false, true, attribute.getValue());
		}
		
		// This has to come before adding filters since
		// we need to fetch these properties. On contrast, the filter properties
		// need not be fetched
		setAugmentedAttributes();

		// Create view property objects for attributes in the filter list that are not in the select list
		for(Filter filter: filters) {
			if(!containsProperty(filter.getAttribute())) // should not be fetched
				addProperty(filter.getAttribute(), true, false, null);
		}
	}
	
	private boolean containsProperty(String attribute) {
		if(viewPropertyByPath.containsKey(QueryProperty.qualifyProperty(attribute)))
			return true;

		return false;
	}	
	
	private QueryProperty addProperty(String attribute, boolean isDynamic, boolean doFetch, String propertyAlias) {

		QueryProperty anchor;
		if(!isQueryable(attribute)) {
			// Only single level for migrate to deal with non-queryable types
			anchor = viewPropertyByPath.get(QueryProperty.ROOT_PROPERTY_NAME);
		} else {
			anchor = getParentQueryProperty(attribute, doFetch);
		}
		//QueryViewProperty anchor = getParentViewProperty(attribute, doFetch);
		QueryProperty viewProperty = new QueryProperty(attribute, isDynamic, anchor);
		viewProperty.setPropertyAlias(propertyAlias);
		viewPropertyByPath.put(QueryProperty.qualifyProperty(attribute), viewProperty);
		viewProperty.setFetch(doFetch);

		return viewProperty;
	}	
	
	/**
	 * If any one of the ancestor is not queryable, then that property is not queryable
	 * using an alias
	 *
	 * @param attribute to check if it is queryable using an alias
	 * @return true if the attribute can be referenced using an alias, false if the full
	 * attribute needs to be used in the SELECT clause
	 */
	private boolean isQueryable(String attribute) {
		String parentPath = getParentPath(attribute);

		while(parentPath != null) {
			Property property = aggregateType.getProperty(QueryProperty.unqualifyProperty(parentPath));
			if(property == null) {
				break;
			}

			Type type = property.getType();
			if(EntityType.class.isAssignableFrom(type.getClass()) && !((EntityType)type).isQueryable()) {
				return false;
			}
			parentPath = getParentPath(parentPath);
		}

		return true;
	}	
	
	public void setAugmentedAttributes() {
		if(narrow && (aggregateView == null || aggregateView.getNativeQuery() == null))
			throw new RuntimeException("Narrowing is only supported with native query. The persistence provider requires the whole object to be loaded inorder to infer the type, which we don't do.");

		logger.debug("ViewBranch#setAugmentedAttributes [name: " + name + ", type: " + aggregateType.getName() + "]");
		Set<ColumnMeta> meta = new HashSet<ColumnMeta>();
		for(QueryProperty viewProperty: viewPropertyByPath.values()) {
			// We do not augment the selected columns for a migrate operation
			meta.addAll(viewProperty.getColumnMeta(narrow, !isMigrateView()));
		}

		// Sort the attributes
		List<String> sortedPath = new ArrayList<String>();
		for(ColumnMeta columnMeta: meta) {
			sortedPath.add(columnMeta.getAttributePath());
		}
		Collections.sort(sortedPath);
		if(logger.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder("\r\n===== S O R T E D =====");
			for(String path: sortedPath) {
				sb.append("\r\n" + path);
			}
			logger.debug(sb.toString());
		}

		Map<String, ColumnMeta> augmentedAttributeMap = new HashMap<String, ColumnMeta>();
		for(ColumnMeta columnMeta: meta) {
			if(augmentedAttributeMap.containsKey(columnMeta.getAttributePath())) { // remove duplicates, preferring the property in the view
				if(!columnMeta.getViewProperty().isDynamic())
					augmentedAttributeMap.put(columnMeta.getAttributePath(), columnMeta);
			} else
				augmentedAttributeMap.put(columnMeta.getAttributePath(), columnMeta);
		}
		if(logger.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder("\r\n===== N O   D U P L I C A T E S =====");
			for(Map.Entry<String, ColumnMeta> entry: augmentedAttributeMap.entrySet()) {
				sb.append("\r\npath" + entry.getKey() + ", column: " + entry.getValue().toString());
			}
			logger.debug(sb.toString());
		}

		if(aggregateType instanceof OpenType) {
			sortedPath = new ArrayList<String>();
			for(String path: aggregateView.getAttributeList()) {
				sortedPath.add(QueryProperty.qualifyProperty(path));
			}
		}
		
		augmentedAttributes = new LinkedHashMap<String, ColumnMeta>(); // maintain the order
		int position = 0;
		for(String path: sortedPath) {
			ColumnMeta columnMeta = augmentedAttributeMap.get(path);
			augmentedAttributes.put(path, columnMeta);
			if(aggregateView != null) { // check that the attribute is covered by the native query
				if(aggregateView.getNativeQuery() != null) {
					position = aggregateView.getNativeQuery().getPosition(
						QueryProperty.unqualifyProperty(
							path));
					if (position == -1) {
						logger.warn(
							"The native query does not populate the attribute "
								+ QueryProperty.unqualifyProperty(path) + " for view: "
								+ getViewName());
						aggregateView.getNativeQuery().setUsable(false);
					}
					else {
						columnMeta.setPosition(position);
					}
				} else {
					columnMeta.setPosition(position++);
				}
			}
		}

		// fix the content meta of dynamically added properties by the view property
		for(Map.Entry<String, ColumnMeta> augmented: augmentedAttributes.entrySet()) {
			if(viewPropertyByPath.get(augmented.getKey()) == null) {
				if( !augmented.getValue().isDependent()) {
					logger.debug("Setting ViewProperty for property: " + augmented.getKey());
					augmented.getValue().setViewProperty(addProperty(augmented.getKey(), true, true, null));
				} else {
					logger.debug("Skipping ViewProperty for dependent property: " + augmented.getKey());
				}
			} else {
				logger.debug("ViewProperty already set for property: " + augmented.getKey());
			}
		}
		if(logger.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder("\r\n===== U P D A T E D   B Y   V I E W =====");
			for(Map.Entry<String, ColumnMeta> entry: augmentedAttributeMap.entrySet()) {
				sb.append("\r\npath:" + entry.getKey() + ", column: " + entry.getValue().toString());
			}
			logger.debug(sb.toString());
		}		
	}	
	
	/**
	 * This method adds the anchor property and any additional properties needed to support the creation of the
	 * correct anchor object. This could be the identifier, version and/or the entity name if supported.
	 * 
	 * @param attribute the attribute path from which the parent QueryProperty is found
	 * @param doFetch decides if the property should be included in the result 
	 * @return the parent QueryViewProperty
	 */
	private QueryProperty getParentQueryProperty(String attribute, boolean doFetch) {

		if(this.aggregateType instanceof OpenType && attribute.indexOf(OpenType.DELIM) != -1) {
			attribute = attribute.substring(attribute.indexOf(OpenType.DELIM)+1);
		}
		attribute = QueryProperty.qualifyProperty(attribute);
		QueryProperty alias = null;
		Property property;
		boolean createAlias = false;
		String parentPath = getParentPath(attribute);

		// Create ancestor aliases first
		while(parentPath != null) {
			if(viewPropertyByPath.containsKey(parentPath)) {
				alias = viewPropertyByPath.get(parentPath);
				property = alias.getProperty();
			} else {
				property = aggregateType.getProperty(QueryProperty.unqualifyProperty(parentPath));
			}

			// Root alias should always be there so we can break here
			if(property == null && parentPath.equals(QueryProperty.ROOT_PROPERTY_NAME)) {
				break;
			}

			if(property == null) {
				logger.error(
					"attribute " + attribute + " is not present in entity: "
						+ aggregateType.getName());
			}

			// If this is a collection, then if the element is an entity type then break
			if(property.isMany()) {
				Type elementType = ((ExtendedProperty)property).getElementType();
				if(EntityType.class.isAssignableFrom(elementType.getClass())) {
					if(!((EntityType)elementType).isEmbedded() && !elementType.isDataType()) {
						createAlias = true; // found it
					}
				}
			} else { // if it is an entity type then break (includes creating alias for embedded type)
				Type type = property.getType();
				if(EntityType.class.isAssignableFrom(type.getClass())) {
					createAlias = true;
				}
			}

			if(createAlias) {
				// If the anchor is not present, then add it
				if(alias == null ) {
					alias = new QueryProperty(parentPath, true, getParentQueryProperty(parentPath, doFetch));
					viewPropertyByPath.put(QueryProperty.qualifyProperty(parentPath), alias);
				}
				alias.setFetch(doFetch);
				createAlias = false;
			}

			parentPath = getParentPath(parentPath);
		}

		// return the parent alias
		parentPath = getParentPath(attribute);
		if(viewPropertyByPath.containsKey(parentPath)) {
			return viewPropertyByPath.get(parentPath);
		}

		return null;
	}	
	
	protected String getParentPath(String propertyPath) {
		if(propertyPath.indexOf(Settings.PATH_DELIMITER) != -1)
			return propertyPath.substring(0, propertyPath.lastIndexOf(Settings.PATH_DELIMITER));
		else
			return QueryProperty.ROOT_PROPERTY_NAME;
	}	
	
	private boolean isMigrateView() {
		AggregateView av = (AggregateView)getContentView();

		QueryPiece current = this;
		while (av == null && current.getParent() != null) {
			av = (AggregateView)getParent().getContentView();
			current = current.getParent();
		}

		return av != null && av.isMigrateView(current.getAggregateType());
	}
	
	private void setAliases() {
		int entityAliasCount = 1;
		for(QueryProperty eProp: viewPropertyByPath.values()) {
			if(eProp.getAlias() != null)
				throw new IllegalStateException("Alias has already been set");

			if(eProp.isEntity())
				eProp.setAlias(QueryTree.ENTITY_ALIAS_PREFIX + entityAliasCount++);
		}
	}	
	
	private String getViewName() {
		if(parent != null)
			return parent.getName();
		else
			return getName();
	}	
	
	public void addAttribute(String attribute) {
		if(!attributes.containsKey(attribute)) {
			attributes.put(attribute, QueryTree.PROPERTY_ALIAS_PREFIX + (attributes.size()+1));
		}
	}	
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}	
	
	public List<Filter> getFilters() {
		return filters;
	}

	public void setFilters(List<Filter> filters) {
		this.filters = filters;
	}	

	public View getContentView() {
		return aggregateView;
	}

	public void setContentView(AggregateView contentView) {
		this.aggregateView = contentView;
	}		
	
	public boolean hasCollection() {
		return collection;
	}

	public void setHasCollection(boolean collection) {
		this.collection = collection;
	}	
	
	public Type getAggregateType() {
		return aggregateType;
	}

	public void setAggregateType(Type aggregateType) {
		this.aggregateType = aggregateType;
	}	

	public QueryPiece getParent() {
		return parent;
	}

	public void setParent(QueryPiece parentBranch) {
		this.parent = parentBranch;
	}
	
	/**
	 * Use the subBranches if there is more than one
	 * @return list of subbranch views
	 */
	public List<QueryPiece> getSubBranches() {
		return Collections.unmodifiableList(this.subBranches);
	}	
}

