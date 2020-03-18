package com.ideathon.search.models;

import java.util.List;

public class HeaderArea {

	private List<FieldRefinement> breadcrumbs;

	public HeaderArea(List<FieldRefinement> breadcrumbs) {
		this.breadcrumbs = breadcrumbs;
	}

	public List<FieldRefinement> getBreadcrumbs() {
		return breadcrumbs;
	}

	public void setBreadcrumbs(List<FieldRefinement> breadcrumbs) {
		this.breadcrumbs = breadcrumbs;
	}
}