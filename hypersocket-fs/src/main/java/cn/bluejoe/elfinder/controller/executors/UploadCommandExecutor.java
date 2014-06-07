package cn.bluejoe.elfinder.controller.executors;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.DefaultMultipartHttpServletRequest;

import cn.bluejoe.elfinder.controller.executor.AbstractJsonCommandExecutor;
import cn.bluejoe.elfinder.controller.executor.CommandExecutor;
import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsService;

public class UploadCommandExecutor extends AbstractJsonCommandExecutor implements CommandExecutor
{
	@Override
	public void execute(FsService fsService, HttpServletRequest request, ServletContext servletContext, JSONObject json)
			throws Exception
	{
		DefaultMultipartHttpServletRequest multipartRequest = (DefaultMultipartHttpServletRequest) request;
		List<FsItemEx> added = new ArrayList<FsItemEx>();
		String target = request.getParameter("target");
		FsItemEx dir = super.findItem(fsService, target);
		
		for(MultipartFile file : multipartRequest.getFileMap().values()) {
			FsItemEx newFile = new FsItemEx(dir, file.getOriginalFilename());
			newFile.createFile();
			InputStream is = file.getInputStream();
			OutputStream os = newFile.openOutputStream();

			IOUtils.copy(is, os);
			os.close();
			is.close();

			added.add(newFile);
		}

		json.put("added", files2JsonArray(request, added));
	}
}
