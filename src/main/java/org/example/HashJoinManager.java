package org.example;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

public class HashJoinManager {

    private static final int FIELD_SIZE = 20; // 각 필드의 크기 (바이트 단위)
    private static final int PARTITION_COUNT = 5;
    private static final int RECORD_SIZE = 140;
    private static final int BLOCK_RECORDS = 3;
    private static final int HEADER_SIZE = 30;
    private static final int FIELDS_PER_RECORD = 7; // 레코드 당 필드 수
    private static final int BLOCK_SIZE = (RECORD_SIZE * BLOCK_RECORDS) + HEADER_SIZE;
    private TableManager tableManager;

    public HashJoinManager(TableManager tableManager) {
        this.tableManager = tableManager;
    }

    public void performHashJoin(String tableR, String tableS, String attrR, String attrS) throws IOException, SQLException {
        // 파티셔닝 단계
        List<RandomAccessFile> rPartitions = partitionTable(tableR, attrR);
        List<RandomAccessFile> sPartitions = partitionTable(tableS, attrS);

        // 조인 단계
        for (int i = 0; i < PARTITION_COUNT; i++) {
            joinPartitions(rPartitions.get(i), sPartitions.get(i), attrR, attrS);
        }
    }

    private List<RandomAccessFile> partitionTable(String tableName, String attr) throws IOException, SQLException {
        // fetchMetadata 대신 validateTableName 사용
        if (!tableManager.validateTableName(tableName)) {
            System.out.println(tableName + " 테이블이 존재하지 않습니다.");
            return null;
        }

        List<RandomAccessFile> partitions = new ArrayList<>();
        for (int i = 0; i < PARTITION_COUNT; i++) {
            partitions.add(new RandomAccessFile(tableName + "_partition_" + i + ".dat", "rw"));
        }

        TableFileManager fileManager = new TableFileManager(tableName);
        List<byte[]> records = fileManager.readAllRecords();
        int attrIndex = tableManager.getAttributeIndex(tableName, attr);

        byte[][] partitionBuffers = new byte[PARTITION_COUNT][BLOCK_SIZE];
        int[] bufferOffsets = new int[PARTITION_COUNT];
        Arrays.fill(bufferOffsets, HEADER_SIZE);

        for (byte[] record : records) {
            int hashValue = hashFunction(record, attrIndex);
            byte[] buffer = partitionBuffers[hashValue];
            int offset = bufferOffsets[hashValue];

            if (offset + RECORD_SIZE > BLOCK_SIZE) {
                partitions.get(hashValue).write(buffer, 0, offset);
                bufferOffsets[hashValue] = HEADER_SIZE;
                offset = HEADER_SIZE;
                Arrays.fill(buffer, HEADER_SIZE, BLOCK_SIZE, (byte) 0); // Clear buffer
            }

            System.arraycopy(record, 0, buffer, offset, RECORD_SIZE);
            bufferOffsets[hashValue] += RECORD_SIZE;
        }

        for (int i = 0; i < PARTITION_COUNT; i++) {
            if (bufferOffsets[i] > HEADER_SIZE) {
                partitions.get(i).write(partitionBuffers[i], 0, bufferOffsets[i]);
            }
            partitions.get(i).close();
        }

        return partitions;
    }

    private void joinPartitions(RandomAccessFile rPartition, RandomAccessFile sPartition, String attrR, String attrS) throws IOException {
        int rAttrIndex = tableManager.getAttributeIndex("tableR", attrR);
        int sAttrIndex = tableManager.getAttributeIndex("tableS", attrS);

        Map<String, List<byte[]>> hashTable = new HashMap<>();

        // S 테이블을 해시 테이블로 빌드
        byte[] sBlock = new byte[BLOCK_SIZE];
        while (sPartition.read(sBlock) != -1) {
            for (int offset = HEADER_SIZE; offset < BLOCK_SIZE; offset += RECORD_SIZE) {
                if (!isRecordEmpty(sBlock, offset)) {
                    byte[] sRecord = Arrays.copyOfRange(sBlock, offset, offset + RECORD_SIZE);
                    String sKey = extractKey(sRecord, sAttrIndex);
                    hashTable.computeIfAbsent(sKey, k -> new ArrayList<>()).add(sRecord);
                }
            }
        }

        // R 테이블과 조인
        byte[] rBlock = new byte[BLOCK_SIZE];
        while (rPartition.read(rBlock) != -1) {
            for (int offset = HEADER_SIZE; offset < BLOCK_SIZE; offset += RECORD_SIZE) {
                if (!isRecordEmpty(rBlock, offset)) {
                    byte[] rRecord = Arrays.copyOfRange(rBlock, offset, offset + RECORD_SIZE);
                    String rKey = extractKey(rRecord, rAttrIndex);
                    if (hashTable.containsKey(rKey)) {
                        for (byte[] sRecord : hashTable.get(rKey)) {
                            printJoinResult(rRecord, sRecord);
                        }
                    }
                }
            }
        }
    }

    private String extractKey(byte[] record, int attrIndex) {
        int start = attrIndex * FIELD_SIZE; // FIELD_SIZE
        int end = start + FIELD_SIZE; // FIELD_SIZE
        return new String(Arrays.copyOfRange(record, start, end), StandardCharsets.UTF_8).trim();
    }

    private int hashFunction(byte[] record, int attrIndex) {
        int start = attrIndex * FIELD_SIZE; // FIELD_SIZE
        int end = start + FIELD_SIZE; // FIELD_SIZE
        String key = new String(Arrays.copyOfRange(record, start, end), StandardCharsets.UTF_8).trim();
        return Math.abs(key.hashCode()) % PARTITION_COUNT;
    }

    private boolean isRecordEmpty(byte[] block, int offset) {
        for (int i = offset; i < offset + RECORD_SIZE; i++) {
            if (block[i] != 0) {
                return false;
            }
        }
        return true;
    }

    private void printJoinResult(byte[] rRecord, byte[] sRecord) {
        System.out.print("조인 결과: ");
        for (int i = 0; i < FIELDS_PER_RECORD; i++) {
            String rField = new String(Arrays.copyOfRange(rRecord, i * FIELD_SIZE, (i + 1) * FIELD_SIZE), StandardCharsets.UTF_8).trim();
            String sField = new String(Arrays.copyOfRange(sRecord, i * FIELD_SIZE, (i + 1) * FIELD_SIZE), StandardCharsets.UTF_8).trim();
            System.out.print(rField + "|" + sField + " ");
        }
        System.out.println();
    }
}
