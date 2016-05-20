package net.datasciencehub.hubber;

import static spark.Spark.*;

import java.util.HashMap;
import java.util.Map;

import org.thymeleaf.resourceresolver.ClassLoaderResourceResolver;
import org.thymeleaf.templateresolver.TemplateResolver;

import spark.ModelAndView;
import spark.template.thymeleaf.ThymeleafTemplateEngine;

public class Hubber {

	private static ThymeleafTemplateEngine tempEngine;

	private static String orcidClientId = "APP-W02BIN0XPD5T5PFL";
	private static String orcidScope = "/authenticate";
	private static String orcidRedirectUrl = "http://hubber.tkuhn.eculture.labs.vu.nl/#login";
	//private static String orcidRedirectUrl = "https://developers.google.com/oauthplayground";

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
		get("/#login", (rq, rs) -> {
			System.err.println("---");
			System.err.println(rq.body());
			System.err.println("---");
			return "";
		});
	}

}