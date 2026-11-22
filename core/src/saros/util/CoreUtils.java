package saros.util;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import saros.net.util.XMPPUtils;
import saros.net.xmpp.JID;
import saros.session.User;

public class CoreUtils {

  /**
   * Returns a string representation of the throughput when processing the given number of bytes in
   * the given time in milliseconds.
   */
  public static String throughput(long length, long deltaMs) {

    String duration = null;

    if (deltaMs == 0) {
      duration = "< 1 ms";
      deltaMs = 1;
    }

    if (duration == null) duration = deltaMs < 1000 ? "< 1 s" : formatDuration(deltaMs / 1000);

    return formatByte(length)
        + " in "
        + duration
        + " at "
        + formatByte(length / deltaMs * 1000)
        + "/s";
  }

  /**
   * Turns a long representing a file size in bytes into a human readable representation based on
   * 1KB = 1000 Byte, 1000 KB=1MB, etc. (SI)
   */
  public static String formatByte(long bytes) {
    int unit = 1000;
    if (bytes < unit) return bytes + " B";
    int exp = (int) (Math.log(bytes) / Math.log(unit));
    char pre = ("kMGTPE").charAt(exp - 1);
    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
  }

  /**
   * Formats a given duration in seconds (e.g. achieved by using a StopWatch) as HH:MM:SS
   *
   * @param seconds
   * @return
   */
  public static String formatDuration(long seconds) {
    String format = "";

    if (seconds <= 0L) {
      return "";
    }
    long hours = seconds / 3600;
    long minutes = (seconds % 3600) / 60;
    long secs = (seconds % 60);

    format += hours > 0 ? String.format("%02d", hours) + "h " : "";
    format += minutes > 0 ? String.format(hours > 0 ? "%02d" : "%d", minutes) + "m " : "";
    format += seconds > 0 ? String.format(minutes > 0 ? "%02d" : "%d", secs) + "s" : "";
    return format;
  }

  /**
   * This method formats patterns for display in the UI by replacing occurrences of {@link User}
   * objects by {@link ModelFormatUtils#determineUserDisplayName(User)}.
   *
   * @param pattern
   * @param arguments occurrences of User objects are replaced by their display name
   * @return the formatted string
   */
  public static String format(String pattern, Object... arguments) {
    List<Object> mappedValues = new ArrayList<Object>(arguments.length);
    for (Object obj : arguments) {
      if (obj instanceof User) {
        User user = (User) obj;
        mappedValues.add(determineUserDisplayName(user));
      } else {
        mappedValues.add(obj);
      }
    }
    return MessageFormat.format(pattern, mappedValues.toArray());
  }

  /**
   * Retrieves a user's nickname from the XMPP roster. If none is present it returns the base name.
   *
   * @param user
   * @return the user's nickname, or if none is set JID's base.
   */
  public static String determineUserDisplayName(User user) {
    JID jid = user.getJID();
    return XMPPUtils.getNickname(null, jid, jid.getBase());
  }

  /**
   * Shortens the given text to 150 characters and escapes it like {@link #escapeForLog(String)}.
   *
   * @param text the text to shorten, may be <code>null</code>
   * @return the shortened and escaped text or <code>null</code> if the text is <code>null</code>
   */
  public static String abbreviateForLog(String text) {
    return escapeForLog(StringUtils.abbreviate(text, 150));
  }

  /**
   * Escapes backslashes, quotes and control characters, so the text fits on one log line.
   *
   * @param text the text to escape, may be <code>null</code>
   * @return the escaped text or <code>null</code> if the text is <code>null</code>
   */
  public static String escapeForLog(String text) {
    if (text == null) return null;

    StringBuilder escaped = new StringBuilder(text.length());

    for (char c : text.toCharArray()) {
      if (c == '\n') escaped.append("\\n");
      else if (c == '\r') escaped.append("\\r");
      else if (c == '\t') escaped.append("\\t");
      else if (c == '\\' || c == '"') escaped.append('\\').append(c);
      else if (Character.isISOControl(c)) escaped.append(String.format("\\u%04x", (int) c));
      else escaped.append(c);
    }

    return escaped.toString();
  }
}
