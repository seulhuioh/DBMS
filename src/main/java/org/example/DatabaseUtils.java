package org.example;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseUtils {
    public static void checkMetadataTables(Connection conn) throws SQLException {
        DatabaseMetaData dbMetaData = conn.getMetaData();

        checkTableExistence(dbMetaData, "Relational_MetaData");
        checkTableExistence(dbMetaData, "Attribute_metaData");
    }

    private static void checkTableExistence(DatabaseMetaData dbMetaData, String tableName) throws SQLException {
        try (ResultSet rs = dbMetaData.getTables(null, null, tableName, null)) {
            if (rs.next()) {
                System.out.println(tableName + " table exists.");
                // 테이블 구조 조회
                printTableStructure(dbMetaData, tableName);
            } else {
                System.out.println(tableName + " table does not exist.");
            }
        }
    }

    private static void printTableStructure(DatabaseMetaData dbMetaData, String tableName) throws SQLException {
        System.out.println("Structure of " + tableName + ":");
        try (ResultSet columns = dbMetaData.getColumns(null, null, tableName, null)) {
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                String datatype = columns.getString("TYPE_NAME");
                int columnsize = columns.getInt("COLUMN_SIZE");
                System.out.println("Column Name: " + columnName + ", Type: " + datatype + ", Size: " + columnsize);
            }
        }
    }
}
