package org.example;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class TableFileManager {
    private static final int RECORD_SIZE = 140;
    private static final int BLOCK_RECORDS = 3;
    private static final int HEADER_SIZE = 30;
    private static final int FIELD_SIZE = 20; // 각 필드의 크기 (바이트 단위)
    private static final int FIELDS_PER_RECORD = 7; // 레코드 당 필드 수
    private static final int BLOCK_SIZE = (RECORD_SIZE * BLOCK_RECORDS) + HEADER_SIZE;
    private String tableName;

    public TableFileManager(String tableName) {
        this.tableName = tableName + ".dat";
        initializeFile();
        System.out.println("테이블 데이터 파일이 초기화 완료");
    }

    private void initializeFile() {
        try (RandomAccessFile file = new RandomAccessFile(tableName, "rw")) {
            if (file.length() == 0) {
                file.writeInt(0); // 전체 레코드 수 초기화
                file.writeInt(1); // 블록 수 초기화
                file.writeInt(BLOCK_SIZE); // 블록 크기 저장
                file.writeInt(HEADER_SIZE); // 첫 번째 빈 레코드의 위치 (Free List Pointer) 초기화
            }
        } catch (IOException e) {
            System.err.println("파일 초기화 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void insertRecord(byte[] recordData) throws IOException {
        if (recordData.length != RECORD_SIZE) {
            throw new IllegalArgumentException("레코드 데이터는 " + RECORD_SIZE + "바이트여야 합니다.");
        }

        try (RandomAccessFile file = new RandomAccessFile(tableName, "rw")) {
            byte[] block = new byte[BLOCK_SIZE];
            boolean inserted = false;

            while (!inserted) {
                int bytesRead = file.read(block);
                if (bytesRead == -1) { // 파일 끝에 도달했거나 읽을 데이터가 없는 경우
                    System.out.println("읽기 실패: 파일 끝 도달 또는 읽을 데이터 없음");
                    break;
                }

                // 블록 내에서 빈 레코드 위치 찾기
                for (int offset = HEADER_SIZE; offset < BLOCK_SIZE; offset += RECORD_SIZE) {
                    if (isRecordEmpty(block, offset)) {
                        System.arraycopy(recordData, 0, block, offset, RECORD_SIZE);
                        long currentPos = file.getFilePointer();
                        file.seek(currentPos - bytesRead);
                        file.write(block);
                        inserted = true;
                        System.out.println("레코드 삽입 성공");
                        break;
                    }
                }

                if (!inserted && bytesRead < BLOCK_SIZE) { // 현재 읽은 블록이 마지막 블록이고 꽉 차지 않았을 경우
                    System.out.println("마지막 블록 처리");
                    Arrays.fill(block, bytesRead, BLOCK_SIZE, (byte) 0);
                    System.arraycopy(recordData, 0, block, bytesRead, RECORD_SIZE);
                    file.write(block, bytesRead, BLOCK_SIZE - bytesRead);
                    inserted = true;
                }
            }

            if (!inserted) { // 모든 블록이 꽉 차서 새 블록을 추가해야 하는 경우
                System.out.println("새 블록 추가");
                Arrays.fill(block, (byte) 0);
                System.arraycopy(recordData, 0, block, HEADER_SIZE, RECORD_SIZE);
                file.write(block);
            }
        }
    }

    private boolean isRecordEmpty(byte[] block, int offset) {
        // 레코드가 비어 있는지
        return block[offset] == 0;
    }


    public byte[] selectSingleRecord(String primaryKey) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(tableName, "rw")) {
            long fileSize = file.length();
            byte[] block = new byte[BLOCK_SIZE];
            byte[] pkBytes = primaryKey.getBytes("UTF-8");
            byte[] searchKey = Arrays.copyOf(pkBytes, 20); // Primary Key를 20바이트로 확장

            while (file.getFilePointer() < fileSize) {
                file.readFully(block);

                for (int offset = HEADER_SIZE; offset < BLOCK_SIZE; offset += RECORD_SIZE) {
                    byte[] currentKey = Arrays.copyOfRange(block, offset, offset + 20); // 레코드의 첫 20바이트 추출

                    if (Arrays.equals(currentKey, searchKey)) {
                        byte[] record = new byte[RECORD_SIZE];
                        System.arraycopy(block, offset, record, 0, RECORD_SIZE);
                        return record;
                    }
                }
            }
        }
        return null; // 레코드를 찾지 못한 경우
    }
    public void deleteRecord(String primaryKey) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(tableName, "rw")) {
            long fileSize = file.length();
            byte[] block = new byte[BLOCK_SIZE];
            byte[] pkBytes = primaryKey.getBytes("UTF-8");
            byte[] searchKey = Arrays.copyOf(pkBytes, FIELD_SIZE); // 주의: 모든 attribute는 char(20)이므로, 20바이트로 맞춤

            // Free List의 헤드를 파일의 시작 부분에서 읽기
            file.seek(0);
            int freeListHead = file.readInt(); // 첫 번째 4바이트는 Free List의 헤드 위치를 저장

            for (long position = 0; position < fileSize; position += BLOCK_SIZE) {
                file.seek(position);
                file.readFully(block);
                ByteBuffer buffer = ByteBuffer.wrap(block);

                // 블록 내에서 주어진 primaryKey와 일치하는 레코드 찾기
                for (int offset = HEADER_SIZE; offset < BLOCK_SIZE; offset += RECORD_SIZE) {
                    byte[] currentKey = new byte[FIELD_SIZE];
                    buffer.position(offset);
                    buffer.get(currentKey, 0, FIELD_SIZE);

                    if (Arrays.equals(currentKey, searchKey)) {
                        // 레코드 삭제: 해당 위치에 Free List의 현재 헤드 위치를 기록하고, Free List의 헤드를 이 위치로 업데이트
                        buffer.position(offset);
                        buffer.putInt(freeListHead); // 현재 Free List 헤드를 가리키도록 함
                        freeListHead = (int) position + offset - HEADER_SIZE; // 새로운 Free List 헤드 업데이트 (블록 상대 위치)

                        // 변경된 블록과 Free List 헤드를 파일에 기록
                        file.seek(position);
                        file.write(block);

                        file.seek(0);
                        file.writeInt(freeListHead); // Free List 헤드 위치 업데이트
                        return; // 레코드 삭제 후 메서드 종료
                    }
                }
            }
        }
    }

    public void selectAllRecords() throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(tableName, "rw")) {
            long fileSize = file.length();
            byte[] block = new byte[BLOCK_SIZE];

            for (long position = 0; position < fileSize; position += BLOCK_SIZE) {
                file.seek(position);
                file.readFully(block);

                // 블록 내 레코드 수를 읽음
                int recordsInBlock = ByteBuffer.wrap(block, 0, 4).getInt();
                for (int i = 0; i < recordsInBlock; i++) {
                    byte[] record = Arrays.copyOfRange(block, HEADER_SIZE + i * RECORD_SIZE, HEADER_SIZE + (i + 1) * RECORD_SIZE);

                    // 각 레코드 처리 (예: 출력)
                    for (int j = 0; j < FIELDS_PER_RECORD; j++) {
                        byte[] fieldData = Arrays.copyOfRange(record, j * FIELD_SIZE, (j + 1) * FIELD_SIZE);

                        // 문자열로 변환, 가정: UTF-8 인코딩 사용
                        String field = new String(fieldData, "UTF-8").trim(); // 공백 제거
                        System.out.print(field + " | ");
                    }
                    System.out.println(); // 레코드 당 줄바꿈
                }
            }
        }
    }








}//class
