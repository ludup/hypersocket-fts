package com.hypersocket.fs.events;

import java.io.IOException;
import java.io.InputStream;

import com.hypersocket.events.SynchronousEvent;

public interface FileOutputTransformationEvent extends SynchronousEvent {

	public InputStream getInputStream() throws IOException;
	
	public void setInputStream(InputStream in) throws IOException;
	
	public String getTransformationFilename();
	
	public String getOriginalFilename();
	
	public void setTransformationFilename(String filename);
}
