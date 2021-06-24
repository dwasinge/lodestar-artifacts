package com.redhat.labs.lodestar.model.gitlab;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectTree {

	@Getter
	private Integer projectId;
	@Builder.Default
	private List<ProjectTreeNode> projectTreeNodes = new ArrayList<>();
	@Builder.Default
	private Optional<ProjectTreeNode> artifactFileNode = Optional.empty();
	@Builder.Default
	private Optional<ProjectTreeNode> engagementFileNode = Optional.empty();

	Optional<ProjectTreeNode> getArtifactJsonFileNode() {
		if (artifactFileNode.isEmpty()) {
			artifactFileNode = getNodeByFileName("artifacts.json");
		}
		return artifactFileNode;
	}

	Optional<ProjectTreeNode> getEngagementJsonFileNode() {
		if (engagementFileNode.isEmpty()) {
			engagementFileNode = getNodeByFileName("engagement.json");
		}
		return engagementFileNode;
	}

	Optional<ProjectTreeNode> getNodeByFileName(String name) {
		return projectTreeNodes.stream().filter(n -> name.equals(n.getName())).findAny();
	}

	public Optional<ProjectTreeNode> getArtifactsProjectTreeNode() {
		return Optional.ofNullable(getArtifactJsonFileNode().orElse(getEngagementJsonFileNode().orElse(null)));
	}

}
