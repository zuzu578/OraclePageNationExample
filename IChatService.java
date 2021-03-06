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

	// usersesssion key Map ??????
	private final ConcurrentHashMap<String, String> userSessionKeyMap = new ConcurrentHashMap<>();
	private static final Logger LOGGER = LoggerFactory.getLogger(GnBotController.class);

	public JSONObject setObject(JSONObject obj) {
		return this.obj = (JSONObject) obj;
	}

	// ?????? Stream ?????? ????????? ??????
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

	// ?????? ??????(??????)
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

	// ?????? Stream ??????
	public void closeStream(Closeable stream) {
		if (stream != null) {
			try {
				stream.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// ??? ????????? ????????????
	public void getChatSetIncoding(String originalStr) {

//		String originalStr = "????????????"; // ????????? 
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
	 * ???????????? ????????????
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
	 * ????????? ??????
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
	 * ????????? Validation
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
	 * ????????? ???????????? ?????? ??????
	 * http://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getMinuDustFrcstDspth?serviceKey=pIjdyg6yRnPqmwfTfG4m3TIDh518lq4lqoOgjavC5e1QPr3Vut5Dri2mQXpGfX5CbeusLqm9VNvju4fmvIkv0g%3D%3D&returnType=json&numOfRows=100&pageNo=1&searchDate=2021-11-16&InformCode=PM10	
	 * @param formatedNow
	 * @param area
	 * @return
	 * @throws Exception
	 */
	public HashMap<String, Object> getAirQualityForeCast(String formatedNow, String area) throws Exception {
		// <EVAPI>???????????? ?????? ?????????</EVAPI>
		String API_TRAN = "http://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getMinuDustFrcstDspth";
		
		String result = "";
		// JSON???????????????
		JSONObject foreCastObj = null;
		
		String InformCode = "PM10"; // InformCode ??? PM10 ????????? ?????????.
		List<HashMap<String , String>> tempInformGrade = new ArrayList<HashMap<String , String>>();
				
		// ?????????????????? ?????????????????? ?????? ???????????? ?????? (+1) ?????? ????????? ??????
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
				
				// ???????????? ?????? + inform ???????????? PM10 ??? ???????????? ?????????????????? map ??? ????????????. 
				// ?????? ?????? : mMap => overall / cause / ex:{?????? : ??????} 
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
					// ex) ?????? ??? param??????????????? ????????? ???????????? ???????????? ????????? ???????????????.
					 List<HashMap<String, String>> mapList = tempInformGrade.stream()
					            .filter(map -> map.get("city").equals(area))
					            .collect(Collectors.toList());
					 mMap.put("informGrade",mapList);
					 // ??? ??????, ??????????????? ???????????? ?????? 
					 break;
					
				}
			}
			// ?????? ?????? ?????? => {rowData=[??? [????????????] ??? ????????? '??????'???'??????'?????? ???????????????., ??? [????????????] ????????? ?????? ????????? ????????? ???????????? ?????? ????????? ????????? '??????' ????????? ????????? ???????????????.], informGrade=[{city=??????, status=??????}]}
			System.out.println("mMap =>" + mMap);
			//return mMap;

		} catch (Exception e) {
			System.out.println("e ==>" + e);
		}
		//return mMap;
		return mMap;

	}

	

	/*
	 * ?????????????????? ?????? ?????? ??????
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

		// ????????????(convQuery)??? null--> userQuery??? ??????, null??? ????????? convQuery??? ?????? ??????
		if ("".equals(convQuery)) {
			obj.put("query", userQuery);
		} else {
			obj.put("query", convQuery);
		}

		log.info("[input] ==>" + obj.toJSONString());

		Boolean stationNameChk = false;
		Boolean tableChk = false;

		// ?????? ?????? ?????? iChatResp
		IChatResp iChatResp = new IChatResp();
		// Intent intentResp = new Intent();

		String posX = "";
		String posY = "";
		String stationName = "";
		JSONArray items = null;
//		Map<String, List<String>> tableAnswer = null;
		String tableAnswer = "";

		ArrayList<HashMap<String, Object>> tablelist = null;
		
		// API ??????
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);

			// http header??? http????????? ?????? ??????
			HttpEntity<String> param = new HttpEntity<String>(obj.toString(), headers);

			// RestAPI ?????????
			RestTemplate restTemplate = new RestTemplate(
					RestTemplateConnectionPooling.getInstance().getRequestFactory());

			// UTF-8??? Charset
			restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));

			// ?????? ?????? API??????(parameter??????) ??????
			
			iChatResp = restTemplate.postForObject(URI.create(url), param, IChatResp.class);
			// test ??? setAnswer
			iChatResp.setAnswer("<EVAPI>???????????? ?????? ?????????</EVAPI>");
			// ?????? ??????(?????? API ??????): ????????? ???????????? API
			if (iChatResp.getAnswer() != null && !iChatResp.getAnswer().equals("")
					&& iChatResp.getAnswer().contains("???????????? ???????????? ?????????")) {

				posX = commandMap.get("startLon1").toString();
				posY = commandMap.get("startLat1").toString();
				Object accessToken = iChatService.getAccessToken(); // errMsg ???????????? (API?????????)

				JSONObject GPSjson = iChatService.getGps2TM(posX, posY, accessToken); // errMsg ????????????(???????????????)
				Map<String, Object> jsonObject = iChatService.getTM2NearStn(GPSjson); // ????????? ????????? 3??? ????????????

				commandMap.put("jsonObject", jsonObject); // jsonObject ?????? ?????? ?????????.
				// commandMap.put("stsKey", "stsKey");
				output.setStationKey("station");

			}else if (iChatResp.getAnswer().contains("???") && iChatResp.getAnswer().contains("?????? ??????")) {
				String tempArea = iChatResp.getAnswer().toString();
				String area = tempArea.substring(8, 10);
				// ?????? ?????? ???????????? api ??? ???????????????.
				// ?????? ?????? ?????????
				LocalDate now = LocalDate.now();
				// ?????? ??????
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
				// ?????? ??????
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

			// ????????? ??????
			intentNm = iChatResp.getResponse().get("topIntentName").toString().trim();

			// ????????? ?????? <IMG>
			if (iChatResp.getAnswer().indexOf("<IMG>") != -1) {
				String imgAnswer = imgTagChange(iChatResp.getAnswer());
				iChatResp.setAnswer(imgAnswer);

			}

			/**
			 * ?????? ?????? ??? : ????????????????????? ====> DataType: TALK 1. RBTN ?????? ?????? ?????? 2. Response Type???
			 * 'REQUESTION'(?????????) ?????? IntentNm??? Type 'IN' =====> INTENT?????? 3. ???????????? : (1)
			 * \"linkType\":\"CL\" ??? ??????----> button 'N' (2) \"linkType\":\"IL\" ??? ??????---->
			 * button 'Y' (3) \"linkType\":\"RL\" ??? ??????----> button ?????????
			 * 
			 * ???{ ????????? ??? ???????????? =====> button 'Y'
			 */

			iChatResp.setAnswer(iChatResp.getAnswer().replaceAll("\r\n", "<br>").replaceAll("\n", "<br>")
					.replaceAll("<br><br>", "<br>").trim());
			if (iChatResp.getAnswer().contains("???")) {
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
			// ??????
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

		// ?????????
		IchatVO iVO = null;
		// ???????????? VO
		IchatVO pVO = new IchatVO();
		IchatVO tempVo = new IchatVO();
		String subAnswer = "";

		if (!iChatResp.getResponse().get("responseType").toString().trim().equals("REQUESTION")) {
			// System.out.println("##################################################");
			// System.out.println(intentNm);
			// ???????????? ?????? ?????? ??????
			pVO.setIntentNm(intentNm);
			pVO.setType("IN");
			pVO.setProjectId(projectId);
			iVO = convertService.selectResultMapping(pVO);
		}

		// ??? ??????
		if (iVO != null) {
			subAnswer = iVO.getContent();

			// ?????? ?????? ??????
			// ?????? ?????? N
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
				// ?????? ?????? Y
			} else if (subAnswer.indexOf("\"linkType\":\"IL\"") != -1) {

				output.setBtnYn("Y");

				// ?????? ?????? ?????????
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
		if (iChatResp.getAnswer().indexOf("???{") != -1) {
			log.info("Answer========================================" + iChatResp.getAnswer());
			output.setSubAnswer(iChatResp.getAnswer());
			output.setBtnYn("Y");

		}

		// ?????? ??????
		logMap.put("logQuery", userQuery);
		logMap.put("logAnswer", output.getResult().getMessage());
		logMap.put("logIntentName", intentNm);
		logMap.put("logCategoryName", iChatResp.getResponse().get("categoryName"));
		// logMap.put("logCategoryName", intentResp.getIntent().get("categoryName"));
		logMap.put("projectId", projectId);

		chatLogService.insertDetailLog(logMap);

		return output;
	}

	// ?????? ?????? ?????? <LBTN>, <SBTN>, <A>
	public boolean isButtonAnswer(String answer) {
		boolean result = false;
		if (answer.contains("<LBTN>") || answer.contains("<SBTN>") || answer.contains("<A>")) {
			result = true;
		}

		return result;
	}

	// ?????? ?????? ?????? <RBTN>
	public boolean isRButtonAnswer(String answer) {
		boolean result = false;
		if (answer.contains("<BTN>")) {
			result = true;
		}

		return result;
	}

	// ????????? ?????? ?????? <TABLE>
	public boolean isTableAnswer(String answer) {
		boolean result = false;
		if (answer.contains("<TABLE>")) {
			result = true;
		}

		return result;
	}

	// ??? ?????? ??????
	public boolean isPhoneAnswer(String answer) {
		boolean result = false;
		if (answer.contains("<PHONE>")) {
			result = true;
		}

		return result;
	}

	/* ???????????? ?????? ?????? ???(??????/ ???) ?????? */
	public boolean isDynamicAnswer(String answer) {
		boolean result = false;
		if (answer.contains("???") && answer.contains("???")) {
			result = true;
		}
		return result;
	}

	// ???????????? ???????????? ???{ (??????) }???(???) /
	public boolean hasReplaceWord(String answer) {
		boolean result = false;
		if (answer.contains("???{") && answer.contains("}???")) {
			result = true;
		}
		return result;
	}

	// ???????????? convert ???????????? ???(??????/ ???)
	public boolean hasReplaceResult(String answer) {
		boolean result = false;
		if (answer.contains("???") && answer.contains("???")) {
			result = true;
		}
		return result;
	}

	/* ???????????? replace */
	public void replaceWord(IChatResp response) {
		Map<String, String> result = new HashMap<>();
		int startFilterContentIndex = 0;
		String content = "";
		String answer = response.getAnswer();

		// result-content.. ??? ???

		if (answer.contains("???{") && answer.contains("}???")) {
			String[] answerArr = answer.split("}???");
			for (String answer2 : answerArr) {
				if (answer2.contains("???{")) {
					startFilterContentIndex = answer2.indexOf("???{");
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
					answer = answer.replace("???{" + content + "}???",
							builderArr.toString().replace("[", "").replace("]", ""));
				}
			}

			log.info("relpaceStr :" + answer);
			response.setAnswer(answer);

		}
	}

	// img tag ????????? ??????
	public String imgTagChange(String answer) {
		Pattern p = Pattern.compile("<IMG>.*</IMG>");
		Matcher m = p.matcher(answer);
		String imgval = "";
		// String strResult = new String("");
		UploadImageVO pVO = new UploadImageVO();
		UploadImageVO rVO = new UploadImageVO();

		// ????????? ??? ?????? ??? ??????
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

	// link tag ????????? ??????
	public String linkTagChange(String answer) {
		Pattern p = Pattern.compile("<A>.*</A>");
		Matcher m = p.matcher(answer);
		String linkval = "";
		// String strResult = new String("");
		UploadImageVO pVO = new UploadImageVO();
		UploadImageVO rVO = new UploadImageVO();

		// ????????? ??? ?????? ??? ??????
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
	 * //params.. if(dynamicAnswer.contains("???")) { //&&
	 * dynamicAnswer.contains("???")) { startFilterParamIndex =
	 * dynamicAnswer.indexOf("???"); endFilterParamIndex =
	 * dynamicAnswer.lastIndexOf("???"); getParam =
	 * dynamicAnswer.substring(startFilterParamIndex + 1, endFilterParamIndex); //
	 * ?????? 2??? ??????????????? -> ??? ?????????2??? int idx = getParam.indexOf(","); if(idx<1){
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

		getParam = Answer.substring(startFilterParamIndex + index, endFilterParamIndex); // ?????? 2??? ??????????????? -> ??? ?????????2???
		log.debug("getParam: " + getParam);
		// }

		return getParam;
	}

	//

	/**
	 * OAuth ????????? API ?????? ??? ???????????? accessToken ????????????
	 * 
	 * @return
	 * @throws Exception errMsg ??? ???????????? ????????? ???
	 */
	public Object getAccessToken() throws Exception {

		String API_TOKEN = "https://sgisapi.kostat.go.kr/OpenAPI3/auth/authentication.json";
		String consumer_key = "c659978309e44c4a9a4f"; // ????????? ID
		String consumer_secret = "ff2ff37ca278484d825b"; // ????????? Secret
		String res = ""; // JSON ?????? ?????????
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

			Object errMsg = jsonObject.get("errMsg"); // ??????????????? ?????? ??????????????? ????????????
			accessToken = result.get("accessToken");

		} catch (Exception e) {
			e.printStackTrace();
		}

		return accessToken;
	}

	/**
	 * ????????? ??????(?????????) ????????? TM????????? ????????????
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
		String src = "4326"; // WGS84 ?????????
		String dst = "5181"; // ???????????? TM??????
//		String posX="127.392925"; //TM?????? X
//		String posY="36.343492"; //TM?????? Y
		String result = ""; // JSON???????????????
		// HttpURLConnection conn = null;
		JSONObject GPSjson = null;
		try {
			// API URL ??????
			URL url = new URL(API_TRAN + "?accessToken=" + accessToken + "&src=" + src + "&dst=" + dst + "&posX="
					+ startLon1 + "&posY=" + startLat1);

			// API URL??? ???????????? JSON data ?????? ??? READ LINE
			BufferedReader bf = null;
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestMethod("GET");

			bf = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));

			System.out.println("url ======> " + url);
			result = bf.readLine();

			// JSON????????? Read ??? posX, posY ??????
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
	/** TM????????? ?????? ????????????????????? ????????? */
	public JSONObject getTM2NearStn(JSONObject GPSjson) {
		String API_NearStn = "http://apis.data.go.kr/B552584/MsrstnInfoInqireSvc/getNearbyMsrstnList";
		String serviceKey = "kz55Vyjh95Lq9n6BSbWZ8V5d2t%2FgR8BR5j5vkXVh3%2Fej5S7DBxKoVT2OaVmPca5OXYzy5WsPyVfgxXDeTSMG8g%3D%3D"; // serviceKey
		String returnType = "json"; // ???????????? xml,json
		String tmX = GPSjson.get("posX").toString(); // ???????????? TM?????? X??????
		String tmY = GPSjson.get("posY").toString();
		; // ???????????? TM?????? Y??????
		String result = "";// JSON???????????????

//		HashMap<String, Object> map = new HashMap<String, Object>();
//		obj = new JSONObject(); // JSONArray items ?????? ?????? ??? ????????? ???

		JSONObject jsonObject = null;
		try {
			// API URL ??????
			URL url = new URL(API_NearStn + "?tmX=" + tmX + "&tmY=" + tmY + "&returnType=" + returnType + "&serviceKey="
					+ serviceKey);

			// API URL??? ???????????? JSON data ?????? ??? READ LINE
			BufferedReader bf = null;
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestMethod("GET");
			bf = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));

			result = bf.readLine();

			// JSON????????? Read ??? items(???????????????) ??????
			JSONParser jsonParser = new JSONParser();
			jsonObject = (JSONObject) jsonParser.parse(result);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return jsonObject;
	}

	/**
	 * ?????? ?????????????????????????????? ???????????? ?????????
	 * 
	 * @throws UnsupportedEncodingException
	 */
	public JSONArray getNearStn2RtmData(String stationName2) throws UnsupportedEncodingException {
		String stationName = stationName2;
		String API_RTMDATA = "http://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getMsrstnAcctoRltmMesureDnsty";
		String serviceKey = "kz55Vyjh95Lq9n6BSbWZ8V5d2t%2FgR8BR5j5vkXVh3%2Fej5S7DBxKoVT2OaVmPca5OXYzy5WsPyVfgxXDeTSMG8g%3D%3D"; // serviceKey
		String returnType = "json"; // ???????????? xml,json
		String dataTerm = "DAILY"; // ????????? ?????? (1??? : DAILY, 1??????: MONTH, 3??????: 3NONTH)
		String pageNo = "1";
		String numOfRows = "100";
		// String ver=""; (????????? ??????)


		String result = ""; // JSON???????????????
		JSONArray items = null;

		try {
			// API URL ??????
			URL url = new URL(API_RTMDATA + "?stationName=" + URLEncoder.encode(stationName, "UTF-8") + "&dataTerm="
					+ dataTerm + "&pageNo=" + pageNo + "&numOfRows=" + numOfRows + "&returnType=" + returnType
					+ "&serviceKey=" + serviceKey);
			// API URL??? ???????????? JSON data ?????? ??? READ LINE
			BufferedReader bf;

			bf = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));

			result = bf.readLine();

			// JSON????????? Read ??? items(????????? ????????????) ??????
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
//				System.out.println(itemsObject.get("dataTime")); //?????????
//				System.out.println(itemsObject.get("mangName")); //????????? ??????
//				System.out.println(itemsObject.get("so2Value")); //??????????????? ??????
//				System.out.println(itemsObject.get("coValue")); //??????????????? ??????
//				System.out.println(itemsObject.get("o3Value")); //?????? ??????
//				System.out.println(itemsObject.get("no2Value")); //??????????????? ??????			
//				System.out.println(itemsObject.get("pm10Value")); //????????????(PM10)??????
//				System.out.println(itemsObject.get("pm10Value24")); //????????????(PM10)24?????? ??????????????????
//				System.out.println(itemsObject.get("pm25Value")); //????????????(PM25)??????				
//				System.out.println(itemsObject.get("pm25Value24")); //????????????(PM25)24?????? ??????????????????
//				System.out.println(itemsObject.get("mangName")); //????????? ??????
//				System.out.println(itemsObject.get("khaiValue")); //????????????????????????		
//				System.out.println(itemsObject.get("khaiGrade")); //????????????????????????
//				System.out.println(itemsObject.get("mangName")); //????????? ??????
//				System.out.println(itemsObject.get("so2Grade")); //??????????????? ??????				
//				System.out.println(itemsObject.get("coGrade")); //??????????????? ??????
//				System.out.println(itemsObject.get("o3Grade")); //?????? ??????
//				System.out.println(itemsObject.get("no2Grade")); //??????????????? ??????			
//				System.out.println(itemsObject.get("pm10Grade")); //????????????(PM10)24?????? ??????
//				System.out.println(itemsObject.get("pm25Grade")); //????????????(PM25)24?????? ??????	
//				System.out.println(itemsObject.get("pm10Grade1h")); //????????????(PM10)1?????? ??????
//				System.out.println(itemsObject.get("pm25Grade1h")); //????????????(PM25)1?????? ??????
//				System.out.println(itemsObject.get("so2Flag")); //??????????????? ?????????
//				System.out.println(itemsObject.get("coFlag")); //??????????????? ?????????				
//				System.out.println(itemsObject.get("o3Flag")); //?????? ?????????
//				System.out.println(itemsObject.get("no2Flag")); //??????????????? ?????????
//				System.out.println(itemsObject.get("pm10Flag")); //????????????(PM10)?????????
//				System.out.println(itemsObject.get("pm25Flag")); //????????????(PM25)?????????				
// 				
//			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return items;
	}

	/** ?????? ?????? ???????????? ?????? ?????? */
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

		getParam = dynamicAnswer.substring(startFilterParamIndex + index, endFilterParamIndex); // ?????? 2??? ??????????????? -> ???
																								// ?????????2???
		log.debug("getParam: " + getParam);
		// }

		return getParam;
	}

	// ????????? ??????
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

	// ??????Talk result ???
	public void makeChatResult(IChatResp response, OutData output) {
		ChatResult result = new ChatResult();
		String answer = response.getAnswer();
		if (answer.contains("???")) {
			int replaceIdx = answer.indexOf("???");
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
//			if(answer.contains("???")){
//				int replaceIdx = answer.indexOf("???");
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
	 * ????????? ???????????? ?????? ??? "^"????????? ????????? ?????? IN ????????? ???????????? ?????? ?????? OUT ????????? ????????? ????????? ?????? URL??? ?????????
	 * ?????????: ?????????1^???1^IN | ?????????2^???2^IN | ... ??? ?????????: ?????????1^???1^IN | ?????????1^URL1^OUt | ... ???
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
			// option.setType (btnOption[2].trim().toUpperCase()); // IN:???????????? , OUT: ????????????
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
