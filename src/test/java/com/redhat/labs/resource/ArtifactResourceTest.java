package com.redhat.labs.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.json.bind.Jsonb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.labs.lodestar.model.Artifact;
import com.redhat.labs.lodestar.model.GetListOptions;
import com.redhat.labs.lodestar.service.ArtifactService;
import com.redhat.labs.mock.ExternalApiWireMock;
import com.redhat.labs.mongo.MongoTestResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
@QuarkusTestResource(ExternalApiWireMock.class)
class ArtifactResourceTest {

	@Inject
	ArtifactService service;

	@Inject
	Jsonb jsonb;

	@BeforeEach
	void setup() {
		service.refresh();
	}

	@Test
	void testCountArtifactsAll() {

		given().when().get("/api/artifacts/count").then().statusCode(200).body("count", equalTo(4));

	}

	@Test
	void testCountArtifactsByEngagement() {

		given().queryParam("engagementUuid", "1111").when().get("/api/artifacts/count").then().statusCode(200)
				.body("count", equalTo(2));

	}

	@Test
	void testGetArtifactsAll() {

		JsonPath path = given().when().get("/api/artifacts").then().statusCode(200).extract().jsonPath();
		assertEquals(4, path.getList(".").size());

	}

	@Test
	void testGetArtifactsAllPaged() {

		// first page
		JsonPath path = given().queryParam("page", 0).queryParam("pageSize", 3).when().get("/api/artifacts").then()
				.statusCode(200).extract().jsonPath();
		assertEquals(3, path.getList(".").size());

		// last page
		path = given().queryParam("page", 1).queryParam("pageSize", 3).when().get("/api/artifacts").then()
				.statusCode(200).extract().jsonPath();
		assertEquals(1, path.getList(".").size());

		// page with no results
		path = given().queryParam("page", 2).queryParam("pageSize", 3).when().get("/api/artifacts").then()
				.statusCode(200).extract().jsonPath();
		assertEquals(0, path.getList(".").size());

	}

	@Test
	void testGetArtifactsByEngagement() {

		JsonPath path = given().queryParam("engagementUuid", "1111").when().get("/api/artifacts").then()
				.statusCode(200).extract().jsonPath();
		assertEquals(2, path.getList(".").size());

	}

	@Test
	void testGetArtifactsByUnknownEngagement() {

		JsonPath path = given().queryParam("engagementUuid", "xxx").when().get("/api/artifacts").then()
				.statusCode(200).extract().jsonPath();
		assertEquals(0, path.getList(".").size());

	}

	@Test
	void testModifyArtifactsMissingArtifactId() {

		Artifact a = mockArtifact("1111");
		a.setTitle(null);
		String requestBody = jsonb.toJson(Arrays.asList(a));

		given().contentType(ContentType.JSON).body(requestBody).post("/api/artifacts").then().statusCode(400);

	}

	@Test
	void testModifyArtifactsMissingDescription() {

		Artifact a = mockArtifact("1111");
		a.setDescription(null);
		String requestBody = jsonb.toJson(Arrays.asList(a));

		given().contentType(ContentType.JSON).body(requestBody).post("/api/artifacts").then().statusCode(400);

	}

	@Test
	void testModifyArtifactsMissingType() {

		Artifact a = mockArtifact("1111");
		a.setType(null);
		String requestBody = jsonb.toJson(Arrays.asList(a));

		given().contentType(ContentType.JSON).body(requestBody).post("/api/artifacts").then().statusCode(400);

	}

	@Test
	void testModifyArtifactsMissingLinkAddress() {

		Artifact a = mockArtifact("1111");
		a.setLinkAddress(null);
		String requestBody = jsonb.toJson(Arrays.asList(a));

		given().contentType(ContentType.JSON).body(requestBody).post("/api/artifacts").then().statusCode(400);

	}

	@Test
	void testModifyArtifactsMissingEngagementUuid() {

		Artifact a = mockArtifact(null);
		String requestBody = jsonb.toJson(Arrays.asList(a));

		given().contentType(ContentType.JSON).body(requestBody).post("/api/artifacts").then().statusCode(400);

	}

	@Test
	void testModifyArtifactsCreateUpdateDelete() {

		// get existing artifact
		GetListOptions options = new GetListOptions();
		options.setEngagementUuid("1111");
		List<Artifact> artifacts = service.getArtifacts(options);
		assertNotNull(artifacts);
		assertFalse(artifacts.isEmpty());
		Artifact modified = artifacts.get(0);
		modified.setDescription("UPDATED");

		// new artifacts
		Artifact a1 = mockArtifact("1111");
		Artifact a2 = mockArtifact("2222");

		String requestBody = jsonb.toJson(Arrays.asList(a1, a2, modified));

		given().contentType(ContentType.JSON).body(requestBody).post("/api/artifacts").then().statusCode(200);

		JsonPath path = given().queryParam("engagementUuid", "1111").when().get("/api/artifacts").then().statusCode(200).extract().jsonPath();
		assertEquals(2, path.getList(".").size());
		assertEquals("newType", path.get("[1].type"));
		assertEquals("UPDATED", path.get("[0].description"));

		path = given().queryParam("engagementUuid", "2222").when().get("/api/artifacts").then().statusCode(200).extract().jsonPath();
		assertEquals(1, path.getList(".").size());
		assertEquals("newType", path.get("[0].type"));

	}
	
	Artifact mockArtifact(String engagementUuid) {
		return Artifact.builder().type("newType").title("New Artifact").linkAddress("http://new-artifact")
				.description("a new artifact").engagementUuid(engagementUuid).build();
	}

}
