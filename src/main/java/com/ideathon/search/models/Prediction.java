package com.ideathon.search.models;

public class Prediction {
	private String value;
	private float score;
	
	public Prediction() {}

	public Prediction(String value, float score) {
		this.value = value;
		this.setScore(score);
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public float getScore() {
		return score;
	}

	public void setScore(float score) {
		this.score = score;
	}
}