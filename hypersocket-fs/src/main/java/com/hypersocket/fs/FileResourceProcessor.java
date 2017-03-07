package com.hypersocket.fs;

import java.util.Map;

public interface FileResourceProcessor {

	void processFileResource(FileResource resource, Map<String,String> properties);
}
