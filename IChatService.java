package egovframework.gnbot.ichat.service;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.sql.Array;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognizeRequest;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.protobuf.ByteString;

import egovframework.gnbot.common.exception.IChatException;
import egovframework.gnbot.common.exception.IChatParsingException;
import egovframework.gnbot.common.model.CommandMap;
import egovframework.gnbot.common.util.RestTemplateConnectionPooling;
import egovframework.gnbot.common.util.SessionUtil;
import egovframework.gnbot.ichat.Intent.IntentType;
import egovframework.gnbot.ichat.model.IChatResp;
import egovframework.gnbot.ichat.model.IchatVO;
import egovframework.gnbot.ichat.model.ResponseIchat;
import egovframework.gnbot.ichat.model.UploadImageVO;
import egovframework.gnbot.web.ctrl.GnBotController;
import egovframework.gnbot.web.model.Carousel;
import egovframework.gnbot.web.model.ChatResult;
import egovframework.gnbot.web.model.DataType;
import egovframework.gnbot.web.model.Option;
import egovframework.gnbot.web.model.OutData;
import egovframework.gnbot.web.service.ChatLogService;
import egovframework.gnbot.web.service.ConvertService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("iChatService")
public class IChatService {

	@Autowired
	private PropertiesService propertiesService;

	@Autowired
	private ChatLogService chatLogService;

	@Autowired
	private ConvertService convertService;

	@Autowired
	private IChatService iChatService;

	JSONObject obj;
	private static final String serviceKey = "pIjdyg6yRnPqmwfTfG4m3TIDh518lq4lqoOgjavC5e1QPr3Vut5Dri2mQXpGfX5CbeusLqm9VNvju4fmvIkv0g%3D%3D";

	// usersesssion key Map 담기
	private final ConcurrentHashMap<String, String> userSessionKeyMap = new ConcurrentHashMap<>();
	private static final Logger LOGGER = LoggerFactory.getLogger(GnBotController.class);

	public JSONObject setObject(JSONObject obj) {
		return this.obj = (JSONObject) obj;
	}

	// 음성 Stream 으로 서버에 저장
	public String uploadVoice(String logSeq, MultipartFile mf) {

		String rslt = "";
		AudioInputStream ais = null;
		AudioInputStream stream = null;
		InputStream is = null;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Process proc = null;
		try {
			ais = AudioSystem.getAudioInputStream(mf.getInputStream());
			AudioFormat af = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000, 16, 1, 2, 16000, false);
			stream = AudioSystem.getAudioInputStream(af, ais);
			byte[] data = new byte[6400];
			int readCount = -1;
			while ((readCount = stream.read(data)) != -1) {
				out.write(data, 0, readCount);
			}

			rslt = sampleRecognize(out.toByteArray());
			LOGGER.error(rslt);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			closeStream(ais);
			closeStream(stream);
			closeStream(is);
			closeStream(out);
			if (proc != null) {
				proc.destroy();
			}
		}
		return rslt;
	}

	// 음성 인식(샘플)
	public String sampleRecognize(byte[] data) {
		String transcript = "";

		try (SpeechClient speechClient = SpeechClient.create()) {

			String languageCode = "ko-KR";
			int sampleRateHertz = 16000;
			RecognitionConfig.AudioEncoding encoding = RecognitionConfig.AudioEncoding.LINEAR16;
			RecognitionConfig config = RecognitionConfig.newBuilder().setLanguageCode(languageCode)
					.setSampleRateHertz(sampleRateHertz).setEncoding(encoding).build();

			ByteString content = ByteString.copyFrom(data);
			RecognitionAudio audio = RecognitionAudio.newBuilder().setContent(content).build();
			RecognizeRequest request = RecognizeRequest.newBuilder().setConfig(config).setAudio(audio).build();
			RecognizeResponse response = speechClient.recognize(request);
			for (SpeechRecognitionResult result : response.getResultsList()) {
				SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
				LOGGER.info("Transcript step 1: %s\n", alternative.getTranscript());

				transcript = alternative.getTranscript();
				if (StringUtils.isNotBlank(transcript)) {
					LOGGER.info("Transcript strp 2: %s\n", transcript);
//					getChatSetIncoding(transcript);
					return transcript;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("Failed to create the client due to: " + e);
		}
		return transcript;
	}

	// 음성 Stream 닫기
	public void closeStream(Closeable stream) {
		if (stream != null) {
			try {
				stream.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// 챗 인코딩 얻어오기
	public void getChatSetIncoding(String originalStr) {

//		String originalStr = "Å×½ºÆ®"; // 테스트 
		String[] charSet = { "utf-8", "euc-kr", "ksc5601", "iso-8859-1", "x-windows-949" };

		for (int i = 0; i < charSet.length; i++) {
			for (int j = 0; j < charSet.length; j++) {
				try {
					System.out.println("[" + charSet[i] + "," + charSet[j] + "] = "
							+ new String(originalStr.getBytes(charSet[i]), charSet[j]));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/*
	 * 프로젝트 목록조회
	 */
	public String getDmApiProjectList() {
		String url = propertiesService.dmProtocol + propertiesService.dmIp + ":" + propertiesService.dmPort
				+ propertiesService.dmApiCommonProjectList;

		String param1 = "{ \"pageNum\" :1, \"countPerPage\" : 20, \"order\" : \"ASC\"}";

		JSONObject obj = new JSONObject();
		obj.put("pageNum", 1);
		obj.put("countPerPage", 20);
		obj.put("order", "ASC");

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> param = new HttpEntity<String>(obj.toString(), headers);

		RestTemplate restTemplate = new RestTemplate(RestTemplateConnectionPooling.getInstance().getRequestFactory());
		String result = restTemplate.postForObject(URI.create(url), param, String.class);
		log.debug(result);
		return result;
	}

	/*
	 * 세션키 발급
	 */
	public String getDmApiCommonSessionRequest(String uniqueKey) {
		userSessionKeyMap.remove(uniqueKey);

		String url = propertiesService.dmProtocol + propertiesService.dmIp + ":" + propertiesService.dmPort
				+ propertiesService.dmApiCommonSessionRequest;

		LOGGER.info(">>>>>> url:" + url);
		String param = "{}";

		RestTemplate restTemplate = new RestTemplate(RestTemplateConnectionPooling.getInstance().getRequestFactory());
		ResponseIchat result = restTemplate.postForObject(URI.create(url), param, ResponseIchat.class);
		return result.getSessionKey();
	}

	/*
	 * 세션키 Validation
	 */
	public String getDmApiCommonSessionValidation(String uniqueKey) {
		String url = propertiesService.dmProtocol + propertiesService.dmIp + ":" + propertiesService.dmPort
				+ propertiesService.dmApiCommonSessionValidation;

		Map<String, String> paramMap = new HashMap<>();
		if (userSessionKeyMap == null) {
			// userSessionKeyMap.remove(uniqueKey);
			paramMap.put("sessionKey", "");
		} else {
			userSessionKeyMap.putIfAbsent(uniqueKey, "");
			paramMap.put("sessionKey", userSessionKeyMap.get(uniqueKey));
		}

		log.debug("userSessionKeyMap :: userSessionKey: " + uniqueKey);

		RestTemplate srestTemplate = new RestTemplate(RestTemplateConnectionPooling.getInstance().getRequestFactory());
		ResponseIchat result = srestTemplate.postForObject(URI.create(url), paramMap, ResponseIchat.class);

		String userSessionKey = "";

		if (userSessionKeyMap != null) {
			userSessionKey = (result.getIsValid()) ? userSessionKeyMap.get(uniqueKey)
					: getDmApiCommonSessionRequest(uniqueKey);
			userSessionKeyMap.put(uniqueKey, userSessionKey);
		}

		log.debug("uniqueKey :: userSessionKey: " + uniqueKey + " :: " + userSessionKey);

		return userSessionKey;
	}

	boolean isRTable = false;
	
	/**
	 * 대기질 예보통보 조회 하기
	 * http://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getMinuDustFrcstDspth?serviceKey=pIjdyg6yRnPqmwfTfG4m3TIDh518lq4lqoOgjavC5e1QPr3Vut5Dri2mQXpGfX5CbeusLqm9VNvju4fmvIkv0g%3D%3D&returnType=json&numOfRows=100&pageNo=1&searchDate=2021-11-16&InformCode=PM10	
	 * @param formatedNow
	 * @param area
	 * @return
	 * @throws Exception
	 */
	public HashMap<String, Object> getAirQualityForeCast(String formatedNow, String area) throws Exception {
		// <EVAPI>♠서울시 대기 예보♠</EVAPI>
		String API_TRAN = "http://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getMinuDustFrcstDspth";
		
		String result = "";
		// JSON응답데이터
		JSONObject foreCastObj = null;
		
		String InformCode = "PM10"; // InformCode 는 PM10 값으로 합니다.
		List<HashMap<String , String>> tempInformGrade = new ArrayList<HashMap<String , String>>();
				
		// 예보데이터를 가져오기위해 현재 날짜에서 내일 (+1) 날짜 데이터 변환
		/**/
		StringBuffer tomorrowDate = new StringBuffer();

		String tomorrow = formatedNow.replace("-", "");
		int tomorrowDate2 = Integer.parseInt(tomorrow) + 1;
		
		String tomorrowDate3 = Integer.toString(tomorrowDate2);

		tomorrowDate.append(tomorrowDate3);
		tomorrowDate.insert(4, "-");
		tomorrowDate.insert(7, "-");

		String tomorrowDateTime = tomorrowDate.toString();	
		
		HashMap<String , Object> mMap = new HashMap<String , Object>();
		
		try {
			// test date 
			//String tomorrowDateTime = "2021-11-21";

			URL url = new URL(API_TRAN + "?serviceKey=" + serviceKey + "&returnType=" + "json" + "&searchDate=" + formatedNow + "&InformCode=" + InformCode);
			System.out.println("url => " + url);
			BufferedReader bf = null;
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestMethod("GET");
			
			bf = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));
			result = bf.readLine();

			JSONParser jsonParser = new JSONParser();
			JSONObject jsonObject = (JSONObject) jsonParser.parse(result);

		
			List<String> rowData = new ArrayList<String>();
			
		
			
			JSONObject response = (JSONObject) jsonObject.get("response");
			JSONObject body = (JSONObject) response.get("body");
			JSONArray items = (JSONArray) body.get("items");

			for (int i = 0; i < items.size(); i++) {
			
				JSONObject jObj = (JSONObject) items.get(i);
			
				String informCode = jObj.get("informCode").toString();
				String informCause = jObj.get("informCause").toString();
				String informOverall = jObj.get("informOverall").toString();
				String informData =  jObj.get("informData").toString();
				String informGrade = jObj.get("informGrade").toString();
				String dataTime = jObj.get("dataTime").toString();
				
				// 내일날짜 조건 + inform 코드값이 PM10 인 것일때만 필터링해줘서 map 에 담습니다. 
				// 최종 출력 : mMap => overall / cause / ex:{서울 : 보통} 
				if (informData.equals(tomorrowDateTime) && informCode.equals(InformCode)) {
				
					//rowData.add(informCode);
					rowData.add(informOverall);
					rowData.add(informCause);
					//rowData.add(informData);
					//rowData.add(informGrade);
					//rowData.add(dataTime);
					mMap.put("rowData",rowData);
				
					String[] informCodeArr = informGrade.split(",");
					for(int q = 0 ; q < informCodeArr.length; q++) {
						HashMap<String , String > informGradeStatus = new HashMap<String , String>();
						String array[] = informCodeArr[q].split(":");
						String city = array[0].replaceAll(" ", "");
						String status = array[1].replaceAll(" ", "");
						informGradeStatus.put("city",city );
						informGradeStatus.put("status",status );
						
						tempInformGrade.add(informGradeStatus);
					}
					// ex) 서울 로 param이들어오면 서울에 해당하는 미세먼지 정보를 필터링한다.
					 List<HashMap<String, String>> mapList = tempInformGrade.stream()
					            .filter(map -> map.get("city").equals(area))
					            .collect(Collectors.toList());
					 mMap.put("informGrade",mapList);
					 // 한 행만, 최종적으로 출력되게 하기 
					 break;
					
				}
			}
			// 최종 결과 예시 => {rowData=[○ [미세먼지] 전 권역이 '좋음'∼'보통'으로 예상됩니다., ○ [미세먼지] 원활한 대기 확산과 강수의 영향으로 대기 상태가 대체로 '보통' 수준일 것으로 예상됩니다.], informGrade=[{city=서울, status=보통}]}
			System.out.println("mMap =>" + mMap);
			//return mMap;

		} catch (Exception e) {
			System.out.println("e ==>" + e);
		}
		//return mMap;
		return mMap;

	}

	

	/*
	 * 사용자질의에 대한 답변 요청
	 */
	public OutData getChatResponse(CommandMap commandMap, String userQuery, String convQuery, String uniqueKey,
			Map<String, Object> logMap) throws IChatException, IChatParsingException, Exception {
		String userSessionKey = this.getDmApiCommonSessionValidation(uniqueKey);

		String url = propertiesService.dmProtocol + propertiesService.dmIp + ":" + propertiesService.dmPort
				+ propertiesService.dmApiCommonWiseIChatResponse;
		/*
		 * String intenturl = propertiesService.dmProtocol + propertiesService.dmIp
		 * +":"+ propertiesService.dmPort + propertiesService.dmApiIntentDetail;
		 */

		String tempAnswer = "";
		String intentNm = "";
		String projectId = propertiesService.dmProjectId.trim();
		// HashMap<String, Object> resultMap = new HashMap<String, Object>();
		OutData output = new OutData();
		// ArrayList<Carousel> caroList = new ArrayList<>();

		JSONObject obj = new JSONObject();

		obj.put("projectId", projectId);
		obj.put("sessionKey", userSessionKey);
		obj.put("isDebug", true);
		obj.put("customKey", userSessionKey);

		// 변환쿼리(convQuery)가 null--> userQuery로 대체, null이 아니면 convQuery로 그냥 사용
		if ("".equals(convQuery)) {
			obj.put("query", userQuery);
		} else {
			obj.put("query", convQuery);
		}

		log.info("[input] ==>" + obj.toJSONString());

		Boolean stationNameChk = false;
		Boolean tableChk = false;

		// 챗봇 대답 변수 iChatResp
		IChatResp iChatResp = new IChatResp();
		// Intent intentResp = new Intent();

		String posX = "";
		String posY = "";
		String stationName = "";
		JSONArray items = null;
//		Map<String, List<String>> tableAnswer = null;
		String tableAnswer = "";

		ArrayList<HashMap<String, Object>> tablelist = null;
		
		// API 호출
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);

			// http header에 http엔터티 변수 넣기
			HttpEntity<String> param = new HttpEntity<String>(obj.toString(), headers);

			// RestAPI 템플릿
			RestTemplate restTemplate = new RestTemplate(
					RestTemplateConnectionPooling.getInstance().getRequestFactory());

			// UTF-8로 Charset
			restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));

			// 챗봇 대답 API호출(parameter포함) 답변
			
			iChatResp = restTemplate.postForObject(URI.create(url), param, IChatResp.class);
			// test 용 setAnswer
			iChatResp.setAnswer("<EVAPI>♠서울시 대기 예보♠</EVAPI>");
			// 챗봇 답변(외부 API 호출): 실시간 대기정보 API
			if (iChatResp.getAnswer() != null && !iChatResp.getAnswer().equals("")
					&& iChatResp.getAnswer().contains("♠실시간 미세먼지 농도♠")) {

				posX = commandMap.get("startLon1").toString();
				posY = commandMap.get("startLat1").toString();
				Object accessToken = iChatService.getAccessToken(); // errMsg 예외처리 (API인증키)

				JSONObject GPSjson = iChatService.getGps2TM(posX, posY, accessToken); // errMsg 예외처리(위경도변환)
				Map<String, Object> jsonObject = iChatService.getTM2NearStn(GPSjson); // 가까운 측정소 3개 얻어오기

				commandMap.put("jsonObject", jsonObject); // jsonObject 얻은 값을 담는다.
				// commandMap.put("stsKey", "stsKey");
				output.setStationKey("station");

			}else if (iChatResp.getAnswer().contains("♠") && iChatResp.getAnswer().contains("대기 예보")) {
				String tempArea = iChatResp.getAnswer().toString();
				String area = tempArea.substring(8, 10);
				// 내일 대기 예보정보 api 를 가져옵니다.
				// 현재 날짜 구하기
				LocalDate now = LocalDate.now();
				// 포맷 정의
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
				// 포맷 적용
				String formatedNow = now.format(formatter);

				formatedNow = formatedNow.replace("/", "-");

				HashMap<String, Object> jsonObject = getAirQualityForeCast(formatedNow, area);

			}

			if (commandMap.get("stationKey") != null && !commandMap.get("stationKey").equals("")
					&& !commandMap.get("stationKey").equals("null")) {
				output.setStationName((String) commandMap.get("query"));
				stationName = output.getStationName();
				LOGGER.info("stationName ======> " + stationName);
				if (items == null) {
					// character (<) at position 0. exception
					items = iChatService.getNearStn2RtmData(stationName);
					tablelist = iChatService.getCvtTable(items);

					tableChk = true;
				}
		
			}

			Map<String, Object> map = (Map<String, Object>) commandMap.getMap();
			JSONObject jsonObject = (JSONObject) map.get("jsonObject");
			if (jsonObject != null) {
				JSONObject response = (JSONObject) jsonObject.get("response");
				JSONObject body = (JSONObject) response.get("body");
				items = (JSONArray) body.get("items");
				String btnInfo = getCvtButton(items);

				if (btnInfo != null && !btnInfo.equals("")) {
					stationNameChk = true;
					iChatResp.setAnswer(btnInfo);
				}
				
			}

			// 인텐트 파싱
			intentNm = iChatResp.getResponse().get("topIntentName").toString().trim();

			// 이미지 답변 <IMG>
			if (iChatResp.getAnswer().indexOf("<IMG>") != -1) {
				String imgAnswer = imgTagChange(iChatResp.getAnswer());
				iChatResp.setAnswer(imgAnswer);

			}

			/**
			 * 동적 답변 ◆ : 다이아몬드이면 ====> DataType: TALK 1. RBTN 이면 버튼 타입 2. Response Type이
			 * 'REQUESTION'(재질의) 이면 IntentNm과 Type 'IN' =====> INTENT매핑 3. 링크타입 : (1)
			 * \"linkType\":\"CL\" 일 경우----> button 'N' (2) \"linkType\":\"IL\" 일 경우---->
			 * button 'Y' (3) \"linkType\":\"RL\" 일 경우----> button 여러개
			 * 
			 * ▶{ 방향표 큰 꺽쇠이면 =====> button 'Y'
			 */

			iChatResp.setAnswer(iChatResp.getAnswer().replaceAll("\r\n", "<br>").replaceAll("\n", "<br>")
					.replaceAll("<br><br>", "<br>").trim());
			if (iChatResp.getAnswer().contains("◆")) {
				if (userSessionKeyMap != null) {
					userSessionKeyMap.clear();
				}
				String sessionKey = null;
				sessionKey = iChatService.getDmApiCommonSessionValidation("NULL");
				SessionUtil.setAttribute("userSessionKey", sessionKey == null ? "null" : sessionKey);
				output.setRequest(true);
			}
			log.info("[IChatResp]############################################### " + url);
			log.info(iChatResp.toString());

		} catch (MalformedURLException e) {
			throw new IChatException("ChatBot Response Error : " + e.getMessage());
		} catch (IOException e) {
			throw new IChatException("ChatBot Response Error : " + e.getMessage());
		}

		if ("error".equals(iChatResp.getStatus())) {
			throw new IChatException("ChatBot Response Error : " + iChatResp.getStatus());
		}

		output.setType(DataType.TALK); // default

		boolean isRButton = false;

		if (stationNameChk) {

			tempAnswer = iChatResp.getAnswer().toUpperCase();
			// 버튼
			isRButton = isRButtonAnswer(tempAnswer);
			
			

		}

		if (tableChk) {
			output.setStationName((String) commandMap.get("query"));
			stationName = output.getStationName();
			String value2 = "";
			String startTable = "<TABLE>";
			String endTable = "</TABLE>";

			value2 = startTable + tablelist + endTable;

			iChatResp.setAnswer(value2);
			tempAnswer = iChatResp.getAnswer().toUpperCase();
			String type = "TABLE";
			tableAnswer = getParamsForDynamicAnswer(tempAnswer, type);
			isRTable = isTableAnswer(tempAnswer);

		}

		boolean isPhone = isPhoneAnswer(tempAnswer);

		if (isRButton) {
			String type = "BTN";
			String btnAnswer = getParamsForDynamicAnswer(tempAnswer, type);
			makeButtonResult(iChatResp, output, btnAnswer, type);
			output.setStationKey("station");
		} else if (isRTable) {
			
			// output.setSubAnswer(tableAnswer);
			makeTableResult(iChatResp, output);
		} else {
			makeChatResult(iChatResp, output);
		}

//		    if(isPhone){
//	    		String type = "PHONE";
//	    		String btnAnswer = getRelationPhone(tempAnswer,type);
//	    		makeButtonResult(iChatResp, output, btnAnswer, type);
//	    	}else {
//		    		makeChatResult(iChatResp, output);
//		    }

		// 재질의
		IchatVO iVO = null;
		// 파라미터 VO
		IchatVO pVO = new IchatVO();
		IchatVO tempVo = new IchatVO();
		String subAnswer = "";

		if (!iChatResp.getResponse().get("responseType").toString().trim().equals("REQUESTION")) {
			// System.out.println("##################################################");
			// System.out.println(intentNm);
			// 인텐트명 답변 매핑 확인
			pVO.setIntentNm(intentNm);
			pVO.setType("IN");
			pVO.setProjectId(projectId);
			iVO = convertService.selectResultMapping(pVO);
		}

		// 부 답변
		if (iVO != null) {
			subAnswer = iVO.getContent();

			// 링크 타입 답변
			// 링크 버튼 N
			if (subAnswer.indexOf("\"linkType\":\"CL\"") != -1) {
				JSONParser parse = new JSONParser();
				JSONObject caro = (JSONObject) parse.parse(subAnswer);
				JSONArray caroArr = (JSONArray) caro.get("dataArray");
				// System.out.println(caroArr.toString());
				Carousel[] caroList = new Carousel[caroArr.size()];

				for (int k = 0; k < caroArr.size(); k++) {
					Carousel tempCaro = new Carousel();
					JSONObject tempObj = (JSONObject) caroArr.get(k);
					tempCaro.setName((String) tempObj.get("name"));
					tempCaro.setDesc((String) tempObj.get("mainUserQuery"));

					pVO.setIntentNm(tempCaro.getName());
					tempVo = convertService.selectResultMapping(pVO);

					if (tempVo != null) {
						tempCaro.setContent(tempVo.getContent());
					}
					caroList[k] = tempCaro;
				}
				output.setCaro(caroList);
				output.setBtnYn("N");
				// 링크 버튼 Y
			} else if (subAnswer.indexOf("\"linkType\":\"IL\"") != -1) {

				output.setBtnYn("Y");

				// 링크 버튼 여러개
			} else if (subAnswer.indexOf("\"linkType\":\"RL\"") != -1) {
				JSONParser parse = new JSONParser();
				JSONObject caro = (JSONObject) parse.parse(subAnswer);
				JSONArray caroArr = (JSONArray) caro.get("dataArray");

				Carousel caroList = new Carousel();
				caroList.setName((String) caro.get("name"));
				caroList.setDesc((String) caro.get("mainUserQuery"));
				caroList.setContent(subAnswer);

				output.setRbtnArr(caroList);
			}

		}

		if (tablelist != null && tablelist.size() > 0) {

			output.setTableList(tablelist);
			output.setStationKey("station");

		}

		// button 'Y'
		if (iChatResp.getAnswer().indexOf("▶{") != -1) {
			log.info("Answer========================================" + iChatResp.getAnswer());
			output.setSubAnswer(iChatResp.getAnswer());
			output.setBtnYn("Y");

		}

		// 로그 등록
		logMap.put("logQuery", userQuery);
		logMap.put("logAnswer", output.getResult().getMessage());
		logMap.put("logIntentName", intentNm);
		logMap.put("logCategoryName", iChatResp.getResponse().get("categoryName"));
		// logMap.put("logCategoryName", intentResp.getIntent().get("categoryName"));
		logMap.put("projectId", projectId);

		chatLogService.insertDetailLog(logMap);

		return output;
	}

	// 버튼 답변 체크 <LBTN>, <SBTN>, <A>
	public boolean isButtonAnswer(String answer) {
		boolean result = false;
		if (answer.contains("<LBTN>") || answer.contains("<SBTN>") || answer.contains("<A>")) {
			result = true;
		}

		return result;
	}

	// 버튼 답변 체크 <RBTN>
	public boolean isRButtonAnswer(String answer) {
		boolean result = false;
		if (answer.contains("<BTN>")) {
			result = true;
		}

		return result;
	}

	// 테이블 답변 체크 <TABLE>
	public boolean isTableAnswer(String answer) {
		boolean result = false;
		if (answer.contains("<TABLE>")) {
			result = true;
		}

		return result;
	}

	// 폰 답변 체크
	public boolean isPhoneAnswer(String answer) {
		boolean result = false;
		if (answer.contains("<PHONE>")) {
			result = true;
		}

		return result;
	}

	/* 동적답변 여부 체크 ♠(시작/ 끝) 포함 */
	public boolean isDynamicAnswer(String answer) {
		boolean result = false;
		if (answer.contains("♠") && answer.contains("♠")) {
			result = true;
		}
		return result;
	}

	// 용어사전 여부체크 ▶{ (시작) }◀(끝) /
	public boolean hasReplaceWord(String answer) {
		boolean result = false;
		if (answer.contains("▶{") && answer.contains("}◀")) {
			result = true;
		}
		return result;
	}

	// 챗봇답변 convert 여부체크 ♣(시작/ 끝)
	public boolean hasReplaceResult(String answer) {
		boolean result = false;
		if (answer.contains("♣") && answer.contains("♣")) {
			result = true;
		}
		return result;
	}

	/* 용어사전 replace */
	public void replaceWord(IChatResp response) {
		Map<String, String> result = new HashMap<>();
		int startFilterContentIndex = 0;
		String content = "";
		String answer = response.getAnswer();

		// result-content.. ▶ ◀

		if (answer.contains("▶{") && answer.contains("}◀")) {
			String[] answerArr = answer.split("}◀");
			for (String answer2 : answerArr) {
				if (answer2.contains("▶{")) {
					startFilterContentIndex = answer2.indexOf("▶{");
					log.info("content: " + content);

					List<String> builderArr = new ArrayList<String>();
					StringBuilder builder = new StringBuilder();
					content = answer2.substring(startFilterContentIndex + 2).trim();
					builder.append("<a class=\'help\' onclick=getDictionay(\'");
					builder.append(content);
					builder.append("\');>");
					builder.append(content);
					builder.append("</a>");
					builderArr.add(builder.toString());
					answer = answer.replace("▶{" + content + "}◀",
							builderArr.toString().replace("[", "").replace("]", ""));
				}
			}

			log.info("relpaceStr :" + answer);
			response.setAnswer(answer);

		}
	}

	// img tag 이미지 변경
	public String imgTagChange(String answer) {
		Pattern p = Pattern.compile("<IMG>.*</IMG>");
		Matcher m = p.matcher(answer);
		String imgval = "";
		// String strResult = new String("");
		UploadImageVO pVO = new UploadImageVO();
		UploadImageVO rVO = new UploadImageVO();

		// 이미지 값 있을 시 변환
		try {
			while (m.find()) {

				imgval = m.group().replace("<IMG>", "").replace("</IMG>", "");
				// System.out.println("id::"+imgval);
				pVO.setImageId(imgval);
				rVO = convertService.selectUploadImage(pVO);
				if (rVO != null) {

					// System.out.println("[CHECK] strResult : " + rVO.getImageFileservername());
					answer = answer.replace(imgval, rVO.getImageFileservername());
				}

			}
		} catch (NullPointerException e) {
			System.out.println("[ERROR] Exception : NullPointerException");
		} catch (Exception e) {
			System.out.println("[ERROR] Exception : Exception");
		}

		return answer;
	}

	// link tag 이미지 변경
	public String linkTagChange(String answer) {
		Pattern p = Pattern.compile("<A>.*</A>");
		Matcher m = p.matcher(answer);
		String linkval = "";
		// String strResult = new String("");
		UploadImageVO pVO = new UploadImageVO();
		UploadImageVO rVO = new UploadImageVO();

		// 이미지 값 있을 시 변환
		try {
			while (m.find()) {

				linkval = m.group().replace("<A>", "").replace("</A>", "");

				String[] btnList = linkval.split("\\|");
				String temp_link = "";
				for (int i = 0; i < btnList.length; i++) {
					String[] btnOption = btnList[i].trim().split("\\^");
					temp_link += "<a class=\"btn btn_block link\" onclick=goLink(\"" + btnOption[1]
							+ "\"); return false;>" + btnOption[0] + "<i class=\"fas fa-angle-right\"></i></a>";
				}
				answer = answer.replace(m.group(), temp_link);
			}
		} catch (NullPointerException e) {
			System.out.println("[ERROR] Exception : NullPointerException");
		} catch (Exception e) {
			System.out.println("[ERROR] Exception : Exception");
		}

		return answer;
	}

	/*
	 * public String[] getParamsForDynamicAnswer(String dynamicAnswer) { int
	 * startFilterParamIndex = 0; int endFilterParamIndex = 0; String getParam = "";
	 * String[] getParams = new String[2];
	 * 
	 * //params.. if(dynamicAnswer.contains("♠")) { //&&
	 * dynamicAnswer.contains("♠")) { startFilterParamIndex =
	 * dynamicAnswer.indexOf("♠"); endFilterParamIndex =
	 * dynamicAnswer.lastIndexOf("♠"); getParam =
	 * dynamicAnswer.substring(startFilterParamIndex + 1, endFilterParamIndex); //
	 * 숫자 2를 체크하세요 -> ♠ 중괄호2개 int idx = getParam.indexOf(","); if(idx<1){
	 * getParams[0] = getParam; getParams[1] = ""; } else { getParams[0] =
	 * getParam.substring(0,getParam.indexOf(",")); getParams[1] =
	 * getParam.substring(getParam.indexOf(",")+1); } log.debug("getParam: " +
	 * getParam); } return getParams; }
	 */

	public String getRelationPhone(String Answer) {
		int startFilterParamIndex = 0;
		int endFilterParamIndex = 0;
		int index = 7;
		String getParam = "";
		String answer = Answer.toUpperCase();
		// dynamicAnswer = dynamicAnswer.toUpperCase();

		// params..
		// if(dynamicAnswer.contains("<LBTN>")) {
		startFilterParamIndex = answer.indexOf("<EVAPI>");
		endFilterParamIndex = answer.lastIndexOf("</EVAPI>");

		getParam = Answer.substring(startFilterParamIndex + index, endFilterParamIndex); // 숫자 2를 체크하세요 -> ♠ 중괄호2개
		log.debug("getParam: " + getParam);
		// }

		return getParam;
	}

	//

	/**
	 * OAuth 인증키 API 호출 시 사용하는 accessToken 가져오기
	 * 
	 * @return
	 * @throws Exception errMsg 로 예외처리 해줘야 함
	 */
	public Object getAccessToken() throws Exception {

		String API_TOKEN = "https://sgisapi.kostat.go.kr/OpenAPI3/auth/authentication.json";
		String consumer_key = "c659978309e44c4a9a4f"; // 서비스 ID
		String consumer_secret = "ff2ff37ca278484d825b"; // 서비스 Secret
		String res = ""; // JSON 응답 데이터
		Object accessToken = "";
		try {
			URL url = new URL(API_TOKEN + "?consumer_key=" + consumer_key + "&consumer_secret=" + consumer_secret);

			BufferedReader bf = null;
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestMethod("GET");

			bf = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));

			res = bf.readLine();

			JSONParser jsonParser = new JSONParser();
			JSONObject jsonObject = (JSONObject) jsonParser.parse(res);
			JSONObject result = (JSONObject) jsonObject.get("result");

			Object errMsg = jsonObject.get("errMsg"); // 예외처리를 위한 에러메시지 가져오기
			accessToken = result.get("accessToken");

		} catch (Exception e) {
			e.printStackTrace();
		}

		return accessToken;
	}

	/**
	 * 사용자 위치(위경도) 얻어와 TM좌표로 전환하기
	 * 
	 * @return
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 * @throws ParseException
	 * @example https://sgisapi.kostat.go.kr/OpenAPI3/transformation/transcoord.json?accessToken=cd649b5c-261a-4fe2-8c1a-8a5047b9aaa4&src=4326&dst=5179&posX=127.1169024&posY=37.51936
	 */
	public JSONObject getGps2TM(String startLon1, String startLat1, Object accessToken) throws Exception {
		String API_TRAN = "https://sgisapi.kostat.go.kr/OpenAPI3/transformation/transcoord.json";
//		String accessToken = "b7d606df-9358-4c66-9fd2-73e971161143";
		String src = "4326"; // WGS84 경위도
		String dst = "5181"; // 중부원점 TM좌표
//		String posX="127.392925"; //TM좌표 X
//		String posY="36.343492"; //TM좌표 Y
		String result = ""; // JSON응답데이터
		// HttpURLConnection conn = null;
		JSONObject GPSjson = null;
		try {
			// API URL 설정
			URL url = new URL(API_TRAN + "?accessToken=" + accessToken + "&src=" + src + "&dst=" + dst + "&posX="
					+ startLon1 + "&posY=" + startLat1);

			// API URL을 연결하여 JSON data 준비 후 READ LINE
			BufferedReader bf = null;
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestMethod("GET");

			bf = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));

			System.out.println("url ======> " + url);
			result = bf.readLine();

			// JSON데이터 Read 후 posX, posY 파싱
			JSONParser jsonParser = new JSONParser();
			JSONObject jsonObject = (JSONObject) jsonParser.parse(result);

			GPSjson = (JSONObject) jsonObject.get("result");

		} catch (Exception e) {
			e.printStackTrace();
		}
		return GPSjson;
	}

	public String getCvtButton(JSONArray items) {
		String sttBtnInfo = "";
		if (items.size() > 0) {

			sttBtnInfo = "<BTN>";
			String value = "";
			for (int i = 0; i < items.size(); i++) {
				JSONObject itemsObject = (JSONObject) items.get(i);
				String tm = itemsObject.get("tm").toString();
				String stationName = itemsObject.get("stationName").toString();
				String addr = itemsObject.get("addr").toString();
				value += stationName + ":" + addr + "^" + stationName + "^IN";
				System.out.println("json array! " + itemsObject.get("tm"));
				System.out.println("json array! " + itemsObject.get("stationName"));
				System.out.println("json array! " + itemsObject.get("addr"));

				if ((items.size() - 1) != i) {
					value += " | ";
				}

				// String tempStr =
				// "<BTN>"+itemsObject.get("stationName")+":"+itemsObject.get("addr")+"^"+itemsObject.get("stationName")+"^IN</BTN>";
			}
			sttBtnInfo += value;
			sttBtnInfo += "</BTN>";
		}
		return sttBtnInfo;
	}

	public ArrayList<HashMap<String, Object>> getCvtTable(JSONArray items2) {
		JSONArray items = items2;
//		[{key:value} , {key:value} , {key:value}]
		ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
		String value2 = "";

		if (items != null) {
			for (int i = 0; i < items.size(); i++) {
				HashMap<String, Object> map = new HashMap<String, Object>();
				// List<String> map = new ArrayList<String>();
				JSONObject itemsObject = (JSONObject) items.get(i);
				String dataTime = itemsObject.get("dataTime").toString();
				String pm10Value = itemsObject.get("pm10Value").toString();
				String pm10Grade = itemsObject.get("pm10Grade").toString();
				map.put("dataTime", dataTime);
				map.put("pm10Value", pm10Value);
				map.put("pm10Grade", pm10Grade);
				list.add(map);

			}
			System.out.println("list !!!!!!!!!!!!  = > " + list);
		}

		return list;
	}

	/*
	 * @example
	 * http://apis.data.go.kr/B552584/MsrstnInfoInqireSvc/getNearbyMsrstnList?tmX=
	 * 966146.6534889903&tmY=1946743.1036618878&returnType=json&serviceKey=
	 * kz55Vyjh95Lq9n6BSbWZ8V5d2t%2FgR8BR5j5vkXVh3%
	 * 2Fej5S7DBxKoVT2OaVmPca5OXYzy5WsPyVfgxXDeTSMG8g%3D%3D
	 */
	/** TM좌표로 인근 기상관측측정소 구하기 */
	public JSONObject getTM2NearStn(JSONObject GPSjson) {
		String API_NearStn = "http://apis.data.go.kr/B552584/MsrstnInfoInqireSvc/getNearbyMsrstnList";
		String serviceKey = "kz55Vyjh95Lq9n6BSbWZ8V5d2t%2FgR8BR5j5vkXVh3%2Fej5S7DBxKoVT2OaVmPca5OXYzy5WsPyVfgxXDeTSMG8g%3D%3D"; // serviceKey
		String returnType = "json"; // 응답타입 xml,json
		String tmX = GPSjson.get("posX").toString(); // 중부원점 TM좌표 X좌표
		String tmY = GPSjson.get("posY").toString();
		; // 중부원점 TM좌표 Y좌표
		String result = "";// JSON응답데이터

//		HashMap<String, Object> map = new HashMap<String, Object>();
//		obj = new JSONObject(); // JSONArray items 에서 뽑은 값 저장할 곳

		JSONObject jsonObject = null;
		try {
			// API URL 설정
			URL url = new URL(API_NearStn + "?tmX=" + tmX + "&tmY=" + tmY + "&returnType=" + returnType + "&serviceKey="
					+ serviceKey);

			// API URL을 연결하여 JSON data 준비 후 READ LINE
			BufferedReader bf = null;
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestMethod("GET");
			bf = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));

			result = bf.readLine();

			// JSON데이터 Read 후 items(측정소정보) 파싱
			JSONParser jsonParser = new JSONParser();
			jsonObject = (JSONObject) jsonParser.parse(result);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return jsonObject;
	}

	/**
	 * 인근 기상관측측정소실시간 측정정보 구하기
	 * 
	 * @throws UnsupportedEncodingException
	 */
	public JSONArray getNearStn2RtmData(String stationName2) throws UnsupportedEncodingException {
		String stationName = stationName2;
		String API_RTMDATA = "http://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getMsrstnAcctoRltmMesureDnsty";
		String serviceKey = "kz55Vyjh95Lq9n6BSbWZ8V5d2t%2FgR8BR5j5vkXVh3%2Fej5S7DBxKoVT2OaVmPca5OXYzy5WsPyVfgxXDeTSMG8g%3D%3D"; // serviceKey
		String returnType = "json"; // 응답타입 xml,json
		String dataTerm = "DAILY"; // 데이터 기간 (1일 : DAILY, 1개월: MONTH, 3개월: 3NONTH)
		String pageNo = "1";
		String numOfRows = "100";
		// String ver=""; (규격서 참조)


		String result = ""; // JSON응답데이터
		JSONArray items = null;

		try {
			// API URL 설정
			URL url = new URL(API_RTMDATA + "?stationName=" + URLEncoder.encode(stationName, "UTF-8") + "&dataTerm="
					+ dataTerm + "&pageNo=" + pageNo + "&numOfRows=" + numOfRows + "&returnType=" + returnType
					+ "&serviceKey=" + serviceKey);
			// API URL을 연결하여 JSON data 준비 후 READ LINE
			BufferedReader bf;

			bf = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));

			result = bf.readLine();

			// JSON데이터 Read 후 items(실시간 측정정보) 파싱
			JSONParser jsonParser = new JSONParser();
			JSONObject jsonObject = (JSONObject) jsonParser.parse(result);
			JSONObject response = (JSONObject) jsonObject.get("response");
			JSONObject body = (JSONObject) response.get("body");
			JSONObject header = (JSONObject) response.get("header");
			items = (JSONArray) body.get("items");

//			
//			for (int i=0; i < items.size(); i++) {
//				System.out.println("=====items  :" + i + "========");
//				JSONObject itemsObject = (JSONObject) items.get(i);
//				
//				System.out.println(itemsObject.get("dataTime")); //측정일
//				System.out.println(itemsObject.get("mangName")); //측정망 정보
//				System.out.println(itemsObject.get("so2Value")); //아황산가스 농도
//				System.out.println(itemsObject.get("coValue")); //일산화탄소 농도
//				System.out.println(itemsObject.get("o3Value")); //오존 농도
//				System.out.println(itemsObject.get("no2Value")); //이산화질소 농도			
//				System.out.println(itemsObject.get("pm10Value")); //미세먼지(PM10)농도
//				System.out.println(itemsObject.get("pm10Value24")); //미세먼지(PM10)24시간 예측이동농도
//				System.out.println(itemsObject.get("pm25Value")); //미세먼지(PM25)농도				
//				System.out.println(itemsObject.get("pm25Value24")); //미세먼지(PM25)24시간 예측이동농도
//				System.out.println(itemsObject.get("mangName")); //측정망 정보
//				System.out.println(itemsObject.get("khaiValue")); //통합대기환경수치		
//				System.out.println(itemsObject.get("khaiGrade")); //통합대기환경지수
//				System.out.println(itemsObject.get("mangName")); //측정망 정보
//				System.out.println(itemsObject.get("so2Grade")); //아황산가스 지수				
//				System.out.println(itemsObject.get("coGrade")); //일산화탄소 지수
//				System.out.println(itemsObject.get("o3Grade")); //오존 지수
//				System.out.println(itemsObject.get("no2Grade")); //이산화질소 지수			
//				System.out.println(itemsObject.get("pm10Grade")); //미세먼지(PM10)24시간 등급
//				System.out.println(itemsObject.get("pm25Grade")); //미세먼지(PM25)24시간 등급	
//				System.out.println(itemsObject.get("pm10Grade1h")); //미세먼지(PM10)1시간 등급
//				System.out.println(itemsObject.get("pm25Grade1h")); //미세먼지(PM25)1시간 등급
//				System.out.println(itemsObject.get("so2Flag")); //아황산가스 플래그
//				System.out.println(itemsObject.get("coFlag")); //일산화탄소 플래그				
//				System.out.println(itemsObject.get("o3Flag")); //오존 플래그
//				System.out.println(itemsObject.get("no2Flag")); //이산화질소 플래그
//				System.out.println(itemsObject.get("pm10Flag")); //미세먼지(PM10)플래그
//				System.out.println(itemsObject.get("pm25Flag")); //미세먼지(PM25)플래그				
// 				
//			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return items;
	}

	/** 동적 답변 파라미터 얻어 오기 */
	public String getParamsForDynamicAnswer(String dynamicAnswer, String type) {
		int startFilterParamIndex = 0;
		int endFilterParamIndex = 0;
		int index = 0;
		String getParam = "";
		String answer = dynamicAnswer.toUpperCase();
		// dynamicAnswer = dynamicAnswer.toUpperCase();

		// params..
		// if(dynamicAnswer.contains("<LBTN>")) {
		startFilterParamIndex = answer.indexOf("<" + type + ">");
		endFilterParamIndex = answer.lastIndexOf("</" + type + ">");

		if (type == "BTN") {
			index = 5;
		} else if (type == "A") {
			index = 3;
		} else if (type == "TABLE") {
			index = 7;
		} else {
			index = 6;
		}

		getParam = dynamicAnswer.substring(startFilterParamIndex + index, endFilterParamIndex); // 숫자 2를 체크하세요 -> ♠
																								// 중괄호2개
		log.debug("getParam: " + getParam);
		// }

		return getParam;
	}

	// 인텐트 조회
	public IntentType getIntentType(String intent) {
		IntentType intentType = null;
		for (IntentType type : IntentType.values()) {
			if (type.getName().contains(intent)) {
				intentType = type;
				return intentType;

			}
		}
		return intentType;
	}

	// 일상Talk result ♣
	public void makeChatResult(IChatResp response, OutData output) {
		ChatResult result = new ChatResult();
		String answer = response.getAnswer();
		if (answer.contains("♣")) {
			int replaceIdx = answer.indexOf("♣");
			result.setMessage(replaceIdx > 0 ? answer.substring(0, replaceIdx) : answer);
		} else {
			result.setMessage(response.getAnswer());
		}

		String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA).format(new Date());
		result.setNodeType(IntentType.CommonTalk);
		result.setTimeStamp(timeStamp);
		output.setResult(result);
	}

	// nodeType Table
	public void makeTableResult(IChatResp response, OutData output) {
		ChatResult result = new ChatResult();
		String answer = response.getAnswer();
//			if(answer.contains("♣")){
//				int replaceIdx = answer.indexOf("♣");
//				result.setMessage(replaceIdx>0? answer.substring(0, replaceIdx): answer );
//			} else {
//				result.setMessage(response.getAnswer());		
//			}

//			String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss",Locale.KOREA).format(new Date());
		result.setNodeType(IntentType.TABLE);
//			result.setTimeStamp(timeStamp);
		output.setResult(result);
	}

	/*
	 * 링크도 버튼으로 통합 됨 "^"값으로 구분이 되며 IN 인경우 챗봇으로 값을 보냄 OUT 인경우 새창을 띄워서 해당 URL을 오픈함
	 * ♠버튼: 버튼명1^값1^IN | 버튼명2^값2^IN | ... ♠ ♠버튼: 버튼명1^값1^IN | 링크명1^URL1^OUt | ... ♠
	 */
	public void makeButtonResult(IChatResp response, OutData output, String params, String type) throws Exception {
		JSONParser parser = new JSONParser();
		ChatResult result = new ChatResult();

		String[] btnList = params.split("\\|");
		Option[] optionList = new Option[btnList.length];
		for (int i = 0; i < btnList.length; i++) {

			Option option = new Option();
			option.setId(Integer.toString(i));
			String[] btnOption = btnList[i].trim().split("\\^");
			option.setLabel(btnOption[0].trim());
			option.setValue(btnOption[1].trim());
			// option.setType (btnOption[2].trim().toUpperCase()); // IN:챗봇으로 , OUT: 새창으로
			option.setType(type);
			option.setOrder(Integer.toString(i));
			optionList[i] = option;

		}
		result.setOptionList(optionList);

		String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA).format(new Date());
		result.setNodeType(IntentType.BUTTON);
		result.setTimeStamp(timeStamp);
		String answer = response.getAnswer();
		int replaceIdx = answer.toUpperCase().indexOf("<" + type + ">");

		result.setMessage(replaceIdx > 0 ? answer.substring(0, replaceIdx) : answer);

		output.setResult(result);

	}

}
