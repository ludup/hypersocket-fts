package com.hypersocket.vfs.upload;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hypersocket.auth.json.UnauthorizedException;
import com.hypersocket.json.ResourceList;
import com.hypersocket.json.ResourceStatus;
import com.hypersocket.server.HypersocketServer;
import com.hypersocket.server.handlers.HttpRequestHandler;
import com.hypersocket.server.handlers.HttpResponseProcessor;
import com.hypersocket.session.SessionService;
import com.hypersocket.session.json.SessionTimeoutException;
import com.hypersocket.session.json.SessionUtils;
import com.hypersocket.upload.FileUpload;
import com.hypersocket.utils.FileUtils;
import com.hypersocket.vfs.VirtualFileService;
import com.hypersocket.vfs.json.FileSystemController;

@Component
public class UploadHttpHandler extends HttpRequestHandler {

	static Logger log = LoggerFactory.getLogger(UploadHttpHandler.class);
	
	@Autowired
	HypersocketServer server;
	
	@Autowired
	SessionUtils sessionUtils;
	
	@Autowired
	SessionService sessionService; 
	
	@Autowired
	VirtualFileService fileService; 
	
	ObjectMapper jsonMapper = new ObjectMapper();
	
	public UploadHttpHandler() {
		super("upload", 1);
	}
	
	@PostConstruct
	private void postConstruct() {
		server.registerHttpHandler(this);
	}

	@Override
	public boolean handlesRequest(HttpServletRequest request) {
		return ServletFileUpload.isMultipartContent(request) 
				&& request.getRequestURI().startsWith(server.resolvePath("api/fs/upload/"));
	}

	@Override
	public void handleHttpRequest(HttpServletRequest request, HttpServletResponse response,
			HttpResponseProcessor responseProcessor) throws IOException {
		
		// Handle upload directly instead of using servlet API spec. This allows us to stream.
		try {
			sessionService.setCurrentSession(sessionUtils.getSession(request),
					sessionUtils.getLocale(request));
		} catch (UnauthorizedException e1) {
			responseProcessor.send401(request, response);
			return;
		} catch (SessionTimeoutException e1) {
			responseProcessor.send401(request, response);
			return;
		}

		try {

			String virtualPath = FileUtils.checkEndsWithSlash(
					FileUtils.checkStartsWithSlash(FileUtils.stripParentPath(server.getApiPath() + "/fs/upload", 
					URLDecoder.decode(request.getRequestURI(), "UTF-8"))));
			
			// Create a new file upload handler
			ServletFileUpload upload = new ServletFileUpload();

			// Parse the request
			FileItemIterator iter = upload.getItemIterator(request);
			List<FileUpload> uploads = new ArrayList<FileUpload>();
			while (iter.hasNext()) {
			    FileItemStream item = iter.next();
			    String name = item.getName();
			    String partDestination = virtualPath + FileUtils.lastPathElement(name);
			    InputStream stream = item.openStream();
			    uploads.add(fileService.uploadFile(
						FileUtils.checkEndsWithNoSlash(partDestination), 
						stream, 
						null, 
						FileSystemController.HTTP_PROTOCOL));
			}
			
			
			if(uploads.size()==1) {
				sendResponse(new ResourceStatus<FileUpload>(uploads.get(0)), response);
			} else {
				sendResponse(new ResourceList<FileUpload>(uploads), response);
			}
			
		} catch(Exception e) { 
			sendResponse(new ResourceStatus<FileUpload>(false, e.getMessage()), response);
		} finally {
			responseProcessor.sendResponse(request, response, false);
			sessionService.clearPrincipalContext();
		}
		
	}
	
	private void sendResponse(Object json, HttpServletResponse response) throws IOException, UnsupportedEncodingException {
		
		String content = jsonMapper.writeValueAsString(json);
		byte[] contentBytes = content.getBytes("UTF-8");
		response.setContentType("application/json; charset=UTF-8");
		response.setContentLength(contentBytes.length);;
		response.getOutputStream().write(contentBytes);
	}

}
