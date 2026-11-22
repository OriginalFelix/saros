package saros.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class CoreUtilsTest {

  @Test
  public void testAbbreviateForLogEscapes() {
    assertEquals("a\\nb\\t\\\"c\\\\ \\u0000 ä", CoreUtils.abbreviateForLog("a\nb\t\"c\\ \0 ä"));
    assertNull(CoreUtils.abbreviateForLog(null));
  }

  @Test
  public void testAbbreviateForLogShortens() {
    assertEquals(150, CoreUtils.abbreviateForLog("x".repeat(500)).length());
  }
}
