package com.redhat.labs.lodestar.resource;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.redhat.labs.lodestar.model.Artifact;
import com.redhat.labs.lodestar.model.ArtifactCount;
import com.redhat.labs.lodestar.model.GetListOptions;
import com.redhat.labs.lodestar.model.GetOptions;
import com.redhat.labs.lodestar.service.ArtifactService;

@Path("/api/artifacts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ArtifactResource {

	@Inject
	ArtifactService service;

	@POST
	public Response modifyArtifacts(@Valid List<Artifact> artifacts,
			@QueryParam("authorEmail") Optional<String> authorEmail,
			@QueryParam("authorName") Optional<String> authorName) {

		service.processArtifacts(artifacts, authorEmail, authorName);
		return Response.ok().build();

	}

	@GET
	public List<Artifact> getArtifacts(@BeanParam GetListOptions options) {
		return service.getArtifacts(options);
	}

	@GET
	@Path("/count")
	public ArtifactCount countArtifacts(@BeanParam GetOptions options) {
		return service.countArtifacts(options);
	}

}
