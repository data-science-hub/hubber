package net.datasciencehub.hubber;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.openrdf.query.BindingSet;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sparql.SPARQLRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TripleStoreAccess {

	private static String endpointURL = HubberConf.get().property("sparql.endpoint.url");
	private static SPARQLRepository repo;
	private static QueryLanguage lang = QueryLanguage.SPARQL;
	private static String sparqlPrefixes = 
			"prefix xsd: <http://www.w3.org/2001/XMLSchema#> " +
			"prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
			"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
			"prefix rdfg: <http://www.w3.org/2004/03/trix/rdfg-1/> " +
			"prefix owl: <http://www.w3.org/2002/07/owl#> " +
			"prefix dct: <http://purl.org/dc/terms/> " +
			"prefix dce: <http://purl.org/dc/elements/1.1/> " +
			"prefix pav: <http://purl.org/pav/> " +
			"prefix prov: <http://www.w3.org/ns/prov#> " +
			"prefix foaf: <http://xmlns.com/foaf/0.1/> ";

	private static Logger logger = LoggerFactory.getLogger(TripleStoreAccess.class);

	static {
		repo = new SPARQLRepository(endpointURL);
		try {
			repo.initialize();
		} catch (Exception ex) {
			logger.error("Failed to connect to SPARQL endpoint", ex);
			System.exit(1);
		}
	}

	public static boolean isTrue(String query) {
		boolean isTrue = false;
		RepositoryConnection connection = repo.getConnection();
		try {
			BooleanQuery booleanQuery = connection.prepareBooleanQuery(lang, sparqlPrefixes + query);
			isTrue = booleanQuery.evaluate();
		} finally {
			connection.close();
		}
		return isTrue;
	}

	public static List<BindingSet> getTuples(String query) {
		List<BindingSet> tuples = new ArrayList<BindingSet>();
		RepositoryConnection connection = repo.getConnection();
		try {
			TupleQuery tupleQuery = connection.prepareTupleQuery(lang, sparqlPrefixes + query);
			TupleQueryResult result = tupleQuery.evaluate();
			try {
				while (result.hasNext()) {
					tuples.add(result.next());
				}
			} finally {
				result.close();
			}
		} finally {
			connection.close();
		}
		return tuples;
	}
	
	public static void runUpdateQuery(String... queries) {
		for (String qu : queries) {
			// Update queries don't seem to work with Virtuoso via SPARQLRepository...
			try {
				URL url = new URL(endpointURL);
			    URLConnection connection = url.openConnection();
			    connection.setDoOutput(true);
			    OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
			    wr.write("query=" + URLEncoder.encode(sparqlPrefixes + qu, "UTF8"));
			    wr.flush();
			    connection.getInputStream().close();
			} catch (IOException ex) {
				logger.error("Failed to run update query", ex);
			}
		}
	}

	public static String get(String property) {
		String p = property;
		if (property.matches("https?://.*")) {
			p = "<" + property + ">";
		}
		List<BindingSet> results = getTuples(
			"SELECT ?o WHERE { " +
				"graph <" + HubberConf.get().property("graph.uri") + "> " +
				"{<" + HubberConf.get().property("website.url") + "> " + p + " ?o} " +
			"} LIMIT 1"
		);
		if (results.isEmpty()) return null;
		return results.get(0).getValue("o").stringValue();
	}

}
