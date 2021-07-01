package com.redhat.labs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.json.bind.Jsonb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.labs.lodestar.model.Artifact;
import com.redhat.labs.lodestar.model.ArtifactCount;
import com.redhat.labs.lodestar.model.GetListOptions;
import com.redhat.labs.lodestar.model.GetOptions;
import com.redhat.labs.lodestar.service.ArtifactService;
import com.redhat.labs.mock.ExternalApiWireMock;
import com.redhat.labs.mock.ResourceLoader;
import com.redhat.labs.mongo.MongoTestResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
@QuarkusTestResource(ExternalApiWireMock.class)
class ArtifactServiceTest {

	@Inject
	ArtifactService artifactService;
	
	@Inject
	Jsonb jsonb;

	@BeforeEach
	void setUp() {
		artifactService.purge();
		artifactService.refresh();
	}

	@Test
	void testRefresh() {

		// then
		GetListOptions options = new GetListOptions();
		options.setEngagementUuid("1111");
		List<Artifact> artifacts = artifactService.getArtifacts(options);
		assertNotNull(artifacts);
		assertEquals(2, artifacts.size());

		options.setEngagementUuid("2222");
		artifacts = artifactService.getArtifacts(options);
		assertNotNull(artifacts);
		assertEquals(2, artifacts.size());

	}

	@Test
	void testModifyByEngagementIdUpdateInGit() {

		// given
		String engagementUuid = "1111";

		// one new
		Artifact newOne = Artifact.builder().engagementUuid(engagementUuid).description("a new artifact")
				.linkAddress("http://a-new-one").title("New One").type("typeOne").build();

		// modify one
		String json = ResourceLoader.load("project-1-artifacts.json");
		List<Artifact> artifacts = jsonb.fromJson(json, new ArrayList<Artifact>() {
			private static final long serialVersionUID = 1L;
		}.getClass().getGenericSuperclass());

		Artifact modifyOne = artifacts.get(0);
		modifyOne.setDescription("Updated");

		// when
		artifactService.process(Arrays.asList(newOne, modifyOne), Optional.empty(), Optional.empty());

		// then
		GetListOptions options = new GetListOptions();
		options.setEngagementUuid(engagementUuid);
		List<Artifact> updated = artifactService.getArtifacts(options);

		assertNotNull(updated);
		assertEquals(2, updated.size());

		updated.stream().forEach(a -> {
			if (a.getType().equals("Demo")) {
				assertEquals("Updated", a.getDescription());
			} else if (a.getType().equals("typeOne")) {
				assertNotNull(a.getUuid());
			}
		});

	}

	@Test
	void testModifyByEngagementIdCreateInGit() {

		// given
		String engagementUuid = "2222";

		// one new
		Artifact newOne = Artifact.builder().engagementUuid(engagementUuid).description("a new artifact")
				.linkAddress("http://a-new-one").title("New One").type("typeOne").build();

		// when
		artifactService.process(Arrays.asList(newOne), Optional.empty(), Optional.empty());

		// then
		GetListOptions options = new GetListOptions();
		options.setEngagementUuid(engagementUuid);
		List<Artifact> updated = artifactService.getArtifacts(options);

		assertNotNull(updated);
		assertEquals(1, updated.size());
		assertNotNull(updated.get(0).getUuid());
		assertEquals("typeOne", updated.get(0).getType());

	}

	@Test
	void testGetArtifactsNoOptions() {

		// given
		GetListOptions options = new GetListOptions();
		
		// when
		List<Artifact> artifacts = artifactService.getArtifacts(options);
		
		// then
		assertNotNull(artifacts);
		assertEquals(4, artifacts.size());

	}

	@Test
	void testGetArtifactsByEngagement() {

		// given
		GetListOptions options = new GetListOptions();
		options.setEngagementUuid("1111");
		
		// when
		List<Artifact> artifacts = artifactService.getArtifacts(options);
		
		// then
		assertNotNull(artifacts);
		assertEquals(2, artifacts.size());

	}

	@Test
	void testGetArtifactsPaging() {

		// given
		GetListOptions options = new GetListOptions();
		options.setPage(0);
		options.setPageSize(3);
		
		// when
		List<Artifact> artifacts = artifactService.getArtifacts(options);
		
		// then
		assertNotNull(artifacts);
		assertEquals(3, artifacts.size());

		//given
		options.setPage(1);
		
		// when
		artifacts = artifactService.getArtifacts(options);
		
		// then
		assertNotNull(artifacts);
		assertEquals(1, artifacts.size());
		

	}

	@Test
	void testCountArtifactsNoOptions() {

		// given
		GetOptions options = new GetOptions();
		
		// when
		ArtifactCount count = artifactService.countArtifacts(options);

		// then
		assertNotNull(count);
		assertEquals(4, count.getCount());

	}

	@Test
	void testCountArtifactsByEngagement() {

		// given
		GetOptions options = new GetOptions("1111");
		
		// when
		ArtifactCount count = artifactService.countArtifacts(options);

		// then
		// then
		assertNotNull(count);
		assertEquals(2, count.getCount());

	}

}
