package org.example;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.*;

public class HashJoinManager {
    private static final int FIELD_SIZE = 20;
    private static final int FIELDS_PER_RECORD = 7;
    private static final int NUM_PARTITIONS = 10; // 파티션 수
    private TableManager tableManager;

    public HashJoinManager(TableManager tableManager) {
        this.tableManager = tableManager;
    }

    public void performHashJoin(String tableR, String tableS, String attrR, String attrS) throws IOException {
        // 파티셔닝 단계
        partitionTable(tableR, attrR, "R");
        partitionTable(tableS, attrS, "S");

        // 조인 단계
        for (int i = 0; i < NUM_PARTITIONS; i++) {
            String partitionFileR = "partition_R_" + i + ".dat";
            String partitionFileS = "partition_S_" + i + ".dat";
            joinPartitions(partitionFileR, partitionFileS);
        }

        // 임시 파일 삭제
        for (int i = 0; i < NUM_PARTITIONS; i++) {
            new File("partition_R_" + i + ".dat").delete();
            new File("partition_S_" + i + ".dat").delete();
        }
    }

    private void partitionTable(String tableName, String partitionAttr, String prefix) throws IOException {
        TableFileManager fileManager = new TableFileManager(tableName);
        List<byte[]> records = fileManager.readAllRecords();
        int partitionAttrIndex = tableManager.getAttributeIndex(tableName, partitionAttr);

        for (byte[] record : records) {
            String partitionKey = new String(record, partitionAttrIndex * FIELD_SIZE, FIELD_SIZE, StandardCharsets.UTF_8).trim();
            int partitionNum = hashPartition(partitionKey, NUM_PARTITIONS);
            try (RandomAccessFile partitionFile = new RandomAccessFile(prefix + "_partition_" + partitionNum + ".dat", "rw")) {
                partitionFile.seek(partitionFile.length());
                partitionFile.write(record);
            }
        }
    }

    private void joinPartitions(String partitionFileR, String partitionFileS) throws IOException {
        Map<String, List<byte[]>> hashTable = new HashMap<>();

        // R 파티션 로드 및 해쉬 테이블 생성
        try (RandomAccessFile fileR = new RandomAccessFile(partitionFileR, "r")) {
            byte[] recordR;
            while ((recordR = readRecord(fileR)) != null) {
                String key = new String(recordR, 0, FIELD_SIZE, StandardCharsets.UTF_8).trim();
                hashTable.computeIfAbsent(key, k -> new ArrayList<>()).add(recordR);
            }
        }

        // S 파티션과 조인 수행
        try (RandomAccessFile fileS = new RandomAccessFile(partitionFileS, "r")) {
            byte[] recordS;
            while ((recordS = readRecord(fileS)) != null) {
                String key = new String(recordS, 0, FIELD_SIZE, StandardCharsets.UTF_8).trim();
                List<byte[]> matchedRecords = hashTable.get(key);
                if (matchedRecords != null) {
                    for (byte[] recordR : matchedRecords) {
                        printJoinedRecord(recordR, recordS);
                    }
                }
            }
        }
    }

    private byte[] readRecord(RandomAccessFile file) throws IOException {
        byte[] record = new byte[FIELD_SIZE * FIELDS_PER_RECORD];
        int bytesRead = file.read(record);
        return bytesRead == -1 ? null : record;
    }

    private void printJoinedRecord(byte[] recordR, byte[] recordS) {
        System.out.println("Joined Record:");
        System.out.println(new String(recordR, StandardCharsets.UTF_8).trim() + " | " + new String(recordS, StandardCharsets.UTF_8).trim());
    }

    private int hashPartition(String key, int numPartitions) {
        return Math.abs(key.hashCode()) % numPartitions;
    }
}
