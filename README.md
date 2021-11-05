# OraclePageNationExample
오라클 페이징 예제 / 구현 

# ROW NUM 사용시 주의사항
이런식으로 rownum 을 밖에다 빼줘야한다. 








SELECT ROWNUM AS NUM,
TA.* FROM
(SELECT DISTINCT MIN(p1.person_id) OVER(PARTITION BY p1.person_ID) MIN_PERSONID,
p1.person_id AS personId,
p1.title_search,
p1.kornm AS kornm,
p1.engnm AS engnm,
p1.kornm_r AS kornmr,
SUBSTR(p1.BIRTH_DATE,1,4) AS
brthYr,
DECODE(p1.life_yn,'D','Y','N') AS deathYn, SUBSTR(p1.death_date,1,4) AS deathYr,
p1.debutyear AS debutYr,
f2.FIELD_NM AS fields,
p1.FILMOGRAPHY AS filmo,
p1.PERSONIMG AS Image1,
CONCAT('https://www.kmdb.or.kr/db/per/', p1.person_id) AS kmdbUrl,
TO_CHAR(SUBSTR(p3.research_note, 1000, 4000)) AS researchs,
p5.FIELD_NO AS fieldNo
FROM
kmdb.PERSON p1
left outer JOIN kmdb.FIELD_CODE f2 ON p1.PERSONFIELD = f2.FIELD_NM
LEFT OUTER JOIN kmdb.PERSON_FIELD p5 ON p1.PERSON_ID = p5.PERSON_ID
left outer JOIN kmdb.PERSON_RESEARCH p3 ON p1.PERSON_ID = p3.PERSON_ID
left outer JOIN (SELECT distinct M.TITLE_SEARCH , C.PERSON_ID FROM kmdb.CREDIT_MOVIE C, kmdb.MOVIE_SE M
WHERE C.MOVIE_ID = M.MOVIE_ID AND C.MOVIE_SEQ = M.MOVIE_SEQ) p5 ON p5.person_id = p1.PERSON_ID
WHERE 1 =1
AND p1.kornm = '이미연'
) TA
WHERE ROWNUM BETWEEN 1 and 10


		

