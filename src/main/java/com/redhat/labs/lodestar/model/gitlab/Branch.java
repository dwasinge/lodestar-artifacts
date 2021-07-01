package com.redhat.labs.lodestar.model.gitlab;

import javax.json.bind.annotation.JsonbProperty;

import lombok.Data;

@Data
public class Branch {

	private String name;
	@JsonbProperty("default")
	private Boolean isDefault;
	
}
