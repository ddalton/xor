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

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.generator.DefaultGenerator;
import tools.xor.generator.StringTemplate.QueryVisitor;
import tools.xor.util.ClassUtil;
import tools.xor.util.graph.StateGraph;

public class QueryGenerator implements Iterator<Object[]>, GeneratorDriver, Closeable
{
    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
    
    private final String sql;
    private final int max;
    private Statement statement;
    private ResultSet rs;
    private int numCols;
    private boolean isLast;
    private int rowCount = 0;
    private Object[] row;
    private int fetchSize = 1000;
    private StateGraph.ObjectGenerationVisitor visitor;
    private List<DefaultGenerator.GeneratorVisit> visits;

    // Used if the generator is a driver
    private Set<IteratorListener> listeners = new HashSet<>();

    public QueryGenerator(String[] args) {
        if(args.length >= 1) {
            this.sql = args[0];
        } else {
            this.sql = null;
        }

        if(args.length >= 2) {
            this.max = Integer.parseInt(args[1]);
        } else {
            // Negative value means we read all the records
            this.max = -1;
        }
    }
    
    public QueryGenerator (String sql,
                           int max)
    {
        this.sql = sql;
        this.max = max;
    }

    public void setFetchSize(int size) {
        this.fetchSize = size;
    }

    public void init (Connection connection, StateGraph.ObjectGenerationVisitor visitor)
    {
        this.visitor = visitor;

        try {
            // Check if we need to reset
            // this can happen if the generator is being used a second time (e.g., UPDATE phase of CSVLoader)
            this.isLast = false;
            if(this.statement != null && !this.statement.isClosed()) {
                this.statement.close();
            }         
            
            QueryVisitor qv = new QueryVisitor();
            String processedSql = qv.process(this.sql);
            this.statement = connection.prepareStatement(processedSql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            
            qv.bindPositions((PreparedStatement) this.statement, visitor);        
            this.statement.setFetchSize(fetchSize);

            logger.debug("QueryGenerator executing query -> " + this.sql);
            this.rs = ((PreparedStatement)this.statement).executeQuery();

            ResultSetMetaData rsmd = rs.getMetaData();
            this.numCols = rsmd.getColumnCount();

            this.row = new Object[numCols + 1];
            
            logger.info("Setting the row object array on the visitor");
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

        return ((rowCount < max || max < 0) && !isLast);
    }

    @Override public Object[] next ()
    {
        try {
            isLast = !rs.next();
            if (isLast) {
                logger.debug("QueryGenerator#next - No more records to process");
                return null;
            }

            row[0] = rowCount++;
            for (int i = 1; i <= numCols; i++) {
                row[i] = rs.getObject(i);
            }
            
            if(logger.isDebugEnabled()) {
                List<String> record = new ArrayList<>();
                for(Object obj: row) {
                    record.add(obj==null?"":(obj.toString()+":"+obj.getClass().getName()));
                }
                logger.debug("QueryGenerator#next -> " + String.join(",", record));
            }
            visitor.setContext(0, rowCount);
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

    public void addVisit(DefaultGenerator.GeneratorVisit visit) {
        if(this.visits == null) {
            this.visits = new LinkedList<>();
        }

        this.visits.add(visit);
    }

    public void processVisitors() {
        if(this.visits != null) {
            for (DefaultGenerator.GeneratorVisit visit : visits) {
                visit.getRecipient().accept(visit.getGenerator());
            }
        }
    }

    public void addListener(IteratorListener listener) {
        listeners.add(listener);
    }

    protected void notifyListeners(int sourceId, StateGraph.ObjectGenerationVisitor visitor) {
        for(IteratorListener listener: listeners) {
            listener.handleEvent(sourceId, visitor);
        }
    }
}
