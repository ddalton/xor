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

import tools.xor.JDBCProperty;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.service.PersistenceOrchestrator;
import tools.xor.view.QueryBuilder;
import tools.xor.view.QueryFragment;

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

    public String getJoinClause(PersistenceOrchestrator po) {
        String className = getEnd().getEntityType().getEntityName();

        if(Settings.doSQL(po)) {
            StringBuilder join = new StringBuilder();
            join.append(" LEFT OUTER JOIN ")
                .append(getNormalizedName())
                .append(" AS ")
                .append(getEnd().getAlias())
                .append(" ON (")
                .append(((JDBCProperty)property).getOnClause(getStart().getAlias(), getEnd().getAlias()))
                .append(")");
            return join.toString();
        } else {
            // If the join edge represents an open content, that means that relationship is
            // not captured by the ORM and the join condition has to be explicitly
            // specified in the WHERE clause of the OQL
            if (property.isOpenContent()) {
                return QueryBuilder.COMMA_DELIMITER + className + QueryBuilder.AS_CLAUSE
                    + getEnd().getAlias();
            }
            else {
                return po.getOQLJoinFragment((IntraQuery<QueryFragment>)this);
            }
        }
    }

    public String getNormalizedName() {
        return getStart().getAlias() + Settings.PATH_DELIMITER + property.getName();
    }
}
