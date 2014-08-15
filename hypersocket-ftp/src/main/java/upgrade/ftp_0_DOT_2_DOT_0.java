package upgrade;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.hypersocket.auth.AuthenticationSchemeRepository;
import com.hypersocket.auth.UsernameAndPasswordAuthenticator;
import com.hypersocket.ftp.FTPServiceImpl;
import com.hypersocket.realm.Realm;
import com.hypersocket.realm.RealmRepository;

public class ftp_0_DOT_2_DOT_0 implements Runnable {

	@Autowired
	RealmRepository realmRepository;
	
	@Autowired
	AuthenticationSchemeRepository schemeRepository;
	
	@Override
	public void run() {
	

		for (Realm realm : realmRepository.allRealms()) {
			if (schemeRepository.getSchemeByResourceKey(realm,
					FTPServiceImpl.AUTHENTICATION_SCHEME_RESOURCE_KEY) == null) {
				List<String> modules = new ArrayList<String>();
				modules.add(UsernameAndPasswordAuthenticator.RESOURCE_KEY);
				schemeRepository.createScheme(realm,
						FTPServiceImpl.AUTHENTICATION_SCHEME_NAME, modules,
						FTPServiceImpl.AUTHENTICATION_SCHEME_RESOURCE_KEY, true);
			}
		}
	}

}
