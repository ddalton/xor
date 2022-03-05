package tools.xor.providers.jdbc;

import tools.xor.service.ForeignKeyEnhancer;
import tools.xor.util.ClassUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Translator for PostgreSQL database
 */
public class PGTranslator extends DBTranslator
{
    private static final String FOREIGN_KEY_SQL = "SELECT KCU.CONSTRAINT_NAME, KCU.TABLE_NAME AS FOREIGN_TABLE, REL_KCU.TABLE_NAME AS PRIMARY_TABLE, KCU.COLUMN_NAME AS FK_COLUMN, REL_KCU.COLUMN_NAME AS PK_COLUMN, RCO.DELETE_RULE, RCO.UPDATE_RULE FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS TCO JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE KCU ON TCO.CONSTRAINT_SCHEMA = KCU.CONSTRAINT_SCHEMA AND TCO.CONSTRAINT_NAME = KCU.CONSTRAINT_NAME JOIN INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS RCO ON TCO.CONSTRAINT_SCHEMA = RCO.CONSTRAINT_SCHEMA AND TCO.CONSTRAINT_NAME = RCO.CONSTRAINT_NAME JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE REL_KCU ON RCO.UNIQUE_CONSTRAINT_SCHEMA = REL_KCU.CONSTRAINT_SCHEMA AND RCO.UNIQUE_CONSTRAINT_NAME = REL_KCU.CONSTRAINT_NAME AND KCU.ORDINAL_POSITION = REL_KCU.ORDINAL_POSITION WHERE TCO.CONSTRAINT_TYPE = 'FOREIGN KEY' AND KCU.TABLE_SCHEMA = current_schema() ORDER BY KCU.TABLE_NAME, KCU.ORDINAL_POSITION";
    private static final String COLUMNS_SQL = "SELECT table_name, column_name, is_nullable, data_type, is_generated, case when character_maximum_length is not null then character_maximum_length else numeric_precision end as max_length from information_schema.columns where TABLE_SCHEMA = current_schema() order by table_name";
    private static final String PRIMARY_KEY_SQL = "SELECT kcu.table_name, tco.constraint_name, kcu.column_name as key_column, kcu.ordinal_position as position from information_schema.table_constraints tco join information_schema.key_column_usage kcu on kcu.constraint_name = tco.constraint_name and kcu.constraint_schema = tco.constraint_schema and kcu.constraint_name = tco.constraint_name where tco.constraint_type = 'PRIMARY KEY' order by kcu.table_schema, kcu.table_name, position";

    // H2 database uses BIGINT as the sequence type
    private static final String SEQUENCES_SQL = "SELECT sequence_name, data_type, maximum_value, minimum_value, increment, start_value, cycle_option FROM information_schema.sequences WHERE sequence_schema = current_schema()";
    private static final String TABLE_EXISTS_SQL = "SELECT count(*) FROM information_schema.tables WHERE TABLE_NAME = '%s' AND TABLE_SCHEMA = current_schema()";

    private Map<String, JDBCDataModel.SequenceInfo> sequenceMap;

    private static final Map<String, String> psql_to_jdbc_map = new HashMap<>();

    static {
        psql_to_jdbc_map.put("TIME", "TIMESTAMP");
        psql_to_jdbc_map.put("TIME WITH TIME ZONE", "TIMESTAMP");
        psql_to_jdbc_map.put("TIMESTAMP WITH TIME ZONE", "TIMESTAMP");
        psql_to_jdbc_map.put("CHARACTER", "CHAR");
        psql_to_jdbc_map.put("CHARACTER VARYING", "VARCHAR");
        psql_to_jdbc_map.put("UUID", "VARCHAR");
        psql_to_jdbc_map.put("JSON", "VARCHAR");
        psql_to_jdbc_map.put("JSONB", "VARCHAR");
        psql_to_jdbc_map.put("BIT VARYING", "BIGINT");
        psql_to_jdbc_map.put("BYTEA", "VARBINARY");
        psql_to_jdbc_map.put("CITEXT", "LONGVARCHAR");
        psql_to_jdbc_map.put("TEXT", "LONGVARCHAR");
        psql_to_jdbc_map.put("SERIAL", "INTEGER");
        psql_to_jdbc_map.put("BIGSERIAL", "BIGINT");
        psql_to_jdbc_map.put("DOUBLE PRECISION", "DOUBLE");
        psql_to_jdbc_map.put("XML", "SQLXML");
    }

    @Override public JDBCDataModel.TableInfo getTable (Connection connection, ForeignKeyEnhancer enhancer, String tableName)
    {
        if(tableMap == null) {
            getTables(connection, enhancer);
        }

        return tableMap.get(tableName);
    }

    @Override public JDBCDataModel.SequenceInfo getSequence (Connection connection, String sequenceName)
    {
        if(sequenceMap == null) {
            getSequences(connection);
        }

        return sequenceMap.get(sequenceName);
    }

    @Override
    protected Class getJavaClass (String sqlType)
    {
        if(psql_to_jdbc_map.containsKey(sqlType)) {
            sqlType = psql_to_jdbc_map.get(sqlType);
        }
        return super.getJavaClass(sqlType);
    }

    @Override
    protected JDBCDataModel.ColumnInfo createColumnInfo (ResultSet rs) throws SQLException
    {
        String columnName = rs.getString(2);
        Boolean nullable = "NO".equals(rs.getString(3)) ? false : true;
        String columnType = rs.getString(4).toUpperCase();
        Boolean generated = "NEVER".equals(rs.getString(5)) ? false : true;
        int length = rs.getInt(6);
        Class javaClass = getJavaClass(columnType);
        if (javaClass == null) {
            throw new RuntimeException("Unknown java mapping for SQL type: " + columnType);
        }
        JDBCDataModel.ColumnInfo ci = new JDBCDataModel.ColumnInfo(columnName, nullable, javaClass,
            columnType, generated, length);

        return ci;
    }

    private void printResult(Connection connection, String sql) {
        try (PreparedStatement ps = connection.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
        ) {
            ResultSetMetaData rsmd=rs.getMetaData();
            int count = rsmd.getColumnCount();

            for(int i = 0; i < count; i++) {
                System.out.println(rsmd.getColumnName(i+1));
            }

            System.out.println("============== SQL =======================");
            System.out.println(sql);
            System.out.println("============== SQL =======================");
            while(rs.next()) {
                StringBuilder row = new StringBuilder();
                for(int i = 0; i < count; i++) {
                    if (row.length() > 0) {
                        row.append(", ");
                    }

                    // JDBC column starts from 1
                    row.append(rs.getString(i+1));
                }
                System.out.println(row.toString());
            }
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        }
    }

    @Override
    protected JDBCDataModel.ForeignKey createForeignKey(ResultSet rs, Map<String, JDBCDataModel.TableInfo> tableMap) throws SQLException
    {
        JDBCDataModel.ForeignKeyRule deleteRule = getForeignKeyRule(rs.getString(6));
        JDBCDataModel.ForeignKeyRule updateRule = getForeignKeyRule(rs.getString(7));
        JDBCDataModel.TableInfo referencing = tableMap.get(rs.getString(2));
        JDBCDataModel.TableInfo referenced = tableMap.get(rs.getString(3));
        JDBCDataModel.ForeignKey fkey = new JDBCDataModel.ForeignKey(rs.getString(1),
            referencing, referenced, deleteRule, updateRule);

        return fkey;
    }

    @Override public List<JDBCDataModel.SequenceInfo> getSequences (Connection connection)
    {
        Map<String, JDBCDataModel.SequenceInfo> result = new HashMap<>();

        try (PreparedStatement ps = connection.prepareStatement(SEQUENCES_SQL);
            ResultSet rs = ps.executeQuery();
        ) {
            JDBCDataModel.SequenceInfo seq = null;
            while(rs.next()) {
                seq = new JDBCDataModel.SequenceInfo(
                    rs.getString(1),
                    rs.getString(2),
                    rs.getLong(3),
                    rs.getLong(4),
                    rs.getInt(5),
                    rs.getLong(6),
                    rs.getString(7).equals("YES") ? true : false);
                result.put(seq.getName(), seq);
            }
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        }

        this.sequenceMap = result;

        return new ArrayList<>(this.sequenceMap.values());
    }

    @Override public String getTableColumnsSQL ()
    {
        return COLUMNS_SQL;
    }

    @Override public String getPrimaryKeySQL ()
    {
        return PRIMARY_KEY_SQL;
    }

    @Override public String getForeignKeysSQL ()
    {
        return FOREIGN_KEY_SQL;
    }

    @Override public String getTableExistsSQL ()
    {
        return TABLE_EXISTS_SQL;
    }

    private JDBCDataModel.ForeignKeyRule getForeignKeyRule(String value) {
        switch(value) {
        case "CASCADE":
            return JDBCDataModel.ForeignKeyRule.CASCADE;
        case "SET NULL":
            return JDBCDataModel.ForeignKeyRule.SET_NULL;
        case "SET DEFAULT":
            return JDBCDataModel.ForeignKeyRule.SET_DEFAULT;
        case "RESTRICT":
            return JDBCDataModel.ForeignKeyRule.RESTRICT;
        case "NO ACTION":
            return JDBCDataModel.ForeignKeyRule.NO_ACTION;
        }

        throw new RuntimeException("Unknown value for foreign key rule: " + value);
    }
}
