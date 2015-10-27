package com.hypersocket.fs.events;

import java.io.IOException;
import java.io.InputStream;

public interface FileTransformationEvent {

	public InputStream getInputStream() throws IOException;
	
	public void setInputStream(InputStream in) throws IOException;
	
	public String getTransformationFilename();
	
	public void setTransformationFilename(String filename);
}
