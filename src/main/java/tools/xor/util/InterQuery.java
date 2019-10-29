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

import tools.xor.view.QueryFragment;
import tools.xor.view.QueryTree;

public class InterQuery<V extends QueryTree> extends Edge<V>
{
    private QueryFragment source;
    private QueryFragment target;

    public InterQuery (String name, V start, V end, QueryFragment source, QueryFragment target)
    {
        super(name, start, end);

        this.source = source;
        this.target = target;
    }

    public static enum JoinType {
        INLIST,    // used if the interquery edge is satisfied using an IN list
        SUBQUERY,  // currently not used, due to issues with mix between SQL, StoredProcedure
                   // and OQL parent queries
        JOINTABLE, // A table/temporary table that is used by the stored procedure child query
                   // or a SQL child query. Helps in reducing SQL plan cache size.
                   // Needed to support stored procedures
        NONE       // used if the interquery edge is not present, such as a root query piece
    };

    public QueryFragment getSource() {
        return this.source;
    }

    public QueryFragment getTarget() {
        return this.target;
    }
}
