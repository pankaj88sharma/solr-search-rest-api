package com.ideathon.search.models;

import java.util.List;

public class FieldValue {
	private String displayValue = null;
	private Long count = null;
	private Boolean selected = null;
	private List<String> action;

	public FieldValue() {
	}

	public FieldValue(String displayValue, Long count, Boolean selected, List<String> action) {
		this.displayValue = displayValue;
		this.count = count;
		this.selected = selected;
		this.action = action;
	}

	public String getDisplayValue() {
		return displayValue;
	}

	public void setDisplayValue(String displayValue) {
		this.displayValue = displayValue;
	}

	public Long getCount() {
		return count;
	}

	public void setCount(Long count) {
		this.count = count;
	}

	public Boolean getSelected() {
		return selected;
	}

	public void setSelected(Boolean selected) {
		this.selected = selected;
	}

	public List<String> getAction() {
		return action;
	}

	public void setAction(List<String> action) {
		this.action = action;
	}
}
