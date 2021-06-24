package com.redhat.labs.lodestar.model.gitlab;

import javax.json.bind.annotation.JsonbProperty;

import lombok.Data;

@Data
public class Project {

	private Integer id;
	@JsonbProperty("path_with_namespace")
	private String pathWithNamespace;

}
