package com.krishagni.catissueplus.core.init;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.InitializingBean;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Status;

public class SpecimenUnitsLoader implements InitializingBean {
	private static final LogUtil logger = LogUtil.getLogger(SpecimenUnitsLoader.class);

	private SessionFactory sessionFactory;

	private DaoFactory daoFactory;

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		loadUnits();
	}

	@PlusTransactional
	private int loadUnits() {
		int count = getUnitsCount();
		if (count > 0) {
			logger.info(count + " specimen units pre-loaded. Stopping the loader");
			return count;
		}


		Set<String> addedUnits = new HashSet<>();
		List<Pair<String, String>> spmnTypeUnits = getSpecimenTypeUnits();
		for (Pair<String, String> spmnTypeUnit : spmnTypeUnits) {
			String unit = StringUtils.isBlank(spmnTypeUnit.second()) ? spmnTypeUnit.first() : spmnTypeUnit.second();
			if (StringUtils.isBlank(unit) || !addedUnits.add(unit)) {
				continue;
			}

			PermissibleValue pv = new PermissibleValue();
			pv.setAttribute(SPECIMEN_UNIT_PV);
			pv.setValue(unit);
			pv.setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
			daoFactory.getPermissibleValueDao().saveOrUpdate(pv);
		}

		logger.info("Added " + addedUnits.size() + " specimen units.");
		return addedUnits.size();
	}

	private int getUnitsCount() {
		Number count = (Number) sessionFactory.getCurrentSession().createNativeQuery(GET_SPMN_UNITS_COUNT_SQL).uniqueResult();
		return count == null ? 0 : count.intValue();
	}

	private List<Pair<String, String>> getSpecimenTypeUnits() {
		List<Pair<String, String>> result = new ArrayList<>();

		List<Object[]> rows = sessionFactory.getCurrentSession().createNativeQuery(GET_SPMN_TYPE_UNITS_SQL).list();
		for (Object[] row : rows) {
			result.add(Pair.make((String) row[0], (String) row[1]));
		}

		return result;
	}

	private static final String SPECIMEN_UNIT_PV = "specimen_unit";

	private static final String GET_SPMN_UNITS_COUNT_SQL = "select count(*) from catissue_permissible_value where public_id = 'specimen_unit'";

	private static final String GET_SPMN_TYPE_UNITS_SQL =
		"select " +
		"  u.value as unit_name, s.value as unit_symbol " +
		"from " +
		"  os_pv_props u " +
		"  left join os_pv_props s " +
		"    on s.pv_id = u.pv_id and " +
		"    (" +
		"      (u.name = 'quantity_unit' and s.name = 'quantity_display_unit') or " +
		"      (u.name = 'concentration_unit' and s.name = 'concentration_display_unit')" +
		"    ) " +
		"where " +
		"  u.name in ('quantity_unit', 'concentration_unit')";
}
