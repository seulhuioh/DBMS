package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TableManager {
    private Connection connection;

    public TableManager(Connection connection) {
        this.connection = connection;
    }

    public int getAttributeIndex(String tableName, String attributeName) {
        try {
            String sql = "SELECT position FROM Attribute_metaData WHERE relational_name = ? AND attribute_name = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, tableName);
                pstmt.setString(2, attributeName);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("position") - 1;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // 속성을 찾지 못한 경우
    }

    public void createTable(String tableName, String[] attributes) {
        // Relational_MetaData 테이블에 정보 저장
        String attributesList = String.join(",", attributes);
        try (PreparedStatement pstmt = connection.prepareStatement(
                "INSERT INTO Relational_MetaData (relational_name, member_of_attribute, storage_organization, location) VALUES (?, ?, ?, ?)")) {
            pstmt.setString(1, tableName);
            pstmt.setString(2, attributesList); // 속성 리스트들
            pstmt.setString(3, "File"); // storage_organization
            pstmt.setString(4, tableName + ".dat"); // location (File Path)
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Attribute_metaData 테이블에 정보 저장
        for (int i = 0; i < attributes.length; i++) {
            try (PreparedStatement pstmt = connection.prepareStatement(
                    "INSERT INTO Attribute_metaData (attribute_name, domain_type, position, length, relational_name) VALUES (?, ?, ?, ?, ?)")) {
                pstmt.setString(1, attributes[i]); // attribute_name
                pstmt.setString(2, "char"); // domain_type
                pstmt.setString(3, Integer.toString(i + 1)); // position
                pstmt.setString(4, "20"); // length
                pstmt.setString(5, tableName); // relational_name
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        System.out.println(tableName +"테이블 삽입 완료" );
    }//create method

    public boolean validateInsert(String tableName, String[] values) {
        try {
            // Attribute_metaData에서 해당 테이블의 속성 정보를 조회
            String sql = "SELECT attribute_name, domain_type, position, length FROM Attribute_metaData WHERE relational_name = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, tableName);
                try (ResultSet rs = pstmt.executeQuery()) {
                    int columnIndex = 0;
                    while (rs.next()) {
                        String domainType = rs.getString("domain_type");
                        int position = rs.getInt("position");
                        int length = rs.getInt("length");

                        // 입력된 값의 길이와 타입을 검증
                        if (values.length <= columnIndex || values[columnIndex].length() > length || !domainType.equals("char")) {
                            return false;
                        }
                        columnIndex++;
                    }
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }



}
