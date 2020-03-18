package com.ideathon.search.models;

import java.util.List;

public class PredictionResponseModel {
	private List<Prediction> predictions;
	
	public PredictionResponseModel() {}

	public PredictionResponseModel(List<Prediction> predictions) {
		this.predictions = predictions;
	}

	public List<Prediction> getPredictions() {
		return predictions;
	}

	public void setPredictions(List<Prediction> predictions) {
		this.predictions = predictions;
	}
}
