package com.ideathon.search.controllers;

import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.google.gson.Gson;
import com.ideathon.search.factories.SolrClientBuilder;
import com.ideathon.search.models.MessageModel;

@CrossOrigin
@RestController
@RequestMapping(value = "/clickstream")
public class ClickStreamApi {

	private static final Logger logger = LoggerFactory.getLogger(ClickStreamApi.class);

	private static final String[] CSV_HEADERS = { "SESSION_ID", "KEYWORD", "BRAND" };

	private static final String HEADER_VALUE_TEMPLATE = "attachment;filename=clickstream-data-%s.csv";

	private final static Gson GSON = new Gson();

	@Value("${solr.ip}")
	private String solrIp;

	@Value("${solr.clickstream.core}")
	private String clickstreamCoreName;

	@RequestMapping(value = "/post", produces = { APPLICATION_JSON_VALUE }, consumes = {
			APPLICATION_JSON_VALUE }, method = RequestMethod.POST)
	public ResponseEntity<Map<String, String>> postMessage(@RequestBody MessageModel message) {
		Map<String, String> response = new LinkedHashMap<>();
		try {
			String jsonString = GSON.toJson(message);
			logger.info("message received - {}", jsonString);
			if (StringUtils.isEmpty(message.getSessionId()) || StringUtils.isEmpty(message.getKeyword())
					|| StringUtils.isEmpty(message.getProductId()) || StringUtils.isEmpty(message.getBrand())
					|| StringUtils.isEmpty(message.getTransactionType())) {
				response.put("status", "failed");
				response.put("message", "message format incorrect");
				return new ResponseEntity<Map<String, String>>(response, HttpStatus.BAD_REQUEST);
			}
			indexToSolr(SolrClientBuilder.getSolrClient(solrIp), message);
			response.put("status", "success");
			response.put("message", "message indexed to solr core");
		} catch (Exception e) {
			logger.error("Exception while indexing message to solr core - {}", e);
			response.put("status", "failed");
			response.put("message", e.getLocalizedMessage());
			return new ResponseEntity<Map<String, String>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<Map<String, String>>(response, HttpStatus.OK);
	}

	@RequestMapping(value = "/clear", produces = { APPLICATION_JSON_VALUE }, method = RequestMethod.GET)
	public ResponseEntity<Map<String, String>> clear() {
		Map<String, String> response = new LinkedHashMap<>();
		try {
			SolrClient solrClient = SolrClientBuilder.getSolrClient(solrIp);
			solrClient.deleteByQuery(clickstreamCoreName, "*:*");
			solrClient.commit(clickstreamCoreName);

			response.put("status", "success");
			response.put("message", "clickstream data cleared from solr core");
		} catch (Exception e) {
			logger.error("Exception while clearing cliskstream data - {}", e);
			response.put("status", "failed");
			response.put("message", e.getLocalizedMessage());
			return new ResponseEntity<Map<String, String>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<Map<String, String>>(response, HttpStatus.OK);
	}

	@RequestMapping(value = "/csv", produces = { APPLICATION_JSON_VALUE }, method = RequestMethod.GET)
	public ResponseEntity<StreamingResponseBody> consumeMessages(HttpServletResponse response) throws Exception {
		StreamingResponseBody writer = null;
		try {
			List<SolrDocument> records = readFromSolrCore(SolrClientBuilder.getSolrClient(solrIp), "*:*");
			if (records.size() == 0) {
				throw new Exception("No message to consume.");
			}
			String headerValue = String.format(HEADER_VALUE_TEMPLATE, Instant.now().toEpochMilli());
			response.addHeader(CONTENT_DISPOSITION, headerValue);
			response.setContentType(APPLICATION_OCTET_STREAM_VALUE);

			writer = new StreamingResponseBody() {

				@Override
				public void writeTo(OutputStream out) throws IOException {
					try (final CSVPrinter csvPrinter = new CSVPrinter(new OutputStreamWriter(out), CSVFormat.DEFAULT
							.withHeader(CSV_HEADERS).withQuoteMode(QuoteMode.MINIMAL).withNullString(""))) {
						for (SolrDocument record : records) {
							List<Object> csvRecord = new ArrayList<>();
							csvRecord.add(record.get("session_id"));
							csvRecord.add(record.get("keyword"));
							csvRecord.add(record.get("brand"));
							csvPrinter.printRecord(csvRecord);
						}
						out.flush();
					}
				}
			};
		} catch (Exception e) {
			logger.error("Exception while consuming messages from solr core - {}", e);
			throw e;
		}
		return new ResponseEntity<StreamingResponseBody>(writer, HttpStatus.OK);
	}

	private SolrDocumentList readFromSolrCore(SolrClient client, String q) throws Exception {

		final SolrQuery query = new SolrQuery(q);
		final QueryResponse response = client.query(clickstreamCoreName, query);
		final SolrDocumentList documents = response.getResults();
		return documents;
	}

	private void indexToSolr(SolrClient client, MessageModel message) throws Exception {

		final SolrInputDocument inputDoc = new SolrInputDocument();
		inputDoc.addField("id", message.getSessionId() + "-" + message.getProductId());
		inputDoc.addField("session_id", message.getSessionId());
		inputDoc.addField("keyword", message.getKeyword());
		inputDoc.addField("brand", message.getBrand());

		client.add(clickstreamCoreName, inputDoc);
		client.commit(clickstreamCoreName);
	}
}