package com.hypersocket.fs.events;

import java.io.IOException;
import java.io.InputStream;

import com.hypersocket.events.SynchronousEvent;

public interface FileOutputTransformationEvent extends SynchronousEvent {

	InputStream getInputStream() throws IOException;
	
	void setInputStream(InputStream in) throws IOException;
	
	String getTransformationFilename();
	
	String getOriginalFilename();
	
	void setTransformationFilename(String filename);
}
