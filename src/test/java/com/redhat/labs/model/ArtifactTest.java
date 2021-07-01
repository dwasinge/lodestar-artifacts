package com.redhat.labs.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.labs.lodestar.model.Artifact;
import com.redhat.labs.lodestar.service.ArtifactService;
import com.redhat.labs.mock.ExternalApiWireMock;
import com.redhat.labs.mongo.MongoTestResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
@QuarkusTestResource(ExternalApiWireMock.class)
class ArtifactTest {

	@Inject
	ArtifactService service;

	@BeforeEach
	void setup() {
		service.purge();
		service.refresh();
	}

	@Test
	void testCountAllArtifacts() {
		assertEquals(4, Artifact.countAllArtifacts().getCount());
	}

	@Test
	void testCountArtifactsByEngagementUuid() {
		assertEquals(2, Artifact.countArtifactsByEngagementUuid("1111").getCount());
	}

	@Test
	void testPagedArtifacts() {
		assertEquals(1, Artifact.pagedArtifacts(0, 1).size());
	}

	@Test
	void testPagedArtifactsByEngagementUuid() {
		assertEquals(1, Artifact.pagedArtifactsByEngagementUuid("1111", 0, 1).size());
	}

	@Test
	void testFindAllByEngagementUuid() {
		assertEquals(2, Artifact.findAllByEngagementUuid("1111").size());
	}

	@Test
	void testFindByUuid() {

		List<Artifact> artifacts = Artifact.findAllByEngagementUuid("1111");
		String uuid = artifacts.get(0).getUuid();

		assertTrue(Artifact.findByUuid(uuid).isPresent());

	}

	@Test
	void testDeleteByUuid() {

		List<Artifact> artifacts = Artifact.findAllByEngagementUuid("1111");
		String uuid = artifacts.get(0).getUuid();

		Artifact.deleteByUuid(uuid);

		assertTrue(Artifact.findByUuid(uuid).isEmpty());

	}

	@Test
	void testRemoveAllArtifacts() {
		assertEquals(4, Artifact.removeAllArtifacts());
	}

}
