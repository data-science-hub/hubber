package net.datasciencehub.hubber;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HubberConf {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private static HubberConf obj = new HubberConf();

	public static HubberConf get() {
		return obj;
	}

	private Properties conf;

	private HubberConf() {
		conf = new Properties();
		loadProperties("conf.properties");
		loadProperties("local.conf.properties");
	}

	private void loadProperties(String fileName) {
		InputStream in = null;
		try {
			in = HubberConf.class.getResourceAsStream(fileName);
			try {
				conf.load(in);
			} catch (IOException ex) {
				logger.error(ex.getMessage(), ex);
				System.exit(1);
			}
		} finally {
			close(in);
		}
	}

	private void close(InputStream st) {
		if (st == null) return;
		try {
			st.close();
		} catch (IOException ex) {
			logger.error(ex.getMessage(), ex);
		}
	}

	public String get(String name) {
		return conf.getProperty(name);
	}

}
