package com.hypersocket.fs.json;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
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
import com.hypersocket.fs.tree.TreeFile;
import com.hypersocket.fs.tree.TreeFolder;
import com.hypersocket.fs.tree.TreeList;
import com.hypersocket.json.RequestStatus;
import com.hypersocket.json.ResourceStatus;
import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.session.json.SessionTimeoutException;
import com.hypersocket.session.json.SessionUtils;
import com.hypersocket.tables.BootstrapTablesResult;
import com.hypersocket.upload.FileUpload;
import com.hypersocket.upload.FileUploadService;
import com.hypersocket.utils.FileUtils;

@Controller
public class FileSystemController extends AuthenticatedController {

	static Logger log = LoggerFactory.getLogger(FileSystemController.class);
	
	public static final String HTTP_PROTOCOL = "HTTP";
	public static final String CONTENT_INPUTSTREAM = "ContentInputStream";
	
	@Autowired
	FileResourceService mountService;

	@Autowired
	SessionUtils sessionUtils;
	
	@Autowired
	FileUploadService fileUploadService; 
	
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
	public ResourceStatus<TreeFolder> createFolder(HttpServletRequest request,
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
			
			FileObject newFile = mountService.createURIFolder(
					request.getHeader("Host"), "api/fs/createFolder",
					uri, HTTP_PROTOCOL);

			return new ResourceStatus(new TreeFolder(newFile, mountFile, resource), "");

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
					"api/fs/download", 
					uri, new HttpDownloadProcessor(
							request, 
							response, 
							0, 
							Long.MAX_VALUE, 
							HTTP_PROTOCOL, 
							sessionUtils.getActiveSession(request)), 
					HTTP_PROTOCOL);
			

		} catch (Exception e) {
			if(log.isInfoEnabled()) {
				log.error("Failed to download file", e);
			}
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
	public ResourceStatus<FileUpload> uploadFile(final HttpServletRequest request,
			HttpServletResponse response,
			final @RequestPart(value = "file") MultipartFile file)
			throws AccessDeniedException, UnauthorizedException, IOException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));

		try {

			String uri = FileUtils.checkEndsWithSlash(URLDecoder.decode(request.getRequestURI(), "UTF-8"));
			uri += FileUtils.lastPathElement(file.getOriginalFilename());

			return new ResourceStatus<FileUpload>(mountService.uploadURIFile(request.getHeader("Host"), 
					"api/fs/upload", uri, file.getInputStream(), null, HTTP_PROTOCOL));
			
			
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

	@AuthenticationRequired
	@RequestMapping(value = "fs/lastError", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public RequestStatus getLastError(HttpServletRequest request) throws UnauthorizedException, SessionTimeoutException {
		
		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {
			Throwable e = (Throwable) request.getSession().getAttribute("lastError");
			String msg = e.getMessage() + (e.getCause()!=null ? "; " + e.getCause().getMessage() : "");
			return new RequestStatus(true, msg);
		
		} finally {
			clearAuthenticatedContext();
		}
	}
	
	@SuppressWarnings("unchecked")
	@AuthenticationRequired
	@RequestMapping(value = "fs/search/**", method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public BootstrapTablesResult search(HttpServletRequest request,
			HttpServletResponse response) throws AccessDeniedException,
			UnauthorizedException, IOException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));
		try {

			int offset = Integer.parseInt(request.getParameter("offset"));
			int limit = Integer.parseInt(request.getParameter("limit"));
//			String order = request.getParameter("order");
			String search = request.getParameter("search");
			
			List<FileObject> filesAndFolders = new ArrayList<FileObject>();
			String uri = URLDecoder.decode(request.getRequestURI(), "UTF-8");
			
			FileResource resource = mountService.getMountForURIPath(
					request.getHeader("Host"), "api/fs/search",
					uri);

			FileObject mountFile = mountService.resolveMountFile(resource);

			String childPath = mountService.resolveURIChildPath(resource,
					"api/fs/search", uri);

			long totalRecords = 0;
			
			FileObject file = mountFile.resolveFile(childPath);

			for (FileObject f : file.getChildren()) {
				if(StringUtils.isNotBlank(search) && f.getName().getBaseName().indexOf(search) == -1) {
					continue;
				}
				if ((!f.isHidden() && !f.getName().getBaseName().startsWith(".")) || resource.isShowHidden()) {
					filesAndFolders.add(f);
					totalRecords++;
				} 
			}

			Collections.sort(filesAndFolders, new Comparator<FileObject>() {

				@Override
				public int compare(FileObject o1, FileObject o2) {
					try {
						if(o1.getType() == FileType.FILE) {
							return 10000 + o1.getName().getBaseName().compareToIgnoreCase(o2.getName().getBaseName());
						} else {
							return o1.getName().getBaseName().compareToIgnoreCase(o2.getName().getBaseName());
						}
					} catch (FileSystemException e) {
						return 0;
					}
				}
			});
			
			@SuppressWarnings("rawtypes")
			List result = new ArrayList();
			
			ListIterator<FileObject> itr = filesAndFolders.listIterator(offset);
			while(itr.hasNext() && limit > 0) {
				FileObject f = itr.next();
				if(f.getType()==FileType.FOLDER) {
					result.add(new TreeFolder(f, mountFile, resource));
				} else {
					result.add(new TreeFile(f, mountFile));
				}
				limit--;
			}
			
			return new BootstrapTablesResult(result, totalRecords);

		} catch (FileSystemException e) {
			request.getSession().setAttribute("lastError", e);
			throw e;
		} catch (IOException e) {
			throw e;
		} finally {
			clearAuthenticatedContext();
		}
	}
}
