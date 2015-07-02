package com.hypersocket.fs;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public class FileResourceDeserializer extends JsonDeserializer<FileResource> {

	@Override
	public FileResource deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		JsonNode node = jp.getCodec().readTree(jp);
		FileResource resource = new FileResource();
		resource.setName(node.get("name").asText());
		resource.setHidden(node.get("hidden").asBoolean());
		resource.setResourceCategory(node.get("resourceCategory").asText());
		resource.setSystem(node.get("system").asBoolean());

		resource.setScheme(node.get("scheme").asText());
		resource.setServer(node.get("server").asText());
		resource.setPort(node.get("port").asInt());
		resource.setPath(node.get("path").asText());
		resource.setUsername(node.get("username").asText());
		resource.setPassword(node.get("password").asText());
		resource.setReadOnly(node.get("readOnly").asBoolean());
		resource.setShowHidden(node.get("showHidden").asBoolean());
		resource.setShowFolders(node.get("showFolders").asBoolean());
		resource.setDisplayInBrowserResourcesTable(node.get("displayInBrowserResourcesTable").asBoolean());
		
		return resource;
	}

}
