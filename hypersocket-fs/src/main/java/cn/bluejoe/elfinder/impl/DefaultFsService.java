package cn.bluejoe.elfinder.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.bluejoe.elfinder.service.FsItem;
import cn.bluejoe.elfinder.service.FsSecurityChecker;
import cn.bluejoe.elfinder.service.FsService;
import cn.bluejoe.elfinder.service.FsServiceConfig;
import cn.bluejoe.elfinder.service.FsVolume;

import com.hypersocket.auth.json.UnauthorizedException;
import com.hypersocket.fs.FileResource;
import com.hypersocket.fs.FileResourceService;
import com.hypersocket.fs.elfinder.FileResourcesFsVolume;
import com.hypersocket.netty.Request;
import com.hypersocket.realm.Principal;
import com.hypersocket.session.json.SessionUtils;

@Service
public class DefaultFsService implements FsService
{
	FsSecurityChecker _securityChecker;

	FsServiceConfig _serviceConfig;

	@Autowired
	SessionUtils sessionUtils;
	
	@Autowired
	FileResourceService fileResourceService;
	
	public FsServiceConfig getServiceConfig()
	{
		return _serviceConfig;
	}

	public void setServiceConfig(FsServiceConfig serviceConfig)
	{
		_serviceConfig = serviceConfig;
	}

	Map<Principal,List<FsVolume>> userVolumes = new HashMap<Principal,List<FsVolume>>();
	Map<Principal,Map<FsVolume,String>> userVolumeIds = new HashMap<Principal,Map<FsVolume,String>>();
	//Map<FsVolume, String> _volumeIds = new HashMap<FsVolume, String>();

	//FsVolume[] _volumes;

	String[][] escapes = { { "+", "_P" }, { "-", "_M" }, { "/", "_S" }, { ".", "_D" }, { "=", "_E" } };

	@Override
	public FsItem fromHash(String hash)
	{
		for (FsVolume v : getUserVolumes())
		{
			String prefix = getVolumeId(v) + "_";

			if (hash.equals(prefix))
			{
				return v.getRoot();
			}

			if (hash.startsWith(prefix))
			{
				String localHash = hash.substring(prefix.length());

				for (String[] pair : escapes)
				{
					localHash = localHash.replace(pair[1], pair[0]);
				}

				String relativePath = new String(Base64.decodeBase64(localHash.getBytes()));
				return v.fromPath(relativePath);
			}
		}

		return null;
	}

	private List<FsVolume> getUserVolumes() {
		
		try {
			Principal principal = sessionUtils.getPrincipal(Request.get());
			
//			if(userVolumes.containsKey(principal)) {
//				return userVolumes.get(principal);
//			}
			
			List<FileResource> resources = fileResourceService.getResources(principal);
			
			List<FsVolume> volumes = new ArrayList<FsVolume>();
			Map<FsVolume, String> volumeIds = new HashMap<FsVolume,String>();
			
			char vid = 'A';
			for(FileResource r : resources) {
				FsVolume v = new FileResourcesFsVolume(r, fileResourceService);
				volumeIds.put(v,"" + vid);
				volumes.add(v);
				vid++;
			}
			
			userVolumes.put(principal, volumes);
			userVolumeIds.put(principal, volumeIds);
			return volumes;
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public String getHash(FsItem item) throws IOException
	{
		String relativePath = item.getVolume().getPath(item);
		String base = new String(Base64.encodeBase64(relativePath.getBytes()));

		for (String[] pair : escapes)
		{
			base = base.replace(pair[0], pair[1]);
		}

		return getVolumeId(item.getVolume()) + "_" + base;
	}

	public FsSecurityChecker getSecurityChecker()
	{
		return _securityChecker;
	}

	@Override
	public String getVolumeId(FsVolume volume)
	{
		try {
			Principal principal = sessionUtils.getPrincipal(Request.get());
			
			if(userVolumeIds.containsKey(principal)) {
				return userVolumeIds.get(principal).get(volume);
			}
		} catch (UnauthorizedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public FsVolume[] getVolumes()
	{
		return getUserVolumes().toArray(new FsVolume[0]);
	}

	public void setSecurityChecker(FsSecurityChecker securityChecker)
	{
		_securityChecker = securityChecker;
	}
}
