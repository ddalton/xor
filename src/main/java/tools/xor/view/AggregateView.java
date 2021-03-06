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
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.AggregateAction;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.service.Shape;

/**
 * If using stored procedures, then a copy of this must be made as
 * the results attribute is populated in every invocation and this instance cannot
 * be shared.
 */
@XmlRootElement(name="AggregateView")
public class AggregateView extends TraversalView {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	
	// Each function should be independent of one another
	protected List<Function> function = new ArrayList<>();;
	
	// OQL query generated by the system. May not be an optimized version.
	protected OQLQuery          systemOQLQuery;
	
	// OQL query crafted by the user, useful for providing an optimized OQL version.
	protected OQLQuery          userOQLQuery;
	
	// It should refer to the full attribute name
	// SQL - the column names should be PROP1, PROP2 etc...
	protected NativeQuery       nativeQuery;
	
	protected List<StoredProcedure>   storedProcedure;

	// Child AggregateViews are considered as inline views and do not persist independently
	// Their name represents the anchor (the edge) where they connect to. It is a full path
	// starting from the root.
	// View references are named views whose anchor is the path where they appear.
	// If the view to which it attaches does not have a type then it is a container view
	// and all its members are aliases
 	protected List<AggregateView> children;

 	@XmlAttribute
	protected boolean tempTablePopulated;

	@XmlTransient
	private boolean isSplitToRoot = true;

	@XmlTransient
	private List results;

	public AggregateView(QueryTree queryTree) {
		super(queryTree);
	}
	
	public AggregateView(Type type, String viewName) {
		this(viewName);
		this.typeName = type.getName();
	}	
	
	public AggregateView(String viewName) {
		setName(viewName);
	}

	public static enum Format {
		PATHS,
		JSON
	};

	/**
	 * No-args constructor required for Unmarshalling purpose and also internally by the framework. 
	 * Don't use this directly.
	 */
	public AggregateView() {
		super();
	}

	@Override
	public boolean isTempTablePopulated ()
	{
		return tempTablePopulated;
	}

	@Override
	public void setTempTablePopulated (boolean tempTablePopulated)
	{
		this.tempTablePopulated = tempTablePopulated;
	}

	public List<AggregateView> getChildren() {
		return children;
	}

	public void setChildren(List<AggregateView> children) {
		this.children = children;
	}

	protected void addChildView(View view, String anchor) {
		if(this.children == null) {
			this.children = new ArrayList<>();
		}
		AggregateView child = (AggregateView)view.copy();
		String anchorStr = extractAnchor(anchor);
		if(anchorStr.endsWith(Settings.PATH_DELIMITER)) {
			anchorStr = Settings.getAnchorName(anchorStr);
		}

		child.setAnchorPath(anchorStr);

		this.children.add(child);
	}

	public OQLQuery getSystemOQLQuery() {
		return systemOQLQuery;
	}

	public void setSystemOQLQuery(OQLQuery systemOQLQuery) {
		this.systemOQLQuery = systemOQLQuery;
	}

	@Override
	public OQLQuery getUserOQLQuery() {
		return userOQLQuery;
	}

	@Override
	public void setUserOQLQuery(OQLQuery userOQLQuery) {
		this.userOQLQuery = userOQLQuery;
	}

	@Override
	public NativeQuery getNativeQuery() {
		return nativeQuery;
	}

	public void setNativeQuery(NativeQuery nativeQuery) {
		this.nativeQuery = nativeQuery;
	}

	@Override
	public StoredProcedure getStoredProcedure(final AggregateAction action) {
		StoredProcedure result = null;

		if(getStoredProcedure() != null) {
			for (StoredProcedure sp : getStoredProcedure()) {
				if (sp.getAction() == action) {
					result = sp.copy();
					break;
				}
			}
		}

		return result;
	}

	@Override
	public List<StoredProcedure> getStoredProcedure() {
		return storedProcedure;
	}

	@Override
	public void setStoredProcedure(List<StoredProcedure> storedProcedure) {
		this.storedProcedure = storedProcedure;
	}

	@Override
	public void setShape(Shape shape) {
		super.setShape(shape);

		if(children != null) {
			for(View child: children) {
				child.setShape(shape);
			}
		}
	}

	@Override
	public AggregateView copy()
	{
		AggregateView copy = new AggregateView();
		copyInto(copy);

		return copy;
	}

	@Override
	protected void copyInto(TraversalView copy) {

		super.copyInto(copy);

		assert(copy instanceof AggregateView);

		AggregateView avCopy = (AggregateView) copy;

		avCopy.tempTablePopulated = tempTablePopulated;
		if(nativeQuery != null) {
			avCopy.setNativeQuery(nativeQuery.copy());
		}

		if(userOQLQuery != null) {
			avCopy.setUserOQLQuery(userOQLQuery.copy());
		}

		if(systemOQLQuery != null) {
			avCopy.setUserOQLQuery(systemOQLQuery.copy());
		}

		if(storedProcedure != null) {
			List<StoredProcedure> spCopy = new ArrayList<>(storedProcedure.size());
			for(StoredProcedure sp: storedProcedure) {
				spCopy.add(sp.copy());
			}
			avCopy.setStoredProcedure(spCopy);
		}

		if(children != null) {
			List<AggregateView> childrenCopy = new ArrayList<>();
			for(AggregateView c: children) {
				childrenCopy.add(c.copy());
			}
			avCopy.setChildren(childrenCopy);
		}
	}

	@Override
	public void expand(List<String> expanding) {
		// We need to expand for querying
		// This means we need to check for view references that have user queries
		// then we create child queries for them

		if(getChildren() != null) {
			for(AggregateView child: getChildren()) {
				// EntityType fragments cannot be expanded
				if(child.getTypeName() != null) {
					return;
				}
			}
		}

		// Expand the children
		if(getChildren() != null) {
			for(View child: getChildren()) {
				child.expand();
			}
		}

		super.expand(expanding);
	}

	@Override public boolean isSplitToRoot ()
	{
		return this.isSplitToRoot;
	}

	@Override public void setSplitToRoot (boolean value)
	{
		this.isSplitToRoot = value;
	}

	@Override
	public boolean isCustom() {
		return getNativeQuery() != null || getUserOQLQuery() != null || getStoredProcedure(AggregateAction.READ) != null || getResultPosition() != null;
	}

	public void setResults(List results) {
		this.results = results;
	}

	public List getResults() {
		return this.results;
	}
}
