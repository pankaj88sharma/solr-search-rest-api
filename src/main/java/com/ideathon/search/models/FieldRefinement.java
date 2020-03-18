package com.ideathon.search.models;

import java.util.List;

public class FieldRefinement {

	private String displayName;
	private List<FieldValue> values;

	public FieldRefinement() {
	}

	public FieldRefinement(String displayName, List<FieldValue> values) {
		this.displayName = displayName;
		this.values = values;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public List<FieldValue> getValues() {
		return values;
	}

	public void setValues(List<FieldValue> values) {
		this.values = values;
	}
}