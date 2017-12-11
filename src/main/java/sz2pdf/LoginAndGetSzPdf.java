package sz2pdf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import sz2pdf.beans.SwiperPageInfoListEntry;
import sz2pdf.beans.SzType;

public class LoginAndGetSzPdf {

	private static String username;

	private static String password;

	private static String PDF_DIR;

	private static int editionId;

	private static String editionString;

	private String sessionId;

	private String aspSessionId;

	private Long issueId;

	ArrayList<SwiperPageInfoListEntry> pageList = new ArrayList<SwiperPageInfoListEntry>();

	private String secureCookie;

	private String filePath;

	final BasicCookieStore cookieStore = new BasicCookieStore();
	final CloseableHttpClient httpclient = HttpClients.custom().setDefaultCookieStore(cookieStore).build();

	final static Logger LOGGER = Logger.getLogger(LoginAndGetSzPdf.class);

	public void getSecureSessionIdAndPages() {

		try {
			/**
			 * 1. get authenticated sessionId
			 */
			final HttpUriRequest login = RequestBuilder.post().setUri(new URI("https://epaper.azmedien.ch/omni/login/"))
					.addParameter("email", username).addParameter("password", password).build();

			loginAndGetSessionId(login);

			/**
			 * 2. get secure cookie and sidUrl
			 */
			getSecureCookie();

			/**
			 * 3. get current issue and pages
			 */
			String xml = "{\"strEditionId\":\"" + editionId + "\"}";
			HttpEntity entity = new ByteArrayEntity(xml.getBytes("UTF-8"));

			HttpUriRequest postReq = RequestBuilder.post()
					.setUri(new URI("https://epaper-service.azmedien.ch/EPaper/EPaper.aspx/GetIssues"))
					.setEntity(entity).build();
			postReq.addHeader("Cookie", "ASP.NET_SessionId=" + aspSessionId + "; AzmJson=" + secureCookie);
			postReq.addHeader("Content-Type", "application/json; charset=UTF-8");

			getCurrentIssueId(postReq);

			xml = "{kRequestedIssueId: -1, kSelectedIssueId: " + issueId + ", bSmallJpeg: false}";
			entity = new ByteArrayEntity(xml.getBytes("UTF-8"));

			postReq = RequestBuilder.post()
					.setUri(new URI("https://epaper-service.azmedien.ch/EPaper/EPaper.aspx/GetSwiperPages"))
					.setEntity(entity).build();
			postReq.addHeader("Cookie", "ASP.NET_SessionId=" + aspSessionId + "; AzmJson=" + secureCookie);
			postReq.addHeader("Content-Type", "application/json; charset=UTF-8");

			getCurrentPages(postReq);

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}

	}

	public String getPdf(SzType szType) throws ClientProtocolException, IOException {
		HttpGet httpget;

		if (szType == SzType.All) {
			httpget = new HttpGet("https://epaper-service.azmedien.ch/EPaper/CreatePDF.aspx?issue=" + issueId
					+ "&pages=" + getAllPages());
		} else {
			httpget = new HttpGet("https://epaper-service.azmedien.ch/EPaper/CreatePDF.aspx?issue=" + issueId
					+ "&pages=" + getMiniPages());
		}
		httpget.addHeader("Cookie", "ASP.NET_SessionId=" + aspSessionId + "; AzmJson=" + secureCookie);

		final CloseableHttpResponse response1 = httpclient.execute(httpget);

		LOGGER.info("Request (" + szType + "): " + httpget.getURI() + " - statusCode: " + response1.getStatusLine());
		LOGGER.debug(response1);
		HttpEntity entity = response1.getEntity();

		DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd");
		Date date = new Date();

		InputStream is = entity.getContent();
		filePath = PDF_DIR + dateFormat.format(date) + "_" + editionString + "_" + szType + ".pdf";
		FileOutputStream fos = new FileOutputStream(new File(filePath));
		int inByte;
		while ((inByte = is.read()) != -1)
			fos.write(inByte);
		is.close();
		fos.close();

		return filePath;

	}

	private String getAllPages() {

		StringBuilder sb = new StringBuilder();

		for (SwiperPageInfoListEntry swiperPageInfoListEntry : pageList) {
			sb.append(swiperPageInfoListEntry.getPageId());
			sb.append(",");
		}

		// delete last comma
		sb.setLength(sb.length() - 1);

		return sb.toString();
	}

	private String getMiniPages() {

		StringBuilder sb = new StringBuilder();

		for (SwiperPageInfoListEntry swiperPageInfoListEntry : pageList) {

			// page 1, leben&wissen, stadt solothurn, kanton solothurn,
			// todesanzeigen, a-z
			if (swiperPageInfoListEntry.getPageNo() == 1)
				sb.append(swiperPageInfoListEntry.getPageId() + ",");

			if (swiperPageInfoListEntry.getSectionName().toUpperCase().equals("REGION"))
				sb.append(swiperPageInfoListEntry.getPageId() + ",");

			if (swiperPageInfoListEntry.getSectionName().toUpperCase().equals("STADT SOLOTHURN"))
				sb.append(swiperPageInfoListEntry.getPageId() + ",");

			if (swiperPageInfoListEntry.getSectionName().toUpperCase().equals("KANTON SOLOTHURN"))
				sb.append(swiperPageInfoListEntry.getPageId() + ",");

			if (swiperPageInfoListEntry.getClassificationName().toUpperCase().equals("TODESANZEIGEN"))
				sb.append(swiperPageInfoListEntry.getPageId() + ",");

			if (swiperPageInfoListEntry.getSectionName().toUpperCase().equals("TODESANZEIGEN"))
				sb.append(swiperPageInfoListEntry.getPageId() + ",");

			if (swiperPageInfoListEntry.getSectionName().toUpperCase().equals("leben u0026 wissen".toUpperCase()))
				sb.append(swiperPageInfoListEntry.getPageId() + ",");

			if (swiperPageInfoListEntry.getSectionName().toUpperCase().equals("morgen".toUpperCase()))
				sb.append(swiperPageInfoListEntry.getPageId() + ",");

			if (swiperPageInfoListEntry.getSectionName().toUpperCase().equals("A bis Z".toUpperCase()))
				sb.append(swiperPageInfoListEntry.getPageId() + ",");

		}

		// delete last comma
		sb.setLength(sb.length() - 1);

		return sb.toString();
	}

	public void getCurrentPages(HttpUriRequest login) throws ClientProtocolException, IOException {
		final CloseableHttpResponse getCookieResponse = httpclient.execute(login);

		LOGGER.info("Request: " + login.getURI() + " - statusCode: " + getCookieResponse.getStatusLine());
		LOGGER.debug(getCookieResponse);

		HttpEntity entity = getCookieResponse.getEntity();
		String responseString = EntityUtils.toString(entity, "UTF-8");

		JSONObject obj = new JSONObject(responseString);
		JSONObject obj2 = new JSONObject(obj.get("d").toString());
		String niceArray = obj2.get("Data").toString().replace("\\\"", "\"").replace("\"[{", "[{").replace("}]\"", "}]")
				.replace("\\\\\"", "").replace("\\\\", "");

		LOGGER.debug(niceArray);

		JSONObject obj3 = new JSONObject(niceArray);
		JSONArray pageArray = obj3.getJSONArray("SwiperPageInfoList");

		for (Object object : pageArray) {

			JSONObject obj4 = new JSONObject(object.toString());
			pageList.add(new SwiperPageInfoListEntry(Long.parseLong(obj4.get("PageId").toString()),
					Long.parseLong(obj4.get("PageNo").toString()), obj4.get("ClassificationName").toString(),
					obj4.get("SectionName").toString()));
		}

		LOGGER.info(" Total pages: " + pageList.size());

	}

	public void getCurrentIssueId(final HttpUriRequest login) throws ClientProtocolException, IOException {
		final CloseableHttpResponse getCookieResponse = httpclient.execute(login);

		LOGGER.info("Request: " + login.getURI() + " - statusCode: " + getCookieResponse.getStatusLine());
		LOGGER.debug(getCookieResponse);

		HttpEntity entity = getCookieResponse.getEntity();
		String responseString = EntityUtils.toString(entity, "UTF-8");
		LOGGER.debug(responseString);

		issueId = (Long.parseLong(responseString.substring(80).split("\\|")[0].split(";")[1]));
		LOGGER.info(" Todays IssueId: " + issueId);
	}

	private void loginAndGetSessionId(final HttpUriRequest login) throws IOException, ClientProtocolException {
		final CloseableHttpResponse getCookieResponse = httpclient.execute(login);
		final HttpEntity entity = getCookieResponse.getEntity();

		LOGGER.info("Request: " + login.getURI() + " - statusCode: " + getCookieResponse.getStatusLine());
		LOGGER.debug(getCookieResponse);

		EntityUtils.consume(entity);

		LOGGER.debug("Cookies:");
		final List<Cookie> cookies = cookieStore.getCookies();
		if (cookies.isEmpty()) {
			LOGGER.debug("None");
		} else {
			for (int i = 0; i < cookies.size(); i++) {
				LOGGER.debug("- " + cookies.get(i).toString());
			}
		}

		Cookie _cookie = cookieStore.getCookies().get(0);
		sessionId = "JSESSIONID=" + _cookie.getValue();
		LOGGER.info(" " + sessionId);
	}

	private void getSecureCookie() throws IOException {
		final HttpGet httpget = new HttpGet("https://epaper.azmedien.ch/omni/epaper/index");
		// httpget.addHeader("Cookie", sessionId);

		final CloseableHttpResponse response1 = httpclient.execute(httpget);

		Cookie _cookie;
		try {
			final HttpEntity entity1 = response1.getEntity();

			LOGGER.info("Request: " + httpget.getURI() + " - statusCode: " + response1.getStatusLine());
			LOGGER.debug(response1);
			HttpEntity entity = response1.getEntity();
			String responseString = EntityUtils.toString(entity, "UTF-8");
			Document doc = Jsoup.parse(responseString);
			Elements metaTags = doc.getElementsByTag("meta");

			for (Element metaTag : metaTags) {
				String property = metaTag.attr("property");
				String content = metaTag.attr("content");

				if ("og:url".equals(property)) {
					LOGGER.debug("sidUrl: " + content);
				}

			}

			EntityUtils.consume(entity1);

			LOGGER.debug("Cookies:");
			final List<Cookie> _cookies = cookieStore.getCookies();
			if (_cookies.isEmpty()) {
				LOGGER.debug("None");
			} else {
				for (int i = 0; i < _cookies.size(); i++) {
					LOGGER.debug("- " + _cookies.get(i).toString());
				}
			}
		} finally {
			response1.close();
		}

		_cookie = cookieStore.getCookies().get(1);
		secureCookie = _cookie.getValue().replace("M-AZAAR", editionString); // !

		_cookie = cookieStore.getCookies().get(0);
		aspSessionId = _cookie.getValue();

		LOGGER.info(" SecureCookie: " + secureCookie);
		LOGGER.info(" AspSessionId: " + aspSessionId);

	}

	public void setUsername(final String username) {
		LoginAndGetSzPdf.username = username;
	}

	public void setPassword(final String password) {
		LoginAndGetSzPdf.password = password;
	}

	public void setEditionId(int editionId) {
		LoginAndGetSzPdf.editionId = editionId;
	}

	public void setPath(String path) {
		LoginAndGetSzPdf.PDF_DIR = path;
	}

	public void setEditionString(String editionString) {
		LoginAndGetSzPdf.editionString = editionString;
	}

}