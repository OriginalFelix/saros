package saros.net.xmpp.contact;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.XMPPError;
import org.junit.Test;

public class AddContactUtilityTest {

  /** Modern servers (RFC 6120) send only the condition, Smack reports the code as -1. */
  private static XMPPException modernError(String condition) {
    return new XMPPException(
        new XMPPError(-1, XMPPError.Type.CANCEL, condition, null, new ArrayList<>()));
  }

  private static String titleFor(XMPPException e) {
    return AddContactUtility.getDialogContent(e).dialogTitle;
  }

  @Test
  public void testServiceUnavailableWithoutLegacyCode() {
    assertEquals("Unknown Contact Status", titleFor(modernError("service-unavailable")));
  }

  @Test
  public void testLegacyCode503() {
    assertEquals("Unknown Contact Status", titleFor(new XMPPException(new XMPPError(503))));
  }

  @Test
  public void testOtherConditions() {
    assertEquals("Contact Unknown", titleFor(modernError("item-not-found")));
    assertEquals("Server Not Found", titleFor(modernError("remote-server-not-found")));
    assertEquals(
        "Unsupported Contact Status Check", titleFor(modernError("feature-not-implemented")));
    assertEquals("Unknown Error", titleFor(modernError("internal-server-error")));
  }

  @Test
  public void testNoResponse() {
    assertEquals(
        "Server Not Responding", titleFor(new XMPPException("No response from the server.")));
  }
}
