package com.redhat.labs.lodestar.rest.client;

import java.util.List;

import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.labs.lodestar.model.gitlab.File;
import com.redhat.labs.lodestar.model.gitlab.Project;

@Path("/api/v4")
@RegisterRestClient(configKey = "gitlab.api")
@RegisterClientHeaders(GitlabTokenFactory.class)
@Produces("application/json")
public interface GitlabRestClient {

	/*
	 * Groups
	 */
	@GET
	@Path("/groups/{id}/projects")
	Response getProjectsbyGroup(@PathParam("id") @Encoded Integer groupId,
			@QueryParam("include_subgroups") @Encoded Boolean includeSubgroups, @QueryParam("per_page") int perPage,
			@QueryParam("page") int page);

	/*
	 * Projects
	 */

	@GET
	@Path("/groups/{id}/search")
	List<Project> getProjectsByEngagementUuid(@PathParam("id") @Encoded String projectPathOrId,
			@QueryParam("scope") String scope, @QueryParam("search") String engagementUuid);

	// GET /projects/:id/repository/tree
	@GET
	@Path("/projects/{id}/repository/tree")
	Response getProjectTree(@PathParam("id") @Encoded Integer id, @QueryParam("recursive") boolean recursive);

	/*
	 * Files
	 */

	@POST
	@Path("/projects/{id}/repository/files/{file_path}")
	@Produces("application/json")
	File createFile(@PathParam("id") @Encoded Integer projectId, @PathParam("file_path") @Encoded String filePath,
			File file);

	@PUT
	@Path("/projects/{id}/repository/files/{file_path}")
	@Produces("application/json")
	File updateFile(@PathParam("id") @Encoded Integer projectId, @PathParam("file_path") @Encoded String filePath,
			File file);

	@GET
	@Path("/projects/{id}/repository/files/{file_path}")
	@Produces("application/json")
	File getFile(@PathParam("id") @Encoded Integer projectId, @PathParam("file_path") @Encoded String filePath,
			@QueryParam("ref") @Encoded String ref);

	/*
	 * Search
	 */
	@GET
	@Path("/groups/{id}/search")
	List<Project> findProjectByEngagementId(@PathParam("id") @Encoded Integer groupId,
			@QueryParam("scope") String scope, @QueryParam("search") String search);

}
