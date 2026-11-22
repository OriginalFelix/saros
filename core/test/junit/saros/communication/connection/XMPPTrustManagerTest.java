package saros.communication.connection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.net.ssl.X509TrustManager;
import org.easymock.EasyMock;
import org.junit.Test;

public class XMPPTrustManagerTest {

  private static X509Certificate certificate(String... dnsNames) throws Exception {
    Collection<List<?>> names = new ArrayList<>();
    for (String dnsName : dnsNames) names.add(List.of(2, dnsName));
    names.add(List.of(7, "127.0.0.1")); // IP address entries must be ignored

    X509Certificate certificate = EasyMock.createNiceMock(X509Certificate.class);
    EasyMock.expect(certificate.getSubjectAlternativeNames()).andStubReturn(names);
    EasyMock.replay(certificate);
    return certificate;
  }

  private static XMPPTrustManager trustManager(String domain) {
    X509TrustManager trustAll = EasyMock.createNiceMock(X509TrustManager.class);
    EasyMock.replay(trustAll);
    return new XMPPTrustManager(trustAll, domain);
  }

  @Test
  public void testExactAndWildcardNames() throws Exception {
    XMPPTrustManager manager = trustManager("Jabber.Example.org");

    assertTrue(manager.isIssuedFor(certificate("other.org", "jabber.example.org")));
    assertTrue(manager.isIssuedFor(certificate("*.example.org")));
  }

  @Test
  public void testForeignNamesAreRejected() throws Exception {
    XMPPTrustManager manager = trustManager("example.org");

    assertFalse(manager.isIssuedFor(certificate("evil.org")));
    assertFalse(manager.isIssuedFor(certificate("*.example.org")));
    assertFalse(manager.isIssuedFor(certificate("example.org.evil.org")));
    assertFalse(trustManager("a.b.example.org").isIssuedFor(certificate("*.example.org")));
    assertFalse(manager.isIssuedFor(certificate()));
  }

  @Test(expected = CertificateException.class)
  public void testValidChainForOtherDomainIsRejected() throws Exception {
    trustManager("example.org")
        .checkServerTrusted(new X509Certificate[] {certificate("evil.org")}, "RSA");
  }

  @Test(expected = CertificateException.class)
  public void testChainRejectedByJdkIsRejected() throws Exception {
    X509TrustManager rejecting = EasyMock.createMock(X509TrustManager.class);
    rejecting.checkServerTrusted(EasyMock.anyObject(), EasyMock.anyString());
    EasyMock.expectLastCall().andThrow(new CertificateException("untrusted"));
    EasyMock.replay(rejecting);

    new XMPPTrustManager(rejecting, "example.org")
        .checkServerTrusted(new X509Certificate[] {certificate("example.org")}, "RSA");
  }

  @Test
  public void testContextUsesJdkTrustStore() throws Exception {
    assertNotNull(XMPPTrustManager.createSSLContext("example.org").getSocketFactory());
  }
}
