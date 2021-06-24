package com.redhat.labs.lodestar.service;

import java.util.ArrayList;
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

import com.redhat.labs.lodestar.model.Artifact;
import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.ModifyType;
import com.redhat.labs.lodestar.model.gitlab.File;
import com.redhat.labs.lodestar.model.gitlab.Project;
import com.redhat.labs.lodestar.model.gitlab.ProjectTree;
import com.redhat.labs.lodestar.model.gitlab.ProjectTreeNode;
import com.redhat.labs.lodestar.model.pagination.PagedResults;
import com.redhat.labs.lodestar.rest.client.GitlabRestClient;

@ApplicationScoped
public class GitService {

	private static final String ARTIFACT_FILE = "artifacts.json";
	private static final String ENGAGEMENT_FILE = "engagement.json";

	@ConfigProperty(name = "group.parent.id")
	Integer groupParentId;

	@ConfigProperty(name = "default.branch", defaultValue = "master")
	String defaultBranch;

	@ConfigProperty(name = "default.commit.message", defaultValue = "updated artifacts list")
	String defaultCommitMessage;

	@ConfigProperty(name = "default.author.name", defaultValue = "lodestar-artifacts-bot")
	String defaultAuthorName;

	@ConfigProperty(name = "default.author.email", defaultValue = "lodestar-backend-bot@bot.com")
	String defaultAuthorEmail;

	@ConfigProperty(name = "page.size", defaultValue = "20")
	Integer pageSize;

	@Inject
	@RestClient
	GitlabRestClient gitlabRestClient;

	@Inject
	Jsonb jsonb;

	ProjectTree createProjectTree(Project project) {

		List<ProjectTreeNode> treeNodes = getProjectTree(project.getId(), false);
		return ProjectTree.builder().projectId(project.getId()).projectTreeNodes(treeNodes).build();

	}

	Optional<File> getArtifactsFile(ProjectTree projectTree) {

		Optional<ProjectTreeNode> artifactsTreeNode = projectTree.getArtifactsProjectTreeNode();
		if (artifactsTreeNode.isEmpty()) {
			return Optional.empty();
		}

		return Optional.ofNullable(
				gitlabRestClient.getFile(projectTree.getProjectId(), artifactsTreeNode.get().getPath(), defaultBranch));

	}

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

	public List<Artifact> parseArtifactsFile(File file) {

		if (null == file || null == file.getContent() || file.getContent().isBlank()) {
			return new ArrayList<>();
		}

		return jsonb.fromJson(file.getContent(), new ArrayList<Artifact>() {
			private static final long serialVersionUID = 1L;
		}.getClass().getGenericSuperclass());

	}

	public List<Artifact> parseEngagementFile(File file) {

		if (null == file || null == file.getContent() || file.getContent().isBlank()) {
			return new ArrayList<>();
		}

		Engagement engagement = jsonb.fromJson(file.getContent(), Engagement.class);

		// set engagement uuid on each artifacts
		List<Artifact> artifacts = engagement.getArtifacts();
		artifacts.stream().forEach(a -> a.setEngagementUuid(engagement.getUuid()));

		return engagement.getArtifacts();

	}

	public List<Project> getProjectsByGroup(Boolean includeSubgroups) {

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

	public List<ProjectTreeNode> getProjectTree(Integer projectId, boolean recursive) {

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

	// TODO: Need to be smarter now that branch can be main or master. HMW look up
	// the branches for a project and choose the correct option.

	public void createOrUpdateArtifactsFile(String engagementUuid, List<Artifact> artifacts,
			Optional<String> authorEmail, Optional<String> authorName, Optional<String> commitMessage) {

		// find project by engagement
		Project project = findProjectByEngagementUuid(engagementUuid)
				.orElseThrow(() -> new WebApplicationException("no project found with engagemnt id" + engagementUuid));

		// get project tree
		ProjectTree tree = createProjectTree(project);

		ModifyType modifyType = ModifyType.CREATE;

		// find artifacts.json node
		Optional<ProjectTreeNode> node = tree.getArtifactsProjectTreeNode();
		if (node.isPresent() && ARTIFACT_FILE.equals(node.get().getName())) {
			modifyType = ModifyType.UPDATE;
		}

		// create json content
		String content = jsonb.toJson(artifacts);

		// create file
		File artifactFile = File.builder().filePath(ARTIFACT_FILE).content(content)
				.authorEmail(authorEmail.orElse(defaultAuthorEmail)).authorName(authorName.orElse(defaultAuthorName))
				.branch(defaultBranch).commitMessage(commitMessage.orElse(defaultCommitMessage)).build();

		// encode before sending
		artifactFile.encodeFileAttributes();

		// create or udpate in git
		if (ModifyType.UPDATE.equals(modifyType)) {
			gitlabRestClient.updateFile(project.getId(), artifactFile.getFilePath(), artifactFile);
		} else {
			gitlabRestClient.createFile(project.getId(), artifactFile.getFilePath(), artifactFile);
		}

	}

	Optional<Project> findProjectByEngagementUuid(String uuid) {

		List<Project> projects = gitlabRestClient.findProjectByEngagementId(groupParentId, "project", uuid);
		if (1 != projects.size()) {
			return Optional.empty();
		}

		return Optional.of(projects.get(0));

	}

}
