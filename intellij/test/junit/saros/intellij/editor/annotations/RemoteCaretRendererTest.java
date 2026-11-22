package saros.intellij.editor.annotations;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RemoteCaretRendererTest {

  @Test
  public void testLabelAboveCaretLineOverlapsItByOnePixel() {
    assertEquals(85, RemoteCaretRenderer.labelTop(100, 16));
  }

  @Test
  public void testLabelInsideFirstLine() {
    assertEquals(0, RemoteCaretRenderer.labelTop(0, 16));
  }
}
