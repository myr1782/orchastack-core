package orchastack.core.security.shiro.ext.authenticators;

import java.net.UnknownHostException;
import java.util.Dictionary;
import java.util.logging.Level;
import java.util.logging.Logger;

import jcifs.UniAddress;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbSession;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.Authenticator;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;

public class WsDomainAuthenticator extends ModularRealmAuthenticator implements
		Authenticator {

	public WsDomainAuthenticator() {
		// Auto-generated constructor stub
	}


	private static final Logger log = Logger.getLogger(WsDomainAuthenticator.class
			.getName());

	public String windowsDomainName = "github.com";
	

	public void setWindowsDomainName(String windowsDomainName) {
		this.windowsDomainName = windowsDomainName;
	}

	

	public void update(Dictionary<String, String> properties) {

		log.log(Level.INFO, "Going to update AuthenticatorImpl's Params! ");
		String domain = properties.get("windows.domain.name");
		if (domain != null)
			windowsDomainName = domain;

		log.log(Level.INFO, "Updated AuthenticatorImpl's Params! ");
	}


	public boolean doAuthenticate(String username, String passwd)
			throws Exception {

		boolean logon = false;

		// while (retry-- > 0) {

		UniAddress dc;
		try {
			dc = UniAddress.getByName(windowsDomainName);
			NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(
					windowsDomainName, username, passwd);
			SmbSession.logon(dc, auth);
			logon = true;
		} catch (UnknownHostException e) {
			log.log(Level.SEVERE,
					"doAuthenticate failed with UnknownHostException!", e);
			throw e;
		} catch (SmbException se) {
			log.log(Level.SEVERE,
					"doAuthenticate failed with Exception! status code = "
							+ se.getNtStatus(), se);

			if (se.getNtStatus() == SmbException.ERROR_NO_BROWSER_SERVERS_FOUND
					|| se.getNtStatus() == SmbException.ERROR_PIPE_BUSY
					|| se.getNtStatus() == SmbException.ERROR_PIPE_NOT_CONNECTED
					|| se.getNtStatus() == SmbException.ERROR_REQ_NOT_ACCEP
					|| se.getNtStatus() == SmbException.NT_STATUS_PATH_NOT_COVERED
					|| se.getNtStatus() == SmbException.NT_STATUS_ACCESS_DENIED
					|| se.getNtStatus() == SmbException.NT_STATUS_CANT_ACCESS_DOMAIN_INFO
					|| se.getNtStatus() == SmbException.NT_STATUS_INSTANCE_NOT_AVAILABLE
					|| se.getNtStatus() == SmbException.NT_STATUS_NETWORK_ACCESS_DENIED
					|| se.getNtStatus() == SmbException.NT_STATUS_NO_LOGON_SERVERS
					|| se.getNtStatus() == SmbException.NT_STATUS_REQUEST_NOT_ACCEPTED
					|| se.getNtStatus() == SmbException.NT_STATUS_PIPE_BROKEN
					|| se.getNtStatus() == SmbException.NT_STATUS_PIPE_BUSY
					|| se.getNtStatus() == SmbException.NT_STATUS_PIPE_NOT_AVAILABLE

			) {
				// // The tape drive is not ready yet, let's wait a little
				// // while
				// try {
				// Thread.sleep(retrySleepPeriod);
				// } catch (InterruptedException e) {
				// if (log != null)
				// log.log(LogService.LOG_WARNING,
				// "Authenticator Thread is interrupted! ");
				// else
				// logger.warn(Marker.ANY_MARKER,
				// "Authenticator Thread is interrupted! ");
				// throw e;
				// }
				//
				// if (log != null)
				// log.log(LogService.LOG_WARNING,
				// "Authenticator failed, Going to try again! ");
				// else
				// logger.warn(Marker.ANY_MARKER,
				// "Authenticator failed, Going to try again! ");

				throw new Exception("network error");

			} else if (se.getNtStatus() == SmbException.NT_STATUS_ACCOUNT_DISABLED
					|| se.getNtStatus() == SmbException.NT_STATUS_ACCOUNT_LOCKED_OUT
					|| se.getNtStatus() == SmbException.NT_STATUS_ACCOUNT_RESTRICTION) {
				throw new Exception("account error");

			} else if (se.getNtStatus() == SmbException.NT_STATUS_OK) {
				log.log(Level.SEVERE, "Authenticator logon Successful! ");

				return true;

			} else {

				log.log(Level.SEVERE, "Logon failed !!! ");

				return false;
			}

		}
		if (logon) {
			log.log(Level.FINE, "Authenticator logon Successful! ");

			return true;
		}
		// }
		return false;
	}

	class AuthenticatorTask implements Runnable {

		private String username = null;
		private String passwd = null;

		public AuthenticatorTask(String username, String passwd) {
			this.username = username;
			this.passwd = passwd;
		}

		private Exception exception = null;
		private boolean logon = false;

		private Object lock = new Object();
		private boolean finished = false;

		public void run() {
			finished = false;
			try {
				logon = doAuthenticate(username, passwd);
			} catch (Exception e) {
				exception = e;
			}
			finished = true;
		}

		public boolean isLogon() {
			while (!finished) {
				synchronized (lock) {
					try {
						lock.wait(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			return logon;
		}

		public Exception getException() {
			while (!finished) {
				synchronized (lock) {
					try {
						lock.wait(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			return this.exception;
		}

	}

	public boolean authenticate(String username, String passwd)
			throws Exception {
		return doAuthenticate(username, passwd);
	}

	public AuthenticationInfo doAuthenticate(AuthenticationToken token)
			throws AuthenticationException {
		boolean login = false;

		UsernamePasswordToken t = (UsernamePasswordToken) token;
		try {
			login = this.authenticate(t.getUsername(),
					String.valueOf(t.getPassword()));
		} catch (Exception e) {

			log.log(Level.SEVERE, "Logon failed!", e);

		}

		AuthenticationInfo info = null;

		if (login)
			info = new SimpleAuthenticationInfo(token.getPrincipal(),
					token.getCredentials(), windowsDomainName);

		return info;
	}

}
