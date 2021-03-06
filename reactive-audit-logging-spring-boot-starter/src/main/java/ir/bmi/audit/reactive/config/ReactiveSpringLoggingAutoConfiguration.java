package ir.bmi.audit.reactive.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.net.ssl.KeyStoreFactoryBean;
import ch.qos.logback.core.net.ssl.SSLConfiguration;
import ir.bmi.audit.reactive.util.UniqueIDGenerator;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import ir.bmi.audit.reactive.filter.ReactiveSpringLoggingFilter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "logging.logstash")
public class ReactiveSpringLoggingAutoConfiguration {

	private static final String LOGSTASH_APPENDER_NAME = "LOGSTASH";

	private String url = "localhost:8500";
	private String ignorePatterns;
	private boolean logHeaders;
	private boolean useContentLength = true;
	private String trustStoreLocation;
	private String trustStorePassword;
	@Value("${spring.application.name:-}")
	String name;

	@Bean
	public UniqueIDGenerator generator() {
		return new UniqueIDGenerator();
	}

	@Bean
	public ReactiveSpringLoggingFilter reactiveSpringLoggingFilter() {
		return new ReactiveSpringLoggingFilter(generator(), ignorePatterns, logHeaders, useContentLength);
	}
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getIgnorePatterns() {
		return ignorePatterns;
	}

	public void setIgnorePatterns(String ignorePatterns) {
		this.ignorePatterns = ignorePatterns;
	}

	public boolean isLogHeaders() {
		return logHeaders;
	}

	public void setLogHeaders(boolean logHeaders) {
		this.logHeaders = logHeaders;
	}

	public boolean isUseContentLength() {
		return useContentLength;
	}

	public void setUseContentLength(boolean useContentLength) {
		this.useContentLength = useContentLength;
	}

	public String getTrustStoreLocation() {
		return trustStoreLocation;
	}

	public void setTrustStoreLocation(String trustStoreLocation) {
		this.trustStoreLocation = trustStoreLocation;
	}

	public String getTrustStorePassword() {
		return trustStorePassword;
	}

	public void setTrustStorePassword(String trustStorePassword) {
		this.trustStorePassword = trustStorePassword;
	}
}
