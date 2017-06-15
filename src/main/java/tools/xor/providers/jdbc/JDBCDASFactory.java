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

package tools.xor.providers.jdbc;

import tools.xor.TypeMapper;
import tools.xor.service.AbstractDASFactory;
import tools.xor.service.DataAccessService;
import tools.xor.service.HibernateDAS;
import tools.xor.service.JPADAS;
import tools.xor.service.PersistenceOrchestrator;

import javax.inject.Inject;
import javax.sql.DataSource;

public class JDBCDASFactory extends AbstractDASFactory
{
    public DataSource getDataSource ()
    {
        return dataSource;
    }

    public void setDataSource (DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @Inject
    private DataSource dataSource;

    public JDBCDASFactory(){}

    public JDBCDASFactory(String name) {
        setName(name);
    }

    @Override protected HibernateDAS createHibernateDAS (TypeMapper typeMapper)
    {
        throw new UnsupportedOperationException("Hibernate configuration is not supported");
    }

    @Override protected JPADAS createJPADAS (TypeMapper typeMapper, String name)
    {
        throw new UnsupportedOperationException("Hibernate configuration is not supported");
    }

    @Override public void injectDependencies (Object bean, String name)
    {

    }

    @Override
    protected DataAccessService createCustomDAS(TypeMapper typeMapper, String name) {
        return new JDBCDAS();
    }

    @Override
    public PersistenceOrchestrator getPersistenceOrchestrator(Object sessionContext) {
        JDBCPersistenceOrchestrator po = (JDBCPersistenceOrchestrator)super.getPersistenceOrchestrator(sessionContext);

        if(po.getDataSource() == null) {
            po.setDataSource(dataSource);
        }

        return po;
    }
}
