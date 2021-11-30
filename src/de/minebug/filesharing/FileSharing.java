package de.minebug.filesharing;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.minebug.filesharing.web.HTTPserver;
import de.theholyexception.holyapi.datastorage.dataconnection.interfaces.MySQLInterface;
import de.theholyexception.holyapi.util.LoggingManager;
import me.kaigermany.utilitys.logging.ConsoleLogInjector;
import me.kaigermany.utilitys.logging.ConsoleType;

public class FileSharing {
	
	public static final String  PATH_CONFIG = "filesharing.properties";
	
	private static Logger logger;
	private static Properties config;
	private static LoggingManager loggingManager;
	private static FileInterface fileManager;

	public static void main(String[] args) {

		ConsoleLogInjector.inject(ConsoleType.OUT, ConsoleLogInjector.createSimpleTimeStampOperator_extended());
		ConsoleLogInjector.inject(ConsoleType.ERR, ConsoleLogInjector.createSimpleTimeStampOperator_extended());
		
		loggingManager = new LoggingManager(new File(".\\logs\\"), "FileSharing");
		logger = loggingManager.getLogger();
		
		//region configurations
		try {
			File configFile = new File(PATH_CONFIG);
			if (!configFile.exists()) {
				logger.log(Level.INFO, "No config file, creating new one");
				createNewConfigFile(configFile);
			}
			config = new Properties();
			FileInputStream fis = new FileInputStream(configFile);
			config.load(fis);
			fis.close();
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.log(Level.SEVERE, "Exception during initialize Config");
		}
		
		//endregion

		MySQLInterface sql = new MySQLInterface(
				config.getProperty("sql.host"), 
				Integer.parseInt(config.getProperty("sql.port")), 
				config.getProperty("sql.username"), 
				config.getProperty("sql.password"), 
				config.getProperty("sql.database")
				);
		
		
		sql.setLogger(logger).asyncDataSettings(1).connect();
		
		sql.execute(""
				+ ""
				+ "CREATE TABLE IF NOT EXISTS `files` ("
				+ "`szKey` VARCHAR(8) NOT NULL,"
				+ "`szFilename` VARCHAR(256) NOT NULL,"
				+ "`szContentType` VARCHAR(200) NOT NULL,"
				+ "`nContentLength` INT(11) NOT NULL,"
				+ "`tCreated` datetime DEFAULT current_timestamp(),"
				+ "`tValid` datetime DEFAULT '9999-12-31 23:59:59',"
				+ "PRIMARY KEY (`szKey`)"
				+ ");"
				+ ""
				);
		
		String storageMode = config.getProperty("datastorage.mode");
		if (storageMode.equalsIgnoreCase("multi")) {
			fileManager = new FileManagerMulti(sql, new File(config.getProperty("datastorage.multi.location")));
		} else if (storageMode.equalsIgnoreCase("single")) {
			fileManager = new FileManagerSingle(sql, new File(config.getProperty("datastorage.single.location")));
		} else throw new IllegalStateException("Invalid storagemode: " + storageMode);
		
		new HTTPserver(80);
	}
	
	private static void createNewConfigFile(File configFile) throws IOException {
		try {
			configFile.createNewFile();			
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream("config-default.properties"));
			byte[] data = bis.readAllBytes();
			bis.close();
			
			FileOutputStream out = new FileOutputStream(configFile);
			out.write(data);
			out.close();
		} catch (IOException ex) {
			ex.printStackTrace();
			logger.log(Level.SEVERE, "Exception during initialize Config: Failed to clone default config!");
			if (configFile.exists()) Files.delete(configFile.toPath());
		}
	}
	
	public static FileInterface getFileManager() {
		return fileManager;
	}
	
	public static Logger getLogger() {
		return logger;
	}
	
}
