package com.ideathon.search;

import java.io.IOException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.PivotField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;

public class IndexTypeAhead {

	@SuppressWarnings("serial")
	public static final class NWordEntry extends SimpleImmutableEntry<Integer, Double> {
		public NWordEntry(int n, double weight) {
			super(n, weight);
		}
	}

	public static void main(String[] args) throws SolrServerException, IOException {

		SolrClient client = new HttpSolrClient.Builder("http://localhost:8983/solr").withConnectionTimeout(1000)
				.withSocketTimeout(6000).build();
		SolrQuery query = new SolrQuery("*:*");
		query.setRows(0);
		query.setFacet(true);
		query.setFacetLimit(Integer.MAX_VALUE);
		query.addFacetField("brand", "product");
		query.addFacetPivotField("gender,brand,product", "gender,brand", "gender,product", "brand,product");

		QueryResponse response = client.query("catalog", query);

		client.deleteByQuery("typeahead", "*:*");
		client.commit("typeahead");
		List<SolrInputDocument> solrDocs = new ArrayList<>();

		for (FacetField facetField : response.getFacetFields()) {
			for (Count value : facetField.getValues()) {
				String product = value.getName();
				SolrInputDocument inputDoc = new SolrInputDocument();
				String suggestion = formatSuggestion(product);
				inputDoc.addField("id", suggestion);
				inputDoc.addField("suggestion", suggestion);
				solrDocs.add(inputDoc);
			}
		}
		
		List<String> validGenders = new ArrayList<>();
		validGenders.add("Mens");
		validGenders.add("Womens");
		validGenders.add("Boys");
		validGenders.add("Girls");
		validGenders.add("Juniors");

		NamedList<List<PivotField>> pivot = response.getFacetPivot();

		List<PivotField> genderProduct = pivot.get("gender,product");

		for (PivotField piv : genderProduct) {
			String gender = piv.getValue().toString();
			
			if(!validGenders.contains(gender)) {
				continue;
			}
			List<PivotField> brandPivot = piv.getPivot();

			for (PivotField brand : brandPivot) {
				String product = brand.getValue().toString();
				SolrInputDocument inputDoc = new SolrInputDocument();
				String suggestion = formatSuggestion(gender + " " + product);
				inputDoc.addField("id", suggestion);
				inputDoc.addField("suggestion", suggestion);
				solrDocs.add(inputDoc);
			}
		}

		List<PivotField> brandProduct = pivot.get("brand,product");

		for (PivotField piv : brandProduct) {
			String gender = piv.getValue().toString();
			List<PivotField> brandPivot = piv.getPivot();

			for (PivotField brand : brandPivot) {
				String product = brand.getValue().toString();
				SolrInputDocument inputDoc = new SolrInputDocument();
				String suggestion = formatSuggestion(gender + " " + product);
				inputDoc.addField("id", suggestion);
				inputDoc.addField("suggestion", suggestion);
				solrDocs.add(inputDoc);
			}
		}

		List<PivotField> genderBrand = pivot.get("gender,brand");

		for (PivotField piv : genderBrand) {
			String gender = piv.getValue().toString();
			if(!validGenders.contains(gender)) {
				continue;
			}
			List<PivotField> brandPivot = piv.getPivot();

			for (PivotField brand : brandPivot) {
				String product = brand.getValue().toString();
				SolrInputDocument inputDoc = new SolrInputDocument();
				String suggestion = formatSuggestion(gender + " " + product);
				inputDoc.addField("id", suggestion);
				inputDoc.addField("suggestion", suggestion);
				solrDocs.add(inputDoc);
			}
		}

		List<PivotField> genderBrandProduct = pivot.get("gender,brand,product");

		for (PivotField piv : genderBrandProduct) {
			String gender = piv.getValue().toString();
			if(!validGenders.contains(gender)) {
				continue;
			}
			List<PivotField> brandPivot = piv.getPivot();

			for (PivotField brand : brandPivot) {
				String brandValue = brand.getValue().toString();
				if(brandValue.equalsIgnoreCase("Unbranded")) {
					continue;
				}
				List<PivotField> productPivot = brand.getPivot();

				for (PivotField prod : productPivot) {
					String product = prod.getValue().toString();
					SolrInputDocument inputDoc = new SolrInputDocument();
					String suggestion = formatSuggestion(gender + " " + brandValue + " " + product);
					inputDoc.addField("id", suggestion);
					inputDoc.addField("suggestion", suggestion);
					solrDocs.add(inputDoc);
				}
			}
		}

		/*
		 * for (String brand : brands) { for (String product : products) {
		 * SolrInputDocument inputDoc = new SolrInputDocument(); String suggestion =
		 * formatSuggestion(brand + " " + product); inputDoc.addField("id", suggestion);
		 * inputDoc.addField("suggestion", suggestion); solrDocs.add(inputDoc); } }
		 * 
		 * for (String gender : genders) { for (String brand : brands) { for (String
		 * product : products) { SolrInputDocument inputDoc = new SolrInputDocument();
		 * String suggestion = formatSuggestion(gender + " " + brand + " " + product);
		 * inputDoc.addField("id", suggestion); inputDoc.addField("suggestion",
		 * suggestion); solrDocs.add(inputDoc); } } }
		 */
		// collectTitleNWords(response.getResults(), solrDocs);
		client.add("typeahead", solrDocs);
		client.commit("typeahead");
		client.close();

	}

	private static String formatSuggestion(String phrase) {
		return phrase.replaceAll("\\|L\\|$", "").toLowerCase().replaceAll("</?(strong|p|b)/?>", " ")
				.replaceAll("[^-&a-z0-9 \u00a0]", "").replaceAll("[\\s\u00a0]+", " ").trim();
	}

	private static final NWordEntry ZERO = new NWordEntry(0, 0);

	public static void collectTitleNWords(SolrDocumentList docList, List<SolrInputDocument> solrInputDocs) {
		int words = 1;
		Map<String, NWordEntry> nwordsMap = new HashMap<>();
		do {
			nwordsMap.clear();
			for (SolrDocument doc : docList) {
				if (doc.containsKey("brand")) {
					String entry = formatSuggestion(doc.getFieldValue("brand").toString());
					collectTitleNWordsOfLength(nwordsMap, entry, words, solrInputDocs);
				}
			}
			words++;
		} while (!nwordsMap.isEmpty());
	}

	private static final Pattern NOT_A_WORD = Pattern.compile("^[^a-z0-9]*$");

	private static void collectTitleNWordsOfLength(Map<String, NWordEntry> nwordsMap, String title, int words,
			List<SolrInputDocument> solrDocs) {
		if (words == 4) {
			return;
		}
		String[] phraseParts = title.split(" ", 0);
		if (phraseParts.length <= words) {
			return;
		}

		StringBuilder nwordBuilder = new StringBuilder();
		Set<String> seen = new HashSet<>();
		for (int pos = 0; pos <= phraseParts.length - words; pos++) {
			if (NOT_A_WORD.matcher(phraseParts[pos]).matches()
					|| NOT_A_WORD.matcher(phraseParts[pos + words - 1]).matches()) {
				// Phrase must start and end with a word (or number)
				// XXX Contested by AC
				continue;
			}

			for (int i = 0; i < words; i++) {
				nwordBuilder.append(phraseParts[i + pos]);
				if (i != words - 1) {
					nwordBuilder.append(' ');
				}
			}
			String nword = nwordBuilder.toString();
			nwordBuilder.setLength(0);

			// Protect against same word in title twice
			// and against nwords starting or ending wit
			if (seen.contains(nword) && !nword.startsWith(" ")) {
				continue;
			}
			NWordEntry existing = nwordsMap.get(nword);
			if (existing == null) {
				existing = ZERO;
			}
			System.out.println(nword);
			SolrInputDocument inputDoc = new SolrInputDocument();
			inputDoc.addField("id", nword);
			inputDoc.addField("suggestion", nword);
			solrDocs.add(inputDoc);
			nwordsMap.put(nword, new NWordEntry(existing.getKey() + 1, existing.getValue() + 100));
			seen.add(nword);
		}
	}
}