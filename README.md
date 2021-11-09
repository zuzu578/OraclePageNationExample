# OraclePageNationExample
오라클 페이징 예제 / 구현 

# ROW NUM 사용시 주의사항
이런식으로 rownum 을 밖에다 빼줘야한다. 
# PARTION BY 를 이용하여 중복제거 




# 임찬구 대리님이 갔다 가심 2021-11-09
# 왜이렇게 했냐면 row num 정렬을 위해 서브쿼리 여러번사용 


SELECT TB.*
FROM (
SELECT ROWNUM AS NUM, ta.*  FROM (SELECT
			DISTINCT MIN(p1.person_id) OVER(PARTITION BY p1.person_ID) PERSONID,
			COUNT(p1.person_ID) OVER(PARTITION BY p1.person_ID) AS DATA_COUNT,
			p1.title_search,
			p1.kornm AS kornm,
			p1.engnm AS engnm,
			p1.kornm_r AS kornmr,
			SUBSTR(p1.BIRTH_DATE,1,4) AS brthYr,
			DECODE(p1.life_yn,'D','Y','N') AS deathYn, SUBSTR(p1.death_date,1,4) AS deathYr,
			p1.debutyear AS debutYr,
			LISTAGG('Fields{'||'Filed='||P.FIELD_NM||'}', '@@') WITHIN GROUP(ORDER BY P1.PERSON_ID) OVER(PARTITION BY P1.PERSON_ID) AS fields,
			p1.FILMOGRAPHY AS filmo3,
			--P2.filmos AS filmos,
			p1.PERSONIMG AS Image1,
			CONCAT('https://www.kmdb.or.kr/db/per/', p1.person_id) AS kmdbUrl
			, P3.RESEARCH_NOTE AS Researchs
			
			FROM kmdb.PERSON p1			
			
			
			LEFT OUTER JOIN(SELECT A.PERSON_ID, A.FIELD_NO, B.FIELD_NM FROM kmdb.PERSON_FIELD A
			LEFT OUTER JOIN (SELECT FIELD_NO, FIELD_NM FROM kmdb.FIELD_CODE) B ON B.FIELD_NO=A.FIELD_NO) P ON P.PERSON_ID=P1.PERSON_ID
			LEFT OUTER JOIN(SELECT PERSON_ID, RESEARCH_TITLE, TO_CHAR(SUBSTR(RESEARCH_NOTE, 1, 100)) AS RESEARCH_NOTE FROM kmdb.PERSON_RESEARCH) P3 ON P3.PERSON_ID=P1.PERSON_ID
			--LEFT OUTER JOIN( SELECT C.PERSON_ID,
			--'filmos{'||'movieID='||m.movie_id||m.movie_seq|| ',' ||'TitleKR='||M.TITLE|| ',' ||'PROD_YEAR='||M.PROD_YEAR|| ',' ||'Director='||M.DIRECTOR|| ',' ||'Role='||(SELECT a.CREDIT_NM from CREDIT_MANG a WHERE a.CREDIT_ID = c.CREDIT_ID)|| ',' ||'RoleDetail='||C.STAFF|| '}' AS filmos
			--FROM CREDIT_MOVIE C, MOVIE_SE M
			--WHERE C.MOVIE_ID = M.MOVIE_ID AND C.MOVIE_SEQ = M.MOVIE_SEQ ) P2 ON P2.PERSON_ID = P1.PERSON_ID
			WHERE 1 = 1 
			AND P3.RESEARCH_NOTE IS NOT NULL
			ORDER BY PERSONID ASC
			)ta ) TB
			WHERE TB.NUM BETWEEN 11 AND 20
		

