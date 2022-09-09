//package krishagni.catissueplus.upgrade;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Properties;
//
//import javax.sql.DataSource;
//
//
//import com.krishagni.catissueplus.core.common.util.LogUtil;
//
//import edu.common.dynamicextensions.domain.nui.UserContext;
//import edu.common.dynamicextensions.ndao.JdbcDao;
//import edu.common.dynamicextensions.ndao.JdbcDaoFactory;
//import edu.common.dynamicextensions.ndao.ResultExtractor;
//import edu.common.dynamicextensions.nutility.DeConfiguration;
//
//public class UpgradeUtil {
//
//	public static UserContext getUserCtx(final String username)
//	throws Exception {
//		final Long userId = getUserId(username);
//		if (userId == null) {
//			return null;
//		}
//
//		return new UserContext() {
//			@Override
//			public String getUserName() {
//				return username;
//			}
//
//			@Override
//			public Long getUserId() {
//				return userId;
//			}
//
//			@Override
//			public String getIpAddress() {
//				return null;
//			}
//		};
//	}
//
//	public static Long getUserId(String username)
//	throws Exception {
//		JdbcDao jdbcDao = JdbcDaoFactory.getJdbcDao();
//		List<Object> params = new ArrayList<Object>();
//		params.add(username);
//
//		Long userId = jdbcDao.getResultSet(GET_USER_ID_SQL, params, new ResultExtractor<Long>() {
//			@Override
//			public Long extract(ResultSet rs) throws SQLException {
//				return rs.next() ? rs.getLong(1) : null;
//			}
//		});
//
//		if (userId == null) {
//			logger.error("No active user with login name: " + username);
//		}
//
//		return userId;
//	}
//
//	public static void initializeDE(Properties prop, DataSource ds) {
//		String dir = new StringBuilder(prop.getProperty("jboss.home.dir")).append(File.separator)
//				.append("os-data").append(File.separator)
//				.append("de-file-data").append(File.separator)
//				.toString();
//		File dirFile = new File(dir);
//
//		if (!dirFile.exists()) {
//			if (!dirFile.mkdirs()) {
//				throw new RuntimeException("Error couldn't create directory for storing de file data");
//			}
//		}
//
//		DeConfiguration.getInstance()
//			.dataSource(ds, null)
//			.fileUploadDir(dir);
//	}
//
//	public static Properties loadInstallProps() {
//		try {
//			Properties prop = new Properties();
//			prop.load(new FileInputStream("caTissueInstall.properties"));
//			return prop;
//		} catch (Exception e) {
//			throw new RuntimeException("Error loading install properties file", e);
//		}
//	}
//
//	private static final String GET_USER_ID_SQL =
//			"select" +
//			"	identifier " +
//			"from catissue_user " +
//			"	where login_name = ? AND activity_status = 'Active'";
//
//	private static final LogUtil logger = LogUtil.getLogger(UpgradeUtil.class);
//}
