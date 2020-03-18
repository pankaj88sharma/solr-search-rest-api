package com.ideathon.search.models;

public class MessageModel {

	private String sessionId;
	private String keyword;
	private String productId;
	private String brand;
	private String transactionType;

	public MessageModel() {}

	public MessageModel(String sessionId, String keyword, String productId, String brand, String transactionType) {
		this.sessionId = sessionId;
		this.keyword = keyword;
		this.productId = productId;
		this.setBrand(brand);
		this.transactionType = transactionType;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public String getBrand() {
		return brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}

	public String getTransactionType() {
		return transactionType;
	}

	public void setTransactionType(String transactionType) {
		this.transactionType = transactionType;
	}
}