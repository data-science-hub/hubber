package net.datasciencehub.hubber;

import static spark.Spark.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.thymeleaf.resourceresolver.ClassLoaderResourceResolver;
import org.thymeleaf.templateresolver.TemplateResolver;

import spark.ModelAndView;
import spark.template.thymeleaf.ThymeleafTemplateEngine;
import spark.utils.IOUtils;

public class Hubber {

	private static ThymeleafTemplateEngine tempEngine;

	private static String orcidClientId = "APP-W02BIN0XPD5T5PFL";
	private static String orcidScope = "/authenticate";
	private static String orcidRedirectUrl = "http://hubber.tkuhn.eculture.labs.vu.nl/login";

	static {
		TemplateResolver tr = new TemplateResolver();
		tr.setTemplateMode("HTML5");
		tr.setPrefix("templates/");
		tr.setSuffix(".html");
		tr.setCacheTTLMs(3600000L);
		tr.setResourceResolver(new ClassLoaderResourceResolver());
		tempEngine = new ThymeleafTemplateEngine(tr);
	}

	public static void main(String[] args) {
		staticFileLocation("/files");
		Map<String,String> map = new HashMap<>();
		map.put("title", "Hubber");
		map.put("message", "This is Hubber.");
		map.put("loginlink", "https://orcid.org/oauth/authorize?" +
				"client_id=" + orcidClientId + "&" +
				"response_type=code&" +
				"scope=" + orcidScope + "&" +
				"redirect_uri=" + orcidRedirectUrl);
		get("/", (rq, rs) -> new ModelAndView(map, "index"), tempEngine);
		get("/login", (rq, rs) -> {
			String authCode = rq.queryParams("code");
			System.err.println("Authentication code: " + authCode);
			HttpPost post = new HttpPost("https://orcid.org/oauth/token");
			post.setHeader("Accept", "application/json");
			List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
			urlParameters.add(new BasicNameValuePair("client_id", orcidClientId));
			urlParameters.add(new BasicNameValuePair("client_secret", ""));
			urlParameters.add(new BasicNameValuePair("grant_type", "authorization_code"));
			urlParameters.add(new BasicNameValuePair("redirect_uri", orcidRedirectUrl));
			urlParameters.add(new BasicNameValuePair("code", authCode));
			post.setEntity(new UrlEncodedFormEntity(urlParameters));
			HttpClient client = HttpClientBuilder.create().build();
			HttpResponse response = client.execute(post);
			System.err.println("Status Code: " + response.getStatusLine().getStatusCode());
			String respString = IOUtils.toString(response.getEntity().getContent());
			System.err.println("Response: " + respString);
			return "";
		});
	}

}