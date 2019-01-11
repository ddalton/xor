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

package tools.xor.view;

import tools.xor.Settings;
import tools.xor.service.QueryCapability;
import tools.xor.util.IntraQuery;

/**
 * Identifies the information of a column value in the query result.
 * The column value could be one of the following:
 * 1. User requested data
 * 2. Framework requested data to satisfy how the data should be constructed
 *    For example:
 *    a. Add the identified field to satisfy SHARED object resolution
 *    b. Add a list index field to satisfy the list ordering property
 *
 * This class is also responsible for producing the OQL representation of the field
 */
public class QueryField implements Comparable<QueryField>
{
    private String  path;           // simple property name or a path if an embedded field
    private int     position;       // position of this field in the query result record
    private QueryFragment fragment; // The QueryFragment to which this field belongs

    public QueryField(String path, int position, QueryFragment fragment) {
        this.path = path;
        this.position = position;
        this.fragment = fragment;
    }

    /**
     * Retrieve the OQL query representation of this field.
     * @return OQL query representation
     */
    public String getOQL() {
        return fragment.getAlias() + Settings.PATH_DELIMITER + path;
    }

    public int getPosition() {
        return position;
    }

    public String getPath() {
        return this.path;
    }

    @Override public int compareTo (QueryField o)
    {
        return getPosition() - o.getPosition();
    }

    public Integer getAttributeLevel() {
        int noOfSteps = 0;
        for(int delimPos = path.indexOf(Settings.PATH_DELIMITER, 0); delimPos != -1; delimPos = path.indexOf(Settings.PATH_DELIMITER, delimPos+1))
            noOfSteps++;

        return noOfSteps;
    }
}
