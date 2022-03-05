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
package tools.xor.logic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.sql.DataSource;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import tools.xor.BusinessObject;
import tools.xor.CollectionElementGenerator;
import tools.xor.CollectionOwnerGenerator;
import tools.xor.CounterGenerator;
import tools.xor.ElementGenerator;
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.GeneratorDriver;
import tools.xor.ImmutableBO;
import tools.xor.JDBCProperty;
import tools.xor.JDBCType;
import tools.xor.MapperSide;
import tools.xor.QueryGenerator;
import tools.xor.Settings;
import tools.xor.TypeMapper;
import tools.xor.generator.Choices;
import tools.xor.generator.DateRange;
import tools.xor.generator.DefaultGenerator;
import tools.xor.generator.ElementPositionGenerator;
import tools.xor.generator.Generator;
import tools.xor.generator.GeneratorRecipient;
import tools.xor.generator.QueryDataField;
import tools.xor.generator.RangePercent;
import tools.xor.generator.StringTemplate;
import tools.xor.providers.jdbc.DBTranslator;
import tools.xor.providers.jdbc.DBType;
import tools.xor.providers.jdbc.ImportMethod;
import tools.xor.providers.jdbc.JDBCDataModel;
import tools.xor.providers.jdbc.JDBCDataStore;
import tools.xor.providers.jdbc.JDBCSessionContext;
import tools.xor.service.AbstractDataModel;
import tools.xor.service.AggregateManager;
import tools.xor.service.DataModel;
import tools.xor.service.DataStore;
import tools.xor.service.SchemaExtension;
import tools.xor.service.Shape;
import tools.xor.util.ClassUtil;
import tools.xor.util.ObjectCreator;
import tools.xor.view.AggregateView;

@ExtendWith(SpringExtension.class)
//@ContextConfiguration(locations = { "classpath:/spring-hana-jdbc-test.xml" })
@ContextConfiguration(locations = { "classpath:/spring-jdbc-test.xml" })
public class HanaPerf1Test
{
    String dbTypeFolderName;
    ImportMethod importMethod = ImportMethod.PREPARED_STATEMENT;

    private static final int US_USERTAB_COUNT = 500;

    @Autowired
    protected AggregateManager am;

    @Autowired
    protected DataSource dataSource;

    @BeforeEach
    public void setup() throws SQLException
    {
        am.configure(new Settings());
        JDBCDataStore po = (JDBCDataStore)am.getDataStore();
        JDBCSessionContext sc = po.getSessionContext();
        sc.beginTransaction();

        try {
            Connection c = sc.getConnection();
            DBType dbType = DBTranslator.getDBType(c);
            dbTypeFolderName = dbType.name().toLowerCase();
        } finally {
            sc.close();
        }

        // Uncomment the following line to create tables related to this test case
        ClassUtil.executeScript(dataSource, String.format("scripts/%s/HanaPerf1_create.sql", dbTypeFolderName));
        ClassUtil.executeScript(dataSource, String.format("scripts/%s/HanaPerf2_create.sql", dbTypeFolderName));

        addAdditionalRelationships();

        populate();
    }

    private void addAdditionalRelationships()
    {
        DataModel das = am.getDataModel();

        SchemaExtension relationshipExtension = new SchemaExtension()
        {
            @Override public void extend (Shape shape)
            {
                // Add FK relationships
                // Responsibilities is a composition relationship
                JDBCType userType = (JDBCType)shape.getType("USERTAB");
                JDBCType respType = (JDBCType)shape.getType("GROUPRESPONSIBLETAB");
                JDBCDataModel.ForeignKey fk = new JDBCDataModel.ForeignKey("FK1_1__N_allresps", respType.getTableInfo(), userType.getTableInfo(),
                    JDBCDataModel.ForeignKeyRule.NO_ACTION,
                    JDBCDataModel.ForeignKeyRule.NO_ACTION);
                JDBCProperty userid = (JDBCProperty) ClassUtil.getDelegate(respType.getProperty("ROOTID"));
                JDBCProperty userProp = new JDBCProperty("user", userid.getColumns(), userType, respType, fk);
                fk.makeComposition();
                userProp.initMappedBy(shape);
                respType.addProperty(userProp);

                // user relationship between USERTAB and US_USERTAB
                JDBCType ususerType = (JDBCType)shape.getType("US_USERTAB");
                fk = new JDBCDataModel.ForeignKey("FK1_1__1_userdetails", userType.getTableInfo(), ususerType.getTableInfo(),
                    JDBCDataModel.ForeignKeyRule.NO_ACTION,
                    JDBCDataModel.ForeignKeyRule.NO_ACTION);
                userProp = (JDBCProperty)ClassUtil.getDelegate(userType.getProperty("US_USER"));
                JDBCProperty userEntityProp = new JDBCProperty("user", userProp.getColumns(), ususerType, userType, fk);
                userProp.initMappedBy(shape);
                userType.addProperty(userEntityProp);

                // Responsibilities is a composition relationship
                // US_GROUPTAB and US_BASEIDTAB using LVID (make LVID as not null)
                JDBCType groupType = (JDBCType)shape.getType("US_GROUPTAB");
                JDBCType baseidType = (JDBCType)shape.getType("US_BASEIDTAB");
                fk = new JDBCDataModel.ForeignKey("FK1_1__N_allusers", baseidType.getTableInfo(), groupType.getTableInfo(),
                    JDBCDataModel.ForeignKeyRule.NO_ACTION,
                    JDBCDataModel.ForeignKeyRule.NO_ACTION);
                JDBCProperty lvid = (JDBCProperty)ClassUtil.getDelegate(baseidType.getProperty("LVID"));
                JDBCProperty groupProp = new JDBCProperty("group", lvid.getColumns(), groupType, baseidType, fk);
                groupProp.setNullable(false); // This field cannot be null and helps with topological sort
                groupProp.initMappedBy(shape);
                groupType.addProperty(groupProp);

                // US_BASEIDTAB and PRIVATEORGGROUPTAB
                JDBCType pogType = (JDBCType)shape.getType("PRIVATEORGGROUPTAB");
                fk = new JDBCDataModel.ForeignKey("FK1_1__N_allids", baseidType.getTableInfo(), pogType.getTableInfo(),
                    JDBCDataModel.ForeignKeyRule.NO_ACTION,
                    JDBCDataModel.ForeignKeyRule.NO_ACTION);
                JDBCProperty rootid = (JDBCProperty)ClassUtil.getDelegate(baseidType.getProperty("ROOTID"));
                JDBCProperty pogProp = new JDBCProperty("pog", rootid.getColumns(), pogType, baseidType, fk);
                fk.makeComposition();
                pogProp.initMappedBy(shape);
                baseidType.addProperty(pogProp);

                // inheritance
                pogType.setParentType(groupType);

                fk = new JDBCDataModel.ForeignKey("FK1_1__N_procresps", respType.getTableInfo(), groupType.getTableInfo(),
                    JDBCDataModel.ForeignKeyRule.NO_ACTION,
                    JDBCDataModel.ForeignKeyRule.NO_ACTION);
                JDBCProperty parentGroup = (JDBCProperty)ClassUtil.getDelegate(respType.getProperty("GRB_GROUP"));
                JDBCProperty parentProp = new JDBCProperty("parentgroup", parentGroup.getColumns(), groupType, respType, fk);
                parentProp.setNullable(false); // needed for dependency
                parentProp.initMappedBy(shape);
                respType.addProperty(parentProp);

                JDBCType procType = (JDBCType)shape.getType("PROCUREMENTUNITTAB");
                fk = new JDBCDataModel.ForeignKey("FK1_1__1_context", procType.getTableInfo(), pogType.getTableInfo(),
                    JDBCDataModel.ForeignKeyRule.NO_ACTION,
                    JDBCDataModel.ForeignKeyRule.NO_ACTION);
                JDBCProperty uniqueProp = (JDBCProperty)ClassUtil.getDelegate(procType.getProperty("PROR_UNIQUENAME"));
                pogProp = new JDBCProperty("pog", uniqueProp.getColumns(), pogType, procType, fk);
                fk.makeComposition();
                pogProp.initMappedBy(shape);
                procType.addProperty(pogProp);

                fk = new JDBCDataModel.ForeignKey("FK1_1__N_procresps", respType.getTableInfo(), procType.getTableInfo(),
                    JDBCDataModel.ForeignKeyRule.NO_ACTION,
                    JDBCDataModel.ForeignKeyRule.NO_ACTION);
                JDBCProperty proc = (JDBCProperty)ClassUtil.getDelegate(respType.getProperty("GRB_PROCUREMENTUNIT"));
                JDBCProperty procProp = new JDBCProperty("procunit", proc.getColumns(), procType, respType, fk);
                procProp.setNullable(false); // needed for dependency
                procProp.initMappedBy(shape);
                respType.addProperty(procProp);
            }
        };

        SchemaExtension generatorExtension = new SchemaExtension()
        {
            @Override public void extend (Shape shape)
            {
                // A user has certain responsibilities, with each responsibility for a
                // procurement unit modelled by a group.
                // The user is then associated with the procurement unit
                // though a N:N relationship.

                // The group's actual members are then checked with these responsibility
                // mappings and if there are any users that are not part of the group's
                // members, then thay are returned along with the reponsibility group
                //
                /*

                    responsibilities (1:N)
       - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -   UserTab
       |               [lvid]                                                               |
       |                                                                                    |  user (1:1)
       |                                                                                    |
       |                                                                                    \/
       |                                    users (1:N)                   val (1:1)
       |                       GroupTab     - - - - ->      BaseIdTab   - - - - - >     US_usertab
       |                                      [lvid]
       |                           ^
       |                           |
       |                           |
       |                                               uniquename
       |                   PrivateOrgGroupTab   < - - - - - - - - - - - - - -
       |                           |                   [context]             |
       |                           |                                         |
       |     group  | - - -> <- - -  parent                                  |
       |            |                         procurementUnit                |
       - - ->   GroupResponsibleTab  - - - - - - - - - - - - ->    ProcurementUnitTab


                  The query is to check if there are any users that are part of a PrivateOrgGroup
                  from group responsibilities, that are not in the users collection of such
                  PrivateOrgGroup

                 */


                Generator rootidgen = new StringTemplate(new String[] {"ID_[VISITOR_CONTEXT]"});
                Generator orgs = new Choices(new String[] {"ORG1", "ORG2", "ORG3", "ORG4", "ORG5"});
                Generator daterange = new DateRange(new String[0]);
                Generator uniquegen = new StringTemplate(new String[] {"UNQ_[VISITOR_CONTEXT]"});
                Generator partition = new DefaultGenerator(new String[] {"10", "10"});
                Generator purgestate = new DefaultGenerator(new String[] {"0", "0"});
                Generator grusersgen = new StringTemplate(new String[] {"USERS_[VISITOR_CONTEXT]"});
                Generator password = new DefaultGenerator(new String[] {"PasswordAdapter1"});

                /*
                 Phase 1 -
                   populate US_USERTAB
                            USERTAB
                            PROCUREMENTUNITTAB
                            US_GROUPTAB
                            PRIVATEORGGROUPTAB
                            BASEIDTAB

                 Phase 2 -
                          populate GROUPRESPONSIBLETAB using below query
                    select u.rootid, u.US_RESPONSIBLE, p.rootid, pog.parent
                    from USERTAB u,
                         US_BASEIDTAB bid,
                         US_GROUPTAB g,
                         PRIVATEORGGROUPTAB pog,
                         PROCUREMENTUNITTAB p
                    WHERE
                        u.rootid = bid.rootid AND
                        bid.lvid = g.GR_USERS AND
                        g.rootid = pog.rootid AND
                        pog.PGR_CONTEXT = p.PROR_UNIQUENAME


                 Support VisitorContext array and refer it using [VISITOR_CONTEXT_1]
                 Also enable batching for performance
                */

                // US_USERTAB
                // ==========

                // Add the generators
                // ROOTID
                EntityType user = (EntityType)shape.getType("US_USERTAB");
                GeneratorDriver gensettings = new CounterGenerator(US_USERTAB_COUNT);
                //EntityGenerator gensettings = new CounterGenerator(3500000);
                user.addGenerator(gensettings);

                ExtendedProperty rootid = (ExtendedProperty)user.getProperty("ROOTID");
                rootid.setGenerator(rootidgen);
                // CUS_ORGANIZATION
                ExtendedProperty org = (ExtendedProperty)user.getProperty("CUS_ORGANIZATION");
                org.setGenerator(orgs);
                // CUS_CREATED
                ExtendedProperty created = (ExtendedProperty)user.getProperty("CUS_CREATED");
                created.setGenerator(daterange);
                // CUS_MODIFIED
                ExtendedProperty modified = (ExtendedProperty)user.getProperty("CUS_MODIFIED");
                modified.setGenerator(daterange);
                // CUS_UNIQUENAME
                ExtendedProperty unique = (ExtendedProperty)user.getProperty("CUS_UNIQUENAME");
                unique.setGenerator(uniquegen);
                ExtendedProperty pwd = (ExtendedProperty)user.getProperty("CUS_PASSWORDADAPTER");
                pwd.setGenerator(password);

                // USERTAB
                // ROOTID
                EntityType u = (EntityType)shape.getType("USERTAB");
                gensettings = new CounterGenerator(500);
                //gensettings = new CounterGenerator(3000000);
                u.addGenerator(gensettings);

                rootid = (ExtendedProperty)u.getProperty("ROOTID");
                rootid.setGenerator(rootidgen);
                ExtendedProperty ususer = (ExtendedProperty)u.getProperty("US_USER");
                ususer.setGenerator(rootidgen);
                ExtendedProperty part = (ExtendedProperty)u.getProperty("US_PARTITIONNUMBER");
                part.setGenerator(partition);
                ExtendedProperty purge = (ExtendedProperty)u.getProperty("US_PURGESTATE");
                purge.setGenerator(purgestate);
                Generator responsible = new StringTemplate(new String[] {"RESP_[VISITOR_CONTEXT]"});
                ExtendedProperty res = (ExtendedProperty)u.getProperty("US_RESPONSIBLE");
                res.setGenerator(responsible);


                // US_GROUPTAB
                EntityType g = (EntityType)shape.getType("US_GROUPTAB");
                gensettings = new CounterGenerator(100);
                //gensettings = new CounterGenerator(29875);
                g.addGenerator(gensettings);

                Generator isglobalgen = new DefaultGenerator(new String[] {"0", "0"});

                rootid = (ExtendedProperty)g.getProperty("ROOTID");
                rootid.setGenerator(rootidgen);
                ExtendedProperty isglobal = (ExtendedProperty)g.getProperty("GR_ISGLOBAL");
                isglobal.setGenerator(isglobalgen);
                part = (ExtendedProperty)g.getProperty("GR_PARTITIONNUMBER");
                part.setGenerator(partition);
                purge = (ExtendedProperty)g.getProperty("GR_PURGESTATE");
                purge.setGenerator(purgestate);
                unique = (ExtendedProperty)g.getProperty("GR_UNIQUENAME");
                unique.setGenerator(uniquegen);
                created = (ExtendedProperty)g.getProperty("GR_CREATED");
                created.setGenerator(daterange);
                modified = (ExtendedProperty)g.getProperty("GR_MODIFIED");
                modified.setGenerator(daterange);
                ExtendedProperty grusers = (ExtendedProperty)g.getProperty("GR_USERS");
                grusers.setGenerator(grusersgen);


                // PRIVATEORGGROUPTAB
                EntityType p = (EntityType)shape.getType("PRIVATEORGGROUPTAB");
                //gensettings = new CounterGenerator(14693, 29875);
                gensettings = new CounterGenerator(400, 100);
                p.addGenerator(gensettings);

                Generator parentgen = new RangePercent(new String[] {"ID_[__]",
                                                                     "0,3:0.06",
                                                                     "4,15:0.18",
                                                                     "16,35:0.30",
                                                                     "36,85:0.36",
                                                                     "86,2576:1.00"
                });
                Generator contextgen = new StringTemplate(new String[] {"CTX_[VISITOR_CONTEXT]"});

                rootid = (ExtendedProperty)p.getProperty("ROOTID");
                rootid.setGenerator(rootidgen);
                ExtendedProperty parent = (ExtendedProperty)p.getProperty("PGR_PARENT");
                parent.setGenerator(parentgen);
                ExtendedProperty context = (ExtendedProperty)p.getProperty("PGR_CONTEXT");
                context.setGenerator(contextgen);

                // inheritance
                p.setParentType(g);


                // US_BASEIDTAB
                // Support 2 types of settings
                // 1. Between us_grouptab and users
                // 2. Between privateorggrouptab and users
                EntityType bid = (EntityType)shape.getType("US_BASEIDTAB");
                GeneratorDriver cegen = new CollectionElementGenerator(new String[] { "1", "500"});
                String[] collectionSizes = new String[] { "4500", "1,100:1", "101,310:2", "311,500:10" };   // NOTE: Do not limit this
                //EntityGenerator cegen = new CollectionElementGenerator(new String[] { "1", "3000000"});
                //String[] collectionSizes = new String[] { "1000000",
                //                                          "40501,40600:10000,50000"
                //};
                /*
                String[] collectionSizes = new String[] { "11259720",
                                                          "1,8920:1",
                                                          "8921,11416:2",
                                                          "11417,12466:3",
                                                          "12467,13355:4",
                                                          "13356,16967:5,10",
                                                          "16968,19009:11,20",
                                                          "19010,22778:21,300",
                                                          "22779,23691:301,10000",
                                                          "23692,23730:10001,50000",
                                                          "23731,23737:50001,240000",
                                                          "29876,33301:1",
                                                          "33302,34067:2",
                                                          "34068,34265:3",
                                                          "34266,34364:4",
                                                          "34365,35563:5,10",
                                                          "35564,36427:11,20",
                                                          "36428,38452:21,300",
                                                          "38452,39139:301,10000",
                                                          "39140,39145:10001,50000"
                                                          };   // NOTE: Do not limit this
                */

                GeneratorDriver cogen = new CollectionOwnerGenerator(collectionSizes,
                    (CollectionElementGenerator)cegen);
                bid.addGenerator(cogen);
                Generator lvidgen = new StringTemplate(new String[] {"USERS_[GENERATOR]"});
                Generator bidrootid = new StringTemplate(new String[] {"ID_[GENERATOR]"});

                ExtendedProperty val = (ExtendedProperty)bid.getProperty("VAL");
                Generator valgen = new StringTemplate(new String[] {"ID_[GENERATOR]"});
                val.setGenerator(valgen);
                ExtendedProperty index = (ExtendedProperty)bid.getProperty("LVINDEX");
                index.setGenerator(new ElementPositionGenerator((CollectionElementGenerator)cegen));
                ExtendedProperty lvid = (ExtendedProperty)bid.getProperty("LVID");
                lvid.setGenerator(lvidgen);
                rootid = (ExtendedProperty)bid.getProperty("ROOTID");
                rootid.setGenerator(bidrootid);

                cogen.addVisit(new DefaultGenerator.GeneratorVisit((Generator)cogen,
                    (GeneratorRecipient)lvidgen));
                cogen.addVisit(new DefaultGenerator.GeneratorVisit((Generator)cogen,
                    (GeneratorRecipient)bidrootid));
                cogen.addVisit(new DefaultGenerator.GeneratorVisit((Generator)cegen,
                    (GeneratorRecipient)valgen));

                // PROCUREMENTUNITTAB
                EntityType pru = (EntityType) shape.getType("PROCUREMENTUNITTAB");
                //EntityGenerator entitySettings = new CounterGenerator(14693, 29875);
                GeneratorDriver entitySettings = new CounterGenerator(400, 100);
                pru.addGenerator(entitySettings);

                rootid = (ExtendedProperty)pru.getProperty("ROOTID");
                rootid.setGenerator(rootidgen);
                unique = (ExtendedProperty)pru.getProperty("PROR_UNIQUENAME");
                unique.setGenerator(contextgen);
                part = (ExtendedProperty)pru.getProperty("PROR_PARTITIONNUMBER");
                part.setGenerator(partition);
                purge = (ExtendedProperty)pru.getProperty("PROR_PURGESTATE");
                purge.setGenerator(purgestate);
                Generator hiergen = new StringTemplate(new String[] {"HIER_[VISITOR_CONTEXT]"});
                ExtendedProperty hier = (ExtendedProperty)pru.getProperty("PROR_HIERARCHYPATH");
                hier.setGenerator(hiergen);


                // GROUPRESPONSIBLETAB
                String sql = " select" +
                    "        u.rootid," +
                    "        u.US_RESPONSIBLE," +
                    "        p.rootid," +
                    "        pog.pgr_parent " +
                    "from USERTAB u," +
                    "        US_BASEIDTAB bid," +
                    "        US_GROUPTAB g," +
                    "        PRIVATEORGGROUPTAB pog," +
                    "        PROCUREMENTUNITTAB p " +
                    "WHERE pog.rootid = g.rootid " +
                    "AND g.gr_users = bid.lvid " +
                    "AND pog.PGR_CONTEXT = p.PROR_UNIQUENAME " +
                    "AND u.rootid = bid.val";
                EntityType gr = (EntityType)shape.getType("GROUPRESPONSIBLETAB");
                gensettings = new QueryGenerator(sql, 4000);
                //gensettings = new QueryGenerator(sql, 3312087);
                gr.addGenerator(gensettings);

                // The above query returns four fields in
                // [QUERY_DATA.1]
                // [QUERY_DATA.2]
                // [QUERY_DATA.3]
                // [QUERY_DATA.4]

                //MYID = USER_DATA.0
                //ROOTID = USER_DATA.1
                //LVID = QUERY_DATA.2
                //grb_ProcurementUnit = QUERY_DATA.3
                //grb_Group=QUERY_DATA.4

                Generator myidgen = new QueryDataField(new String[] {"QUERY_DATA.0"});
                rootidgen = new QueryDataField(new String[] {"QUERY_DATA.1"});
                lvidgen = new QueryDataField(new String[] {"QUERY_DATA.2"});
                Generator pugen = new QueryDataField(new String[] {"QUERY_DATA.3"});
                Generator groupgen = new QueryDataField(new String[] {"QUERY_DATA.4"});

                ExtendedProperty myid = (ExtendedProperty)gr.getProperty("MYID");
                myid.setGenerator(myidgen);
                rootid = (ExtendedProperty)gr.getProperty("ROOTID");
                rootid.setGenerator(rootidgen);
                lvid = (ExtendedProperty)gr.getProperty("LVID");
                lvid.setGenerator(lvidgen);
                ExtendedProperty pu = (ExtendedProperty)gr.getProperty("GRB_PROCUREMENTUNIT");
                pu.setGenerator(pugen);
                ExtendedProperty grp = (ExtendedProperty)gr.getProperty("GRB_GROUP");
                grp.setGenerator(groupgen);


                // OREXPRESSIONTAB
                Generator exoOper = new DefaultGenerator(new String[] {"0", "0"});
                Generator exoExpr = new DefaultGenerator(new String[] {"xyz"});

                EntityType ore = (EntityType)shape.getType("OREXPRESSIONTAB");
                cegen = new CollectionElementGenerator(new String[] { "1", "180"});
                collectionSizes = new String[] { "5000000", "1,3:28" };
                //String[] typeSizes =  new String[] { "5000000", "1,5:10", "6,6:50000" };
                String[] typeSizes =  new String[] { "5000000", "1,5:10", "6,6:5" };

                cogen = new CollectionOwnerGenerator(collectionSizes, (ElementGenerator) cegen);
                CollectionOwnerGenerator typecogen = new CollectionOwnerGenerator(typeSizes,
                    (ElementGenerator)cogen);
                ore.addGenerator(typecogen);
                Generator orerootid = new StringTemplate(new String[] {"[GENERATOR]"});
                myidgen = new StringTemplate(new String[] {"[GENERATOR]"});

                rootid = (ExtendedProperty)ore.getProperty("ROOTID");
                rootid.setGenerator(orerootid);
                myid = (ExtendedProperty)ore.getProperty("MYID");
                myid.setGenerator(myidgen);
                ExtendedProperty operProp = (ExtendedProperty)ore.getProperty("EXO_OPERATION");
                operProp.setGenerator(exoOper);
                ExtendedProperty exprProp = (ExtendedProperty)ore.getProperty("EXO_EXPRESSIONS");
                exprProp.setGenerator(exoExpr);


                Base64Generator rootbidgen = new Base64Generator(null, typecogen, (CollectionOwnerGenerator) cogen);
                Base64Generator myidbidgen = new Base64Generator(null, typecogen, null);

                typecogen.addVisit(new DefaultGenerator.GeneratorVisit(rootbidgen,
                        (GeneratorRecipient)orerootid));
                typecogen.addVisit(new DefaultGenerator.GeneratorVisit(myidbidgen,
                        (GeneratorRecipient)myidgen));


                // Once generated, can import using the command
                // import from csv file '/tmp/scripts/US_USERTAB.csv' into "US_USERTAB" with column list in first row optionally enclosed by '''' threads 10 batch 5000
            }
        };

        // Rebuild the types
        das.removeShape(AbstractDataModel.DEFAULT_SHAPE);
        das.removeShape(AbstractDataModel.RELATIONAL_SHAPE);
        das.createShape(AbstractDataModel.DEFAULT_SHAPE, relationshipExtension);
        das.createShape(AbstractDataModel.RELATIONAL_SHAPE, generatorExtension);
    }

    private void populate() {
        String[] types = new String[] {
            "US_USERTAB",
            "USERTAB",
            "US_GROUPTAB",
            "PRIVATEORGGROUPTAB",
            "US_BASEIDTAB",
            "PROCUREMENTUNITTAB",
            "GROUPRESPONSIBLETAB",
            "OREXPRESSIONTAB"
        };

        Settings settings = new Settings();
        settings.setImportMethod(importMethod);
        am.generate(AbstractDataModel.RELATIONAL_SHAPE, Arrays.asList(types), settings);
    }

    @AfterEach
    public void teardown() throws SQLException
    {
        // Uncomment following line to drop all the tables
        ClassUtil.executeScript(dataSource, String.format("scripts/%s/HanaPerf1_drop.sql", dbTypeFolderName));
        ClassUtil.executeScript(dataSource, String.format("scripts/%s/HanaPerf2_drop.sql", dbTypeFolderName));
    }

    @Test
    /**
    SELECT
    Pri1.rootId,
    us_2.rootId
    FROM PrivateOrgGroupTab Pri1
    INNER JOIN us_GroupTab us_6 ON (Pri1.rootId = us_6.rootId),
    us_UserTab us_2 ,
    UserTab Use4
    INNER JOIN GroupResponsibleTab Gro3 ON (Use4.us_Responsible = Gro3.lvId
    AND Use4.rootId = Gro3.rootId)
    INNER JOIN ProcurementUnitTab Pro5 ON (Gro3.grb_ProcurementUnit = Pro5.rootId)
    WHERE (Pro5.pror_UniqueName = Pri1.pgr_Context
        AND Gro3.grb_Group = Pri1.pgr_Parent
        AND Use4.us_User = us_2.rootId
        AND us_2.rootId NOT IN ( SELECT
        us_101.rootId
        FROM us_BaseIdTab us_102
        INNER JOIN us_UserTab us_101 ON (us_102.val = us_101.rootId)
    WHERE us_102.rootId = Pri1.rootId
    AND us_102.lvId = us_6.gr_Users
                      AND (us_101.cus_Active = 1)
    AND (us_101.cus_PurgeState = 0)
    AND (us_101.cus_PartitionNumber = 14800)))
    AND (us_6.gr_Active = 1)
    AND (us_6.gr_PurgeState = 0)
    AND (us_6.gr_PartitionNumber = 14800)
    AND (us_2.cus_Active = 1)
    AND (us_2.cus_PurgeState = 0)
    AND (us_2.cus_PartitionNumber = 14800)
    AND (Use4.us_Active = 1)
    AND (Use4.us_PurgeState = 0)
    AND (Use4.us_PartitionNumber = 14800)
    AND (Pro5.pror_Active = 1)
    AND (Pro5.pror_PurgeState = 0)
    AND (Pro5.pror_PartitionNumber = 14800)
    ORDER BY 1 ASC
     */
    public void testQuery1() {

        if(importMethod == ImportMethod.PREPARED_STATEMENT) {
            am.configure(new Settings());
            JDBCDataStore po = (JDBCDataStore)am.getDataStore();
            JDBCSessionContext sc = po.getSessionContext();
            sc.beginTransaction();

            try {
                Connection c = sc.getConnection();

                // Check if the data has been populated on all the tables
                String sql = String.format("Select count(*) from US_USERTAB");
                try (
                    PreparedStatement ps = c.prepareStatement(sql);
                    ResultSet rs = ps.executeQuery();) {
                    if(rs.next()) {
                        int count = rs.getInt(1);
                        assert (count == US_USERTAB_COUNT);
                    }
                }
            }
            catch (SQLException e) {
                e.printStackTrace();
            } finally {
                sc.close();
            }
        }
    }

/*
stats are collected by 2 queries:
For example:
Used to get the distinct count of the collection owners
select count(distinct(rootid)) from US_BASEIDTAB  where rootid in (select rootid from privateorggrouptab) and lvid in (select lvid from us_baseidtab group by lvid having count(*) > 300 and count(*) <= 10000) and lvid in (select gr_users from us_grouptab)

Used to the count of the collection elements
dividing this by the total number for that collection type gives the percentage
select count(rootid) from US_BASEIDTAB  where rootid in (select rootid from privateorggrouptab) and lvid in (select lvid from us_baseidtab group by lvid having count(*) > 300 and count(*) <= 10000) and lvid in (select gr_users from us_grouptab)

 */


    //@Test
    public void deleteBaseId() {
        DataModel das = am.getDataModel();
        Shape shape = das.getShape(AbstractDataModel.RELATIONAL_SHAPE);
        TypeMapper typeMapper = am.getDataModel().getTypeMapper().newInstance(MapperSide.DOMAIN);
        typeMapper.setDomainShape(shape);

        Settings settings = new Settings();
        JDBCType entityType = (JDBCType)shape.getType("US_BASEIDTAB");
        settings.setEntityType(entityType);
        AggregateView view = new AggregateView();
        view.setAttributeList( Arrays.asList(new String[] { "ROOTID", "lvindex" }) );
        settings.setView(view);
        settings.init(shape);

        am.configure(settings);

        JDBCDataStore po = (JDBCDataStore)am.getDataStore();
        JDBCSessionContext sc = po.getSessionContext();

        sc.beginTransaction();

        // Get the connection
        Connection c = sc.getConnection();
        String sql = "Select rootid, lvindex from us_baseidtab where lvid like 'USERS_237%' order by rootid limit 50000";

        int i = 1;
        List<JSONObject> bids = new LinkedList<>();
        try (PreparedStatement ps = c.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {

            while(rs.next()) {
                String rootid = rs.getString(1);
                JSONObject json = new JSONObject();
                json.put("ROOTID", rootid);
                json.put("LVINDEX", rs.getString(2));
                bids.add(json);

                if(i++%1000 == 0) {
                    delete(bids, entityType, sc, settings, po, typeMapper);
                    sc.commit();
                    bids = new LinkedList<>();
                }
            }
            delete(bids, entityType, sc, settings, po, typeMapper);
            sc.commit();
        }
        catch (SQLException e) {
            e.printStackTrace();
        } finally {
            sc.close();
        }
    }

    //@Test
    public void updateUser1() {
        updateUser(10001);
    }

    //@Test
    public void updateUser2() {
        updateUser(20001);
    }

    //@Test
    public void updateUser3() {
        updateUser(30001);
    }

    public void updateUser(int offset) {
        DataModel das = am.getDataModel();
        Shape shape = das.getShape(AbstractDataModel.RELATIONAL_SHAPE);
        TypeMapper typeMapper = am.getDataModel().getTypeMapper().newInstance(MapperSide.DOMAIN);
        typeMapper.setDomainShape(shape);

        Settings settings = new Settings();
        JDBCType entityType = (JDBCType)shape.getType("US_USERTAB");
        entityType.setIdentifierProperty("ROOTID");
        settings.setEntityType(entityType);
        AggregateView view = new AggregateView();
        view.setAttributeList( Arrays.asList(new String[] { "CUS_PARTITIONNUMBER" }) );
        settings.setView(view);
        settings.init(shape);

        am.configure(settings);

        JDBCDataStore po = (JDBCDataStore)am.getDataStore();
        JDBCSessionContext sc = po.getSessionContext();

        sc.beginTransaction();

        // Get the connection
        Connection c = sc.getConnection();
        String sql = String.format("Select rootid, cus_partitionnumber from us_usertab  order by rootid limit 10000 offset %s", offset);

        int i = 1;
        List<JSONObject> users = new LinkedList<>();
        List<JSONObject> originalUsers = new LinkedList<>();
        try (PreparedStatement ps = c.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {

            while(rs.next()) {
                String rootid = rs.getString(1);
                JSONObject original = new JSONObject();
                original.put("ROOTID", rootid);
                original.put("CUS_PARTITIONNUMBER", rs.getString(2));
                //sc.process(original, entityType);
                originalUsers.add(original);

                JSONObject json = new JSONObject();
                json.put("ROOTID", rootid);
                json.put("CUS_PARTITIONNUMBER", 10);
                users.add(json);

                if(i++%1000 == 0) {
                    //am.update(users, settings);
                    update(users, originalUsers, entityType, sc, settings, po, typeMapper);
                    sc.commit();
                    users = new LinkedList<>();
                    originalUsers = new LinkedList<>();
                }
            }
            //am.update(users, settings);
            update(users, originalUsers, entityType, sc, settings, po, typeMapper);
            sc.commit();
        }
        catch (SQLException e) {
            e.printStackTrace();
        } finally {
            sc.close();
        }
    }

    private void update(List<JSONObject> users, List<JSONObject> originalUsers, EntityType entityType, JDBCSessionContext sc, Settings settings, DataStore po, TypeMapper typeMapper) {
        ObjectCreator oc = new ObjectCreator(settings, po, typeMapper);        
        for(int i = 0; i < users.size(); i++) {
            BusinessObject bo = new ImmutableBO(entityType, null, null, oc);
            bo.setInstance(users.get(i));
            BusinessObject dbBO = new ImmutableBO(entityType, null, null, oc);
            dbBO.setInstance(originalUsers.get(i));

            sc.update(bo, dbBO);
        }
    }

    private void delete(List<JSONObject> bids, EntityType entityType, JDBCSessionContext sc, Settings settings, DataStore po, TypeMapper typeMapper) {
        ObjectCreator oc = new ObjectCreator(settings, po, typeMapper);
        for(int i = 0; i < bids.size(); i++) {
            BusinessObject bo = new ImmutableBO(entityType, null, null, oc);
            bo.setInstance(bids.get(i));

            sc.delete(bo);
        }
    }
}
