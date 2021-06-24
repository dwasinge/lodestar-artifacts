package com.redhat.labs.lodestar.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bson.types.ObjectId;

import com.redhat.labs.lodestar.model.Artifact;
import com.redhat.labs.lodestar.model.ArtifactCount;
import com.redhat.labs.lodestar.model.GetListOptions;
import com.redhat.labs.lodestar.model.GetOptions;
import com.redhat.labs.lodestar.model.ModifyType;
import com.redhat.labs.lodestar.model.gitlab.Project;

@ApplicationScoped
public class ArtifactService {

	@Inject
	GitService gitService;

	/**
	 * Refreshes the database.
	 */
	public void refresh() {

		// remove all artifacts
		Artifact.deleteAll();

		// get all projects
		List<Project> projects = gitService.getProjectsByGroup(true);

		// parse engagement files for artifacts
		List<Artifact> artifacts = projects.parallelStream().map(gitService::createProjectTree)
				.map(gitService::getArtifactsFile).map(gitService::parseFile).flatMap(Collection::stream).map(a -> {
					if (null == a.getUuid()) {
						a.setUuid(UUID.randomUUID().toString());
					}
					return a;
				}).collect(Collectors.toList());

		// persist or update artifacts
		Artifact.persistOrUpdate(artifacts);

	}

	public List<Artifact> getArtifacts(GetListOptions options) {

		Integer page = options.getPage().orElse(0);
		Integer pageSize = options.getPageSize().orElse(20);

		Optional<String> engagementUuid = options.getEngagementUuid();

		return engagementUuid.isPresent() ? Artifact.pagedArtifactsByEngagementId(engagementUuid.get(), page, pageSize)
				: Artifact.pagedArtifacts(page, pageSize);

	}

	public ArtifactCount countArtifacts(GetOptions options) {

		Optional<String> engagementUuid = options.getEngagementUuid();

		return engagementUuid.isPresent() ? Artifact.countArtifactsByEngagementId(engagementUuid.get())
				: Artifact.countAllArtifacts();

	}

	public void processArtifacts(List<Artifact> artifacts, Optional<String> authorEmail, Optional<String> authorName) {

		// TODO: Validate all artifacts have engagement id

		// group artifacts by engagement id
		Map<String, List<Artifact>> artifactsByEngagement = artifacts.stream()
				.collect(Collectors.groupingBy(Artifact::getEngagementUuid));

		// process each artifacts list
		artifactsByEngagement.entrySet().stream()
				.forEach(e -> modifyArtifactsForEngagement(e.getKey(), e.getValue(), authorEmail, authorName));

	}

	void modifyArtifactsForEngagement(String engagementUuid, List<Artifact> artifacts, Optional<String> authorEmail,
			Optional<String> authorName) {

		// set engagement uuid on artifacts and create/update
		Map<ModifyType, Long> results = artifacts.stream().map(this::createOrUpdateArtifact)
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

		// remove deleted artifacts
		List<Artifact> toDelete = Artifact.streamArtifactsByEngagementId(engagementUuid)
				.filter(a -> artifacts.stream().noneMatch(p -> p.getUuid().equals(a.getUuid())))
				.collect(Collectors.toList());

		results.put(ModifyType.DELETE, Long.valueOf(toDelete.size()));
		toDelete.stream().forEach(Artifact::delete);

		// create commit message
		String commitMessage = createCommitMessage(engagementUuid, results);
		// TODO: probably should be async
		// update git
		createOrUpdateArtifactsJsonFile(engagementUuid, commitMessage, authorEmail, authorName);

	}

	ModifyType createOrUpdateArtifact(Artifact artifact) {

		Optional<Artifact> persisted = Artifact.findByUuid(artifact.getUuid());
		if (persisted.isPresent()) {
			updateArtifact(artifact, persisted.get().id);
			return ModifyType.UPDATE;
		}

		createArtifact(artifact);
		return ModifyType.CREATE;

	}

	void createArtifact(Artifact artifact) {

		String now = getNowAsZulu();

		artifact.setUuid(UUID.randomUUID().toString());
		artifact.setCreated(now);
		artifact.setModified(now);

		Artifact.persist(artifact);

	}

	void updateArtifact(Artifact artifact, ObjectId persistedId) {

		String now = getNowAsZulu();
		artifact.id = persistedId;
		artifact.setModified(now);

		Artifact.update(artifact);

	}

	void createOrUpdateArtifactsJsonFile(String engagementUuid, String commitMessage, Optional<String> authorEmail,
			Optional<String> authorName) {

		// get all artifacts for the engagement id
		List<Artifact> artifacts = Artifact.streamArtifactsByEngagementId(engagementUuid).collect(Collectors.toList());

		// create/update artifacts file
		gitService.createOrUpdateArtifactsFile(engagementUuid, artifacts, authorEmail, authorName,
				Optional.ofNullable(commitMessage));

	}

	String createCommitMessage(String engagementUuid, Map<ModifyType, Long> counts) {

		Long create = getCountFromMap(ModifyType.CREATE, counts);
		Long update = getCountFromMap(ModifyType.UPDATE, counts);
		Long delete = getCountFromMap(ModifyType.DELETE, counts);

		return new StringBuilder("Artifacts modified for engagement '").append(engagementUuid).append("'")
				.append("\n\tCreated: ").append(create).append("\n\tUpdated: ").append(update).append("\n\tDeleted: ")
				.append(delete).toString();

	}

	Long getCountFromMap(ModifyType modifyType, Map<ModifyType, Long> map) {
		return (null == map.get(modifyType)) ? Long.valueOf(0) : map.get(modifyType);
	}

	String getNowAsZulu() {
		return LocalDateTime.now(ZoneId.of("Z")).toString();
	}

}
