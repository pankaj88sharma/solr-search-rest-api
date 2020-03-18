package com.ideathon.search.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ideathon.search.models.Prediction;
import com.ideathon.search.models.PredictionResponseModel;

@Service
public class ReRankService {

	private static final Logger logger = LoggerFactory.getLogger(ReRankService.class);

	@Autowired
	private RestTemplate restTemplate;

	public List<Prediction> getApiResponse(String query, String endPoint, String threshold, boolean reload) {
		List<Prediction> result = new ArrayList<>();
		try {
			Map<String, Object> uriVariables = new HashMap<>();
			uriVariables.put("query", query);
			uriVariables.put("threshold", threshold);
			uriVariables.put("reload", reload);
			ResponseEntity<PredictionResponseModel> response = restTemplate.exchange(endPoint, HttpMethod.GET, null,
					PredictionResponseModel.class, uriVariables);
			if (response.getStatusCode() == HttpStatus.OK) {
				result = response.getBody().getPredictions();
			}
		} catch (Exception e) {
			logger.error("Exception while calling brand prediction api - {}", e);
		}
		return result;
	}

	public void callTrainApi(String endPoint) {
		Map<String, Object> uriVariables = new HashMap<>();
		restTemplate.exchange(endPoint, HttpMethod.GET, null, String.class, uriVariables);
	}
}