package com.redhat.labs.lodestar.model;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.validation.constraints.NotBlank;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Artifact extends PanacheMongoEntity {

	private String uuid;
	private String created;
	private String modified;

	@NotBlank
	private String engagementUuid;
	@NotBlank
	private String title;
	@NotBlank
	private String description;
	@NotBlank
	private String type;
	@NotBlank
	private String linkAddress;

	public static Optional<Artifact> findByUuid(String uuid) {
		return Artifact.find("uuid", uuid).firstResultOptional();
	}

	public static ArtifactCount countAllArtifacts() {
		return ArtifactCount.builder().count(Artifact.count()).build();
	}

	public static ArtifactCount countArtifactsByEngagementId(String engagementId) {
		return ArtifactCount.builder().count(Artifact.count("engagementUuid", engagementId)).build();
	}

	public static List<Artifact> pagedArtifacts(Integer page, Integer pageSize) {
		return Artifact.findAll().page(page, pageSize).list();
	}

	public static List<Artifact> pagedArtifactsByEngagementId(String engagementId, Integer page, Integer pageSize) {
		return Artifact.find("engagementUuid", engagementId).page(page, pageSize).list();
	}

	public static long deleteByUuid(String uuid) {
		return Artifact.delete("uuid", uuid);
	}
	
	public static Stream<Artifact> streamArtifactsByEngagementId(String engagementId) {
		return Artifact.stream("engagementUuid", engagementId);
	}

}
