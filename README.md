# 설계과제 Part II
## 1절. 데이터 및 테스트 시나리오

 선택 응용분야, 그 응용에서 생성할 테이블 스키마, primary key, 삽입 투플들, 삭제 투플들, 테스트 시나리오 등
### part1 에 대한 설명
part1 에서 Human Resoure Management (인사 관리)시스템의 간단한 Human Information 테이블을 형성해
보았다.생성한 데이터는 기업의 면접자의 인적 관리를 위해 간단한 인적 정보를 기록해둔 테이
블로, 해당 테이블로만 인사관리를 하기에는 부족하지만 가장 기반이 되는 테이블을 형성해 보았
다.
테이블은 기업의 면접자의 기본 정보를 저장하고 primary key(HumanID)를 통해 조회할 수 있도록 구현 하였다.
### part2 에 대한 설명  : 응용 스키마 소개
이번에는 part1에서 생성한 테이블을 기반으로 인사관리 시스템을 확장해보고자 한다.
PassInfo 테이블을 추가하여 합격한 면접자의 면접 결과를 저장하고, 이를 HumanInfo 테이블과 조인하여 면접자의 구체적인 정보를 조회할 수 있도록 한다.
Part1 에서 테이블 생성에 대한 내용을 구현했으므로, 이를 기반으로 Part2에서는 PassInfo 테이블을 추가하고, 이를 HumanInfo 테이블과 PassInfo 테이블을 Hash Join하여 면접자의 정보를 조회하는 기능을 구현한다.

CREATE TABLE "PassINFO" ( `PassID` ,`HumanID`,`Name`,`Score` ,`Part`,`SeniorID`, `JoinDate` )

각각의 attribute는 합격한 면접자의 합격 아이디(primary hey), 면접 아이디( foreign key), 이름, 면접 점수, 배정된 직무 , 직속 상사ID , 입사 일자를 의미한다.
* PassID :면접자의 합격 아이디(pk)
* HumanID :면접아이디 (fk)
* Name : 면접자 이름
* Score : 면접 점수
* Part  : 배정된 직무
* SeniorID : 직속 상사 아이디
* JoinDate : 입사 일자

* 이때 앞서 생성한 HumanINFO 테이블의 primary key인 HumanID와 PassINFO 테이블의 foreign key인 HumanID를 조인하여 면접자의 정보를 조회할 수 있도록 한다.

다음은 passInfo 테이블의 예제 part1 예제 시나리오 이다.


### 테이블 생성
CREATE TABLE "PassINFO" ( `PassID` ,`HumanID`,`Name`,`Score` ,`Part`,`SeniorID`, `JoinDate` )
### 튜플 삽입(Insert Tuple)
INSERT VALUES ( '1', '1', 'Apily', '82', 'Frontend', '12', '20210101') 
INSERT VALUES ( '2', '2', 'Bpily', '85', 'Backend', '13', '20210102')
INSERT VALUES ( '3', '3', 'Cpily', '88', 'Frontend', '14', '20210103')

### 튜플 삭제(Delete Tuple)
DELETE VALUES ( '7' ) WHERE table= "PassINFO"
### 단일 레코드 조회(Query Single Record)
SELECT 1 FROM "PassINFO" where id = 2

### 전체 레코드 조회(Select All)
SELECT * FROM "PassINFO"

### 해쉬 조인(Hash Join)
JOIN "HumanINFO" AND "PassINFO" ON "HumanINFO"."HumanID" = "PassINFO"."HumanID"

##  2절. 설계
구현을 위한 상세 설계 내용 설명. 특히, 아래를 포함
-과제 내용의 해당 교재 및 수업 설명에서 명시되지 않은 (unspecified) 구현 세부사항(implementation detail) 즉, 구현을 위해 결정 및 상세 설계되어야 하는 사항들의 구체적인 설계 설명 -시스템 모듈 구성 블록 다이어그램 (형식 참조 예: 교재 그림 1.3) -메타데이타 저장 용 테이블 SQL create table 문
-관계 DB 시스템의 기능별 사용자 인터페이스 설계


## 3절. 구현
-2절의 설계 내용 중 변경 사항
-기능별 구현 로직 주요 부분의 소스코드 캡쳐를 삽입하고 설명 (블록 크기 단위의 화일 I/O 프로그래밍 설명 포함)
## 4절. 테스트 및 정확성 검증
레포트 1절의 내용에 따른 프로그램 실행결과 화면 캡쳐 및 기능 작동 정확성 설명
## 5절. Java/JDBC 프로그램 소스코드 전체
※ 유의사항:
1. 레포트 화일 format: pdf로 한정
2. 레포트 2절의 SQL create table 문과 5절의 Java/JDBC 소스코드는 캡쳐 이미지로 제출 불허
