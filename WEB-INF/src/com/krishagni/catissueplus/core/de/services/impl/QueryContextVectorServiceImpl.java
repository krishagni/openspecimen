package com.krishagni.catissueplus.core.de.services.impl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.krishagni.catissueplus.core.administrative.repository.FormListCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.OpenSpecimenAppCtxProvider;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.common.domain.Lock;
import com.krishagni.catissueplus.core.de.events.FormFieldSummary;
import com.krishagni.catissueplus.core.de.events.FormSummary;
import com.krishagni.catissueplus.core.de.events.GetFormFieldPvsOp;
import com.krishagni.catissueplus.core.de.events.ListFormFields;
import com.krishagni.catissueplus.core.de.events.VectorContextResult;
import com.krishagni.catissueplus.core.de.events.VectorContextSearchOp;
import com.krishagni.catissueplus.core.de.events.VectorFieldContext;
import com.krishagni.catissueplus.core.de.events.VectorMetadataSyncOp;
import com.krishagni.catissueplus.core.de.services.FormService;
import com.krishagni.catissueplus.core.de.services.QueryContextVectorService;

import edu.common.dynamicextensions.domain.nui.PermissibleValue;

public class QueryContextVectorServiceImpl implements QueryContextVectorService {
	private static final LogUtil logger = LogUtil.getLogger(QueryContextVectorServiceImpl.class);

	private static final String CFG_MOD = "query_context";

	private static final String LOCK_TYPE = "query_context_vector_sync";

	private final AtomicBoolean syncEnqueued = new AtomicBoolean(false);

	private final ExecutorService syncExecutor = Executors.newSingleThreadExecutor();

	private final MilvusStore milvusStore = new MilvusStore();

	private final EmbeddingClient embeddingClient = new EmbeddingClient();

	private FormService formSvc;

	private DaoFactory daoFactory;

	public void setFormSvc(FormService formSvc) {
		this.formSvc = formSvc;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Boolean> syncMetadata(RequestEvent<VectorMetadataSyncOp> req) {
		try {
			VectorMetadataSyncOp op = req != null && req.getPayload() != null ? req.getPayload() : new VectorMetadataSyncOp();
			ContextKey ctx = ContextKey.from(op.getCpId(), op.getCpGroupId());
			int pvBatchSize = op.getPvBatchSize() != null && op.getPvBatchSize() > 0 ? op.getPvBatchSize() : 1000;

			if (!acquireLock()) {
				logger.info("Another node is syncing query context metadata. Skipping this run.");
				return ResponseEvent.response(false);
			}

			try {
				List<FieldDef> fields = getFields(ctx);
				Map<String, SourceDef> sources = buildSources(ctx, fields, pvBatchSize);
				milvusStore.ensureCollections(embeddingClient.dim());
				milvusStore.replaceContext(ctx, fields, sources.values().stream().collect(Collectors.toList()), embeddingClient);
				return ResponseEvent.response(true);
			} finally {
				releaseLock();
			}
		} catch (Exception e) {
			logger.error("Error syncing query context metadata into Milvus", e);
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public ResponseEvent<VectorContextResult> search(RequestEvent<VectorContextSearchOp> req) {
		try {
			VectorContextSearchOp op = req.getPayload();
			ContextKey ctx = ContextKey.from(op.getCpId(), op.getCpGroupId());
			int maxResults = op.getMaxResults() != null && op.getMaxResults() > 0 ? op.getMaxResults() : 25;

			List<Map<String, Object>> rows = milvusStore.searchFields(ctx, op.getText(), maxResults, embeddingClient);
			List<VectorFieldContext> fields = new ArrayList<>();
			for (Map<String, Object> row : rows) {
				Map<String, Object> entity = milvusStore.toEntity(row);
				VectorFieldContext field = new VectorFieldContext();
				field.setFieldId(str(entity.get("fieldId")));
				field.setRootForm(str(entity.get("rootForm")));
				field.setFormCaption(str(entity.get("formCaption")));
				field.setFieldCaption(str(entity.get("fieldCaption")));
				field.setType(str(entity.get("type")));
				field.setSourceId(str(entity.get("sourceId")));
				field.setScore(toDouble(row.get("score")));
				fields.add(field);
			}

			VectorContextResult result = new VectorContextResult();
			result.setFields(fields);
			return ResponseEvent.response(result);
		} catch (Exception e) {
			logger.error("Error searching query context from Milvus", e);
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public void onMetadataChanged() {
		if (!ConfigUtil.getInstance().getBoolSetting(CFG_MOD, "auto_sync_on_metadata_change", true)) {
			return;
		}

		if (syncEnqueued.compareAndSet(false, true)) {
			syncExecutor.submit(() -> {
				try {
					QueryContextVectorService svc = OpenSpecimenAppCtxProvider.getBean("queryContextVectorSvc");
					svc.syncMetadata(RequestEvent.wrap(new VectorMetadataSyncOp()));
				} finally {
					syncEnqueued.set(false);
				}
			});
		}
	}

	private boolean acquireLock() {
		Lock lock = daoFactory.getLockDao().getLockForUpdate(LOCK_TYPE);
		if (lock == null) {
			lock = new Lock();
			lock.setType(LOCK_TYPE);
			lock.setLocked(false);
			daoFactory.getLockDao().saveOrUpdate(lock, true);
			lock = daoFactory.getLockDao().getLockForUpdate(LOCK_TYPE);
		}

		if (Boolean.TRUE.equals(lock.getLocked())) {
			return false;
		}

		lock.setLocked(true);
		lock.setNode(Utility.getNodeName());
		lock.setLockTime(Calendar.getInstance().getTime());
		daoFactory.getLockDao().saveOrUpdate(lock, true);
		return true;
	}

	private void releaseLock() {
		Lock lock = daoFactory.getLockDao().getLockForUpdate(LOCK_TYPE);
		if (lock != null && Objects.equals(lock.getNode(), Utility.getNodeName())) {
			lock.setLocked(false);
			lock.setNode(null);
			lock.setLockTime(null);
			daoFactory.getLockDao().saveOrUpdate(lock, true);
		}
	}

	private List<FieldDef> getFields(ContextKey ctx) {
		List<FieldDef> fields = new ArrayList<>();
		for (FormSummary form : getQueryForms()) {
			ListFormFields op = new ListFormFields();
			op.setFormId(form.getFormId());
			op.setCpId(ctx.cpId);
			op.setCpGroupId(ctx.cpGroupId);
			op.setExtendedFields(true);
			List<FormFieldSummary> formFields = getFormFields(op);
			flatten(form, null, form.getName(), formFields, fields);
		}

		return fields;
	}

	private Map<String, SourceDef> buildSources(ContextKey ctx, List<FieldDef> fields, int pvBatchSize) {
		Map<String, SourceDef> sources = new LinkedHashMap<>();
		for (FieldDef field : fields) {
			SourceDef source = resolveSource(ctx, field, pvBatchSize);
			field.sourceId = source.id;
			sources.putIfAbsent(source.sourceKey, source);
		}

		return sources;
	}

	private SourceDef resolveSource(ContextKey ctx, FieldDef field, int pvBatchSize) {
		Map<String, Object> lookupProps = field.lookupProps != null ? field.lookupProps : Collections.emptyMap();
		String apiUrl = str(lookupProps.get("apiUrl"));
		if (StringUtils.contains(apiUrl, "users")) {
			return SourceDef.of("user_ctrl:" + normalizedMap(lookupProps), "USER", Collections.emptyList());
		}

		if (StringUtils.contains(apiUrl, "sites")) {
			return SourceDef.of("site_ctrl:" + normalizedMap(lookupProps), "SITE", Collections.emptyList());
		}

		if (StringUtils.contains(apiUrl, "permissible-values")) {
			Map<String, Object> filters = map(lookupProps.get("filters"));
			String sourceKey = "pv_lookup:" + normalizedMap(filters);
			return SourceDef.of(sourceKey, "PV", fetchPvsInBatches(field, filters, pvBatchSize));
		}

		if (field.pvs != null && !field.pvs.isEmpty()) {
			List<String> values = field.pvs;
			if (field.pvs.size() >= 100 && field.rootFormId != null) {
				List<String> allValues = fetchControlPvs(field, pvBatchSize);
				if (!allValues.isEmpty()) {
					values = allValues;
				}
			}

			String sourceKey = "static_pvs:" + DigestUtils.sha1Hex(values.stream().sorted().collect(Collectors.joining("|")));
			return SourceDef.of(sourceKey, "PV", values);
		}

		return SourceDef.of("field:" + field.fieldId, "FIELD", Collections.emptyList());
	}

	private List<String> fetchPvsInBatches(FieldDef field, Map<String, Object> filters, int pvBatchSize) {
		List<String> values = fetchControlPvs(field, pvBatchSize);
		if (!values.isEmpty()) {
			return values;
		}

		Object attribute = filters.get("attribute");
		if (attribute == null) {
			return Collections.emptyList();
		}

		Set<String> uniq = new LinkedHashSet<>();
		for (String prefix : prefixes()) {
			GetFormFieldPvsOp op = new GetFormFieldPvsOp();
			op.setFormId(field.rootFormId);
			op.setControlName(field.relativePath);
			op.setUseUdn(true);
			op.setMaxResults(pvBatchSize);
			op.setQueries(Collections.singletonList(prefix));

			List<PermissibleValue> pvs = ResponseEvent.unwrap(formSvc.getPvs(RequestEvent.wrap(op)));
			for (PermissibleValue pv : pvs) {
				if (pv != null && StringUtils.isNotBlank(pv.getValue())) {
					uniq.add(pv.getValue());
				}
			}
		}

		return new ArrayList<>(uniq);
	}

	private List<String> fetchControlPvs(FieldDef field, int pvBatchSize) {
		if (field.rootFormId == null || StringUtils.isBlank(field.relativePath)) {
			return Collections.emptyList();
		}

		Set<String> uniq = new LinkedHashSet<>();
		for (String prefix : prefixes()) {
			GetFormFieldPvsOp op = new GetFormFieldPvsOp();
			op.setFormId(field.rootFormId);
			op.setControlName(field.relativePath);
			op.setUseUdn(true);
			op.setMaxResults(pvBatchSize);
			op.setQueries(Collections.singletonList(prefix));

			List<PermissibleValue> pvs = ResponseEvent.unwrap(formSvc.getPvs(RequestEvent.wrap(op)));
			for (PermissibleValue pv : pvs) {
				if (pv != null && StringUtils.isNotBlank(pv.getValue())) {
					uniq.add(pv.getValue());
				}
			}
		}

		return new ArrayList<>(uniq);
	}

	private List<String> prefixes() {
		List<String> prefixes = new ArrayList<>();
		prefixes.add("");
		for (char c = 'a'; c <= 'z'; ++c) {
			prefixes.add(String.valueOf(c));
		}

		for (char c = '0'; c <= '9'; ++c) {
			prefixes.add(String.valueOf(c));
		}

		String extras = "_-";
		for (char c : extras.toCharArray()) {
			prefixes.add(String.valueOf(c));
		}

		for (char c1 = 'a'; c1 <= 'z'; ++c1) {
			for (char c2 = 'a'; c2 <= 'z'; ++c2) {
				prefixes.add(new String(new char[] {c1, c2}));
			}
		}

		return prefixes;
	}

	private void flatten(FormSummary rootForm, FormFieldSummary currentSubForm, String prefix, List<FormFieldSummary> formFields, List<FieldDef> out) {
		for (FormFieldSummary field : formFields) {
			if ("SUBFORM".equals(field.getType())) {
				if (("customFields".equals(field.getName()) || "extensions".equals(field.getName())) && field.getSubFields() != null) {
					for (FormFieldSummary extnForm : field.getSubFields()) {
						flatten(rootForm, extnForm, prefix + "." + field.getName() + "." + extnForm.getName(),
							extnForm.getSubFields() != null ? extnForm.getSubFields() : Collections.emptyList(), out);
					}
				} else {
					flatten(rootForm, currentSubForm, prefix + "." + field.getName(),
						field.getSubFields() != null ? field.getSubFields() : Collections.emptyList(), out);
				}

				continue;
			}

			FieldDef def = new FieldDef();
			def.rootForm = rootForm.getName();
			def.rootFormId = rootForm.getFormId();
			def.fieldId = prefix + "." + field.getName();
			def.relativePath = StringUtils.removeStart(def.fieldId, rootForm.getName() + ".");
			def.formCaption = rootForm.getCaption() + (currentSubForm != null ? ": " + currentSubForm.getCaption() : "");
			def.fieldCaption = field.getCaption();
			def.type = field.getType();
			def.pvs = field.getPvs() != null ? field.getPvs() : Collections.emptyList();
			def.lookupProps = field.getLookupProps();
			out.add(def);
		}
	}

	private List<FormSummary> getQueryForms() {
		FormListCriteria crit = new FormListCriteria().entityTypes(Collections.singletonList("Query")).maxResults(Integer.MAX_VALUE);
		ResponseEvent<List<FormSummary>> resp = formSvc.getForms(new RequestEvent<>(crit));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload() != null ? resp.getPayload() : Collections.emptyList();
	}

	private List<FormFieldSummary> getFormFields(ListFormFields op) {
		ResponseEvent<List<FormFieldSummary>> resp = formSvc.getFormFields(new RequestEvent<>(op));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload() != null ? resp.getPayload() : Collections.emptyList();
	}

	private Map<String, Object> map(Object obj) {
		if (obj instanceof Map) {
			return (Map<String, Object>) obj;
		}

		return Collections.emptyMap();
	}

	private String normalizedMap(Map<String, Object> map) {
		if (map == null || map.isEmpty()) {
			return "";
		}

		return map.entrySet().stream()
			.sorted(Map.Entry.comparingByKey())
			.map(e -> e.getKey() + "=" + str(e.getValue()))
			.collect(Collectors.joining("&"));
	}

	private String str(Object value) {
		return value == null ? "" : value.toString();
	}

	private Double toDouble(Object value) {
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		}

		return null;
	}

	private static class ContextKey {
		private final long cpId;
		private final long cpGroupId;

		private ContextKey(long cpId, long cpGroupId) {
			this.cpId = cpId;
			this.cpGroupId = cpGroupId;
		}

		private static ContextKey from(Long cpId, Long cpGroupId) {
			return new ContextKey(cpId != null ? cpId : -1L, cpGroupId != null ? cpGroupId : -1L);
		}

		private String ctx() {
			return cpId + ":" + cpGroupId;
		}
	}

	private static class FieldDef {
		private Long rootFormId;

		private String rootForm;

		private String fieldId;

		private String relativePath;

		private String formCaption;

		private String fieldCaption;

		private String type;

		private List<String> pvs;

		private Map<String, Object> lookupProps;

		private String sourceId;
	}

	private static class SourceDef {
		private String sourceKey;

		private String id;

		private String type;

		private List<String> values;

		private static SourceDef of(String sourceKey, String type, List<String> values) {
			SourceDef src = new SourceDef();
			src.sourceKey = sourceKey;
			src.id = DigestUtils.sha1Hex(sourceKey);
			src.type = type;
			src.values = values != null ? values : Collections.emptyList();
			return src;
		}
	}

	private static class EmbeddingClient {
		private static final ObjectMapper MAPPER = new ObjectMapper();

		private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

		private List<Float> embed(String text) {
			String url = ConfigUtil.getInstance().getStrSetting(CFG_MOD, "embedding_url", "");
			if (StringUtils.isBlank(url)) {
				throw new RuntimeException("Embedding URL is not configured");
			}

			Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("model", ConfigUtil.getInstance().getStrSetting(CFG_MOD, "embedding_model", ""));
			payload.put("input", text);

			try {
				HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
					.uri(URI.create(url))
					.timeout(Duration.ofSeconds(ConfigUtil.getInstance().getIntSetting(CFG_MOD, "embedding_timeout_secs", 30)))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(payload)));

				String apiKey = ConfigUtil.getInstance().getStrSetting(CFG_MOD, "embedding_api_key", "");
				if (StringUtils.isNotBlank(apiKey)) {
					reqBuilder.header("Authorization", "Bearer " + apiKey);
				}

				HttpResponse<String> resp = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
				if (resp.statusCode() >= 400) {
					throw new RuntimeException("Embedding HTTP error: " + resp.statusCode() + ", " + resp.body());
				}

				Map<String, Object> output = MAPPER.readValue(resp.body(), new TypeReference<Map<String, Object>>(){});
				List<Map<String, Object>> data = (List<Map<String, Object>>) output.get("data");
				if (data == null || data.isEmpty()) {
					throw new RuntimeException("Embedding service returned empty vectors");
				}

				List<Number> values = (List<Number>) data.get(0).get("embedding");
				List<Float> vector = new ArrayList<>(values.size());
				for (Number value : values) {
					vector.add(value.floatValue());
				}

				return vector;
			} catch (Exception e) {
				throw new RuntimeException("Error generating embeddings", e);
			}
		}

		private int dim() {
			Integer dim = ConfigUtil.getInstance().getIntSetting(CFG_MOD, "embedding_dim", null);
			if (dim != null && dim > 0) {
				return dim;
			}

			return embed("dimension_probe").size();
		}
	}

	private static class MilvusStore {
		private static final ObjectMapper MAPPER = new ObjectMapper();

		private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

		private volatile boolean collectionsReady = false;

		private String baseUrl() {
			return StringUtils.removeEnd(ConfigUtil.getInstance().getStrSetting(CFG_MOD, "milvus_url", ""), "/");
		}

		private String token() {
			return ConfigUtil.getInstance().getStrSetting(CFG_MOD, "milvus_token", "");
		}

		private String fieldsCollection() {
			return ConfigUtil.getInstance().getStrSetting(CFG_MOD, "milvus_fields_collection", "os_query_ctx_fields");
		}

		private String sourcesCollection() {
			return ConfigUtil.getInstance().getStrSetting(CFG_MOD, "milvus_sources_collection", "os_query_ctx_sources");
		}

		private int timeoutSecs() {
			return ConfigUtil.getInstance().getIntSetting(CFG_MOD, "milvus_timeout_secs", 30);
		}

		private void ensureCollections(int dim) {
			if (collectionsReady) {
				return;
			}

			synchronized (this) {
				if (collectionsReady) {
					return;
				}

				createCollection(fieldsCollection(), Arrays.asList(
					field("id", "VarChar", true, 64),
					field("ctx", "VarChar", false, 64),
					field("fieldId", "VarChar", false, 512),
					field("rootForm", "VarChar", false, 128),
					field("formCaption", "VarChar", false, 1024),
					field("fieldCaption", "VarChar", false, 1024),
					field("type", "VarChar", false, 64),
					field("sourceId", "VarChar", false, 64),
					vector("vector", dim)
				));

				createCollection(sourcesCollection(), Arrays.asList(
					field("id", "VarChar", true, 64),
					field("ctx", "VarChar", false, 64),
					field("sourceId", "VarChar", false, 64),
					field("sourceType", "VarChar", false, 32),
					field("value", "VarChar", false, 2048),
					vector("vector", dim)
				));

				collectionsReady = true;
			}
		}

		private void replaceContext(ContextKey ctx, List<FieldDef> fields, List<SourceDef> sources, EmbeddingClient embeddingClient) {
			clearContext(ctx);

			List<Map<String, Object>> fieldRows = new ArrayList<>();
			for (FieldDef field : fields) {
				Map<String, Object> row = new HashMap<>();
				row.put("id", DigestUtils.sha1Hex(ctx.ctx() + "|" + field.fieldId));
				row.put("ctx", ctx.ctx());
				row.put("fieldId", field.fieldId);
				row.put("rootForm", field.rootForm);
				row.put("formCaption", field.formCaption);
				row.put("fieldCaption", field.fieldCaption);
				row.put("type", field.type);
				row.put("sourceId", field.sourceId);
				row.put("vector", embeddingClient.embed(field.fieldId + " " + field.formCaption + " " + field.fieldCaption));
				fieldRows.add(row);
			}

			if (!fieldRows.isEmpty()) {
				upsert(fieldsCollection(), fieldRows);
			}

			List<Map<String, Object>> sourceRows = new ArrayList<>();
			for (SourceDef source : sources) {
				if (source.values == null || source.values.isEmpty()) {
					continue;
				}

				for (String value : source.values) {
					Map<String, Object> row = new HashMap<>();
					row.put("id", DigestUtils.sha1Hex(ctx.ctx() + "|" + source.id + "|" + value));
					row.put("ctx", ctx.ctx());
					row.put("sourceId", source.id);
					row.put("sourceType", source.type);
					row.put("value", value);
					row.put("vector", embeddingClient.embed(value));
					sourceRows.add(row);
				}
			}

			if (!sourceRows.isEmpty()) {
				upsert(sourcesCollection(), sourceRows);
			}
		}

		private List<Map<String, Object>> searchFields(ContextKey ctx, String text, int limit, EmbeddingClient embeddingClient) {
			Map<String, Object> payload = new HashMap<>();
			payload.put("collectionName", fieldsCollection());
			payload.put("annsField", "vector");
			payload.put("limit", limit);
			payload.put("outputFields", Arrays.asList("fieldId", "rootForm", "formCaption", "fieldCaption", "type", "sourceId"));
			payload.put("filter", "ctx == \"" + escape(ctx.ctx()) + "\"");
			payload.put("data", Collections.singletonList(embeddingClient.embed(text)));

			Map<String, Object> resp = post("/v2/vectordb/entities/search", payload, false);
			Object data = resp.get("data");
			if (data instanceof List) {
				return (List<Map<String, Object>>) data;
			}

			return Collections.emptyList();
		}

		private void clearContext(ContextKey ctx) {
			delete(fieldsCollection(), "ctx == \"" + escape(ctx.ctx()) + "\"");
			delete(sourcesCollection(), "ctx == \"" + escape(ctx.ctx()) + "\"");
		}

		private Map<String, Object> toEntity(Map<String, Object> row) {
			Object entity = row.get("entity");
			if (entity instanceof Map) {
				return (Map<String, Object>) entity;
			}

			return row;
		}

		private Map<String, Object> field(String name, String dataType, boolean primary, int maxLength) {
			Map<String, Object> field = new HashMap<>();
			field.put("fieldName", name);
			field.put("dataType", dataType);
			field.put("isPrimary", primary);
			field.put("maxLength", maxLength);
			return field;
		}

		private Map<String, Object> vector(String name, int dim) {
			Map<String, Object> field = new HashMap<>();
			field.put("fieldName", name);
			field.put("dataType", "FloatVector");
			field.put("dimension", dim);
			return field;
		}

		private void createCollection(String name, List<Map<String, Object>> fields) {
			Map<String, Object> payload = new HashMap<>();
			payload.put("collectionName", name);
			payload.put("schema", Collections.singletonMap("fields", fields));
			post("/v2/vectordb/collections/create", payload, true);
		}

		private void upsert(String collectionName, List<Map<String, Object>> data) {
			Map<String, Object> payload = new HashMap<>();
			payload.put("collectionName", collectionName);
			payload.put("data", data);
			post("/v2/vectordb/entities/upsert", payload, false);
		}

		private void delete(String collectionName, String filter) {
			Map<String, Object> payload = new HashMap<>();
			payload.put("collectionName", collectionName);
			payload.put("filter", filter);
			post("/v2/vectordb/entities/delete", payload, false);
		}

		private String escape(String value) {
			return StringUtils.defaultString(value).replace("\\", "\\\\").replace("\"", "\\\"");
		}

		private Map<String, Object> post(String path, Map<String, Object> payload, boolean ignoreErr) {
			try {
				HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
					.uri(URI.create(baseUrl() + path))
					.timeout(Duration.ofSeconds(timeoutSecs()))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(payload)));

				if (StringUtils.isNotBlank(token())) {
					reqBuilder.header("Authorization", "Bearer " + token());
				}

				HttpResponse<String> resp = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
				if (resp.statusCode() >= 400) {
					if (!ignoreErr) {
						throw new RuntimeException("Milvus HTTP error: " + resp.statusCode() + ", body: " + resp.body());
					}

					return Collections.emptyMap();
				}

				Map<String, Object> output = MAPPER.readValue(resp.body(), new TypeReference<Map<String, Object>>(){});
				Object code = output.get("code");
				if (!ignoreErr && code instanceof Number && ((Number) code).intValue() != 0) {
					throw new RuntimeException("Milvus operation failed. code=" + code + ", response=" + resp.body());
				}

				return output;
			} catch (Exception e) {
				if (!ignoreErr) {
					throw new RuntimeException("Error calling Milvus endpoint: " + path, e);
				}

				return Collections.emptyMap();
			}
		}
	}
}
