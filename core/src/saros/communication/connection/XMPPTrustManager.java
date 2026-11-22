package saros.communication.connection;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Validates XMPP server certificates with the JDK's PKIX trust manager and checks that the
 * certificate was issued for the XMPP domain. Replaces Smack 3's own ServerTrustManager, which does
 * not check basic constraints in the certificate chain (CVE-2014-0363).
 */
final class XMPPTrustManager implements X509TrustManager {

  private static final int SAN_DNS_NAME = 2;

  private final X509TrustManager delegate;
  private final String domain;

  XMPPTrustManager(X509TrustManager delegate, String domain) {
    this.delegate = delegate;
    this.domain = domain.toLowerCase(Locale.ROOT);
  }

  /** Returns a TLS context that trusts the JDK's CA certificates for the given XMPP domain. */
  static SSLContext createSSLContext(String domain) throws GeneralSecurityException {
    TrustManagerFactory factory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    factory.init((KeyStore) null);

    X509TrustManager jdkTrustManager =
        Arrays.stream(factory.getTrustManagers())
            .filter(X509TrustManager.class::isInstance)
            .map(X509TrustManager.class::cast)
            .findFirst()
            .orElseThrow(() -> new GeneralSecurityException("no X509 trust manager available"));

    SSLContext context = SSLContext.getInstance("TLS");
    context.init(null, new TrustManager[] {new XMPPTrustManager(jdkTrustManager, domain)}, null);
    return context;
  }

  @Override
  public void checkServerTrusted(X509Certificate[] chain, String authType)
      throws CertificateException {
    delegate.checkServerTrusted(chain, authType);

    if (!isIssuedFor(chain[0]))
      throw new CertificateException("server certificate is not valid for domain " + domain);
  }

  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType)
      throws CertificateException {
    delegate.checkClientTrusted(chain, authType);
  }

  @Override
  public X509Certificate[] getAcceptedIssuers() {
    return delegate.getAcceptedIssuers();
  }

  // shortcut: only DNS subjectAltNames are checked, not XmppAddr; add it if a server needs it
  boolean isIssuedFor(X509Certificate certificate) throws CertificateException {
    Collection<List<?>> names = certificate.getSubjectAlternativeNames();

    if (names == null) return false;

    for (List<?> name : names) {
      if (Integer.valueOf(SAN_DNS_NAME).equals(name.get(0)) && matches((String) name.get(1)))
        return true;
    }

    return false;
  }

  private boolean matches(String dnsName) {
    String pattern = dnsName.toLowerCase(Locale.ROOT);

    if (!pattern.startsWith("*.")) return pattern.equals(domain);

    // a wildcard covers exactly one leftmost label
    int firstDot = domain.indexOf('.');
    return firstDot > 0 && domain.substring(firstDot).equals(pattern.substring(1));
  }
}
