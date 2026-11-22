package saros.misc.xstream;

import static org.junit.Assert.assertEquals;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.ForbiddenClassException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class XStreamFactoryTest {

  private final XStream xstream = XStreamFactory.getSecureXStream();

  @Test
  public void testJdkCollectionsAreAllowed() {
    List<String> list = new ArrayList<>(List.of("a", "b"));
    Map<String, String> map = new HashMap<>(Map.of("k", "v"));

    assertEquals(list, xstream.fromXML(xstream.toXML(list)));
    assertEquals(map, xstream.fromXML(xstream.toXML(map)));
  }

  @Test(expected = ForbiddenClassException.class)
  public void testNonJdkMapIsRejected() {
    xstream.fromXML("<javax.management.openmbean.TabularDataSupport/>");
  }

  @Test(expected = ForbiddenClassException.class)
  public void testRepackagedThirdPartyCodeIsRejected() {
    xstream.fromXML("<saros.repackaged.picocontainer.DefaultPicoContainer/>");
  }

  @Test(expected = ForbiddenClassException.class)
  public void testArbitraryJdkClassIsRejected() {
    xstream.fromXML("<java.lang.ProcessBuilder/>");
  }
}
