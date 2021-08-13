{\rtf1\ansi\ansicpg949\cocoartf2580
\cocoatextscaling0\cocoaplatform0{\fonttbl\f0\fnil\fcharset0 Menlo-Regular;\f1\fnil\fcharset129 AppleSDGothicNeo-Regular;\f2\fnil\fcharset0 Menlo-Italic;
}
{\colortbl;\red255\green255\blue255;\red204\green108\blue29;\red217\green232\blue247;\red230\green230\blue250;
\red18\green144\blue195;\red249\green250\blue244;\red128\green128\blue128;\red102\green225\blue248;\red104\green151\blue187;
\red30\green181\blue64;\red121\green171\blue255;\red167\green236\blue33;\red150\green236\blue63;\red160\green160\blue160;
\red23\green198\blue163;}
{\*\expandedcolortbl;;\csgenericrgb\c80000\c42353\c11373;\csgenericrgb\c85098\c90980\c96863;\csgenericrgb\c90196\c90196\c98039;
\csgenericrgb\c7059\c56471\c76471;\csgenericrgb\c97647\c98039\c95686;\csgenericrgb\c50196\c50196\c50196;\csgenericrgb\c40000\c88235\c97255;\csgenericrgb\c40784\c59216\c73333;
\csgenericrgb\c11765\c70980\c25098;\csgenericrgb\c47451\c67059\c100000;\csgenericrgb\c65490\c92549\c12941;\csgenericrgb\c58824\c92549\c24706;\csgenericrgb\c62745\c62745\c62745;
\csgenericrgb\c9020\c77647\c63922;}
\paperw11900\paperh16840\margl1440\margr1440\vieww20480\viewh11160\viewkind0
\deftab720
\pard\pardeftab720\partightenfactor0

\f0\fs24 \cf2 package\cf3  com\cf4 .\cf3 exhibition\cf4 .\cf3 project\cf4 .\cf3 BoardDto\cf4 ;\cf0 \
\
\cf2 public\cf3  \cf2 class\cf3  \cf5 PagingVO\cf3  \cf6 \{\cf0 \
\pard\pardeftab720\partightenfactor0
\cf3 	\cf7 // 
\f1 \'c7\'f6\'c0\'e7\'c6\'e4\'c0\'cc\'c1\'f6
\f0 , 
\f1 \'bd\'c3\'c0\'db\'c6\'e4\'c0\'cc\'c1\'f6
\f0 , 
\f1 \'b3\'a1\'c6\'e4\'c0\'cc\'c1\'f6
\f0 , 
\f1 \'b0\'d4\'bd\'c3\'b1\'db
\f0  
\f1 \'c3\'d1
\f0  
\f1 \'b0\'b9\'bc\'f6
\f0 , 
\f1 \'c6\'e4\'c0\'cc\'c1\'f6\'b4\'e7
\f0  
\f1 \'b1\'db
\f0  
\f1 \'b0\'b9\'bc\'f6
\f0 , 
\f1 \'b8\'b6\'c1\'f6\'b8\'b7\'c6\'e4\'c0\'cc\'c1\'f6
\f0 , SQL
\f1 \'c4\'f5\'b8\'ae\'bf\'a1
\f0  
\f1 \'be\'b5
\f0  start, end\cf0 \
\cf3 		\cf2 private\cf3  \cf2 int\cf3  \cf8 nowPage\cf4 ,\cf3  \cf8 startPage\cf4 ,\cf3  \cf8 endPage\cf4 ,\cf3  \cf8 total\cf4 ,\cf3  \cf8 cntPerPage\cf4 ,\cf3  \cf8 lastPage\cf4 ,\cf3  \cf8 start\cf4 ,\cf3  \cf8 end\cf4 ;\cf0 \
\cf3 		\cf2 private\cf3  \cf2 int\cf3  \cf8 cntPage\cf3  \cf4 =\cf3  \cf9 5\cf4 ;\cf0 \
\cf3 		\cf0 \
\cf3 		\cf2 public\cf3  \cf10 PagingVO\cf6 ()\cf3  \cf6 \{\cf0 \
\cf3 		\cf6 \}\cf0 \
\cf3 		\cf2 public\cf3  \cf10 PagingVO\cf6 (\cf2 int\cf3  \cf11 total\cf4 ,\cf3  \cf2 int\cf3  \cf11 nowPage\cf4 ,\cf3  \cf2 int\cf3  \cf11 cntPerPage\cf6 )\cf3  \cf6 \{\cf0 \
\cf3 			\cf12 setNowPage\cf6 (\cf11 nowPage\cf6 )\cf4 ;\cf0 \
\cf3 			\cf12 setCntPerPage\cf6 (\cf11 cntPerPage\cf6 )\cf4 ;\cf0 \
\cf3 			\cf12 setTotal\cf6 (\cf11 total\cf6 )\cf4 ;\cf0 \
\cf3 			\cf12 calcLastPage\cf6 (\cf12 getTotal\cf6 ()\cf4 ,\cf3  \cf12 getCntPerPage\cf6 ())\cf4 ;\cf0 \
\cf3 			\cf12 calcStartEndPage\cf6 (\cf12 getNowPage\cf6 ()\cf4 ,\cf3  \cf8 cntPage\cf6 )\cf4 ;\cf0 \
\cf3 			\cf12 calcStartEnd\cf6 (\cf12 getNowPage\cf6 ()\cf4 ,\cf3  \cf12 getCntPerPage\cf6 ())\cf4 ;\cf0 \
\cf3 		\cf6 \}\cf0 \
\cf3 		\cf7 // 
\f1 \'c1\'a6\'c0\'cf
\f0  
\f1 \'b8\'b6\'c1\'f6\'b8\'b7
\f0  
\f1 \'c6\'e4\'c0\'cc\'c1\'f6
\f0  
\f1 \'b0\'e8\'bb\'ea
\f0 \cf0 \
\cf3 		\cf2 public\cf3  \cf2 void\cf3  \cf10 calcLastPage\cf6 (\cf2 int\cf3  \cf11 total\cf4 ,\cf3  \cf2 int\cf3  \cf11 cntPerPage\cf6 )\cf3  \cf6 \{\cf0 \
\cf3 			\cf12 setLastPage\cf6 ((\cf2 int\cf6 )\cf3  \cf5 Math\cf4 .
\f2\i \cf13 ceil
\f0\i0 \cf6 ((\cf2 double\cf6 )\cf11 total\cf3  \cf4 /\cf3  \cf6 (\cf2 double\cf6 )\cf11 cntPerPage\cf6 ))\cf4 ;\cf0 \
\cf3 		\cf6 \}\cf0 \
\cf3 		\cf7 // 
\f1 \'bd\'c3\'c0\'db
\f0 , 
\f1 \'b3\'a1
\f0  
\f1 \'c6\'e4\'c0\'cc\'c1\'f6
\f0  
\f1 \'b0\'e8\'bb\'ea
\f0 \cf0 \
\cf3 		\cf2 public\cf3  \cf2 void\cf3  \cf10 calcStartEndPage\cf6 (\cf2 int\cf3  \cf11 nowPage\cf4 ,\cf3  \cf2 int\cf3  \cf11 cntPage\cf6 )\cf3  \cf6 \{\cf0 \
\cf3 			\cf12 setEndPage\cf6 (((\cf2 int\cf6 )\cf5 Math\cf4 .
\f2\i \cf13 ceil
\f0\i0 \cf6 ((\cf2 double\cf6 )\cf11 nowPage\cf3  \cf4 /\cf3  \cf6 (\cf2 double\cf6 )\cf11 cntPage\cf6 ))\cf3  \cf4 *\cf3  \cf11 cntPage\cf6 )\cf4 ;\cf0 \
\cf3 			\cf2 if\cf3  \cf6 (\cf12 getLastPage\cf6 ()\cf3  \cf4 <\cf3  \cf12 getEndPage\cf6 ())\cf3  \cf6 \{\cf0 \
\cf3 				\cf12 setEndPage\cf6 (\cf12 getLastPage\cf6 ())\cf4 ;\cf0 \
\cf3 			\cf6 \}\cf0 \
\cf3 			\cf12 setStartPage\cf6 (\cf12 getEndPage\cf6 ()\cf3  \cf4 -\cf3  \cf11 cntPage\cf3  \cf4 +\cf3  \cf9 1\cf6 )\cf4 ;\cf0 \
\cf3 			\cf2 if\cf3  \cf6 (\cf12 getStartPage\cf6 ()\cf3  \cf4 <\cf3  \cf9 1\cf6 )\cf3  \cf6 \{\cf0 \
\cf3 				\cf12 setStartPage\cf6 (\cf9 1\cf6 )\cf4 ;\cf0 \
\cf3 			\cf6 \}\cf0 \
\cf3 		\cf6 \}\cf0 \
\cf3 		\cf7 // DB 
\f1 \'c4\'f5\'b8\'ae\'bf\'a1\'bc\'ad
\f0  
\f1 \'bb\'e7\'bf\'eb\'c7\'d2
\f0  start, end
\f1 \'b0\'aa
\f0  
\f1 \'b0\'e8\'bb\'ea
\f0 \cf0 \
\cf3 		\cf2 public\cf3  \cf2 void\cf3  \cf10 calcStartEnd\cf6 (\cf2 int\cf3  \cf11 nowPage\cf4 ,\cf3  \cf2 int\cf3  \cf11 cntPerPage\cf6 )\cf3  \cf6 \{\cf0 \
\cf3 			\cf12 setEnd\cf6 (\cf11 nowPage\cf3  \cf4 *\cf3  \cf11 cntPerPage\cf6 )\cf4 ;\cf0 \
\cf3 			\cf12 setStart\cf6 (\cf12 getEnd\cf6 ()\cf3  \cf4 -\cf3  \cf11 cntPerPage\cf3  \cf4 +\cf3  \cf9 1\cf6 )\cf4 ;\cf0 \
\cf3 		\cf6 \}\cf0 \
\cf3 		\cf0 \
\cf3 		\cf2 public\cf3  \cf2 int\cf3  \cf10 getNowPage\cf6 ()\cf3  \cf6 \{\cf0 \
\cf3 			\cf2 return\cf3  \cf8 nowPage\cf4 ;\cf0 \
\cf3 		\cf6 \}\cf0 \
\cf3 		\cf2 public\cf3  \cf2 void\cf3  \cf10 setNowPage\cf6 (\cf2 int\cf3  \cf11 nowPage\cf6 )\cf3  \cf6 \{\cf0 \
\cf3 			\cf2 this\cf4 .\cf8 nowPage\cf3  \cf4 =\cf3  \cf11 nowPage\cf4 ;\cf0 \
\cf3 		\cf6 \}\cf0 \
\cf3 		\cf2 public\cf3  \cf2 int\cf3  \cf10 getStartPage\cf6 ()\cf3  \cf6 \{\cf0 \
\cf3 			\cf2 return\cf3  \cf8 startPage\cf4 ;\cf0 \
\cf3 		\cf6 \}\cf0 \
\cf3 		\cf2 public\cf3  \cf2 void\cf3  \cf10 setStartPage\cf6 (\cf2 int\cf3  \cf11 startPage\cf6 )\cf3  \cf6 \{\cf0 \
\cf3 			\cf2 this\cf4 .\cf8 startPage\cf3  \cf4 =\cf3  \cf11 startPage\cf4 ;\cf0 \
\cf3 		\cf6 \}\cf0 \
\cf3 		\cf2 public\cf3  \cf2 int\cf3  \cf10 getEndPage\cf6 ()\cf3  \cf6 \{\cf0 \
\cf3 			\cf2 return\cf3  \cf8 endPage\cf4 ;\cf0 \
\cf3 		\cf6 \}\cf0 \
\cf3 		\cf2 public\cf3  \cf2 void\cf3  \cf10 setEndPage\cf6 (\cf2 int\cf3  \cf11 endPage\cf6 )\cf3  \cf6 \{\cf0 \
\cf3 			\cf2 this\cf4 .\cf8 endPage\cf3  \cf4 =\cf3  \cf11 endPage\cf4 ;\cf0 \
\cf3 		\cf6 \}\cf0 \
\cf3 		\cf2 public\cf3  \cf2 int\cf3  \cf10 getTotal\cf6 ()\cf3  \cf6 \{\cf0 \
\cf3 			\cf2 return\cf3  \cf8 total\cf4 ;\cf0 \
\cf3 		\cf6 \}\cf0 \
\cf3 		\cf2 public\cf3  \cf2 void\cf3  \cf10 setTotal\cf6 (\cf2 int\cf3  \cf11 total\cf6 )\cf3  \cf6 \{\cf0 \
\cf3 			\cf2 this\cf4 .\cf8 total\cf3  \cf4 =\cf3  \cf11 total\cf4 ;\cf0 \
\cf3 		\cf6 \}\cf0 \
\cf3 		\cf2 public\cf3  \cf2 int\cf3  \cf10 getCntPerPage\cf6 ()\cf3  \cf6 \{\cf0 \
\cf3 			\cf2 return\cf3  \cf8 cntPerPage\cf4 ;\cf0 \
\cf3 		\cf6 \}\cf0 \
\cf3 		\cf2 public\cf3  \cf2 void\cf3  \cf10 setCntPerPage\cf6 (\cf2 int\cf3  \cf11 cntPerPage\cf6 )\cf3  \cf6 \{\cf0 \
\cf3 			\cf2 this\cf4 .\cf8 cntPerPage\cf3  \cf4 =\cf3  \cf11 cntPerPage\cf4 ;\cf0 \
\cf3 		\cf6 \}\cf0 \
\cf3 		\cf2 public\cf3  \cf2 int\cf3  \cf10 getLastPage\cf6 ()\cf3  \cf6 \{\cf0 \
\cf3 			\cf2 return\cf3  \cf8 lastPage\cf4 ;\cf0 \
\cf3 		\cf6 \}\cf0 \
\cf3 		\cf2 public\cf3  \cf2 void\cf3  \cf10 setLastPage\cf6 (\cf2 int\cf3  \cf11 lastPage\cf6 )\cf3  \cf6 \{\cf0 \
\cf3 			\cf2 this\cf4 .\cf8 lastPage\cf3  \cf4 =\cf3  \cf11 lastPage\cf4 ;\cf0 \
\cf3 		\cf6 \}\cf0 \
\cf3 		\cf2 public\cf3  \cf2 int\cf3  \cf10 getStart\cf6 ()\cf3  \cf6 \{\cf0 \
\cf3 			\cf2 return\cf3  \cf8 start\cf4 ;\cf0 \
\cf3 		\cf6 \}\cf0 \
\cf3 		\cf2 public\cf3  \cf2 void\cf3  \cf10 setStart\cf6 (\cf2 int\cf3  \cf11 start\cf6 )\cf3  \cf6 \{\cf0 \
\cf3 			\cf2 this\cf4 .\cf8 start\cf3  \cf4 =\cf3  \cf11 start\cf4 ;\cf0 \
\cf3 		\cf6 \}\cf0 \
\cf3 		\cf2 public\cf3  \cf2 int\cf3  \cf10 getEnd\cf6 ()\cf3  \cf6 \{\cf0 \
\cf3 			\cf2 return\cf3  \cf8 end\cf4 ;\cf0 \
\cf3 		\cf6 \}\cf0 \
\cf3 		\cf2 public\cf3  \cf2 void\cf3  \cf10 setEnd\cf6 (\cf2 int\cf3  \cf11 end\cf6 )\cf3  \cf6 \{\cf0 \
\cf3 			\cf2 this\cf4 .\cf8 end\cf3  \cf4 =\cf3  \cf11 end\cf4 ;\cf0 \
\cf3 		\cf6 \}\cf3 	\cf0 \
\cf3 		\cf2 public\cf3  \cf2 int\cf3  \cf10 setCntPage\cf6 ()\cf3  \cf6 \{\cf0 \
\cf3 			\cf2 return\cf3  \cf8 cntPage\cf4 ;\cf0 \
\cf3 		\cf6 \}\cf0 \
\cf3 		\cf2 public\cf3  \cf2 void\cf3  \cf10 getCntPage\cf6 (\cf2 int\cf3  \cf11 cntPage\cf6 )\cf3  \cf6 \{\cf0 \
\cf3 			\cf2 this\cf4 .\cf8 cntPage\cf3  \cf4 =\cf3  \cf11 cntPage\cf4 ;\cf0 \
\cf3 		\cf6 \}\cf0 \
\cf3 		
\f2\i \cf14 @Override
\f0\i0 \cf0 \
\cf3 		\cf2 public\cf3  \cf5 String\cf3  \cf10 toString\cf6 ()\cf3  \cf6 \{\cf0 \
\cf3 			\cf2 return\cf3  \cf15 "PagingVO [nowPage="\cf3  \cf4 +\cf3  \cf8 nowPage\cf3  \cf4 +\cf3  \cf15 ", startPage="\cf3  \cf4 +\cf3  \cf8 startPage\cf3  \cf4 +\cf3  \cf15 ", endPage="\cf3  \cf4 +\cf3  \cf8 endPage\cf3  \cf4 +\cf3  \cf15 ", total="\cf3  \cf4 +\cf3  \cf8 total\cf0 \
\cf3 					\cf4 +\cf3  \cf15 ", cntPerPage="\cf3  \cf4 +\cf3  \cf8 cntPerPage\cf3  \cf4 +\cf3  \cf15 ", lastPage="\cf3  \cf4 +\cf3  \cf8 lastPage\cf3  \cf4 +\cf3  \cf15 ", start="\cf3  \cf4 +\cf3  \cf8 start\cf3  \cf4 +\cf3  \cf15 ", end="\cf3  \cf4 +\cf3  \cf8 end\cf0 \
\cf3 					\cf4 +\cf3  \cf15 ", cntPage="\cf3  \cf4 +\cf3  \cf8 cntPage\cf3  \cf4 +\cf3  \cf15 "]"\cf4 ;\cf0 \
\cf3 		\cf6 \}\cf0 \
\
\pard\pardeftab720\partightenfactor0
\cf6 \}\cf0 \
}