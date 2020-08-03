/**
 * XOR, empowering Model Driven Architecture in J2EE applications
 *
 * Copyright (c) 2019, Dilip Dalton
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

import java.util.Collection;

import tools.xor.JDBCProperty;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.service.DataStore;
import tools.xor.view.QueryBuilder;
import tools.xor.view.QueryFragment;
import tools.xor.view.QueryTree;

public class IntraQuery<V extends QueryFragment> extends Edge<V>
{
    private Property property;

    public IntraQuery (String name, V start, V end, Property property)
    {
        super(name, start, end);

        this.property = property;
    }

    public Property getProperty ()
    {
        return property;
    }

    @Override
    public String getDisplayName ()
    {
        //if(DFAtoNFA.UNLABELLED.equals(getName())) {
        //    return DFAtoRE.LiteralExpression.EPSILON;
        //}

        return super.getDisplayName();
    }

    public String getJoinClause(QueryTree queryTree, DataStore po) {
        String className = getEnd().getEntityType().getEntityName();

        if(Settings.doSQL(po)) {
            StringBuilder join = new StringBuilder();
            join.append(" LEFT OUTER JOIN ")
                .append(getJoinTableName())
                .append(" AS ")
                .append(getEnd().getAlias())
                .append(" ON (")
                .append(((JDBCProperty)ClassUtil.getDelegate(property)).getOnClause(getStart().getAlias(), getEnd().getAlias()))
                .append(")");
            return join.toString();
        } else {
            // If the join edge represents an open content, that means that relationship is
            // not captured by the ORM and the join condition has to be explicitly
            // specified in the WHERE clause of the OQL
            if (property != null && property.isOpenContent()) {
                return QueryBuilder.COMMA_DELIMITER + className + QueryBuilder.AS_CLAUSE
                    + getEnd().getAlias();
            }
            else {
                return po.getOQLJoinFragment(queryTree, (IntraQuery<QueryFragment>)this);
            }
        }
    }

    private String getJoinTableName() {
        if(property.isMany()) {
            return GraphUtil.getPropertyEntityType(property, null).getName();
        } else {
            return property.getType().getName();
        }
    }

    public IntraQuery getAssociationEdge(QueryTree queryTree) {
        IntraQuery result = this;

        if(getProperty() == null) {
            QueryFragment parent = getParentFragment(queryTree, getStart());
            Collection<IntraQuery> inEdges = queryTree.getInEdges(parent);
            if (inEdges.size() == 1) {
                return inEdges.iterator().next();
            }
        }

        return result;
    }

    private QueryFragment getParentFragment(QueryTree queryTree, QueryFragment child)
    {
        Collection<IntraQuery> inEdges = queryTree.getInEdges(child);
        if (inEdges.size() == 1) {
            IntraQuery incomingEdge = inEdges.iterator().next();
            if(incomingEdge.getProperty() == null) {
                return getParentFragment(queryTree, (QueryFragment)incomingEdge.getStart());
            }
        }

        return child;
    }

    public String getNormalizedName() {
        return getStart().getAlias() + Settings.PATH_DELIMITER + property.getName();
    }
}
