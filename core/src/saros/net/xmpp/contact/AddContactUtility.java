package saros.net.xmpp.contact;

import java.util.Objects;
import java.util.function.BiPredicate;
import org.apache.log4j.Logger;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.XMPPError;
import saros.SarosConstants;
import saros.exceptions.OperationCanceledException;
import saros.net.util.XMPPUtils;
import saros.net.xmpp.JID;
import saros.net.xmpp.XMPPConnectionService;

/**
 * This class allows to add a contact on the XMPP Roster.
 *
 * <p>TODO This code is mainly moved code from Eclipse {@code AddContactWizard} and has space for
 * improvements:
 *
 * <ul>
 *   <li>hard coded messages should be extracted
 *   <li>usage of BiPredicate for dialog handling should be changed to a different approach
 *   <li>isJIDonServer is not working reliable
 *   <li>handling in getDialogContent is not correct working in all cases / for all xmpp servers
 * </ul>
 *
 * but probably update smack first.
 */
class AddContactUtility {
  private static final Logger log = Logger.getLogger(AddContactUtility.class);

  static final class DialogContent {
    final String dialogTitle;
    private final String dialogMessage;
    private final String invocationTargetExceptionMessage;

    DialogContent(
        String dialogTitle, String dialogMessage, String invocationTargetExceptionMessage) {
      this.dialogTitle = dialogTitle;
      this.dialogMessage = dialogMessage;
      this.invocationTargetExceptionMessage = invocationTargetExceptionMessage;
    }
  }

  private AddContactUtility() {
    // Helper Class
  }

  /**
   * @see XMPPContactsService#addContact(JID, String, BiPredicate)
   * @throws OperationCanceledException If information about JID can not be found / retrieved from
   *     Server and User canceled further trying, or Smack experienced an error.
   */
  static void addToRoster(
      XMPPConnectionService connectionService,
      JID jid,
      String nickname,
      BiPredicate<String, String> questionDialog)
      throws OperationCanceledException {
    Connection connection =
        Objects.requireNonNull(connectionService.getConnection(), "connection is null");
    Objects.requireNonNull(jid, "jid is null");

    try {
      if (!XMPPUtils.isJIDonServer(connection, jid, SarosConstants.RESOURCE)) {
        if (!questionDialog.test(
            "Contact Unknown",
            "You entered a valid XMPP server.\n\n"
                + "Unfortunately your entered JID is unknown to the server.\n"
                + "Please make sure you spelled the JID correctly.\n\n"
                + "Do you want to add the contact anyway?")) {
          throw new OperationCanceledException("Please make sure you spelled the JID correctly.");
        }

        log.debug(
            "The contact "
                + jid
                + " couldn't be found on the server."
                + " The user chose to add it anyway.");
      }
    } catch (XMPPException e) {
      DialogContent dialogContent = getDialogContent(e);

      if (!questionDialog.test(dialogContent.dialogTitle, dialogContent.dialogMessage))
        throw new OperationCanceledException(dialogContent.invocationTargetExceptionMessage);

      log.warn(
          "Problem while adding a contact.CancellationException User decided to add contact anyway. Problem: "
              + e.getMessage());
    }

    try {
      connection.getRoster().createEntry(jid.getBase(), nickname, null);
    } catch (XMPPException e) {
      log.error("Problem while adding a contact.", e);
      throw new OperationCanceledException("Problem while adding a contact:" + e.getMessage());
    }
  }

  private static final int NO_CODE = -2;

  /**
   * Returns whether the exception carries the given RFC 6120 error condition or, for older servers,
   * the given legacy error code (-1 means "no code" in Smack, so NO_CODE never matches).
   */
  private static boolean hasError(XMPPException e, String condition, int legacyCode) {
    XMPPError error = e.getXMPPError();

    if (error != null)
      return condition.equals(error.getCondition()) || error.getCode() == legacyCode;

    return String.valueOf(e.getMessage()).contains(condition);
  }

  static DialogContent getDialogContent(XMPPException e) {

    if (hasError(e, "item-not-found", NO_CODE)) {
      return new DialogContent(
          "Contact Unknown",
          "The contact is unknown to the XMPP server.\n\n"
              + "Do you want to add the contact anyway?",
          "Contact unknown to XMPP server.");
    }

    if (hasError(e, "remote-server-not-found", NO_CODE)) {
      return new DialogContent(
          "Server Not Found",
          "The responsible XMPP server could not be found.\n\n"
              + "Do you want to add the contact anyway?",
          "Unable to find the responsible XMPP server.");
    }

    if (hasError(e, "feature-not-implemented", 501)) {
      return new DialogContent(
          "Unsupported Contact Status Check",
          "The responsible XMPP server does not support status requests.\n\n"
              + "If the contact exists you can still successfully add him.\n\n"
              + "Do you want to try to add the contact?",
          "Contact status check unsupported by XMPP server.");
    }

    if (hasError(e, "service-unavailable", 503)) {
      return new DialogContent(
          "Unknown Contact Status",
          "For privacy reasons the XMPP server does not reply to status requests.\n\n"
              + "If the contact exists you can still successfully add him.\n\n"
              + "Do you want to try to add the contact?",
          "Unable to check the contact status.");
    }

    if (String.valueOf(e.getMessage()).contains("No response from the server")) {
      return new DialogContent(
          "Server Not Responding",
          "The responsible XMPP server is not connectable.\n"
              + "The server is either inexistent or offline right now.\n\n"
              + "Do you want to add the contact anyway?",
          "The XMPP server did not respond.");
    }

    return new DialogContent(
        "Unknown Error",
        "An unknown error has occured:\n\n"
            + e.getMessage()
            + "\n\n"
            + "Do you want to add the contact anyway?",
        "Unknown error: " + e.getMessage());
  }
}
