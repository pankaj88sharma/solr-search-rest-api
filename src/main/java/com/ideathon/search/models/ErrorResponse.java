package com.ideathon.search.models;

public class ErrorResponse {
	private String status;

	public ErrorResponse(String status, String description) {
		this.status = status;
		this.description = description;
	}

	private String description;

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
