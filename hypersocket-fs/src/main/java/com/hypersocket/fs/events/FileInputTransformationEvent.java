package com.hypersocket.fs.events;

import java.io.IOException;
import java.io.OutputStream;

import com.hypersocket.events.SynchronousEvent;

public interface FileInputTransformationEvent extends SynchronousEvent {

	public OutputStream getOutputStream() throws IOException;
	
	public void setOutputStream(OutputStream out) throws IOException;
	
	public String getTransformationFilename();
	
	public String getOriginalFilename();
	
	public void setTransformationFilename(String filename) throws IOException;
}
