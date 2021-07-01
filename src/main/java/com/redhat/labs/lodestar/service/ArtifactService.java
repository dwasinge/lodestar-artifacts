package com.redhat.labs.lodestar.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.javers.core.ChangesByObject;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.javers.core.diff.ListCompareAlgorithm;

import com.redhat.labs.lodestar.model.Artifact;
import com.redhat.labs.lodestar.model.ArtifactCount;
import com.redhat.labs.lodestar.model.GetListOptions;
import com.redhat.labs.lodestar.model.GetOptions;
import com.redhat.labs.lodestar.model.gitlab.Project;

@ApplicationScoped
public class ArtifactService {

	@Inject
	GitService gitService;

	private static final Javers JAVERS = JaversBuilder.javers()
			.withListCompareAlgorithm(ListCompareAlgorithm.LEVENSHTEIN_DISTANCE).build();

	/**
	 * Remove all {@link Artifact}s from the database.
	 */
	public void purge() {
		Artifact.removeAllArtifacts();
	}

	/**
	 * Fetches all {@link Artifact}s from all projects in the configured Git group
	 * and inserts into the database.
	 */
	public void refresh() {

		// get all projects
		List<Project> projects = gitService.getProjectsByGroup(true);

		// get artifacts for each project and persist
		projects.parallelStream().map(gitService::createProjectTree).map(gitService::getArtifactsFile)
				.map(gitService::parseFile).flatMap(Collection::stream).forEach(a -> {

					// set uuid if missing
					if (null == a.getUuid()) {
						a.setUuid(UUID.randomUUID().toString());
					}

					// persist the artifact
					createOrUpdateArtifact(a);

				});

	}

	/**
	 * Processes the given {@link List} of {@link Artifact}s. The artifacts will be
	 * split based on engagement uuid. Each {@link List} of engagement
	 * {@link Artifact}s will be updated in both the database and Git.
	 * 
	 * @param artifacts
	 * @param authorEmail
	 * @param authorName
	 */
	public void process(List<Artifact> artifacts, Optional<String> authorEmail, Optional<String> authorName) {

		// split by engagement uuid and process each resulting artifact list
		processArtifacts(artifacts).entrySet().parallelStream().forEach(e -> {

			// modify artifacts in db
			String changeLog = modifyArtifactsByEngagementUuid(e.getKey(), e.getValue());
			// create commit message
			String commitMessage = new StringBuilder("Changed Engagement Artifacts\n").append(changeLog).toString();

			// pull latest artifacts from db
			List<Artifact> latest = Artifact.findAllByEngagementUuid(e.getKey());
			// send to git to update file
			gitService.createOrUpdateArtifactsFile(e.getKey(), latest, authorEmail, authorName,
					Optional.ofNullable(commitMessage));

		});

	}

	/**
	 * Returns a {@link List} of {@link Artifact}s matching the specified
	 * {@link GetListOptions}.
	 * 
	 * @param options
	 * @return
	 */
	public List<Artifact> getArtifacts(GetListOptions options) {

		Optional<String> engagementUuid = options.getEngagementUuid();

		return engagementUuid.isPresent()
				? Artifact.pagedArtifactsByEngagementUuid(engagementUuid.get(), options.getPage(),
						options.getPageSize())
				: Artifact.pagedArtifacts(options.getPage(), options.getPageSize());

	}

	/**
	 * Returns a {@link ArtifactCount} with the count of {@link Artifact}s matching
	 * the specified {@link GetOptions}.
	 * 
	 * @param options
	 * @return
	 */
	public ArtifactCount countArtifacts(GetOptions options) {

		Optional<String> engagementUuid = options.getEngagementUuid();

		return engagementUuid.isPresent() ? Artifact.countArtifactsByEngagementUuid(engagementUuid.get())
				: Artifact.countAllArtifacts();

	}

	/**
	 * Returns a {@link Map} of {@link List}s for each {@link Artifact} for each
	 * unique Engagement ID.
	 * 
	 * @param artifacts
	 * @return
	 */
	Map<String, List<Artifact>> processArtifacts(List<Artifact> artifacts) {
		return artifacts.stream().map(a -> {
			if (null == a.getUuid()) {
				a.setUuid(UUID.randomUUID().toString());
			}
			return a;
		}).collect(Collectors.groupingBy(Artifact::getEngagementUuid));
	}

	String modifyArtifactsByEngagementUuid(String engagementUuid, List<Artifact> artifacts) {

		StringBuilder changeLog = new StringBuilder();

		// compare incoming with database
		Diff diff = compareArtifactsWithDatabase(engagementUuid, artifacts);

		// group by object
		List<ChangesByObject> l = diff.groupByObject();

		// create/update/delete artifacts in database
		l.stream().filter(c -> c.getGlobalId().value().contains("Artifact")).forEach(cbo -> {

			// process the change
			processObjectChange(cbo, artifacts);

			changeLog.append(cbo.toString());

		});

		return changeLog.toString();

	}

	void processObjectChange(ChangesByObject cbo, List<Artifact> incoming) {

		// get artifact uuid from global id
		String globalId = cbo.getGlobalId().value();
		String aUuid = globalId.substring(globalId.indexOf("/") + 1);

		if (!cbo.getObjectsRemoved().isEmpty()) {

			Artifact.deleteByUuid(aUuid);

		} else {

			// find artifact in incoming list and create or update in database
			incoming.stream().filter(a -> aUuid.equals(a.getUuid())).findAny().ifPresent(this::createOrUpdateArtifact);

		}

	}

	/**
	 * Returns a {@link Diff} from comparing the provided {@link List} of
	 * {@link Artifact}s with the current state in the database for the given
	 * Engagement UUID.
	 * 
	 * @param engagementUuid
	 * @param artifacts
	 * @return
	 */
	Diff compareArtifactsWithDatabase(String engagementUuid, List<Artifact> artifacts) {

		// get list from database by uuid
		List<Artifact> existing = Artifact.findAllByEngagementUuid(engagementUuid);

		// compare old and new lists
		return JAVERS.compareCollections(existing, artifacts, Artifact.class);

	}

	/**
	 * Creates or updates the {@link Artifact} in the database.
	 * 
	 * @param artifact
	 * @return
	 */
	void createOrUpdateArtifact(Artifact artifact) {

		Optional<Artifact> persisted = Artifact.findByUuid(artifact.getUuid());
		if (persisted.isPresent()) {
			updateArtifact(artifact, persisted.get());
		} else {
			createArtifact(artifact);
		}

	}

	/**
	 * Sets any required attributes on the {@link Artifact} and inserts into the
	 * database.
	 * 
	 * @param artifact
	 */
	void createArtifact(Artifact artifact) {

		String now = getNowAsZulu();

		if (null == artifact.getUuid()) {
			artifact.setUuid(UUID.randomUUID().toString());
		}
		artifact.setCreated(now);
		artifact.setModified(now);

		artifact.persist();

	}

	/**
	 * Sets any required attributes on the {@link Artifact} and updates in the
	 * database.
	 * 
	 * @param artifact
	 */
	void updateArtifact(Artifact artifact, Artifact existing) {

		artifact.setId(existing.getId());
		artifact.setCreated(existing.getCreated());
		artifact.setModified(getNowAsZulu());

		artifact.update();

	}

	/**
	 * Returns a {@link String} representing the current Zulu time.
	 * 
	 * @return
	 */
	String getNowAsZulu() {
		return LocalDateTime.now(ZoneId.of("Z")).toString();
	}

}
