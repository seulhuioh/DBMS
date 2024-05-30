package org.example;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.Scanner;
import java.sql.SQLException;


public class Main {
    private static final int FIELD_SIZE = 20; // 각 필드의 크기 (바이트 단위), TableFileManager와 일치해야 함
    private static final int FIELDS_PER_RECORD = 7; // 레코드 당 필드 수, TableFileManager와 일치해야 함
    private static TableManager tableManager;

    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/meta_data?serverTimezone=UTC";
        String user = "root";
        String password = "1230";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Scanner scanner = new Scanner(System.in)) {
            TableManager tableManager = new TableManager(conn);
            HashJoinManager hashJoinManager = new HashJoinManager(tableManager);

            while (true) {
                System.out.println("테이블 생성: CREATE TABLE \"table_name\" (attr1, attr2, ...)");
                System.out.println("레코드 삽입: INSERT VALUES (...); WHERE table=\"table_name\"");
                System.out.println("레코드 삭제: DELETE VALUES (primaryKeyValue); WHERE table=\"table_name\"");
                System.out.println("레코드 단일 조회: SELECT 1 FROM \"table_name\"");
                System.out.println("레코드 전체 조회: SELECT * FROM \"table_name\"");
                System.out.println("해쉬 조인: JOIN \"tableR\" \"tableS\" ON \"tableR\".\"attrR\" = \"tableS\".\"attrS\"");
                System.out.println("종료: exit");
                System.out.println("인터페이스를 입력하세요. EX):");
                String input = scanner.nextLine(); // 사용자 입력 받기
                String command = input.trim().toLowerCase();//앞뒤 공백을 제거, 모든 문자를 소문자로 변환
                if (command.startsWith("create table")) {
                    // 테이블 생성 로직
                    handleCreateTable(input, tableManager);
                } else if (command.startsWith("insert values")) {
                    // 레코드 삽입 로직
                    handleInsertRecord(input, tableManager);
                } else if (command.startsWith("delete values")) {
                    // 레코드 삭제 로직
                    handleDeleteRecord(input);
                } else if (command.startsWith("select 1 from")) {
                    // 단일 레코드 조회 로직
                    handleSelectSingle(input);
                } else if (command.startsWith("select * from")) {
                    // 전체 레코드 조회 로직
                    handleSelectAll(input);
                }
                else if (input.toLowerCase().equals("join")) {
                    // 해쉬 조인 로직
                    handleHashJoin(input, hashJoinManager);
                }

                else if (input.toLowerCase().equals("exit")) {
                    System.out.println("프로그램을 종료합니다.");
                    break;
                }

                else {
                    System.out.println("알 수 없는 명령어입니다. 다시 입력해주세요.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void handleHashJoin(String input, HashJoinManager hashJoinManager) {
        String[] parts = input.split("\"");
        if (parts.length < 5) {
            System.out.println("명령어 형식이 올바르지 않습니다. 예시: HASH JOIN \"tableR\" \"tableS\" ON \"attrR\" = \"attrS\"");
            return;
        }

        String tableR = parts[1];
        String tableS = parts[3];
        String attrR = parts[5].split("=")[0].trim();
        String attrS = parts[5].split("=")[1].trim();

        try {
            hashJoinManager.performHashJoin(tableR, tableS, attrR, attrS);
        } catch (IOException | SQLException e) {
            System.err.println("해쉬 조인 중 오류 발생: " + e.getMessage());
        }
    }
    private static String extractPrimaryKey(String input) {
        // 'INSERT VALUES' 명령에서 첫 번째 값을 기본 키로 추출
        // 예시: INSERT VALUES ('1', '김철수', '1990-01-01', ...);에서 '1' 추출
        String valuesPart = input.substring(input.indexOf("(") + 1, input.indexOf(")"));
        String[] values = valuesPart.split(",\\s*(?=['\"])");
        if (values.length > 0) {
            // 첫 번째 값의 양쪽에 존재할 수 있는 작은따옴표(') 또는 큰따옴표(") 제거
            return values[0].trim().replaceAll("^['\"]|['\"]$", "");
        }
        return ""; // 기본 키를 찾을 수 없는 경우 빈 문자열 반환
    }

    private static String extractTableName(String input) {
        try {
            return input.split("\"")[1]; // 따옴표 내의 테이블 이름 추출
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("명령어 형식이 올바르지 않습니다. '테이블 이름'을 올바르게 입력하세요.");
            return null; // 형식에 맞지 않음을 나타내기 위해 null 반환
        }
    }

    private static String[] extractValuesFromInsert(String input) {
        // 'INSERT VALUES' 명령에서 값을 추출하기 전에 올바른 형식인지 확인
        int start = input.indexOf("(");
        int end = input.lastIndexOf(")");
        if (start == -1 || end == -1 || start >= end) {
            System.out.println("명령어 형식이 올바르지 않습니다. 입력예시");
            System.out.println("INSERT VALUES ('1', '김철수', '1990', '11', 'A', 'M', '010=') WHERE table= \"HumanINFO\"");
            return null; // 형식에 맞지 않음을 나타내기 위해 null 반환
        }

        String valuesPart = input.substring(start + 1, end).trim();
        if (valuesPart.isEmpty()) {
            System.out.println("값이 비어있어요.");
            return null; // 값이 비어있음
        }

        // 값을 ','로 구분하여 추출
        return valuesPart.split("',\\s*'");
    }


    private static String[] extractValues(String input) {
        String valuesStr = input.substring(input.indexOf("(") + 1, input.lastIndexOf(")"));
        return valuesStr.split(",");
        //
    }


    private static void handleCreateTable(String input, TableManager tableManager)
    {
        //입력 예시
        // CREATE TABLE "HumanINFO" ( `HumanID` ,`Name`,`Birth` ,`BankAccount`, `AddressID`, `Gender` , `PhoneNumber` )
        // 테이블 이름 추출
        int firstQuoteIndex = input.indexOf("\"");// 따옴표의 인덱스(위치)를 반환
        int secondQuoteIndex = input.indexOf("\"", firstQuoteIndex + 1);// firstQuoteIndex + 1 위치부터 두 번째 (") 인덱스
        if (firstQuoteIndex == -1 || secondQuoteIndex == -1) { // 2개 따옴표가 없는 경우
            System.out.println("테이블 이름을 찾을 수 없습니다.");
            return;
        }
        String tableName = input.substring(firstQuoteIndex + 1, secondQuoteIndex);

        // 속성 추출 "(" --- ")" 괄호 사이의 문자열을 추출
        int openParenIndex = input.indexOf("(", secondQuoteIndex); // 두번째 " 부터 ( 인덱스를 찾기
        int closeParenIndex = input.lastIndexOf(")"); // ) 인덱스 찾기
        if (openParenIndex == -1 || closeParenIndex == -1) {

            System.out.println("속성 목록을 찾을 수 없습니다. : 괄호 형식이 잘못 됨");
            return;
        }
        String attributesStr = input.substring(openParenIndex + 1, closeParenIndex).trim(); // 띄어쓰기 제거
        if (attributesStr.isEmpty()) {
            System.out.println("속성 목록이 비어 있습니다.");
            return;
        }

        String[] attributes = attributesStr.split(","); // , 단위 속성 추출 -> attributes[i] 에 저장
        for (int i = 0; i < attributes.length; i++) {
            attributes[i] = attributes[i].trim().replaceAll("`", ""); // 공백과 백틱(`) 제거
        }

        tableManager.createTable(tableName, attributes);
        // TableFileManager fileManager = new TableFileManager(tableName);
        System.out.println("테이블과 관련된 데이터 파일이 성공적으로 생성되었습니다.");
    }// handleCreateTable



    private static byte[] prepareRecordData(String[] values) {
        // 값을 바이트 배열로 변환하는 로직
        byte[] recordData = new byte[FIELD_SIZE * FIELDS_PER_RECORD];
        Arrays.fill(recordData, (byte) ' '); // 공백으로 초기화

        for (int i = 0; i < values.length; i++) {
            byte[] valueBytes = values[i].getBytes(StandardCharsets.UTF_8);
            System.arraycopy(valueBytes, 0, recordData, i * FIELD_SIZE, Math.min(valueBytes.length, FIELD_SIZE));
        }
        return recordData;
    }

    private static void handleInsertRecord(String input, TableManager tableManager)
    {
        String tableName = extractTableName(input);
        if (tableName == null) {
            System.out.println("테이블 이름 추출 실패.");
            return;
        }

        String[] values = extractValuesFromInsert(input);
        if (values == null) {
            System.out.println("레코드 값 추출 실패.");
            return;
        }

        // TableManager를 사용하여 MySQL에서 Attribute_metaData 테이블의 정보 확인
        boolean isValid = tableManager.validateInsert(tableName, values);
        if (!isValid) {
            System.out.println("입력 데이터가 테이블 포맷과 일치하지 않습니다.");
            return;
        }

        // TableFileManager를 사용하여 파일에 데이터 저장
        try {
            TableFileManager fileManager = new TableFileManager(tableName);
            byte[] preparedData = prepareRecordData(values); // 실제 구현 필요
            if (preparedData != null) {
                fileManager.insertRecord(preparedData);
                System.out.println("레코드가 성공적으로 삽입되었습니다.");
            } else {
                System.out.println("레코드 데이터 준비 중 문제가 발생했습니다.");
            }
        } catch (IOException e) {
            System.err.println("레코드 삽입 중 오류 발생: " + e.getMessage());
        }
    }//





    private static void handleDeleteRecord(String input) {
        String tableName = extractTableName(input);
        String primaryKey = extractPrimaryKey(input); // 실제 구현 필요
        try {
            TableFileManager fileManager = new TableFileManager(tableName);
            fileManager.deleteRecord(primaryKey);
            System.out.println("레코드 삭제 완료.");
        } catch (IOException e) {
            System.err.println("레코드 삭제 중 오류 발생: " + e.getMessage());
        }
    }



    private static void handleSelectSingle(String input) {
        String tableName = extractTableName(input);
        String primaryKey = extractPrimaryKey(input); // 실제 구현 필요
        try {
            TableFileManager fileManager = new TableFileManager(tableName);
            byte[] record = fileManager.selectSingleRecord(primaryKey);
            if (record != null) {
                System.out.println("조회된 레코드: " + new String(record, StandardCharsets.UTF_8));
            } else {
                System.out.println("레코드를 찾을 수 없습니다.");
            }
        } catch (IOException e) {
            System.err.println("레코드 조회 중 오류 발생: " + e.getMessage());
        }
    }



    private static void handleSelectAll(String input) {
        String tableName = extractTableName(input);
        try {
            TableFileManager fileManager = new TableFileManager(tableName);
            fileManager.selectAllRecords();
        } catch (IOException e) {
            System.err.println("레코드 전체 조회 중 오류 발생: " + e.getMessage());
        }
    }


}
