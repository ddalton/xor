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

package tools.xor.service;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;

import tools.xor.providers.jdbc.DBTranslator;
import tools.xor.util.ClassUtil;
import tools.xor.view.QueryJoinAction;
import tools.xor.view.StoredProcedure;

public class HibernatePersistenceUtil implements PersistenceUtil {
    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

    public void execute (Work work, EntityManager entityManager)
    {
        Session session = entityManager.unwrap(Session.class);
        session.doWork(work);
    }   
    
    private static class BlobCreator implements Work {

        Blob blob;

        public Blob getBlob() {
            return this.blob;
        }

        @Override public void execute (Connection connection) throws SQLException
        {
            this.blob = connection.createBlob();
        }
    }    
    
    @Override
    public Blob createBlob(DataStore po) {
        BlobCreator blobCreator = new BlobCreator();
        
        Work work = new Work()
        {
            @Override
            public void execute(Connection connection) throws SQLException {
                blobCreator.execute(connection);              
            }
        };       
        execute(work, ((JPADataStore)po).getEntityManager());        

        return blobCreator.getBlob();
    }

    @Override
    public void createStatement(DataStore po, StoredProcedure sp) {
        Work work = new Work()
        {
            @Override
            public void execute(Connection connection) throws SQLException {

                try {
                    DatabaseMetaData dbmd = connection.getMetaData();
                    if (!dbmd.supportsStoredProcedures()) {
                        throw new UnsupportedOperationException(
                            "Stored procedures with JDBC escape syntax is not supported");
                    }

                    if (sp.isImplicit()) {
                        sp.setStatement(connection.createStatement());
                    }
                    else {
                        sp.setStatement(connection.prepareCall(sp.jdbcCallString()));
                    }
                }
                catch (SQLException e) {
                    logger.info("Unable to retrieve JDBC metadata: " + e.getMessage());
                }                
            }
        };       
        execute(work, ((JPADataStore)po).getEntityManager());
    }

    @Override
    public void saveQueryJoinTable(DataStore po, String invocationId, Set ids) {
        Work work = new Work()
        {
            @Override
            public void execute (Connection connection) throws SQLException
            {
                ((JPADataStore)po).saveQueryJoinTable(connection, invocationId, ids);
            }
        };
        execute(work, ((JPADataStore)po).getEntityManager());        
    }

    @Override
    public void createQueryJoinTable(DataStore po, Integer stringKeyLen) {
        Work work = new Work()
        {
            @Override
            public void execute (Connection connection) throws SQLException
            {
                DBTranslator translator = DBTranslator.getTranslator(connection);
                if(translator.tableExists(connection, QueryJoinAction.JOIN_TABLE_NAME)) {
                    return;
                }
                String sql = translator.getCreateQueryJoinTableSQL(stringKeyLen);

                try {
                    Statement statement = connection.createStatement();
                    statement.executeUpdate(sql);
                }
                catch (SQLException e) {
                    throw ClassUtil.wrapRun(e);
                }
            }
        };
        execute(work, ((JPADataStore)po).getEntityManager());        
    }
}
