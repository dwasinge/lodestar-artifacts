package com.redhat.labs.mock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import java.util.HashMap;
import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class ExternalApiWireMock implements QuarkusTestResourceLifecycleManager {

	private WireMockServer wireMockServer;

	@Override
	public Map<String, String> start() {

		wireMockServer = new WireMockServer();
		wireMockServer.start();

		// project trees

		String body = ResourceLoader.load("project-1-tree.json");

		stubFor(get(urlEqualTo("/api/v4/projects/1/repository/tree?recursive=false"))
				.willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));

		body = ResourceLoader.load("project-2-tree.json");

		stubFor(get(urlEqualTo("/api/v4/projects/2/repository/tree?recursive=false"))
				.willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));

		// artifacts.json

		body = ResourceLoader.load("project-1-artifacts-file.json");

		stubFor(get(urlEqualTo("/api/v4/projects/1/repository/files/artifacts.json?ref=master"))
				.willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));

		stubFor(get(urlEqualTo("/api/v4/projects/2/repository/files/artifacts.json?ref=master"))
				.willReturn(aResponse().withStatus(404)));

		// engagement.json

		body = ResourceLoader.load("project-2-engagement-file.json");

		stubFor(get(urlEqualTo("/api/v4/projects/2/repository/files/engagement.json?ref=master"))
				.willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));

		// projects by group

		body = ResourceLoader.load("projects-by-group-1234.json");

		stubFor(get(urlEqualTo("/api/v4/groups/1234/projects?include_subgroups=true&per_page=20&page=1"))
				.willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));

		stubFor(get(urlEqualTo("/api/v4/groups/1234/projects?include_subgroups=true&per_page=20&page=2"))
				.willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("[]")));

		// projects by engagement

		body = ResourceLoader.load("project-by-engagement-1.json");

		stubFor(get(urlEqualTo("/api/v4/groups/1234/"))
				.willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));

		// search project by engagement uuid

		body = ResourceLoader.load("project-by-engagement-1.json");

		stubFor(get(urlEqualTo("/api/v4/groups/1234/search?scope=projects&search=1111"))
				.willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));

		body = ResourceLoader.load("project-by-engagement-2.json");

		stubFor(get(urlEqualTo("/api/v4/groups/1234/search?scope=projects&search=2222"))
				.willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));


		// update existing artifacts.json

		stubFor(put(urlEqualTo("/api/v4/projects/1/repository/files/artifacts.json"))
				.willReturn(aResponse().withHeader("Content-Type", "application/json").withStatus(200)));

		stubFor(post(urlEqualTo("/api/v4/projects/2/repository/files/artifacts.json"))
				.willReturn(aResponse().withHeader("Content-Type", "application/json").withStatus(200)));

		
		// set endpoint

		Map<String, String> config = new HashMap<>();
		config.put("gitlab.api/mp-rest/url", wireMockServer.baseUrl());

		return config;

	}

	@Override
	public void stop() {

		if (null != wireMockServer) {
			wireMockServer.stop();
		}

	}

}
