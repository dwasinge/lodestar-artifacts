package com.redhat.labs.lodestar.model;

import java.util.Optional;

import javax.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GetListOptions extends GetOptions {

	@Parameter(name = "page", required = false, description = "0 based index of page of results to return")
	@QueryParam("page")
	private Integer page;

	@Parameter(name = "pageSize", required = false, description = "number of results to return per page")
	@QueryParam("pageSize")
	private Integer pageSize;

	public Optional<Integer> getPage() {
		return Optional.ofNullable(page);
	}

	public Optional<Integer> getPageSize() {
		return Optional.ofNullable(pageSize);
	}

}
