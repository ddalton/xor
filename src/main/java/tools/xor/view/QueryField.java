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

import tools.xor.JDBCType;
import tools.xor.Settings;
import tools.xor.service.QueryCapability;

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
    private boolean augmenter;      // This property is not part of the result but is
                                    // needed to construct the result

    public QueryField(String path, int position, QueryFragment fragment) {
        this.path = path;
        this.position = position;
        this.fragment = fragment;
    }

    public QueryField(String path, int position, QueryFragment fragment, boolean augmenter) {
        this(path, position, fragment);
        this.augmenter = augmenter;
    }

    public boolean isAugmenter() {
        return this.augmenter;
    }

    /**
     * Retrieve the OQL query representation of this field.
     * @param qc QueryCapability instance
     * @return OQL query representation
     */
    public String getOQL(QueryCapability qc) {
        if(qc == null) {
            return getSQL();
        }

        String result = fragment.getAlias() + Settings.PATH_DELIMITER + path;

        if(path.endsWith(QueryFragment.LIST_INDEX_ATTRIBUTE))
            result = qc.getListIndexMechanism(fragment.getAlias());
        else if(path.endsWith(QueryFragment.MAP_KEY_ATTRIBUTE))
            result = qc.getMapKeyMechanism(fragment.getAlias());
        else if(path.equals(fragment.getEntityType().getIdentifierProperty().getName())) {
                // Is this an id property
            result = qc.getSurrogateValueMechanism(fragment.getAlias(), Settings.PATH_DELIMITER + path);
        }

        return result;
    }

    public String getSQL() {
        if(path.endsWith(QueryFragment.LIST_INDEX_ATTRIBUTE) || path.endsWith(QueryFragment.MAP_KEY_ATTRIBUTE)) {
            throw new UnsupportedOperationException("LIST or MAP functions not supported in a SQL context");
        }

        String result = ((JDBCType) fragment.getEntityType()).getColumn(path);
        result = fragment.getAlias() + Settings.PATH_DELIMITER + result;

        return result;
    }

    public QueryFragment getQueryFragment() {
        return this.fragment;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int value) {
        this.position = value;
    }

    public String getPath() {
        return this.path;
    }

    public String getFullPath() {
        return this.fragment.getFullPath(this.path);
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
