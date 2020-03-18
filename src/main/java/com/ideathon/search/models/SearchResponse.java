package com.ideathon.search.models;

public class SearchResponse {
	private SearchResultInfo searchResultInfo;
	private NavigationArea navigationArea;
	private HeaderArea headerArea;
	private SpellCheckCorrection spellCheckCorrection;

	public SearchResponse(SearchResultInfo searchResultInfo, NavigationArea navigationArea, HeaderArea headerArea,
			SpellCheckCorrection spellCheckCorrection) {
		this.searchResultInfo = searchResultInfo;
		this.navigationArea = navigationArea;
		this.headerArea = headerArea;
		this.spellCheckCorrection = spellCheckCorrection;
	}

	public SearchResultInfo getSearchResultInfo() {
		return searchResultInfo;
	}

	public void setSearchResultInfo(SearchResultInfo searchResultInfo) {
		this.searchResultInfo = searchResultInfo;
	}

	public NavigationArea getNavigationArea() {
		return navigationArea;
	}

	public void setNavigationArea(NavigationArea navigationArea) {
		this.navigationArea = navigationArea;
	}

	public HeaderArea getHeaderArea() {
		return headerArea;
	}

	public void setHeaderArea(HeaderArea headerArea) {
		this.headerArea = headerArea;
	}

	public SpellCheckCorrection getSpellCheckCorrection() {
		return spellCheckCorrection;
	}

	public void setSpellCheckCorrection(SpellCheckCorrection spellCheckCorrection) {
		this.spellCheckCorrection = spellCheckCorrection;
	}
}