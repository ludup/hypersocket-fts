package com.hypersocket.fs.json;

import java.io.IOException;
import java.io.InputStream;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.vfs2.FileObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hypersocket.fs.ContentInputStream;
import com.hypersocket.fs.DownloadEventProcessor;
import com.hypersocket.fs.DownloadProcessor;
import com.hypersocket.fs.FileResource;
import com.hypersocket.fs.events.DownloadStartedEvent;
import com.hypersocket.session.Session;

public class HttpDownloadProcessor implements DownloadProcessor {

	static Logger log = LoggerFactory.getLogger(HttpDownloadProcessor.class);

	public static final String CONTENT_INPUTSTREAM = "ContentInputStream";

	HttpServletRequest request;
	HttpServletResponse response;
	long start;
	long length;
	String protocol;
	Session session;

	static MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();

	public HttpDownloadProcessor(HttpServletRequest request,
			HttpServletResponse response, long start, long length, String protocol, Session session) {
		this.request = request;
		this.response = response;
		this.start = start;
		this.length = length;
		this.protocol = protocol;
		this.session = session;
	}

	@Override
	public void startDownload(FileResource resource, String childPath,
			FileObject file, DownloadEventProcessor downloadEventProcessor) {

		InputStream in = null;
		try {
			
			in = file.getContent().getInputStream();
			DownloadStartedEvent evt = downloadEventProcessor.downloadStarted(resource, childPath, file, in, protocol);
			
			response.setContentType(mimeTypesMap.getContentType(evt.getTransformationFilename()));

			if ("true".equals(request.getParameter("forceDownload"))) {
				response.setHeader("Content-disposition",
						"attachment; filename=\"" + evt.getTransformationFilename() + "\"");
			}

			request.setAttribute(CONTENT_INPUTSTREAM,
						new ContentInputStream(resource, childPath, file, evt.getInputStream(),
								start, length, downloadEventProcessor, evt.getTimestamp(), protocol, session));
			
		} catch (IOException e) {
			downloadEventProcessor.downloadFailed(resource, childPath, file, e, protocol, session);
		}

	}

}
