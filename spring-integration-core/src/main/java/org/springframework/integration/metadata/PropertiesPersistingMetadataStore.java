/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.metadata;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.DefaultPropertiesPersister;

/**
 * Properties file-based implementation of {@link MetadataStore}. To avoid conflicts
 * each instance should be constructed with the unique key from which unique file name
 * will be generated. The file name will be 'persistentKey' + ".last.entry".
 * Files will be written to the 'java.io.tmpdir' +  "/spring-integration/".
 *
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class PropertiesPersistingMetadataStore implements MetadataStore, InitializingBean, DisposableBean {

	private final Log logger = LogFactory.getLog(getClass());

	private final Properties metadata = new Properties();

	private final DefaultPropertiesPersister persister = new DefaultPropertiesPersister();

	private volatile File file;

	private volatile String baseDirectory = System.getProperty("java.io.tmpdir") + "/spring-integration/";


	public void setBaseDirectory(String baseDirectory) {
		Assert.hasText(baseDirectory, "'baseDirectory' must be non-empty");
		this.baseDirectory = baseDirectory;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		File baseDir = new File(baseDirectory);
		baseDir.mkdirs();
		this.file = new File(baseDir, "metadata-store.properties");
		try {
			if (!this.file.exists()) {
				this.file.createNewFile();
			}
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Failed to create metadata-store file '"
					+ this.file.getAbsolutePath() + "'", e);
		}
		this.loadMetadata();
	}

	@Override
	public void put(String key, String value) {
		this.metadata.setProperty(key, value);
	}

	@Override
	public String get(String key) {
		return this.metadata.getProperty(key);
	}

	@Override
	public String remove(String key) {
		return (String) this.metadata.remove(key);
	}

	@Override
	public void destroy() throws Exception {
		this.saveMetadata();
	}

	private void saveMetadata() {
		OutputStream outputStream = null;
		try {
			outputStream = new BufferedOutputStream(new FileOutputStream(this.file));
			this.persister.store(this.metadata, outputStream, "Last entry");
		}
		catch (IOException e) {
			// not fatal for the functionality of the component
			logger.warn("Failed to persist entry. This may result in a duplicate "
					+ "entry after this component is restarted.", e);
		}
		finally {
			try {
				if (outputStream != null) {
					outputStream.close();
				}
			}
			catch (IOException e) {
				// not fatal for the functionality of the component
				logger.warn("Failed to close OutputStream to " + this.file.getAbsolutePath(), e);
			}
		}
	}

	private void loadMetadata() {
		InputStream inputStream = null;
		try {
			inputStream = new BufferedInputStream(new FileInputStream(this.file));
			this.persister.load(this.metadata, inputStream);
		}
		catch (Exception e) {
			// not fatal for the functionality of the component
			logger.warn("Failed to load entry from the persistent store. This may result in a duplicate " +
					"entry after this component is restarted", e);
		}
		finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
			}
			catch (Exception e2) {
				// non fatal
				logger.warn("Failed to close InputStream for: " + this.file.getAbsolutePath());
			}
		}
	}

}
