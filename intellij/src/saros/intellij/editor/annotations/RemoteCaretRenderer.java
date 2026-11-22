package saros.intellij.editor.annotations;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import org.jetbrains.annotations.NotNull;
import saros.intellij.editor.colorstorage.ColorManager;
import saros.net.util.XMPPUtils;
import saros.net.xmpp.JID;
import saros.session.User;

/**
 * Draws the caret of a remote user as a bar in the user's color with a label showing the user's
 * name at the top right of the caret.
 */
class RemoteCaretRenderer implements CustomHighlighterRenderer {
  private static final int CARET_WIDTH = 2;
  private static final int PADDING = 2;

  private final User user;

  /** The area the label was last painted in, <code>null</code> if it was never painted. */
  private Rectangle lastLabel;

  RemoteCaretRenderer(@NotNull User user) {
    this.user = user;
  }

  @Override
  public void paint(
      @NotNull Editor editor, @NotNull RangeHighlighter highlighter, @NotNull Graphics g) {

    EditorColorsScheme scheme = editor.getColorsScheme();
    TextAttributes attributes =
        scheme.getAttributes(
            ColorManager.getColorKeys(user.getColorID()).getContributionColorKey());
    Color color = attributes.getBackgroundColor();

    Point caret = editor.offsetToXY(highlighter.getStartOffset());

    g.setColor(color);
    g.fillRect(caret.x, caret.y, CARET_WIDTH, editor.getLineHeight());

    JID jid = user.getJID();
    String name = XMPPUtils.getNickname(null, jid, jid.getName());

    Font font = scheme.getFont(EditorFontType.PLAIN);
    font = font.deriveFont(font.getSize2D() * 0.75f);
    FontMetrics metrics = g.getFontMetrics(font);

    Rectangle label =
        new Rectangle(
            caret.x,
            labelTop(caret.y, metrics.getHeight()),
            metrics.stringWidth(name) + 2 * PADDING,
            metrics.getHeight());

    g.fillRect(label.x, label.y, label.width, label.height);
    g.setColor(scheme.getDefaultForeground());
    g.setFont(font);
    g.drawString(name, label.x + PADDING, label.y + metrics.getAscent());

    // The label reaches into the line above, which IntelliJ does not repaint for this highlighter.
    // shortcut: a repaint of only the line above can hide the label until the caret line is
    // repainted, draw it in an editor overlay if this turns out to be a problem
    if (!label.equals(lastLabel)) {
      repaintLabel(editor);
      lastLabel = label;

      Rectangle clip = g.getClipBounds();
      if (clip == null || !clip.contains(label)) {
        editor.getContentComponent().repaint(label);
      }
    }
  }

  /**
   * Repaints the area the label was last painted in. Must be called after the highlighter was
   * removed to not leave the label behind.
   *
   * @param editor the editor the highlighter belonged to
   */
  void repaintLabel(@NotNull Editor editor) {
    if (lastLabel != null) {
      editor.getContentComponent().repaint(lastLabel);
    }
  }

  /**
   * Returns the y coordinate of the label so that it sits above the caret line while overlapping it
   * by one pixel. The overlap makes IntelliJ paint this highlighter when the label area is
   * repainted. On the first line, the label is drawn inside the caret line instead.
   *
   * @param caretY the y coordinate of the top of the caret line
   * @param labelHeight the height of the label
   * @return the y coordinate of the top of the label
   */
  static int labelTop(int caretY, int labelHeight) {
    return Math.max(0, caretY - labelHeight + 1);
  }
}
