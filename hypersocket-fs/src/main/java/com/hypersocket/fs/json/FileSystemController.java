package com.hypersocket.fs.json;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

import com.hypersocket.auth.json.AuthenticatedController;
import com.hypersocket.auth.json.AuthenticationRequired;
import com.hypersocket.auth.json.UnauthorizedException;
import com.hypersocket.fs.FileResource;
import com.hypersocket.fs.FileResourceService;
import com.hypersocket.fs.UploadProcessor;
import com.hypersocket.fs.tree.TreeFile;
import com.hypersocket.fs.tree.TreeFolder;
import com.hypersocket.fs.tree.TreeList;
import com.hypersocket.json.ResourceStatus;
import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.session.json.SessionTimeoutException;
import com.hypersocket.session.json.SessionUtils;
import com.hypersocket.util.FileUtils;

@Controller
public class FileSystemController extends AuthenticatedController {

	static Logger log = LoggerFactory.getLogger(FileSystemController.class);
	
	public static final String HTTP_PROTOCOL = "HTTP";
	public static final String CONTENT_INPUTSTREAM = "ContentInputStream";
	
	@Autowired
	FileResourceService mountService;

	@Autowired
	SessionUtils sessionUtils;
	
	@SuppressWarnings("rawtypes")
	@AuthenticationRequired
	@RequestMapping(value = "fs/delete/**", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceStatus<?> delete(HttpServletRequest request,
			HttpServletResponse response) throws AccessDeniedException,
			UnauthorizedException, IOException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {
			
			return new ResourceStatus(mountService.deleteURIFile(
					request.getHeader("Host"), "api/fs/delete",
					URLDecoder.decode(request.getRequestURI(), "UTF-8"), HTTP_PROTOCOL));

		} finally {
			clearAuthenticatedContext();
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@AuthenticationRequired
	@RequestMapping(value = "fs/createFolder/**", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceStatus<TreeList> createFolder(HttpServletRequest request,
			HttpServletResponse response) throws AccessDeniedException,
			UnauthorizedException, IOException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {

			String uri = URLDecoder.decode(request.getRequestURI(), "UTF-8");
			
			FileResource resource = mountService.getMountForURIPath(
					request.getHeader("Host"), "api/fs/createFolder",
					uri);

			FileObject mountFile = mountService.resolveMountFile(resource);
			
			List folders = new ArrayList();
			
			FileObject newFile = mountService.createURIFolder(
					request.getHeader("Host"), "api/fs/createFolder",
					uri, HTTP_PROTOCOL);
			
			folders.add(new TreeFolder(newFile, mountFile, resource));
			return new ResourceStatus(new TreeList(folders), "");

		} finally {
			clearAuthenticatedContext();
		}
	}
	
	@SuppressWarnings("rawtypes")
	@AuthenticationRequired
	@RequestMapping(value = "fs/rename/**", method = RequestMethod.POST, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public ResourceStatus<?> rename(HttpServletRequest request,
			HttpServletResponse response,
			@RequestParam String toUri) throws AccessDeniedException,
			UnauthorizedException, IOException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {

			return new ResourceStatus(mountService.renameURIFile(
					request.getHeader("Host"), "api/fs/rename",
					URLDecoder.decode(request.getRequestURI(), "UTF-8"),
					URLDecoder.decode(toUri, "UTF-8"), HTTP_PROTOCOL));

		} finally {
			clearAuthenticatedContext();
		}
	}
	
	@AuthenticationRequired
	@RequestMapping(value = "fs/download/**", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseStatus(value = HttpStatus.OK)
	public void downloadFile(HttpServletRequest request,
			HttpServletResponse response,
			@RequestParam String forceDownload) throws AccessDeniedException,
			UnauthorizedException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {

			String uri = URLDecoder.decode(request.getRequestURI(), "UTF-8");
			mountService.downloadURIFile(request.getHeader("Host"), 
					"api/fs/download", uri, new HttpDownloadProcessor(request, response, 0, Long.MAX_VALUE, HTTP_PROTOCOL, sessionUtils.getActiveSession(request)), HTTP_PROTOCOL);
			

		} catch (Exception e) {
			try {
				response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value());
			} catch (IOException e1) {
			}
		} finally {
			clearAuthenticatedContext();
		}
	}
	
	@AuthenticationRequired
	@RequestMapping(value = "fs/upload/**", method = RequestMethod.POST, produces = {"application/json" })
	@ResponseStatus(value = HttpStatus.OK)
	@ResponseBody
	public ResourceStatus<TreeFile> uploadFile(HttpServletRequest request,
			HttpServletResponse response,
			@RequestPart(value = "file") MultipartFile file)
			throws AccessDeniedException, UnauthorizedException, IOException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));

		try {

			String uri = FileUtils.checkEndsWithSlash(URLDecoder.decode(request.getRequestURI(), "UTF-8"));
			uri += FileUtils.lastPathElement(file.getOriginalFilename());
			
			UploadProcessor<TreeFile> processor = new UploadProcessor<TreeFile>() {

				TreeFile treeFile;
				@Override
				public void processUpload(FileResource resource,
						FileObject mountFile,
						String childPath, FileObject file) throws FileSystemException {
					treeFile = new TreeFile(file,  mountFile);
				}

				@Override
				public TreeFile getResult() {
					return treeFile;
				}
				
			};
			mountService.uploadURIFile(request.getHeader("Host"), 
					"api/fs/upload", uri, file.getInputStream(), processor, HTTP_PROTOCOL);
			
			return new ResourceStatus<TreeFile>(processor.getResult());
			
			
		} finally {
			clearAuthenticatedContext();
		}
	}
	
	@SuppressWarnings("unchecked")
	@AuthenticationRequired
	@RequestMapping(value = "fs/list/**", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public TreeList list(HttpServletRequest request,
			HttpServletResponse response) throws AccessDeniedException,
			UnauthorizedException, IOException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {

			@SuppressWarnings("rawtypes")
			List folders = new ArrayList();
			String uri = URLDecoder.decode(request.getRequestURI(), "UTF-8");
			FileResource resource = mountService.getMountForURIPath(
					request.getHeader("Host"), "api/fs/list",
					uri);

			FileObject mountFile = mountService.resolveMountFile(resource);

			String childPath = mountService.resolveURIChildPath(resource,
					"api/fs/list", uri);

			FileObject file = mountFile.resolveFile(childPath);

			for (FileObject f : file.getChildren()) {
				if (f.getType() == FileType.FOLDER
						&& (!f.isHidden() || resource.isShowHidden())) {
					folders.add(new TreeFolder(f, mountFile, resource));
				}
			}

			for (FileObject f : file.getChildren()) {
				if (f.getType() == FileType.FILE
						&& (!f.isHidden() || resource.isShowHidden())) {
					folders.add(new TreeFile(f, mountFile));
				}
			}

			return new TreeList(folders);

		} catch (FileSystemException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		} finally {
			clearAuthenticatedContext();
		}
	}

}
