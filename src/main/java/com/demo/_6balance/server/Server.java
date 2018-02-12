package com.demo._6balance.server;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

public interface Server {

	public void bind() throws CertificateException, SSLException;

}
