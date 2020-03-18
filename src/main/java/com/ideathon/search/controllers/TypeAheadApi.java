package com.ideathon.search.controllers;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ideathon.search.components.ResponseGenerator;
import com.ideathon.search.components.SolrSearcher;
import com.ideathon.search.models.ErrorResponse;

@CrossOrigin
@RestController
@RequestMapping(value = "/typeahead")
public class TypeAheadApi {

	@Autowired
	private SolrSearcher solrSearcher;

	@Autowired
	private ResponseGenerator responseGenerator;

	private static final Logger logger = LoggerFactory.getLogger(TypeAheadApi.class);

	@RequestMapping(value = "", method = RequestMethod.GET)
	public ResponseEntity<?> search(@RequestParam(required = true) String query) {
		TypeAheadResponse taResponse = null;
		try {
			QueryResponse queryResponse = solrSearcher.executeSuggest(query);
			taResponse = responseGenerator.generateSuggestResponse(queryResponse);
		} catch (Exception e) {
			logger.error("Exception while searching solr - {}", e);
			ErrorResponse errorResponse = new ErrorResponse("exception", e.getMessage());
			return new ResponseEntity<ErrorResponse>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<TypeAheadResponse>(taResponse, HttpStatus.OK);
	}
}
