package com.redhat.labs.lodestar.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.lodestar.model.Artifact;
import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.gitlab.Branch;
import com.redhat.labs.lodestar.model.gitlab.File;
import com.redhat.labs.lodestar.model.gitlab.Project;
import com.redhat.labs.lodestar.model.gitlab.ProjectTree;
import com.redhat.labs.lodestar.model.gitlab.ProjectTreeNode;
import com.redhat.labs.lodestar.model.pagination.PagedResults;
import com.redhat.labs.lodestar.rest.client.GitlabRestClient;

@ApplicationScoped
public class GitService {

	private static final Logger LOGGER = LoggerFactory.getLogger(GitService.class);

	private static final String ARTIFACT_FILE = "artifacts.json";
	private static final String ENGAGEMENT_FILE = "engagement.json";

	@ConfigProperty(name = "group.parent.id")
	Integer groupParentId;

	@ConfigProperty(name = "default.branch")
	String defaultBranch;

	@ConfigProperty(name = "default.commit.message")
	String defaultCommitMessage;

	@ConfigProperty(name = "default.author.name")
	String defaultAuthorName;

	@ConfigProperty(name = "default.author.email")
	String defaultAuthorEmail;

	@ConfigProperty(name = "default.page.size")
	Integer pageSize;

	@Inject
	@RestClient
	GitlabRestClient gitlabRestClient;

	@Inject
	Jsonb jsonb;

	/**
	 * Returns the name of the default branch for the given project ID. If not
	 * found, the configured value is returned.
	 * 
	 * @param projectId
	 * @return
	 */
	String findDefaultBranch(Integer projectId) {

		List<Branch> branches = gitlabRestClient.findProjectBranches(projectId);
		Optional<Branch> branch = branches.stream().filter(Branch::getIsDefault).findAny();

		return branch.isPresent() ? branch.get().getName() : defaultBranch;

	}

	/**
	 * Creates a {@link ProjectTree} for the given {@link Project}.
	 * 
	 * @param project
	 * @return
	 */
	ProjectTree createProjectTree(Project project) {

		LOGGER.debug("creating project tree for {}", project);

		List<ProjectTreeNode> treeNodes = getProjectTree(project.getId(), false);
		return ProjectTree.builder().projectId(project.getId()).projectTreeNodes(treeNodes).build();

	}

	/**
	 * Returns an {@link Optional} containing the {@link ProjectTreeNode} for the
	 * artifacts.json file. Otherwise, an empty {@link Optional} is returned.
	 * 
	 * @param projectTree
	 * @return
	 */
	Optional<File> getArtifactsFile(ProjectTree projectTree) {

		Optional<ProjectTreeNode> artifactsTreeNode = projectTree.getArtifactsProjectTreeNode();
		if (artifactsTreeNode.isEmpty()) {
			return Optional.empty();
		}

		// get default branch
		String branch = findDefaultBranch(projectTree.getProjectId());

		return Optional.ofNullable(
				gitlabRestClient.getFile(projectTree.getProjectId(), artifactsTreeNode.get().getPath(), branch));

	}

	/*
	 * Returns a {@link List} of {@link Artifact}s for the given {@link File}. An
	 * empty {@link List} is returned if the {@link File} is not found or contains
	 * no {@link Artifact}s.
	 */
	List<Artifact> parseFile(Optional<File> file) {

		List<Artifact> artifacts = new ArrayList<>();

		if (file.isEmpty()) {
			return artifacts;
		}

		File artifactsFile = file.get();

		artifactsFile.decodeFileAttributes();

		if (ARTIFACT_FILE.equals(artifactsFile.getFilePath())) {
			artifacts = parseArtifactsFile(artifactsFile);
		} else if (ENGAGEMENT_FILE.equals(artifactsFile.getFilePath())) {
			artifacts = parseEngagementFile(artifactsFile);
		}

		return artifacts;

	}

	/**
	 * Returns a {@link List} of {@link Artifact}s from the given {@link File}. An
	 * empty {@link List} is returned if no file found or contains no
	 * {@link Artifact}s.
	 * 
	 * @param file
	 * @return
	 */
	List<Artifact> parseArtifactsFile(File file) {

		if (null == file || null == file.getContent() || file.getContent().isBlank()) {
			return new ArrayList<>();
		}

		return Arrays.asList(jsonb.fromJson(file.getContent(), Artifact[].class));

	}

	/**
	 * Returns a {@link List} of {@link Artifact}s from the given {@link File}. An
	 * empty {@link List} is returned if no file found or contains no
	 * {@link Artifact}s.
	 * 
	 * @param file
	 * @return
	 */
	List<Artifact> parseEngagementFile(File file) {

		if (null == file || null == file.getContent() || file.getContent().isBlank()) {
			return new ArrayList<>();
		}

		Engagement engagement = jsonb.fromJson(file.getContent(), Engagement.class);

		// set engagement uuid on each artifacts
		List<Artifact> artifacts = null == engagement.getArtifacts() ? new ArrayList<>() : engagement.getArtifacts();
		artifacts.stream().forEach(a -> a.setEngagementUuid(engagement.getUuid()));

		return artifacts;

	}

	/**
	 * Returns a {@link List} of {@link Project} found in the configured group or
	 * subgroups.
	 * 
	 * @param includeSubgroups
	 * @return
	 */
	List<Project> getProjectsByGroup(Boolean includeSubgroups) {

		Response response = null;
		PagedResults<Project> page = new PagedResults<>(pageSize);

		while (page.hasMore()) {
			response = gitlabRestClient.getProjectsbyGroup(groupParentId, includeSubgroups, pageSize, page.getNumber());
			page.update(response, new GenericType<List<Project>>() {
			});
		}

		if (null != response) {
			response.close();
		}

		return page.getResults();
	}

	/**
	 * Returns a {@link List} containing the {@link ProjectTreeNode}s for the given
	 * project ID.
	 * 
	 * @param projectId
	 * @param recursive
	 * @return
	 */
	List<ProjectTreeNode> getProjectTree(Integer projectId, boolean recursive) {

		Response response = null;
		PagedResults<ProjectTreeNode> page = new PagedResults<>(pageSize);

		while (page.hasMore()) {
			response = gitlabRestClient.getProjectTree(projectId, recursive);
			page.update(response, new GenericType<List<ProjectTreeNode>>() {
			});
		}

		if (null != response) {
			response.close();
		}

		return page.getResults();

	}

	/**
	 * Creates, updates, or deletes the {@link Artifact}s in the artifacts.json for
	 * the given engagement ID. If any {@link Optional} parameter is not supplied
	 * the configured default will be used.
	 * 
	 * @param engagementUuid
	 * @param artifacts
	 * @param authorEmail
	 * @param authorName
	 * @param commitMessage
	 */
	void createOrUpdateArtifactsFile(String engagementUuid, List<Artifact> artifacts, Optional<String> authorEmail,
			Optional<String> authorName, Optional<String> commitMessage) {

		// find project by engagement
		Project project = findProjectByEngagementUuid(engagementUuid)
				.orElseThrow(() -> new WebApplicationException("no project found with engagemnt id" + engagementUuid));

		// does artifact file exist
		boolean isUpate = artifactsFileExists(project);

		// create json content
		String content = jsonb.toJson(artifacts);

		// find default branch
		String branch = findDefaultBranch(project.getId());

		// create
		File artifactsFile = createArtifactsFile(content, branch, authorName, authorEmail, commitMessage);

		// create or udpate in git
		if (isUpate) {
			gitlabRestClient.updateFile(project.getId(), artifactsFile.getFilePath(), artifactsFile);
		} else {
			gitlabRestClient.createFile(project.getId(), artifactsFile.getFilePath(), artifactsFile);
		}

	}

	/**
	 * Returns true if artifacts.json is found in the project's tree. Otherwise,
	 * false.
	 * 
	 * @param project
	 * @return
	 */
	boolean artifactsFileExists(Project project) {

		// get project tree
		ProjectTree projectTree = createProjectTree(project);

		// update if file already exists
		return projectTree.hasArtifactsJsonFile();

	}

	/**
	 * Creates and returns a {@link File} with the given parameters and encodes the
	 * content.
	 * 
	 * @param content
	 * @param branch
	 * @param authorName
	 * @param authorEmail
	 * @param commitMessage
	 * @return
	 */
	File createArtifactsFile(String content, String branch, Optional<String> authorName, Optional<String> authorEmail,
			Optional<String> commitMessage) {

		// create file
		File artifactFile = File.builder().filePath(ARTIFACT_FILE).content(content)
				.authorEmail(authorEmail.orElse(defaultAuthorEmail)).authorName(authorName.orElse(defaultAuthorName))
				.branch(branch).commitMessage(commitMessage.orElse(defaultCommitMessage)).build();

		// encode before sending
		artifactFile.encodeFileAttributes();

		return artifactFile;

	}

	/**
	 * Returns an {@link Optional} containing the {@link Project} found for the
	 * given engagement ID. Otherwise, an empty {@link Optional} is returned.
	 * 
	 * @param uuid
	 * @return
	 */
	Optional<Project> findProjectByEngagementUuid(String uuid) {

		List<Project> projects = gitlabRestClient.findProjectByEngagementId(groupParentId, "projects", uuid);
		if (1 != projects.size()) {
			return Optional.empty();
		}

		return Optional.of(projects.get(0));

	}

}
