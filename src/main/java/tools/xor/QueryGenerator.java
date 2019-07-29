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

package tools.xor;

import tools.xor.util.ClassUtil;
import tools.xor.util.graph.StateGraph;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;

public class QueryGenerator implements Iterator<Object[]>, EntityGenerator, Closeable
{
    private final String sql;
    private final int max;
    private Statement statement;
    private ResultSet rs;
    private int numCols;
    private boolean isLast;
    private int rowCount;
    private Object[] row;
    private int fetchSize = 1000;
    private StateGraph.ObjectGenerationVisitor visitor;

    public QueryGenerator (String sql,
                           int max)
    {
        this.sql = sql;
        this.max = max;
        this.rowCount = 0;
    }

    public void setFetchSize(int size) {
        this.fetchSize = size;
    }

    public void init (Connection connection, StateGraph.ObjectGenerationVisitor visitor)
    {
        this.visitor = visitor;

        try {
            this.statement = connection.createStatement(
                ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY);
            this.statement.setFetchSize(fetchSize);

            this.rs = this.statement.executeQuery(this.sql);

            ResultSetMetaData rsmd = rs.getMetaData();
            this.numCols = rsmd.getColumnCount();

            this.row = new Object[numCols + 1];
            this.visitor.setContext(row);
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        }
    }

    @Override public boolean hasNext ()
    {
        if (statement == null) {
            throw new RuntimeException("The generator has not been initialized");
        }

        return (rowCount < max && !isLast);
    }

    @Override public Object[] next ()
    {
        try {
            isLast = !rs.next();
            if (isLast) {
                return null;
            }

            row[0] = rowCount++;
            for (int i = 1; i <= numCols; i++) {
                row[i] = rs.getObject(i);
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }

        return row;
    }

    @Override public void close () throws IOException
    {
        try {
            if (rs != null) {
                rs.close();
                statement.close();
            }
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        }
    }
}
