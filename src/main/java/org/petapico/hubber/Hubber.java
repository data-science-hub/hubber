package org.petapico.hubber;

import static spark.Spark.*;
import spark.ModelAndView;
import spark.template.thymeleaf.ThymeleafTemplateEngine;

import java.util.HashMap;
import java.util.Map;

import org.thymeleaf.resourceresolver.ClassLoaderResourceResolver;
import org.thymeleaf.templateresolver.TemplateResolver;

public class Hubber {
	public static void main(String[] args) {
		Map<String,String> map = new HashMap<>();
		map.put("name", "Sam");
		TemplateResolver tr = new TemplateResolver();
		tr.setTemplateMode("HTML5");
		tr.setPrefix("templates/");
		tr.setSuffix(".html");
		tr.setCacheTTLMs(3600000L);
		tr.setResourceResolver(new ClassLoaderResourceResolver());
		get("/hello", (rq, rs) -> new ModelAndView(map, "hello"), new ThymeleafTemplateEngine(tr));
	}
}