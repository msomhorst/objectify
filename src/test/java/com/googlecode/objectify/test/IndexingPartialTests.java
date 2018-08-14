/*
 */

package com.googlecode.objectify.test;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Value;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Unindex;
import com.googlecode.objectify.condition.IfFalse;
import com.googlecode.objectify.condition.PojoIf;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

/**
 * Tests of partial indexing.  Doesn't stress test the If mechanism; that is
 * checked in the NotSavedTests.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class IndexingPartialTests extends TestBase {
	/** */
	private static final String TEST_VALUE = "blah";

	/** */
	@Entity
	@Cache
	@Index
	@Data
	private static class UnindexedWhenFalse {
		@Id Long id;
		@Unindex(IfFalse.class) boolean foo;
	}

	/** */
	@Test
	void basicPartialIndexing() throws Exception {
		factory().register(UnindexedWhenFalse.class);

		final UnindexedWhenFalse thing = new UnindexedWhenFalse();

		// Should be able to query for it when true
		thing.foo = true;
		ofy().save().entity(thing).now();
		assertThat(ofy().load().type(UnindexedWhenFalse.class).filter("foo", true).first().now()).isEqualTo(thing);

		// Should not be able to query for it when false
		thing.foo = false;
		ofy().save().entity(thing).now();
		assertThat(ofy().load().type(UnindexedWhenFalse.class).filter("foo", true)).isEmpty();
	}

	/** */
	private static class IfComplicated extends PojoIf<IndexedOnOtherField> {
		@Override
		public boolean matchesPojo(IndexedOnOtherField pojo)
		{
			return pojo.indexBar;
		}
	}

	/** */
	@Entity
	@Cache
	@Unindex
	@Data
	private static class IndexedOnOtherField {
		@Id Long id;
		public boolean indexBar;
		public @Index(IfComplicated.class) boolean bar;
	}

	/** */
	@Test
	void partialIndexingWithAComplexMatcher() throws Exception {
		factory().register(IndexedOnOtherField.class);

		final IndexedOnOtherField thing = new IndexedOnOtherField();
		thing.bar = true;

		// Should be able to query for bar when true
		thing.indexBar = true;
		ofy().save().entity(thing).now();
		assertThat(ofy().load().type(IndexedOnOtherField.class).filter("bar", true).first().now()).isEqualTo(thing);

		// Should not be able to query for bar when false
		thing.indexBar = false;
		ofy().save().entity(thing).now();
		assertThat(ofy().load().type(IndexedOnOtherField.class).filter("bar", true)).isEmpty();
	}

	/** */
	@Entity
	@Cache
	@Data
	private static class EmbeddedEntity {
		@Id Long id;
		boolean foo;
	}

	/** */
	@Entity
	@Cache
	@Index
	@Data
	private static class UnindexEmbeddedEntity {
		@Id Long id;
		@Unindex EmbeddedEntity embeddedEntity;
	}

	/** */
	@Test
	void unindexEmbeddedEntity() throws Exception {
		factory().register(EmbeddedEntity.class);
		factory().register(UnindexEmbeddedEntity.class);

		final EmbeddedEntity embeddedEntity = new EmbeddedEntity();
		embeddedEntity.id = 1L;
		embeddedEntity.foo = true;
		final UnindexEmbeddedEntity thing = new UnindexEmbeddedEntity();
		thing.id = 2L;
		thing.embeddedEntity = embeddedEntity;

		ofy().save().entity(thing).now();

		Datastore datastore = ObjectifyService.factory().datastore();
		com.google.cloud.datastore.Entity datastoreEntity = datastore.get(datastore.newKeyFactory().setKind("UnindexEmbeddedEntity").newKey(2L));
		Value<?> datastoreEmbeddedEntity = datastoreEntity.getValue("embeddedEntity");
		assertNotNull(datastoreEmbeddedEntity);
		/**
		 * The following line fails. Apparently, the embedded entity is indexed, dispite the @Unindex annotation
		 */
		assertTrue(datastoreEmbeddedEntity.excludeFromIndexes());
	}
}