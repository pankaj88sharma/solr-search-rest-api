package com.ideathon.search.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.ContentStreamBase.StringStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.ideathon.search.factories.SolrClientBuilder;
import com.ideathon.search.models.Prediction;
import com.ideathon.search.models.SearchParams;

@Component
public class SolrSearcher {

	private static final Logger logger = LoggerFactory.getLogger(SolrSearcher.class);

	@Value("${solr.ip}")
	private String solrIp;

	@Value("${solr.catalog.core}")
	private String catalogCoreName;

	@Value("${solr.typeahead.core}")
	private String typeAheadCoreName;

	@Value("${solr.concepts.core}")
	private String conceptsCoreName;

	@Value("${brand.prediction.host}")
	private String brandPredictionHost;

	@Value("${brand.prediction.api}")
	private String brandPredictionEndpoint;

	@Value("${p1.prediction.host}")
	private String p1PredictionHost;

	@Value("${p1.prediction.api}")
	private String p1PredictionEndpoint;

	@Value("${brand.prediction.threshold}")
	private String brandThreshold;

	@Value("${p1.prediction.threshold}")
	private String p1Threshold;

	@Autowired
	private ReRankService reRankService;

	public QueryResponse executeSearch(SearchParams params, Multimap<String, String> filtersMap) throws Exception {
		Multimap<String, String> conceptsFilters = getConcepts(params.getKeyword(), filtersMap);
		final SolrQuery query = new SolrQuery();
		query.add("defType", "edismax");
		String q = buildQuery(params.getKeyword(), conceptsFilters);
		query.add("q", q);
		query.add("mm", "75%");
		query.setFacet(true);
		query.add("facet.field", "{!ex=departmentRefine}department", "{!ex=categoryRefine}category",
				"{!ex=productRefine}product", "{!ex=brandRefine}brand");
		query.setFacetMinCount(1);
		query.setFacetLimit(Integer.MAX_VALUE);
		query.setRows(params.getCount());
		query.setStart(params.getStart());

		for (String key : filtersMap.keySet()) {
			String fieldName = key;
			Collection<String> fieldValues = filtersMap.get(key);
			String fq = "{!tag=" + fieldName + "Refine" + "}" + fieldName + ":" + "(\""
					+ StringUtils.join(fieldValues, "\" OR \"") + "\")";
			query.addFilterQuery(fq);
		}

		if (params.getReRank()) {
			String p1PredictionURL = "http://" + p1PredictionHost + p1PredictionEndpoint;
			List<Prediction> p1Predictions = reRankService.getApiResponse(params.getKeyword(), p1PredictionURL,
					p1Threshold, false);

			for (Prediction concept : p1Predictions) {
				String[] split = concept.getValue().split(":", 2);
				if (split.length == 2) {
					String fieldName = split[0];
					String value = split[1];
					query.add("bq", fieldName + ":" + "(\"" + value + "\")^" + concept.getScore() * 3);
				}
			}

			String brandPredictionURL = "http://" + brandPredictionHost + brandPredictionEndpoint;
			List<Prediction> brandPredictions = reRankService.getApiResponse(params.getKeyword(), brandPredictionURL,
					brandThreshold, params.getReloadModel());
			if (brandPredictions.size() > 0) {
				List<String> predictionValues = brandPredictions.stream().map(pred -> pred.getValue())
						.collect(Collectors.toList());
				query.add("rq", "{!rerank reRankQuery=$rqq reRankDocs=10 reRankWeight=3}");
				query.add("rqq", "brand:" + "(\"" + StringUtils.join(predictionValues, "\" OR \"") + "\")");
			}
		}

		logger.info("solr query -> " + query);

		final QueryResponse response = SolrClientBuilder.getSolrClient(solrIp).query(catalogCoreName, query);
		return response;
	}

	private String buildQuery(String keyword, Multimap<String, String> conceptsFilters) {
		StringBuilder query = new StringBuilder();
		query.append("title:(" + keyword + ")^5");

		for (String key : conceptsFilters.keySet()) {
			query.append(" ");
			String fieldName = key;
			Collection<String> fieldValues = conceptsFilters.get(key);
			query.append(fieldName + ":" + "(\"" + StringUtils.join(fieldValues, "\" OR \"") + "\")");
		}
		return query.toString();
	}

	private Multimap<String, String> getConcepts(String keyword, Multimap<String, String> filtersMap) throws Exception {
		final SolrQuery q = new SolrQuery();
		q.setRequestHandler("/tag");
		q.add("overlaps", "ALL");
		q.add("matchText", "true");
		q.add("text", keyword);

		QueryRequest request = new QueryRequest(q, SolrRequest.METHOD.POST) {
			private static final long serialVersionUID = 1L;

			public Collection<ContentStream> getContentStreams() {
				return Collections.<ContentStream>singletonList(new StringStream(keyword));
			};
		};
		final QueryResponse response = request.process(SolrClientBuilder.getSolrClient(solrIp), conceptsCoreName);

		List<String> conceptsRefinements = new ArrayList<>();
		for (SolrDocument doc : response.getResults()) {
			if(doc.getFieldValue("type").toString().equalsIgnoreCase("size_concept")) {
				continue;
			}
			conceptsRefinements.add(doc.getFieldValue("refine").toString());
		}
		return populateConceptsMap(conceptsRefinements, filtersMap);
	}

	public QueryResponse executeSuggest(String keyword) throws Exception {
		final SolrQuery q = new SolrQuery();
		q.setRequestHandler("/suggest");
		q.add("suggest.q", keyword);
		final QueryResponse response = SolrClientBuilder.getSolrClient(solrIp).query(typeAheadCoreName, q);
		return response;
	}

	public QueryResponse executeSpellCheck(String keyword) throws Exception {
		final SolrQuery q = new SolrQuery();
		q.setRequestHandler("/spell");
		q.add("spellcheck.q", keyword);
		q.add("spellcheck.collateParam.q.op", "AND");
		q.add("spellcheck.onlyMorePopular", "true");
		q.add("spellcheck.collateParam.mm", "100%");
		q.add("df", "title");
		final QueryResponse response = SolrClientBuilder.getSolrClient(solrIp).query(catalogCoreName, q);
		return response;
	}

	private Multimap<String, String> populateConceptsMap(List<String> refine, Multimap<String, String> filtersMap) {
		Multimap<String, String> result = ArrayListMultimap.create();
		for (String ref : refine) {
			String[] split = ref.split(":", 2);
			if (split.length == 2) {
				result.put(split[0], split[1]);
			}
		}
		return result;
	}
}