package com.redhat.labs.lodestar.model;

import java.util.Optional;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ArtifactCount {

	@Builder.Default
	private Long count = 0l;
	@Builder.Default
	private Optional<String> engagementId = Optional.empty();

}
