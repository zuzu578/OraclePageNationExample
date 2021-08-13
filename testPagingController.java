{\rtf1\ansi\ansicpg949\cocoartf2580
\cocoatextscaling0\cocoaplatform0{\fonttbl\f0\fnil\fcharset0 Menlo-Italic;\f1\fnil\fcharset0 Menlo-Regular;\f2\fnil\fcharset0 Menlo-BoldItalic;
}
{\colortbl;\red255\green255\blue255;\red160\green160\blue160;\red249\green250\blue244;\red23\green198\blue163;
\red217\green232\blue247;\red204\green108\blue29;\red18\green144\blue195;\red30\green181\blue64;\red128\green242\blue246;
\red121\green171\blue255;\red230\green230\blue250;\red242\green242\blue0;\red128\green246\blue167;\red102\green225\blue248;
\red243\green236\blue121;\red141\green218\blue248;\red167\green236\blue33;\red150\green236\blue63;}
{\*\expandedcolortbl;;\csgenericrgb\c62745\c62745\c62745;\csgenericrgb\c97647\c98039\c95686;\csgenericrgb\c9020\c77647\c63922;
\csgenericrgb\c85098\c90980\c96863;\csgenericrgb\c80000\c42353\c11373;\csgenericrgb\c7059\c56471\c76471;\csgenericrgb\c11765\c70980\c25098;\csgenericrgb\c50196\c94902\c96471;
\csgenericrgb\c47451\c67059\c100000;\csgenericrgb\c90196\c90196\c98039;\csgenericrgb\c94902\c94902\c0;\csgenericrgb\c50196\c96471\c65490;\csgenericrgb\c40000\c88235\c97255;
\csgenericrgb\c95294\c92549\c47451;\csgenericrgb\c55294\c85490\c97255;\csgenericrgb\c65490\c92549\c12941;\csgenericrgb\c58824\c92549\c24706;}
\paperw11900\paperh16840\margl1440\margr1440\vieww11520\viewh8400\viewkind0
\deftab720
\pard\pardeftab720\partightenfactor0

\f0\i\fs24 \cf2 @RequestMapping
\f1\i0 \cf3 (\cf4 "/testPaging"\cf3 )\cf0 \
\pard\pardeftab720\partightenfactor0
\cf5 	\cf6 public\cf5  \cf7 String\cf5  \cf8 testboardList\cf3 (\cf9 HttpServletRequest\cf5  \cf10 req\cf5  \cf11 ,\cf9 Model\cf5  \cf10 model\cf3 )\cf5  \cf3 \{\cf0 \
\cf5 		\cf7 String\cf5  \cf12 nowPage\cf5  \cf11 =\cf5  \cf10 req\cf11 .\cf13 getParameter\cf3 (\cf4 "nowPage"\cf3 )\cf11 ;\cf0 \
\cf5 		\cf7 String\cf5  \cf12 cntPerPage\cf5  \cf11 =\cf5  \cf10 req\cf11 .\cf13 getParameter\cf3 (\cf4 "cntPerPage"\cf3 )\cf11 ;\cf0 \
\cf5 		\cf9 BoardDao\cf5  \cf12 dao\cf11 =\cf14 sqlSession\cf11 .\cf13 getMapper\cf3 (\cf9 BoardDao\cf11 .\cf6 class\cf3 )\cf11 ;\cf5  \cf0 \
\cf5 		\cf6 int\cf5  \cf12 total\cf5  \cf11 =\cf5  \cf15 dao\cf11 .\cf13 allCount\cf3 ()\cf11 ;\cf0 \
\cf5 		\cf6 if\cf5  \cf3 (\cf15 nowPage\cf5  \cf11 ==\cf5  \cf6 null\cf5  \cf11 &&\cf5  \cf15 cntPerPage\cf5  \cf11 ==\cf5  \cf6 null\cf3 )\cf5  \cf3 \{\cf0 \
\cf5 			\cf15 nowPage\cf5  \cf11 =\cf5  \cf4 "1"\cf11 ;\cf0 \
\cf5 			\cf15 cntPerPage\cf5  \cf11 =\cf5  \cf4 "5"\cf11 ;\cf0 \
\cf5 		\cf3 \}\cf5  \cf6 else\cf5  \cf6 if\cf5  \cf3 (\cf15 nowPage\cf5  \cf11 ==\cf5  \cf6 null\cf3 )\cf5  \cf3 \{\cf0 \
\cf5 			\cf15 nowPage\cf5  \cf11 =\cf5  \cf4 "1"\cf11 ;\cf0 \
\cf5 		\cf3 \}\cf5  \cf0 \
\cf5 			\cf7 System\cf11 .
\f2\i\b \cf16 out
\f1\i0\b0 \cf11 .\cf17 println\cf3 (\cf4 "nowPage ===> "\cf5  \cf11 +\cf5  \cf15 nowPage\cf3 )\cf11 ;\cf0 \
\cf5 			\cf7 System\cf11 .
\f2\i\b \cf16 out
\f1\i0\b0 \cf11 .\cf17 println\cf3 (\cf4 "cntPerPage ==> "\cf11 +\cf5  \cf15 cntPerPage\cf3 )\cf11 ;\cf0 \
\cf5 			\cf7 PagingVO\cf5  \cf12 vo\cf5  \cf11 =\cf5  \cf6 new\cf5  \cf17 PagingVO\cf3 (\cf15 total\cf11 ,\cf5  \cf7 Integer\cf11 .
\f0\i \cf18 parseInt
\f1\i0 \cf3 (\cf15 nowPage\cf3 )\cf11 ,\cf5  \cf7 Integer\cf11 .
\f0\i \cf18 parseInt
\f1\i0 \cf3 (\cf15 cntPerPage\cf3 ))\cf11 ;\cf0 \
\cf5 			\cf10 model\cf11 .\cf13 addAttribute\cf3 (\cf4 "paging"\cf11 ,\cf5  \cf15 vo\cf3 )\cf11 ;\cf0 \
\cf5 			\cf10 model\cf11 .\cf13 addAttribute\cf3 (\cf4 "viewAll"\cf11 ,\cf5  \cf15 dao\cf11 .\cf13 selectBoard\cf3 (\cf15 vo\cf3 ))\cf11 ;\cf0 \
\cf5 			\cf15 cntPerPage\cf5  \cf11 =\cf5  \cf4 "5"\cf11 ;\cf0 \
\
\cf5 		\cf0 \
\cf5 		\cf6 return\cf5  \cf4 "testPaging"\cf11 ;\cf0 \
\cf5 	\cf3 \}}