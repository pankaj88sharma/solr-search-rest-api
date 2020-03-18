package com.ideathon.search.factories;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;

public class SolrClientBuilder {

	private static SolrClient client = null;

	private SolrClientBuilder() {
	}

	public static SolrClient getSolrClient(String solrIp) {
		if (client == null) {
			client = new HttpSolrClient.Builder(solrIp).withConnectionTimeout(1000).withSocketTimeout(6000).build();
		}
		return client;
	}
}