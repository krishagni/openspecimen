//package krishagni.catissueplus.upgrade;
//
//import java.util.Properties;
//
//import javax.sql.DataSource;
//
//import org.apache.commons.dbcp.BasicDataSource;
//
//public class DbUtil {
//	public static String dbType = null;
//
//	public static synchronized DataSource getDataSource()
//	throws Exception {
//		Properties prop = UpgradeUtil.loadInstallProps();
//		return getDataSource(prop);
//	}
//
//	public static synchronized DataSource getDataSource(Properties prop)
//	throws Exception{
//		BasicDataSource ds = new BasicDataSource();
//
//		String driverClass = null;
//		String databaseType = null;
//		String separator = null;
//		dbType = databaseType = prop.getProperty("database.type");
//
//		if (databaseType.equals("mysql")) {
//			driverClass = "com.mysql.jdbc.Driver";
//			databaseType = databaseType + "://";
//			separator = "/";
//
//		} else if (databaseType.equals("oracle")) {
//			driverClass = "oracle.jdbc.OracleDriver";
//			databaseType = databaseType + ":thin:@";
//			separator = ":";
//		}
//
//
//		ds.setDriverClassName(driverClass);
//		ds.setUsername(prop.getProperty("database.username"));
//		ds.setPassword(prop.getProperty("database.password"));
//
//		String jdbcUrl = "jdbc:" + databaseType +  prop.getProperty("database.host")
//							+ ":" + prop.getProperty("database.port") + separator + prop.getProperty("database.name");
//		ds.setUrl(jdbcUrl);
//		return ds;
//	}
//
//	public static boolean isOracle() {
//		return dbType != null && dbType.equals("oracle");
//	}
//
//	public static boolean isMySQL() {
//		return dbType != null && dbType.equals("mysql");
//	}
//}
