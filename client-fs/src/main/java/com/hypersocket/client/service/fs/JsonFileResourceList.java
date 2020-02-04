package com.hypersocket.client.service.fs;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class JsonFileResourceList {

	private boolean success;
	private String error;
	private JsonFileResource[] resources;
	private Map<String,String> properties;
	
	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public JsonFileResource[] getResources() {
		return resources;
	}

	public void setResources(JsonFileResource[] resources) {
		this.resources = resources;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}
	
}
