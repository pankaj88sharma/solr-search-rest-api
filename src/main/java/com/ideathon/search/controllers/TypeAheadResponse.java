package com.ideathon.search.controllers;

import java.util.List;

public class TypeAheadResponse {

	private List<String> suggestions;

	public TypeAheadResponse() {
	}

	public TypeAheadResponse(List<String> suggestions) {
		this.suggestions = suggestions;
	}

	public List<String> getSuggestions() {
		return suggestions;
	}

	public void setSuggestions(List<String> suggestions) {
		this.suggestions = suggestions;
	}
}