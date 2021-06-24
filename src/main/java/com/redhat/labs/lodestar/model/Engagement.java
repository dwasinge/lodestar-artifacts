package com.redhat.labs.lodestar.model;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Engagement {

	private String uuid;
	private List<Artifact> artifacts;

}
