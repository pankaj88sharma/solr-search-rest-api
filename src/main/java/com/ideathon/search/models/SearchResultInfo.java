package com.ideathon.search.models;

import org.apache.solr.common.SolrDocumentList;

public class SearchResultInfo {
	private Long numFound;
	private SolrDocumentList resultPage;

	public SearchResultInfo(Long numFound, SolrDocumentList resultPage) {
		this.numFound = numFound;
		this.resultPage = resultPage;
	}

	public Long getNumFound() {
		return numFound;
	}

	public void setNumFound(Long numFound) {
		this.numFound = numFound;
	}

	public SolrDocumentList getResultPage() {
		return resultPage;
	}

	public void setResultPage(SolrDocumentList resultPage) {
		this.resultPage = resultPage;
	}
}
