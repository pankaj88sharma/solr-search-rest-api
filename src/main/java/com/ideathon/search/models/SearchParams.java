package com.ideathon.search.models;

import java.util.ArrayList;
import java.util.List;

public class SearchParams {
	private String keyword = "*:*";
	private List<String> refine = new ArrayList<>();
	private Integer count = 60;
	private Integer start = 0;
	private Boolean reRank = false;
	private Boolean reloadModel = false;

	public SearchParams() {
	}

	public SearchParams(String keyword, List<String> refine, Integer count, Integer start, Boolean reRank,
			Boolean reloadModel) {
		super();
		this.keyword = keyword;
		this.refine = refine;
		this.count = count;
		this.start = start;
		this.setReRank(reRank);
		this.setReloadModel(reloadModel);
	}

	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	public List<String> getRefine() {
		return refine;
	}

	public void setRefine(List<String> refine) {
		this.refine = refine;
	}

	public Integer getCount() {
		return count;
	}

	public void setCount(Integer count) {
		this.count = count;
	}

	public Integer getStart() {
		return start;
	}

	public void setStart(Integer start) {
		this.start = start;
	}

	public Boolean getReRank() {
		return reRank;
	}

	public void setReRank(Boolean reRank) {
		this.reRank = reRank;
	}

	public Boolean getReloadModel() {
		return reloadModel;
	}

	public void setReloadModel(Boolean reloadModel) {
		this.reloadModel = reloadModel;
	}
}