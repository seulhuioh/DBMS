# 설계 과제 part 1 정리
전체 파일 헤더 내용을 포함하기 위해 파일 헤더에는 레코드 수, 블록 수, 블록 크기, Free List
Pointer 등을 포함해야한다
파일 생성(초기화 시) 헤더 또한 초기화
* 레코드 수: 파일에 저장된 총 레코드의 수 표시. : 초기 0
* 블록 수: 파일을 구성하는 총 블록의 수. : 초기 1
* 블록 크기 [BLOCK_SIZE]: 각 블록의 크기를 바이트 단위. 
* Free List 포인터 [HEADER_SIZE]: 현재 사용 가능한 첫 번째 빈 레코드(블록)의 위치.

# 0. MySQL workbench 내 meta data 

# 1. create table
Main Class 에서 시작
    
    CREATE TABLE "HumanINFO" ( `ID` ,`Name`,`Birth` ,`BankAccount`,`AddressID`, `Gender` ,`PhoneNumber` ) 
자바 콘솔창 인터페이스에 입력 시 

* Main Class : handleCreateTable
  테이블 이름과 속성 튜플 들을 뽑아냄

* TableManager Class:createTable
 테이블 이름과 속성 튜플 들을 받아서 메타 테이블에 저장


# 2. insert values
* ## Main Class : handleInsertRecord 
    insert의 시작 함수
  * ### Main Class : extractValuesFromInsert :
    값을 ','로 구분하여 추출 
  
    * ### TableManager : validateInsert :
    입력 데이터가 테이블 포맷과 일치하는지 확인
    * ### TableFileManager Class: 
    생성자를 사용하여 파일에 데이터 저장 
      * ### initializeFile : 
    RandomAccessFile 에서 레코드 수, 블록 수, 블록 크기, (Free List Pointer) 초기화
      * ### Main Class: prepareRecordData :
        1. 각 필드의 크기(FIELD_SIZE)와 필드 수(FIELDS_PER_RECORD)를 곱하여 레코드 데이터를 저장할 바이트 배열을 생성. 
        2. 공백 문자(' ')로 초기화 
        3. values 배열의 각 값을 UTF-8 형식의 바이트 배열로 변환
        4. 변환된 바이트 배열을 recordData 변수에 복사 후 리턴
        
    * ### TableFileManager Class : insertRecord
    현재  
    변환된 바이트 배열을 

* TableManager Class: 





## 3. delete values
* Main Class : 



* TableManager Class:

* TableFileManager Class:


## 4. select 1 values
* Main Class : 



* TableManager Class:

* TableFileManager Class:


## 5. select * values


* Main Class : 



* TableManager Class:

* TableFileManager Class:

