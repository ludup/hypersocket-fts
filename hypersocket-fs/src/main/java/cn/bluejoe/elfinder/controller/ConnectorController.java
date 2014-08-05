package cn.bluejoe.elfinder.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import cn.bluejoe.elfinder.controller.executor.CommandExecutionContext;
import cn.bluejoe.elfinder.controller.executor.CommandExecutor;
import cn.bluejoe.elfinder.controller.executor.CommandExecutorFactory;
import cn.bluejoe.elfinder.service.FsServiceFactory;

import com.hypersocket.auth.json.AuthenticatedController;
import com.hypersocket.auth.json.AuthenticationRequired;
import com.hypersocket.auth.json.UnauthorizedException;
import com.hypersocket.fs.FileResource;
import com.hypersocket.fs.FileResourceService;
import com.hypersocket.session.json.SessionTimeoutException;
import com.hypersocket.session.json.SessionUtils;

@Controller
public class ConnectorController extends AuthenticatedController {
	static Logger log = LoggerFactory.getLogger(ConnectorController.class);

	@Resource(name = "commandExecutorFactory")
	private CommandExecutorFactory _commandExecutorFactory;

	@Resource(name = "fsServiceFactory")
	private FsServiceFactory _fsServiceFactory;

	@Autowired
	SessionUtils sessionUtils;

	@Autowired
	FileResourceService fileResourceService;
	
	@AuthenticationRequired
	@RequestMapping(value = "connector/{resourceName}", method = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT })
	public void connector(HttpServletRequest request,
			final HttpServletResponse response, @PathVariable String resourceName) throws IOException, UnauthorizedException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request));

		try {

			
			List<FileResource> resources = fileResourceService.getPersonalResources(sessionUtils.getPrincipal(request));
			
			FileResource targetResource = null;
			for(FileResource resource : resources) {
				if(resource.getName().equals(resourceName)) {
					targetResource = resource;
					break;
				}
			}
			
			if(targetResource==null) {
				throw new UnauthorizedException();
			}
			
			List<FileResource> tmp = new ArrayList<FileResource>();
			tmp.add(targetResource);
			request.setAttribute("resources", tmp);
			
			String cmd = request.getParameter("cmd");
			CommandExecutor ce = _commandExecutorFactory.get(cmd);

			if (ce == null) {
				throw new FsException(String.format("unknown command: %s", cmd));
			}

			try {
				final HttpServletRequest finalRequest = request;
				ce.execute(new CommandExecutionContext() {

					@Override
					public FsServiceFactory getFsServiceFactory() {
						return _fsServiceFactory;
					}

					@Override
					public HttpServletRequest getRequest() {
						return finalRequest;
					}

					@Override
					public HttpServletResponse getResponse() {
						return response;
					}

					@Override
					public ServletContext getServletContext() {
						return finalRequest.getSession().getServletContext();
					}
				});
			} catch (Throwable e) {
				throw new FsException("unknown error", e);
			}

		} finally {
			clearAuthenticatedContext();
		}
	}
}