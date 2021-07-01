package com.redhat.labs.lodestar.model;

import java.util.List;
import java.util.Optional;

import javax.json.bind.annotation.JsonbTransient;
import javax.validation.constraints.NotBlank;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;
import org.javers.core.metamodel.annotation.DiffIgnore;
import org.javers.core.metamodel.annotation.Id;
import org.javers.core.metamodel.annotation.TypeName;

import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import io.quarkus.panache.common.Sort;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TypeName("Artifact")
@EqualsAndHashCode(callSuper = true)
public class Artifact extends PanacheMongoEntityBase {

	private static final String ENGAGEMENT_UUID = "engagementUuid";
	private static final String MODIFIED = "modified";
	private static final String UUID = "uuid";

	@BsonId
	@DiffIgnore
	@JsonbTransient
	private ObjectId id;

	@Id
	private String uuid;
	@DiffIgnore
	private String created;
	@DiffIgnore
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

	/**
	 * Returns an {@link ArtifactCount} containing the count for the total number of
	 * {@link Artifact}s in the database.
	 * 
	 * @return
	 */
	public static ArtifactCount countAllArtifacts() {
		return ArtifactCount.builder().count(count()).build();
	}

	/**
	 * Returns an {@link ArtifactCount} containing the count for the number of
	 * {@link Artifact} matching the engagement uuid.
	 * 
	 * @param engagementUuid
	 * @return
	 */
	public static ArtifactCount countArtifactsByEngagementUuid(String engagementUuid) {
		return ArtifactCount.builder().count(count(ENGAGEMENT_UUID, engagementUuid)).build();
	}

	/**
	 * Returns {@link List} of {@link Artifact}s sorted descending on modified
	 * timestamp using the page specified.
	 * 
	 * @param page
	 * @param pageSize
	 * @return
	 */
	public static List<Artifact> pagedArtifacts(int page, int pageSize) {
		return findAll(Sort.descending(MODIFIED)).page(page, pageSize).list();
	}

	/**
	 * Returns a {@link List} of {@link Artifact}s for the given engagement uuid,
	 * sorted descending by modified timestamp using the page specified.
	 * 
	 * @param engagementUuid
	 * @param page
	 * @param pageSize
	 * @return
	 */
	public static List<Artifact> pagedArtifactsByEngagementUuid(String engagementUuid, int page, int pageSize) {
		return find(ENGAGEMENT_UUID, Sort.descending(MODIFIED), engagementUuid).page(page, pageSize).list();
	}

	/**
	 * Returns a {@link List} containing all {@link Artifact}s that match the given
	 * engagement uuid.
	 * 
	 * @param engagementUuid
	 * @return
	 */
	public static List<Artifact> findAllByEngagementUuid(String engagementUuid) {
		return list(ENGAGEMENT_UUID, engagementUuid);
	}

	/**
	 * Returns and {@link Optional} containing the {@link Artifact} that matches the
	 * given uuid. Otherwise, and empty {@link Optional} is returned.
	 * 
	 * @param uuid
	 * @return
	 */
	public static Optional<Artifact> findByUuid(String uuid) {
		return find(UUID, uuid).singleResultOptional();
	}

	/**
	 * Returns the number of {@link Artifact}s delete with the given uuid.
	 * 
	 * @param uuid
	 * @return
	 */
	public static long deleteByUuid(String uuid) {
		return Artifact.delete(UUID, uuid);
	}

	/**
	 * Removes all {@link Artifact}s from the database.
	 */
	public static long removeAllArtifacts() {
		return deleteAll();
	}

}
