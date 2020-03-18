package com.ideathon.search.controllers;

import java.util.List;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.ideathon.search.components.ResponseGenerator;
import com.ideathon.search.components.SolrSearcher;
import com.ideathon.search.models.ErrorResponse;
import com.ideathon.search.models.SearchParams;
import com.ideathon.search.models.SearchResponse;

@CrossOrigin
@RestController
@RequestMapping(value = "/search")
public class SearchApi {

	@Autowired
	private SolrSearcher solrSearcher;

	@Autowired
	private ResponseGenerator responseGenerator;

	private static final Logger logger = LoggerFactory.getLogger(SearchApi.class);

	@RequestMapping(value = "", method = RequestMethod.GET)
	public ResponseEntity<?> search(SearchParams params) {
		SearchResponse searchResponse = null;
		try {
			Multimap<String, String> filtersMap = populateFiltersMap(params.getRefine());
			QueryResponse queryResponse = solrSearcher.executeSearch(params, filtersMap);
			QueryResponse spellCheckResponse = solrSearcher.executeSpellCheck(params.getKeyword());
			searchResponse = responseGenerator.generateResponse(queryResponse, spellCheckResponse, filtersMap, params.getRefine());
		} catch (Exception e) {
			logger.error("Exception while searching solr - {}", e);
			ErrorResponse errorResponse = new ErrorResponse("exception", e.getMessage());
			return new ResponseEntity<ErrorResponse>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<SearchResponse>(searchResponse, HttpStatus.OK);
	}

	private Multimap<String, String> populateFiltersMap(List<String> refine) {
		Multimap<String, String> result = ArrayListMultimap.create();
		for (String ref : refine) {
			String[] split = ref.split(":", 2);
			if (split.length == 2) {
				result.put(split[0], split[1]);
			}
		}
		return result;
	}
}