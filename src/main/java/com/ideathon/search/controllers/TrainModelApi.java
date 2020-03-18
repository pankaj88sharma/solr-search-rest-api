package com.ideathon.search.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.ideathon.search.components.ReRankService;
import com.ideathon.search.models.ErrorResponse;
import com.ideathon.search.models.SearchParams;

@CrossOrigin
@RestController
@RequestMapping(value = "/train")
public class TrainModelApi {

	private static final Logger logger = LoggerFactory.getLogger(TrainModelApi.class);

	@Value("${brand.train.host}")
	private String brandTrainHost;

	@Value("${brand.train.api}")
	private String brandTrainEndpoint;

	@Autowired
	private ReRankService reRankService;

	@RequestMapping(value = "", method = RequestMethod.GET)
	public ResponseEntity<?> train(SearchParams params) {
		String searchResponse = null;
		try {
			String endPoint = "http://" + brandTrainHost + brandTrainEndpoint;
			reRankService.callTrainApi(endPoint);
			searchResponse = "Model training completed. Files dumped succesfully.";
		} catch (Exception e) {
			logger.error("Exception while calling train api - {}", e);
			ErrorResponse errorResponse = new ErrorResponse("exception", e.getMessage());
			return new ResponseEntity<ErrorResponse>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<String>(searchResponse, HttpStatus.OK);
	}
}
