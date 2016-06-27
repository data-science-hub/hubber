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
import spark.Request;
import spark.Response;
import spark.template.thymeleaf.ThymeleafTemplateEngine;
import spark.utils.IOUtils;

public class Hubber {

	private static ThymeleafTemplateEngine tempEngine;

	private static Map<String,OrcidLoginResponse> users = new HashMap<>();

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
		HubberConf conf = HubberConf.get();
		String orcidRedirectUrl = conf.getWebsiteUrl() + "/login";
		get("/", (rq, rs) -> {
			OrcidLoginResponse u = getUser(rq, rs);
			Map<String,Object> map = new HashMap<>();
			map.put("title", "Hubber");
			map.put("message", "This is Hubber.");
			boolean loggedin = (u != null);
			map.put("loggedin", loggedin);
			if (loggedin) {
				map.put("username", u.getName() + " (" + u.getOrcid() + ")");
				map.put("orcidlink", "https://orcid.org/" + u.getOrcid());
			} else {
				map.put("loginlink", "https://orcid.org/oauth/authorize?" +
						"client_id=" + conf.getOrcidClientId() + "&" +
						"response_type=code&" +
						"scope=/authenticate&" +
						"redirect_uri=" + orcidRedirectUrl);
			}
			return new ModelAndView(map, "index");
		}, tempEngine);
		get("/login", (rq, rs) -> {
			String authCode = rq.queryParams("code");
			HttpPost post = new HttpPost("https://orcid.org/oauth/token");
			post.setHeader("Accept", "application/json");
			List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
			urlParameters.add(new BasicNameValuePair("client_id", conf.getOrcidClientId()));
			urlParameters.add(new BasicNameValuePair("client_secret", conf.getOrcidClientSecret()));
			urlParameters.add(new BasicNameValuePair("grant_type", "authorization_code"));
			urlParameters.add(new BasicNameValuePair("redirect_uri", orcidRedirectUrl));
			urlParameters.add(new BasicNameValuePair("code", authCode));
			post.setEntity(new UrlEncodedFormEntity(urlParameters));
			HttpClient client = HttpClientBuilder.create().build();
			HttpResponse response = client.execute(post);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode >= 200 && statusCode < 300) {
				// Success
				String respString = IOUtils.toString(response.getEntity().getContent());
				OrcidLoginResponse r = OrcidLoginResponse.fromJson(respString);
				rs.cookie("orcid", r.getOrcid());
				rs.cookie("orcid-access-token", r.getAccessToken());
				users.put(r.getOrcid(), r);
				rs.redirect("/");
				return "";
			} else {
				// Something went wrong
				return statusCode + " " + response.getStatusLine().getReasonPhrase();
			}
		});
		get("/logout", (rq, rs) -> {
			OrcidLoginResponse u = getUser(rq, rs);
			if (u != null) {
				users.remove(u.getOrcid());
				rs.cookie("orcid", null);
				rs.cookie("orcid-access-token", null);
			}
			rs.redirect("/");
			return null;
		});
	}

	private static OrcidLoginResponse getUser(Request rq, Response rs) {
		String orcidC = rq.cookie("orcid");
		String orcidAccessTokenC = rq.cookie("orcid-access-token");
		if (orcidC != null && users.containsKey(orcidC) && users.get(orcidC).getAccessToken().equals(orcidAccessTokenC)) {
			return users.get(orcidC);
		} else {
			rs.cookie("orcid", null);
			rs.cookie("orcid-access-token", null);
			return null;
		}
		
	}

}