package org.petapico.hubber;

import static spark.Spark.*;
import spark.ModelAndView;
import spark.template.thymeleaf.ThymeleafTemplateEngine;

import java.util.HashMap;
import java.util.Map;

public class Hubber {
	public static void main(String[] args) {
		Map<String,String> map = new HashMap<>();
		map.put("name", "Sam");
		get("/hello", (rq, rs) -> new ModelAndView(map, "hello"), new ThymeleafTemplateEngine());
	}
}