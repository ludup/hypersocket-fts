package com.hypersocket.fs.events;

import java.io.IOException;
import java.io.OutputStream;

import com.hypersocket.events.SynchronousEvent;

public interface FileInputTransformationEvent extends SynchronousEvent {

	OutputStream getOutputStream() throws IOException;
	
	void setOutputStream(OutputStream out) throws IOException;
	
	String getTransformationFilename();
	
	String getOriginalFilename();
	
	void setTransformationFilename(String filename) throws IOException;
}
