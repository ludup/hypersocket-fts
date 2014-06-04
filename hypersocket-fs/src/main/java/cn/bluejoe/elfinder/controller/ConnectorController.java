package cn.bluejoe.elfinder.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import cn.bluejoe.elfinder.controller.executor.CommandExecutionContext;
import cn.bluejoe.elfinder.controller.executor.CommandExecutor;
import cn.bluejoe.elfinder.controller.executor.CommandExecutorFactory;
import cn.bluejoe.elfinder.service.FsServiceFactory;

import com.hypersocket.auth.json.AuthenticatedController;
import com.hypersocket.auth.json.AuthenticationRequired;
import com.hypersocket.auth.json.UnauthorizedException;
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
	@RequestMapping(value = "connector", method = { RequestMethod.GET, RequestMethod.POST })
	public void connector(HttpServletRequest request,
			final HttpServletResponse response) throws IOException, UnauthorizedException, SessionTimeoutException {

		setupAuthenticatedContext(sessionUtils.getSession(request),
				sessionUtils.getLocale(request),
				fileResourceService);

		try {
			try {
				request = parseMultipartContent(request);
			} catch (Exception e) {
				throw new IOException(e.getMessage());
			}

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
				e.printStackTrace();
				throw new FsException("unknown error", e);
			}

		} finally {
			clearAuthenticatedContext(fileResourceService);
		}
	}

	private HttpServletRequest parseMultipartContent(
			final HttpServletRequest request) throws Exception {
		if (!ServletFileUpload.isMultipartContent(request))
			return request;

		final Map<String, String> requestParams = new HashMap<String, String>();
		List<FileItemStream> listFiles = new ArrayList<FileItemStream>();

		// Parse the request
		ServletFileUpload sfu = new ServletFileUpload();
		String characterEncoding = request.getCharacterEncoding();
		sfu.setHeaderEncoding(characterEncoding);
		FileItemIterator iter = sfu.getItemIterator(request);

		while (iter.hasNext()) {
			final FileItemStream item = iter.next();
			String name = item.getFieldName();
			InputStream stream = item.openStream();
			if (item.isFormField()) {
				requestParams.put(name,
						Streams.asString(stream, characterEncoding));
			} else {
				String fileName = item.getName();
				if (fileName != null && !"".equals(fileName.trim())) {
					ByteArrayOutputStream os = new ByteArrayOutputStream();
					IOUtils.copy(stream, os);
					final byte[] bs = os.toByteArray();
					stream.close();

					listFiles.add((FileItemStream) Proxy.newProxyInstance(this
							.getClass().getClassLoader(),
							new Class[] { FileItemStream.class },
							new InvocationHandler() {
								@Override
								public Object invoke(Object proxy,
										Method method, Object[] args)
										throws Throwable {
									if ("openStream".equals(method.getName())) {
										return new ByteArrayInputStream(bs);
									}

									return method.invoke(item, args);
								}
							}));
				}
			}
		}

		request.setAttribute(FileItemStream.class.getName(), listFiles);
		return (HttpServletRequest) Proxy.newProxyInstance(this.getClass()
				.getClassLoader(), new Class[] { HttpServletRequest.class },
				new InvocationHandler() {
					@Override
					public Object invoke(Object arg0, Method arg1, Object[] arg2)
							throws Throwable {
						if ("getParameter".equals(arg1.getName())) {
							return requestParams.get(arg2[0]);
						}

						return arg1.invoke(request, arg2);
					}
				});
	}
}