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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.EntityType;
import tools.xor.Property;
import tools.xor.Type;
import tools.xor.service.Shape;
import tools.xor.util.graph.StateGraph;
import tools.xor.view.TraversalView;

/**
 * Uses Brzozowski algebraic method to convert a Deterministic Finite Automata to a regular expression.
 * @author Dilip Dalton
 *
 */
public class DFAtoRE {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	
	public static final String UNION_SYMBOL   = "|";
	public static final String RECURSE_SYMBOL = "*";

	private Type                  aggregateType;
	private StateGraph<State, Edge<State>> stateGraph;
	private StateGraph<State, Edge<State>> stateGraphExact; // No subtype expansion, supertypes ok
	private Map<Type, Expression> equations = new HashMap<Type, DFAtoRE.Expression>();
	private Map<State, Expression> regEx = new HashMap<State, DFAtoRE.Expression>();
	

	public DFAtoRE() {
	}

	public DFAtoRE(Type aggregateType, Shape shape) {
		this.aggregateType = aggregateType;
		this.stateGraph = new StateGraph<>(this.aggregateType, shape);

		buildDFA(shape);

		this.stateGraphExact = this.stateGraph.copy();
		DFAtoNFA.processInheritance(this.stateGraphExact, DFAtoNFA.TypeCategory.SUPERTYPES);
		DFAtoNFA.processInheritance(this.stateGraph, DFAtoNFA.TypeCategory.ALL);

		solve();
	}
	
	public StateGraph<State, Edge<State>> getGraph() {
		return stateGraph;
	}
	
	public StateGraph<State, Edge<State>> getFullStateGraph() {
		return stateGraph.getFullStateGraph();
	}

	public StateGraph<State, Edge<State>> getExactStateGraph() {
		return stateGraphExact.getFullStateGraph();
	}
	
	private State getConstrained(State state, Map<State, State> existing) {
		if(!existing.containsKey(state)) {
			State constrained = new State(state.getType(), state.isStartState());
			existing.put(state, constrained);
		}

		return existing.get(state);
	}

	/**
	 * An expression might consist of an operation or a literal 
	 *
	 */
	public static interface Expression {

		public Expression append(Expression expression);		

		public Expression prepend(Expression expression);	

		public Expression substitute(Type typeToRemove, Expression expression);	

		public Expression reduce(Type type);

		public int getNumUnknowns();

		public Expression copy(); // return a deep copy

		public int getNumExpressions();

		public boolean isRecursive(Type type);
	}

	public static abstract class AbstractExpression implements Expression {

		@Override
		public Expression substitute(Type typeToRemove, Expression expression) {
			return this;
		}

		@Override
		public Expression reduce(Type type) {
			return this;
		}

		@Override
		public int getNumUnknowns() {
			return 0;
		}

		@Override
		public boolean isRecursive(Type type) {
			return false;
		}
	}

	public static class ConcatExpression extends AbstractExpression {
		private List<Expression> children = new ArrayList<DFAtoRE.Expression>();

		public ConcatExpression(Expression expression) {
			children.add(expression);
		}

		public Expression append(Expression expression) {
			if(LiteralExpression.class.isAssignableFrom(expression.getClass()) && (LiteralExpression)expression == LiteralExpression.EMPTY_STRING)
				return this;

			if(TypedExpression.class.isAssignableFrom(expression.getClass())) {
				expression = expression.copy();
				Expression result = expression.prepend(this);
				return result;
			} else if(UnionExpression.class.isAssignableFrom(expression.getClass())) {
				if(expression.getNumUnknowns() == 0) {
					children.add(expression);
					return this;
				} else {
					expression = expression.copy();
					Expression result = expression.prepend(this);
					return result;
				}
			} else {
				children.add(expression);
				return this;
			}
		}

		public Expression prepend(Expression expression) {
			expression = expression.copy();
			if(LiteralExpression.class.isAssignableFrom(expression.getClass()) && (LiteralExpression)expression == LiteralExpression.EMPTY_STRING)
				return this;

			children.add(0, expression);
			return this;
		}		

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			for(Expression expression: children)
				result.append(expression.toString());

			return result.toString();			
		}

		@Override
		public Expression copy() {
			Iterator<Expression> iter = children.iterator();
			ConcatExpression result = new ConcatExpression(iter.next().copy());

			while(iter.hasNext()) 
				result.children.add(iter.next().copy());

			return result;
		}

		@Override
		public int getNumExpressions() {
			int result = 0;

			for(Expression child: children)
				result += child.getNumExpressions();

			return result;
		}
	}	

	/**
	 * Uses the '|' operator. For example, A|B
	 * NOTE: Do not provide a space otherwise the pattern being matched should also have
	 * the corresponding space.
	 *
	 * @author daltond
	 *
	 */
	public static class UnionExpression extends AbstractExpression {
		private Set<Expression> children = new HashSet<DFAtoRE.Expression>();

		public UnionExpression(Expression expression) {
			if(UnionExpression.class.isAssignableFrom(expression.getClass()))
				children.addAll( ((UnionExpression)expression).children );
			else
				children.add(expression);
		}

		public void addAlternate(Expression expression) {
			if(UnionExpression.class.isAssignableFrom(expression.getClass()))
				children.addAll( ((UnionExpression)expression).children );
			else
				children.add(expression);
		}
		
		public Set<Expression> getChildren() {
			return Collections.unmodifiableSet(children);
		}

		public void setChildren(Set<Expression> newChildren) {
			children = newChildren;

			for(Expression child: children) {
				if(child == this)
					throw new IllegalArgumentException("Cannot recursively reference an expression");
			}
		}

		@Override
		public Expression substitute(Type typeToRemove, Expression expression) {
			expression = expression.copy();
			Set<Expression> result = new HashSet<DFAtoRE.Expression>();		

			for(Expression child: children) {
				Expression exp = child.substitute(typeToRemove, expression);
				if(UnionExpression.class.isAssignableFrom(exp.getClass()))
					result.addAll( ((UnionExpression)exp).children );
				else
					result.add(exp);
			}

			setChildren(result);
			return consolidateTypes();
		}

		private Set<Expression> getNonTyped() {
			Set<Expression> result = new HashSet<DFAtoRE.Expression>();

			for(Expression child: children)
				if(!TypedExpression.class.isAssignableFrom(child.getClass()))
					result.add(child);

			return result;
		}

		@Override
		public Expression reduce(Type type) {
			Set<Expression> rest = new HashSet<Expression>();
			Expression starExpression = null;
			for(Expression child: children) {
				if(TypedExpression.class.isAssignableFrom(child.getClass()) && ((TypedExpression)child).getType() == type) {
					starExpression = ((TypedExpression)child).getExpression();
					if(starExpression != LiteralExpression.EMPTY_STRING) {
						starExpression = new StarExpression(starExpression);
					}
				} else
					rest.add(child);
			}

			if(starExpression != null)
				if(rest.size() == 0)
					return starExpression;
				else if(rest.size() == 1)
					return starExpression.append(rest.iterator().next());
				else {
					setChildren(rest);
					return starExpression.append(this);
				}
			else
				return this;
		}

		@Override
		public Expression append(Expression expression) {
			if(LiteralExpression.class.isAssignableFrom(expression.getClass()) && (LiteralExpression)expression == LiteralExpression.EMPTY_STRING)
				return this;

			Set<Expression> result = new HashSet<Expression>();
			for(Expression child: children) {
				Expression newExpression = child.append(expression);
				if(UnionExpression.class.isAssignableFrom(newExpression.getClass()))
					result.addAll( ((UnionExpression)newExpression).children );
				else
					result.add(newExpression);				
			}
			setChildren(result);
			return this;
		}			

		public Expression prepend(Expression expression) {
			expression = expression.copy();
			if(LiteralExpression.class.isAssignableFrom(expression.getClass()) && (LiteralExpression)expression == LiteralExpression.EMPTY_STRING)
				return this;

			Set<Expression> result = new HashSet<Expression>();
			for(Expression child: children) {
				Expression newExpression = child.prepend(expression);
				if(UnionExpression.class.isAssignableFrom(newExpression.getClass()))
					result.addAll( ((UnionExpression)newExpression).children );
				else
					result.add(newExpression);
			}
			setChildren(result);
			return this;
		}	

		// find all the expressions of the same type and create a union expression out of them
		public Expression consolidateTypes() {
			logger.debug("Before consolidation: " + this.toString());

			boolean needsConsolidation = false;
			Map<Type, Set<Expression>> expressionsByType = new HashMap<Type, Set<Expression>>();
			for(Expression child: children) {
				if(TypedExpression.class.isAssignableFrom(child.getClass())) {
					TypedExpression typedChild = (TypedExpression) child;
					Set<Expression> expressions = expressionsByType.get(typedChild.getType());
					if(expressions == null) {
						expressions = new HashSet<DFAtoRE.Expression>();
						expressionsByType.put(typedChild.getType(), expressions);
					}
					expressions.add(typedChild.getExpression());
					if(expressions.size() > 1)
						needsConsolidation = true;
				}
			}

			if(needsConsolidation) {
				Set<Expression> result = getNonTyped();
				for(Map.Entry<Type, Set<Expression>> entry: expressionsByType.entrySet()) {
					Set<Expression> expressions = entry.getValue();
					if(expressions.size() > 1) {
						// create UnionExpression with the appropriate type
						Iterator<Expression> iter = expressions.iterator();
						UnionExpression expression = new UnionExpression(iter.next());
						while(iter.hasNext()) 
							expression.addAlternate(iter.next());
						result.add(new TypedExpression(expression, entry.getKey()));
					} else
						result.add(new TypedExpression(expressions.iterator().next(), entry.getKey()));
				}
				setChildren(result);

				if(result.size() == 1)
					return result.iterator().next();
			}

			return this;
		}

		@Override
		public int getNumUnknowns() {
			// The types should have been consolidated before this method is invoked, otherwise we will get the wrong answer
			int result = 0;
			for(Expression child: children)
				result += child.getNumUnknowns();

			return result;
		}	

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			for(Expression expression: children) {
				if(result.length() > 0)
					result.append(UNION_SYMBOL);
				result.append(expression.toString());
			}
			if(children.size() < 2) {
				logger.warn(
					"Union expression should consist of 2 or more expressions");
			} else {
				result.insert(0, "(");
				result.append(")");
			}

			return result.toString();			
		}

		@Override
		public Expression copy() {
			Iterator<Expression> iter = children.iterator();
			UnionExpression result = new UnionExpression(iter.next().copy());

			while(iter.hasNext()) 
				result.children.add(iter.next().copy());

			return result;
		}	

		@Override
		public int getNumExpressions() {
			int result = 0;

			for(Expression child: children)
				result += child.getNumExpressions();

			return result;
		}	

		@Override
		public boolean isRecursive(Type type) {
			for(Expression child: children)
				if(child.isRecursive(type))
					return true;

			return false;
		}
	}

	public static class StarExpression extends AbstractExpression {
		private Expression expression;

		public StarExpression(Expression expression) {
			this.expression = expression;

			if(LiteralExpression.class.isAssignableFrom(expression.getClass()) && (LiteralExpression)expression == LiteralExpression.EMPTY_STRING)
				throw new IllegalArgumentException("star on an empty string is an empty string, do not wrap with a star expression");

			if(StarExpression.class.isAssignableFrom(expression.getClass()))
				throw new IllegalArgumentException("Does not make sense to wrap a star expression inside another star expression");
		}

		@Override
		public Expression prepend(Expression expression) {
			expression = expression.copy();
			if(LiteralExpression.class.isAssignableFrom(expression.getClass()) && (LiteralExpression)expression == LiteralExpression.EMPTY_STRING)
				return this;

			ConcatExpression concatExp = new ConcatExpression(this);
			concatExp.prepend(expression);
			return concatExp;
		}

		@Override
		public Expression append(Expression expression) {
			if(LiteralExpression.class.isAssignableFrom(expression.getClass()) && (LiteralExpression)expression == LiteralExpression.EMPTY_STRING)
				return this;

			expression = expression.copy();
			if(TypedExpression.class.isAssignableFrom(expression.getClass())) {
				ConcatExpression concatExp = new ConcatExpression(this.copy());
				((TypedExpression)expression).nonTyped = concatExp.append( ((TypedExpression)expression).getExpression() );
				return expression;				

			} else {
				Expression concatExp = new ConcatExpression(this);
				concatExp = concatExp.append(expression);
				return concatExp;
			}
		}

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder(expression.toString());

			if(ConcatExpression.class.isAssignableFrom(expression.getClass())) {
				result.insert(0, "(");
				result.append(")");
			}
			result.append(RECURSE_SYMBOL);

			return result.toString();
		}

		@Override
		public Expression copy() {
			return new StarExpression(expression.copy());
		}		

		@Override
		public int getNumExpressions() {
			return expression.getNumExpressions() + 1;
		}			
	}

	public static class LiteralExpression extends AbstractExpression {
		public static final LiteralExpression EMPTY_STRING = new LiteralExpression();
		public static final String EPSILON = "\u025B";		

		private Edge transition;		

		private LiteralExpression() {
		}

		/**
		 * By default the literal refers to a graph edge. We support string
		 * literals by overloading this capability.
		 * @param value of the literal
		 */
		public LiteralExpression(String value) {
			this.transition = new Edge(value, null, null);
		}

		public LiteralExpression(Edge transition) {
			this.transition = transition;
		}

		public static LiteralExpression instance(Edge transition) {
			if(transition.getQualifiedName() != null && transition.getQualifiedName().equals("")) {
				return LiteralExpression.EMPTY_STRING;
			}

			return new LiteralExpression(transition);
		}

		@Override
		public Expression prepend(Expression expression) {
			expression = expression.copy();
			if(LiteralExpression.class.isAssignableFrom(expression.getClass())) {
				Expression result = this;
				if( expression != LiteralExpression.EMPTY_STRING) {
					result = new ConcatExpression(expression);
					result = result.append(this);
				}
				return result;
			} else {
				if(this == LiteralExpression.EMPTY_STRING)
					return expression;
				else
					return expression.append(this);
			}
		}

		@Override
		public Expression append(Expression expression) {
			if(LiteralExpression.class.isAssignableFrom(expression.getClass()) && (LiteralExpression)expression == LiteralExpression.EMPTY_STRING)
				return this;

			expression = expression.copy();
			return expression.prepend(this);
		}

		@Override
		public String toString() {
			//return (this == LiteralExpression.EMPTY_STRING) ? LiteralExpression.EPSILON : (transition.getQualifiedName());
			return (this == LiteralExpression.EMPTY_STRING) ? "" : (transition.getQualifiedName());
		}

		@Override
		public Expression copy() {
			if(this == LiteralExpression.EMPTY_STRING)
				return LiteralExpression.EMPTY_STRING;
			return new LiteralExpression(transition);
		}

		@Override
		public int getNumExpressions() {
			return 1;
		}		
	}	

	public static class TypedExpression extends AbstractExpression {
		private Expression nonTyped;
		private Type type;

		public TypedExpression(Expression nonTyped, Type type) {
			this.nonTyped = nonTyped;
			this.type = type;

			if(TypedExpression.class.isAssignableFrom(nonTyped.getClass()))
				throw new IllegalArgumentException("The expression is expected to be non-typed");
		}

		public Type getType() {
			return type;
		}

		public Expression getExpression() {
			return nonTyped;
		}

		@Override
		// We are doing substitution
		public Expression append(Expression expression) {
			return nonTyped.append(expression);
		}

		@Override
		public Expression prepend(Expression expression) {
			expression = expression.copy();
			nonTyped = nonTyped.prepend(expression);
			return this;
		}

		@Override
		public Expression substitute(Type typeToRemove, Expression substituteExp) {
			if(this.type == typeToRemove) {
				if(TypedExpression.class.isAssignableFrom(substituteExp.getClass())) {
					ConcatExpression concatExp = new ConcatExpression(nonTyped.copy());
					concatExp.append( ((TypedExpression)substituteExp).getExpression() );
					nonTyped = concatExp;
					this.type = ((TypedExpression)substituteExp).type;
					return this;
				} else {
					nonTyped = nonTyped.append(substituteExp.copy());
					return nonTyped;
				}
			}

			return this;
		}	

		@Override
		public int getNumUnknowns() {
			return 1;
		}		

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder(nonTyped.toString());
			result.append(type.getName());

			return result.toString();
		}

		@Override
		public Expression copy() {
			return new TypedExpression(nonTyped.copy(), type);
		}		

		@Override
		public int getNumExpressions() {
			return nonTyped.getNumExpressions() + 1;
		}		

		@Override
		public boolean isRecursive(Type type) {
			if(this.type == type)
				return true;

			return false;
		}

		@Override
		public Expression reduce(Type type) {
			if(this.type == type)
				return new StarExpression( nonTyped );

			return this;
		}			
	}

	/** 
	 * This changes the state graph
	 * 
	 * @param type to set
	 * @param shape on which the state graph is based
	 */
	public void setAggregateType(Type type, Shape shape) {
		this.aggregateType = type;
		this.stateGraph = new StateGraph<State, Edge<State>>(this.aggregateType, shape);
	}

	public void addState(State state) {
		stateGraph.addVertex(state);
	}

	public void solve() {
		regEx = new HashMap<State, DFAtoRE.Expression>();
		
		int numStartStates = 0;
		for(State state: stateGraph.getVertices()) {
			if(state.isStartState())
				numStartStates++;
		}
		if(numStartStates != 1) 
			throw new IllegalArgumentException("There should be one start state");

		for(State state: stateGraph.getVertices()) {
			state.setFinishState(true);
			createEquations();
			Expression result = processEquations();

			if(result != null) {
				regEx.put(state, result);
				logger.debug(
					"State: " + state.getType().getName() + ", Expression : " + result.toString());
			}

			state.setFinishState(false);
		}
	}
	
	public Map<State, Expression> getRegEx() {
		return (Map<State, Expression>) Collections.unmodifiableMap(regEx);
	}

	private void processReachability() {
		State finishState = null;
		for(State state: stateGraph.getVertices()) {
			if(state.isFinishState())
				finishState = state;
			state.setInScope(false);
		}

		stateGraph.scopeStart(finishState);
	}	

	private void buildDFA(Shape shape) {
		// build the set of states and transitions for the aggregate type
		StackFrame sf = new StackFrame();
		State startState = new State(aggregateType, true);
		stateGraph.addVertex(startState);
		sf.navigationPath.push(startState);
		
		execute(sf, shape);
	}

	/**
	 * Any attribute path that is not of the form
	 * attr1.attr2
	 * is considered as a regular expression
	 *
	 * @param attrPath full path of an attribute/pattern
	 * @return true if the path refers to a RegEx
	 */
	public static boolean isRegex(String attrPath) {
		return TraversalView.REGEX_STRING_MATCHER.matcher(attrPath).find();
	}

	/**
	 * Build constrained state graph of only non-RECURSE attributes.
	 * RECURSE attributes are checked dynamically.
	 *
	 * @param aggregateView representing the graph
	 * @param type of entity
	 * @return state graph instance
	 */
	public static StateGraph<State, Edge<State>> build(TraversalView aggregateView, Type type) {
		if(aggregateView == null) {
			return null;
		}

		State startState = new State(type, true);
		StateGraph<State, Edge<State>> constrainedGraph = new StateGraph<State, Edge<State>>(type, aggregateView.getShape());
		constrainedGraph.addVertex(startState);

		for(String attrPath: aggregateView.getAttributeList()) {
			if(!isRegex(attrPath)) {
				constrainedGraph.extend(attrPath, startState, false);
			}
		}

		// Have the supertypes added by default
		DFAtoNFA.processInheritance(constrainedGraph, DFAtoNFA.TypeCategory.SUPERTYPES);

		return constrainedGraph;
	}
	
	public static class StackFrame {
		Stack<State> navigationPath = new Stack<State>();		
	}	
	
	protected void execute(StackFrame sf, Shape shape) {

		boolean includeEmbedded = false;
		if (ApplicationConfiguration.config().containsKey(Constants.Config.INCLUDE_EMBEDDED)
			&& ApplicationConfiguration.config().getBoolean(Constants.Config.INCLUDE_EMBEDDED)) {
			includeEmbedded = true;
		}

		State state = sf.navigationPath.peek();
		Type type = state.getType();
		for(Property childProperty: type.getProperties()) {
			Type propertyType = GraphUtil.getPropertyEntityType(childProperty, shape);

			// Don't walk through reference associations unless they are required
			if(childProperty != null && !childProperty.isContainment() && childProperty.isNullable() )
				continue;	

			if(logger.isDebugEnabled()) {
				if(childProperty != null && !childProperty.isContainment() && !childProperty.isNullable() ) {
					if(childProperty.getContainingType() != null) 
						logger.debug("Required association: " + childProperty.getName() + ", containing Type: " + ((childProperty.getContainingType() != null) ? childProperty.getContainingType().getName() : ""));
				}
			}

			if(!includeEmbedded && (propertyType instanceof EntityType) && ((EntityType)propertyType).isEmbedded()) {
				continue;
			}
			
			// check if it has been processed
			if(!propertyType.isDataType()) {			
				State endState = stateGraph.getVertex(propertyType);
				
				boolean processEndState = false;
				if(endState == null) {
					processEndState = true;
					endState = new State(propertyType, false); 
					stateGraph.addVertex(endState);
				}
				// add the transition
				Edge transition = new Edge(childProperty.getName(), state, endState, true);
				stateGraph.addEdge(transition);
				
				// Do we need to follow this transition
				// Don't follow for required reference association
				if(processEndState && (childProperty == null || childProperty.isContainment())) {
					sf.navigationPath.push(endState);
					execute(sf, shape);
				}
			}
		}
		
		// Finished processing the state, so pop it
		sf.navigationPath.pop();
	}

	public void createEquations() {
		equations = new HashMap<Type, DFAtoRE.Expression>();

		processReachability();
		for(State state: stateGraph.getVertices()) {
			if(!state.isInScope())
				continue;

			Expression exp = stateGraph.getExpression(state);
			if(exp == null)
				continue;
			logger.debug("Initial equation for type " + state.getType().getName() + " is " + exp.toString());

			equations.put(state.getType(), exp);
		}
	}

	private Type getTypeToRemove() {
		// Keep the logic simple
		// We choose the type with the minimum number of unknowns

		int minUnknowns = Integer.MAX_VALUE;
		Type typeWithMinUnknowns = null;
		for(Map.Entry<Type, Expression> entry: equations.entrySet()) {
			State state = stateGraph.getVertex(entry.getKey());
			if(state.isStartState()) // We don't replace the start state, since that is the state we are interested in
				continue;

			Expression exp = entry.getValue();
			if(exp.getNumExpressions() < minUnknowns) {
				minUnknowns = exp.getNumExpressions();
				typeWithMinUnknowns = entry.getKey();
			}
		}

		// reduce the recursive expression
		if(typeWithMinUnknowns != null) {
			Expression exp = equations.get(typeWithMinUnknowns);
			if(exp.isRecursive(typeWithMinUnknowns)) {
				Expression reducedExp = exp.reduce(typeWithMinUnknowns);
				logger.debug("reducing type " + typeWithMinUnknowns.getName() + " : " + reducedExp.toString());
				equations.put(typeWithMinUnknowns, reducedExp);
			}
		}

		return typeWithMinUnknowns;
	}

	private void substitute(Type typeToRemove) {
		for(Map.Entry<Type, Expression> entry: equations.entrySet()) {
			if(entry.getKey() == typeToRemove)
				continue;
			Expression exp = entry.getValue();
			equations.put(entry.getKey(), exp.substitute(typeToRemove, equations.get(typeToRemove)));
		}
	}

	private void printEquations(String message, Type typeToRemove) {
		if(typeToRemove == null)
			return;

		logger.debug(message + typeToRemove.getName());
		for(Map.Entry<Type, Expression> entry: equations.entrySet()) {
			logger.debug(entry.getKey().getName() + " = " + entry.getValue().toString());
		}
	}

	public Expression processEquations() {
		Type typeToRemove = getTypeToRemove();
		printEquations("After reduction: ", typeToRemove);
		while(typeToRemove != null) {
			substitute(typeToRemove);
			printEquations("After substitution: ", typeToRemove);
			equations.remove(typeToRemove);			
			typeToRemove = getTypeToRemove();
		}

		// For a parent type, it may not be reachable from the start state
		// So in this situation, there might not be any equations to process

		if(equations.size() == 1 && equations.containsKey(aggregateType)) {
			Expression result = equations.get(aggregateType);
			result = result.reduce(aggregateType);

			return result;
		}

		return null;
	}	
}
