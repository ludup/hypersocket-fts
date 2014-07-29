/*******************************************************************************
 * Copyright (c) 2013 Hypersocket Limited.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package upgrade;

import org.springframework.beans.factory.annotation.Autowired;

import com.hypersocket.auth.AuthenticationModuleRepository;
import com.hypersocket.auth.AuthenticationScheme;
import com.hypersocket.auth.AuthenticationSchemeRepository;
import com.hypersocket.ftp.FTPServiceImpl;

public class ftp_0_DOT_2_DOT_0 implements Runnable {

	@Autowired
	AuthenticationModuleRepository authenticationRepository;

	@Autowired
	AuthenticationSchemeRepository schemeRepository;
	
	@Override
	public void run() {

		try {
			// This file was originally and incorrectly named core_ which means
			// existing installs may execute this corrected class again.
			AuthenticationScheme s = schemeRepository.getSchemeByName(FTPServiceImpl.AUTHENTICATION_SCHEME_NAME);
			s.setResourceKey(FTPServiceImpl.AUTHENTICATION_SCHEME_RESOURCE_KEY);
			schemeRepository.saveScheme(s);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
