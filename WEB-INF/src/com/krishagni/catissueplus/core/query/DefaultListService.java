package com.krishagni.catissueplus.core.query;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

public class DefaultListService implements ListService {
	private Map<String, Function<Map<String, Object>, ListConfig>> listConfigFns = new HashMap<>();

	private ListGenerator listGenerator;

	public void setListGenerator(ListGenerator listGenerator) {
		this.listGenerator = listGenerator;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ListConfig> getListCfg(RequestEvent<Map<String, Object>> req) {
		try {
			ListConfig cfg = getListConfig(req.getPayload());
			cfg.setFilters(listGenerator.getFilters(cfg));
			return ResponseEvent.response(cfg);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ListDetail> getList(RequestEvent<Map<String, Object>> req) {
		try {
			Map<String, Object> params = req.getPayload();
			ListConfig cfg = getListConfig(params);
			ListDetail list = listGenerator.getList(
				cfg,
				(List<Column>)params.get("filters"),
				(Column)params.get("orderBy")
			);

			if (cfg.getFixedColumnsGenerator() != null) {
				Map<String, Object> resp = cfg.getFixedColumnsGenerator().apply(params, list.getRows());
				if (resp != null && Boolean.TRUE.equals(resp.getOrDefault("noFixedColumns", false))) {
					list.setFixedColumns(null);
				} else if (resp != null) {
					list.setRows((List<Row>) resp.get("rows"));
				}
			}

			return ResponseEvent.response(list);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Integer> getListSize(RequestEvent<Map<String, Object>> req) {
		try {
			Map<String, Object> params = req.getPayload();
			ListConfig cfg = getListConfig(params);
			return ResponseEvent.response(listGenerator.getListSize(cfg, (List<Column>)params.get("filters")));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Collection<Object>> getListExprValues(RequestEvent<Map<String, Object>> req) {
		try {
			Map<String, Object> params = req.getPayload();
			ListConfig cfg = getListConfig(params);

			String expr = (String)params.get("expr");
			String searchTerm = (String)params.get("searchTerm");
			return ResponseEvent.response(listGenerator.getExpressionValues(cfg, expr, searchTerm));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public void registerListConfigurator(String name, Function<Map<String, Object>, ListConfig> configFn) {
		listConfigFns.put(name, configFn);
	}

	private ListConfig getListConfig(Map<String, Object> params) {
		String listName = (String) params.get("listName");
		if (StringUtils.isBlank(listName)) {
			throw OpenSpecimenException.userError(ListError.NAME_REQ);
		}

		Function<Map<String, Object>, ListConfig> configFn = listConfigFns.get(listName);
		if (configFn == null) {
			throw OpenSpecimenException.userError(ListError.INVALID, listName);
		}

		ListConfig cfg = configFn.apply(params);
		if (cfg == null) {
			throw OpenSpecimenException.userError(ListError.NO_CFG, listName);
		}

		return cfg;
	}
}
