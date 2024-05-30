##  2절. 설계
1절의 HASH JOIN 예제 시나리오 에서 본것 과 같이 콘솔 인터페이스 상에 다음과 같이 입력시 Join 연산을 수행하여야 한다.
``` 
JOIN "HumanINFO" AND "PassINFO" ON "HumanINFO"."HumanID" = "PassINFO"."HumanID"
```
이 때 part 1에서와 같이, 입력 string의 큰 따옴표 위치로 테이블 이름을 읽고, .의 위치 인덱스를 이용하여 JOIN하고자 하는 attribute의 이름을 찾아야 한다.(Main Class에서 수행)

1번 해쉬함수로 S,R테이블 각각 Partition을 분리한 뒤 2번 해쉬함수로 S테이블(빌드INPUT)을 메모리에 전체 적재 및 해쉬테이블 구성해야한다.
