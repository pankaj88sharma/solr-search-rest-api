package com.ideathon.search.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SpellCheckResponse.Collation;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.stereotype.Component;

import com.google.common.collect.Multimap;
import com.ideathon.search.controllers.TypeAheadResponse;
import com.ideathon.search.models.FieldRefinement;
import com.ideathon.search.models.FieldValue;
import com.ideathon.search.models.HeaderArea;
import com.ideathon.search.models.NavigationArea;
import com.ideathon.search.models.SearchResponse;
import com.ideathon.search.models.SearchResultInfo;
import com.ideathon.search.models.SpellCheckCorrection;

@Component
public class ResponseGenerator {

	public SearchResponse generateResponse(QueryResponse queryResponse, QueryResponse spellCheckResponse,
			Multimap<String, String> filtersMap, List<String> refine) {
		SolrDocumentList results = queryResponse.getResults();
		Long numFound = results.getNumFound();
		SearchResultInfo searchResultInfo = new SearchResultInfo(numFound, queryResponse.getResults());
		NavigationArea navigationArea = generateNavigationArea(queryResponse, filtersMap);
		HeaderArea headerArea = generateHeaderArea(filtersMap, refine);
		SpellCheckCorrection spellCheckCorrection = generateSpellCheckCorrection(spellCheckResponse,
				queryResponse.getResults().getNumFound());
		SearchResponse response = new SearchResponse(searchResultInfo, navigationArea, headerArea,
				spellCheckCorrection);
		return response;
	}

	private SpellCheckCorrection generateSpellCheckCorrection(QueryResponse spellCheckResponse, long numFound) {
		SpellCheckCorrection spellCheckCorrection = new SpellCheckCorrection();
		if (spellCheckResponse.getSpellCheckResponse() != null
				&& spellCheckResponse.getSpellCheckResponse().getCollatedResults() != null
				&& spellCheckResponse.getSpellCheckResponse().getCollatedResults().size() > 0) {
			List<Collation> collatedResults = spellCheckResponse.getSpellCheckResponse().getCollatedResults();
			Collections.sort(collatedResults, new Comparator<Collation>() {
				public int compare(Collation o1, Collation o2) {
					return Float.compare(o2.getNumberOfHits(), o1.getNumberOfHits());
				}
			});
			spellCheckCorrection.setCorrectionAvailable(true);
			spellCheckCorrection.setCorrectedTerm(collatedResults.get(0).getCollationQueryString());
		}
		return spellCheckCorrection;
	}

	private HeaderArea generateHeaderArea(Multimap<String, String> filtersMap, List<String> refine) {
		List<FieldRefinement> breadcrumbs = new ArrayList<>();
		HeaderArea headerArea = new HeaderArea(breadcrumbs);

		for (String ref : refine) {
			String[] split = ref.split(":", 2);
			if (split.length == 2) {
				FieldRefinement refn = new FieldRefinement();
				String displayName = split[0];
				String displayValue = split[1];
				refn.setDisplayName(StringUtils.capitalize(displayName));
				List<FieldValue> fieldValues = new ArrayList<>();
				FieldValue fieldValue = new FieldValue();
				fieldValue.setDisplayValue(displayValue);
				setSelectedAndValue(fieldValue, filtersMap, displayName, displayValue);
				fieldValues.add(fieldValue);
				refn.setValues(fieldValues);
				breadcrumbs.add(refn);
			}
		}

		return headerArea;
	}

	private NavigationArea generateNavigationArea(QueryResponse queryResponse, Multimap<String, String> filtersMap) {
		List<FieldRefinement> fieldRefinements = new ArrayList<>();
		List<FacetField> facetFields = queryResponse.getFacetFields();

		for (FacetField facet : facetFields) {
			FieldRefinement refn = new FieldRefinement();
			refn.setDisplayName(StringUtils.capitalize(facet.getName()));

			List<FieldValue> fieldValues = new ArrayList<>();
			for (Count value : facet.getValues()) {
				FieldValue fieldValue = new FieldValue();
				fieldValue.setDisplayValue(value.getName());
				fieldValue.setCount(value.getCount());
				setSelectedAndValue(fieldValue, filtersMap, facet.getName(), value.getName());
				fieldValues.add(fieldValue);
			}
			refn.setValues(fieldValues);
			fieldRefinements.add(refn);
		}

		NavigationArea navArea = new NavigationArea(fieldRefinements);
		return navArea;
	}

	private void setSelectedAndValue(FieldValue fieldValue, Multimap<String, String> filtersMap, String displayName,
			String displayValue) {
		Boolean selected = filtersMap.get(displayName).contains(displayValue);
		fieldValue.setSelected(selected);
		List<String> action = new ArrayList<>();

		if (selected) {
			for (String key : filtersMap.keySet()) {
				String refName = key;
				Collection<String> refValues = filtersMap.get(key);
				for (String val : refValues) {
					if (!val.equals(displayValue)) {
						action.add(refName + ":" + val);
					}
				}
			}
		} else {
			action.add(displayName + ":" + displayValue);
			for (String key : filtersMap.keySet()) {
				String refName = key;
				Collection<String> refValues = filtersMap.get(key);
				for (String val : refValues) {
					action.add(refName + ":" + val);
				}
			}
		}
		fieldValue.setAction(action);
	}

	public TypeAheadResponse generateSuggestResponse(QueryResponse queryResponse) {
		TypeAheadResponse taResponse = new TypeAheadResponse();
		List<String> suggestions = new ArrayList<>();
		Map<String, List<String>> suggestedTermsMap = queryResponse.getSuggesterResponse().getSuggestedTerms();
		for (Entry<String, List<String>> suggestionEntry : suggestedTermsMap.entrySet()) {
			suggestions.addAll(suggestionEntry.getValue());
		}
		taResponse.setSuggestions(suggestions);
		return taResponse;
	}
}
