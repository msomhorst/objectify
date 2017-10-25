package com.googlecode.objectify.test.util;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

/**
 * Sets up and tears down the GAE local unit test harness environment
 */
public class GAEExtension implements BeforeEachCallback, AfterEachCallback {

	private static final Namespace NAMESPACE = Namespace.create(GAEExtension.class);

	@Override
	public void beforeEach(final ExtensionContext context) throws Exception {

		final LocalServiceTestHelper helper =
				new LocalServiceTestHelper(
						// Our tests assume strong consistency
						new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy(),
						new LocalMemcacheServiceTestConfig());

		context.getStore(NAMESPACE).put(LocalServiceTestHelper.class, helper);

		helper.setUp();
	}

	@Override
	public void afterEach(final ExtensionContext context) throws Exception {
		final LocalServiceTestHelper helper = context.getStore(NAMESPACE).get(LocalServiceTestHelper.class, LocalServiceTestHelper.class);

		helper.tearDown();
	}
}