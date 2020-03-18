package com.ideathon.search.models;

import java.util.List;

public class NavigationArea {
	private List<FieldRefinement> fieldRefinements;

	public NavigationArea(List<FieldRefinement> fieldRefinements) {
		this.fieldRefinements = fieldRefinements;
	}

	public List<FieldRefinement> getFieldRefinements() {
		return fieldRefinements;
	}

	public void setFieldRefinements(List<FieldRefinement> fieldRefinements) {
		this.fieldRefinements = fieldRefinements;
	}
}