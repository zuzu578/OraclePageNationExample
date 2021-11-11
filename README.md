# OraclePageNationExample
오라클 페이징 예제 / 구현 

# ROW NUM 사용시 주의사항
이런식으로 rownum 을 밖에다 빼줘야한다. 
# PARTION BY 를 이용하여 중복제거 

# 속보 속보 페이징 하다가 rownum 정렬이 수틀릴때 
- 간혹 그런경우는 서브쿼리를 또 써서 rownum 정렬을 해주도록.


 SELECT TB.*
		FROM (
		SELECT  ROWNUM AS NUM,ta.* FROM (
		SELECT
		A.MOVIE_SEQ AS movieSeq
		, A.TITLE AS title
		,
		A.TITLE_SEARCH AS titleSearch
		, A.ORG_TIT AS orgTit
		,
		A.ORG_TIT_ENG AS orgTitEng
		, A.TITLE_ENG AS titleEng
		,
		A.DIRECTOR AS director
		, A.ORG_AUTHOR AS orgAuthor
		,
		A.SCENARIO AS scenario
		, A.CASTS AS casts
		, TA.CODE_NM AS
		nationalClss
		, A.NATION_CLSS AS nationClss
		, A.PROD_YEAR
		AS prodYear
		, A.COMPY_CLSS AS compyClss
		, TC.CODE_NM AS
		pattenClss
		, A.TYPE_CLSS AS typeClss
		, A.CNS_DATE AS
		cnsDate
		, TD.CODE_NM AS filmcnsClss
		, C.CREDIT_ID
		AS creditId
		, C.CREDIT_SEQ AS creditSeq
		,
		C.PERSON_NM AS personNm
		, C.STAFF AS staff
		, B.PROD_NM AS prodNm
		, B.PROD_ENG_NM AS prodEngNm

		FROM kmdb.MOVIE_SE A
		LEFT JOIN (SELECT MOVIE_ID, MOVIE_SEQ,PROD_NM,PROD_ENG_NM,CREDIT_ID, CREDIT_SEQ, STAFF FROM kmdb.MOVIE_PROD_REL MR 
		WHERE 1 = 1 
		AND ROWID IN (SELECT MAX(ROWID) FROM KMDB.MOVIE_PROD_REL KS WHERE  MR.MOVIE_SEQ  = KS.MOVIE_SEQ )) B on
		B.MOVIE_SEQ=A.MOVIE_SEQ
		LEFT JOIN
		(SELECT A.CREDIT_ID, A.CREDIT_SEQ, A.PERSON_NM, A.STAFF, A.MOVIE_SEQ, A.MOVIE_ID 
	FROM KMDB.CREDIT_MOVIE A
	WHERE 1 = 1
	AND ROWID IN (SELECT MAX(ROWID) FROM KMDB.CREDIT_MOVIE B
                     WHERE A.MOVIE_SEQ = B.MOVIE_SEQ)) C on
		C.MOVIE_SEQ=A.MOVIE_SEQ
		LEFT OUTER JOIN
		(SELECT CODE_NM ,CODE FROM
		kmdb.CODEINFO WHERE DIV_ID
		='A16') TA ON
		TA.CODE = A.NATIONAL_CLSS
		--LEFT OUTER JOIN (SELECT CODE_NM ,CODE FROM kmdb.CODEINFO WHERE DIV_ID ='A11') TB ON TA.CODE = A.COMPY_CLSS
		LEFT OUTER JOIN (SELECT CODE_NM
		,CODE FROM kmdb.CODEINFO WHERE DIV_ID
		='A01') TC ON TC.CODE =
		A.PATTEN_CLSS
		LEFT OUTER JOIN (SELECT CODE_NM
		,CODE FROM kmdb.CODEINFO
		WHERE DIV_ID
		='A29') TD ON TD.CODE =
		A.FILMCNS_CLSS
		WHERE 1 = 1
		<if test="title != ''">
		 AND A.TITLE LIKE '%' || #{title} || '%'
		</if>
		<if test="compyClss != ''">
		 AND TB.CODE_NM LIKE '%' || #{compyClss} || '%'
		
		</if>
		<if test="prodYear != ''">
			AND A.PROD_YEAR = LIKE '%' || #{prodYear} || '%'
			
		</if>
		<if test="movieSeq != ''">
		
			AND A.MOVIE_SEQ = #{movieSeq}
			
		</if>
		<if test='sortProdYear.equals("asc")'>

			ORDER BY A.PROD_YEAR ASC

		</if>

		<if test='sortProdYear.equals("desc")'>
			ORDER BY A.PROD_YEAR desc

		</if>
		)ta 
		WHERE 1 = 1 
		
		)TB
		
	WHERE TB.NUM BETWEEN #{itemFirstNum} and #{itemLastNum}
