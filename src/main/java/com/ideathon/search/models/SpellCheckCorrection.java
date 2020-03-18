package com.ideathon.search.models;

public class SpellCheckCorrection {

	private Boolean correctionAvailable = false;
	private String correctedTerm;

	public SpellCheckCorrection() {
	}

	public SpellCheckCorrection(Boolean correctionAvailable, String correctedTerm) {
		this.correctionAvailable = correctionAvailable;
		this.correctedTerm = correctedTerm;
	}

	public Boolean getCorrectionAvailable() {
		return correctionAvailable;
	}

	public void setCorrectionAvailable(Boolean correctionAvailable) {
		this.correctionAvailable = correctionAvailable;
	}

	public String getCorrectedTerm() {
		return correctedTerm;
	}

	public void setCorrectedTerm(String correctedTerm) {
		this.correctedTerm = correctedTerm;
	}
}