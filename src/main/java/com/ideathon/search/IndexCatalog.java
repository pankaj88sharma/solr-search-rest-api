package com.ideathon.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONException;
import org.json.JSONObject;

public class IndexCatalog {
	public static void main(String[] args) throws SolrServerException, IOException, JSONException {
		String filePath = "/Users/admin/Documents/ideathon/solrDocs.txt";
		String urlString = "http://localhost:8983/solr/catalog";
		HttpSolrClient client = getClient(urlString);

		client.deleteByQuery("*:*");
		client.commit();

		File file = new File(filePath);
		FileReader fileReader = new FileReader(file);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		String line;
		while ((line = bufferedReader.readLine()) != null) {

			JSONObject json = new JSONObject(line);
			Iterator<String> keys = json.keys();
			SolrInputDocument doc = new SolrInputDocument();
			while (keys.hasNext()) {
				String key = keys.next();
				doc.addField(key, json.get(key));
			}
			client.add(doc);
		}
		bufferedReader.close();

		client.commit();
		client.close();

	}

	private static HttpSolrClient getClient(String url) {
		return new HttpSolrClient.Builder(url).build();
	}
}
