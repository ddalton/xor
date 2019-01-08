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

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import tools.xor.OpenType;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.TypeMapper;
import tools.xor.TypeNarrower;
import tools.xor.service.AggregateManager;
import tools.xor.service.DataAccessService;
import tools.xor.service.PersistenceOrchestrator;
import tools.xor.service.Shape;
import tools.xor.util.PersistenceType;
import tools.xor.view.AggregateView;
import tools.xor.view.QueryTransformer;
import tools.xor.view.View;

import javax.sql.DataSource;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * This class is part of the Data Access Service framework
 *
 * @author Dilip Dalton
 */
public abstract class JDBCDAS implements DataAccessService
{

    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

    public abstract DataSource getDataSource();

    @Override public Shape getShape ()
    {
        return null;
    }

    @Override public Shape getOwner (tools.xor.EntityType entityType)
    {
        return null;
    }

    @Override public void addShape (String name)
    {

    }

    @Override public Shape getOrCreateShape (String name, Shape parent)
    {
        return null;
    }

    @Override public Type getType (Class<?> clazz)
    {
        return null;
    }

    @Override public Type getExternalType (Class<?> clazz)
    {
        return null;
    }

    @Override public Type getType (String name)
    {
        return null;
    }

    @Override public Type getExternalType (String name)
    {
        return null;
    }

    @Override public List<Type> getTypes ()
    {
        return null;
    }

    @Override public void postProcess (Object newInstance, boolean autoWire)
    {

    }

    @Override public TypeMapper getTypeMapper ()
    {
        return null;
    }

    @Override public QueryTransformer getQueryBuilder ()
    {
        return new QueryTransformer();
    }

    @Override
    public PersistenceType getAccessType ()
    {
        return PersistenceType.JDBC;
    }

    @Override public void sync (AggregateManager am, Map<String, List<AggregateView>> avVersions)
    {

    }

    @Override public void refresh (TypeNarrower typeNarrower)
    {

    }

    @Override public void populateNarrowedClass (Class<?> entityClass, TypeNarrower typeNarrower)
    {

    }

    @Override public Class<?> getNarrowedClass (Class<?> entityClass, String viewName)
    {
        return null;
    }

    @Override public View getView (String viewName)
    {
        return null;
    }

    @Override public View getView (tools.xor.EntityType entityType)
    {
        return null;
    }

    @Override public void addView (AggregateView view)
    {

    }

    @Override public List<View> getViews ()
    {
        return null;
    }

    @Override public View getBaseView (tools.xor.EntityType entityType)
    {
        return null;
    }

    @Override public List<String> getViewNames ()
    {
        return null;
    }

    @Override public PersistenceOrchestrator createPO (Object sessionContext, Object data)
    {
        JDBCPersistenceOrchestrator po = new JDBCPersistenceOrchestrator(sessionContext, data);
        po.setDataSource(getDataSource());

        return po;
    }

    @Override public void addProperty (Property property)
    {

    }

    @Override public void removeProperty (Property property)
    {

    }

    @Override public void addOpenProperty (Property openProperty)
    {

    }

    @Override public void removeOpenProperty (Property openProperty)
    {

    }

    @Override public void addOpenType (OpenType type)
    {

    }

    @Override public void initGenerators (InputStream is)
    {

    }

    @Override public Settings.SettingsBuilder settings ()
    {
        return new Settings.SettingsBuilder(null);
    }

}

