package com.ideathon.search.controllers;

import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
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
import com.ideathon.search.models.MessageModel;

@CrossOrigin
@RestController
@RequestMapping(value = "/userdata")
public class KafkaApi {

	private static final Logger logger = LoggerFactory.getLogger(KafkaApi.class);

	private static Properties producerProps = null;

	private static Properties consumerProps = null;

	private static KafkaProducer<String, String> producer = null;

	private static KafkaConsumer<String, String> consumer = null;

	private final static String TOPIC = "clickstream";

	@Value("${bootstrap.servers}")
	private String bootstrapServers;

	private final static Gson GSON = new Gson();

	private static final String[] CSV_HEADERS = { "SESSION_ID", "KEYWORD", "PRODUCT_ID", "TRANSACTION_TYPE" };

	private static final String HEADER_VALUE_TEMPLATE = "attachment;filename=clickstream-data-%s.csv";

	@RequestMapping(value = "/post", produces = { APPLICATION_JSON_VALUE }, consumes = {
			APPLICATION_JSON_VALUE }, method = RequestMethod.POST)
	public ResponseEntity<Map<String, String>> postMessage(@RequestBody MessageModel message) {
		Map<String, String> response = new LinkedHashMap<>();
		try {
			String jsonString = GSON.toJson(message);
			logger.info("message received - {}", jsonString);
			if (StringUtils.isEmpty(message.getSessionId()) || StringUtils.isEmpty(message.getKeyword())
					|| StringUtils.isEmpty(message.getProductId())
					|| StringUtils.isEmpty(message.getTransactionType())) {
				response.put("status", "failed");
				response.put("message", "message format incorrect");
				return new ResponseEntity<Map<String, String>>(response, HttpStatus.BAD_REQUEST);
			}
			postToKafka(message.getKeyword(), jsonString);
			response.put("status", "success");
			response.put("message", "message posted to kafka topic");
		} catch (Exception e) {
			logger.error("Exception while posting message to kafka - {}", e);
			response.put("status", "failed");
			response.put("message", e.getLocalizedMessage());
			return new ResponseEntity<Map<String, String>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<Map<String, String>>(response, HttpStatus.OK);
	}

	@RequestMapping(value = "/closeProducer", produces = { APPLICATION_JSON_VALUE }, method = RequestMethod.GET)
	public ResponseEntity<Map<String, String>> closeProducer() {
		Map<String, String> response = new LinkedHashMap<>();
		try {
			if (producer != null) {
				producer.close();
				producer = null;
			}
			response.put("status", "success");
			response.put("message", "producer closed successfully");
		} catch (Exception e) {
			logger.error("Exception while closing producer - {}", e);
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
			List<MessageModel> records = readFromKafka();
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
						for (MessageModel record : records) {
							List<Object> csvRecord = new ArrayList<>();
							csvRecord.add(record.getSessionId());
							csvRecord.add(record.getKeyword());
							csvRecord.add(record.getProductId());
							csvRecord.add(record.getTransactionType());
							csvPrinter.printRecord(csvRecord);
						}
						out.flush();
					}
				}
			};
		} catch (Exception e) {
			if (consumer != null) {
				consumer.unsubscribe();
				consumer.close();
				consumer = null;
			}
			logger.error("Exception while consuming messages from kafka - {}", e);
			throw e;
		}
		return new ResponseEntity<StreamingResponseBody>(writer, HttpStatus.OK);
	}

	private List<MessageModel> readFromKafka() throws Exception {
		List<MessageModel> records = new ArrayList<>();
		if (consumerProps == null) {
			consumerProps = initializeConsumerKafkaConfig();
		}
		if (consumer == null) {
			consumer = new KafkaConsumer<>(consumerProps);
			consumer.subscribe(Collections.singletonList(TOPIC));
		}

		consumer.poll(Duration.ZERO);
		ConsumerRecords<String, String> kafkaRecords = consumer.poll(Duration.ofMillis(100));

		kafkaRecords.forEach(record -> {
			MessageModel message = GSON.fromJson(record.value(), MessageModel.class);
			records.add(message);
		});

		consumer.unsubscribe();
		consumer.close();
		consumer = null;
		return records;
	}

	private void postToKafka(String key, String value) {
		if (producerProps == null) {
			producerProps = initializeProducerKafkaConfig();
		}
		if (producer == null) {
			producer = new KafkaProducer<>(producerProps);
		}
		producer.send(new ProducerRecord<String, String>(TOPIC, key, value));
	}

	private Properties initializeProducerKafkaConfig() {
		Properties props = new Properties();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ProducerConfig.CLIENT_ID_CONFIG, "kafka.clickstream.producer");
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		return props;
	}

	private Properties initializeConsumerKafkaConfig() {
		Properties props = new Properties();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka.consumer.group" + UUID.randomUUID().toString());
		props.put(ProducerConfig.CLIENT_ID_CONFIG, "kafka.clickstream.consumer");
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		return props;
	}
}
