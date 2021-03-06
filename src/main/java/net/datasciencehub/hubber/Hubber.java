package net.datasciencehub.hubber;

import static spark.Spark.get;
import static spark.Spark.staticFileLocation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
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
		String orcidRedirectUrl = conf.property("website.url") + "/login";
		get("/", (rq, rs) -> {
			OrcidLoginResponse u = getUser(rq, rs);
			Map<String,Object> map = new HashMap<>();
			map.put("title", conf.property("website.name"));
			map.put("message", conf.property("website.message"));

			// Testing triple store update:
			TripleStoreAccess.set("dce:description", "Test: " + SimpleDateFormat.getInstance().format(new Date()));

			// Testing triple store access:
			map.put("message", TripleStoreAccess.get("dce:description") + "");

			boolean loggedin = (u != null);
			map.put("loggedin", loggedin);
			if (loggedin) {
				map.put("username", u.getName() + " (" + u.getOrcid() + ")");
				map.put("orcidlink", "https://orcid.org/" + u.getOrcid());
			} else {
				map.put("loginlink", "https://orcid.org/oauth/authorize?" +
						"client_id=" + conf.property("orcid.client.id") + "&" +
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
			List<NameValuePair> urlParams = new ArrayList<NameValuePair>();
			urlParams.add(new BasicNameValuePair("client_id", conf.property("orcid.client.id")));
			urlParams.add(new BasicNameValuePair("client_secret", conf.property("orcid.client.secret")));
			urlParams.add(new BasicNameValuePair("grant_type", "authorization_code"));
			urlParams.add(new BasicNameValuePair("redirect_uri", orcidRedirectUrl));
			urlParams.add(new BasicNameValuePair("code", authCode));
			post.setEntity(new UrlEncodedFormEntity(urlParams));
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