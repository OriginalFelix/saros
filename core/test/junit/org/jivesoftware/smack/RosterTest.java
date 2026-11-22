package org.jivesoftware.smack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.RosterPacket;
import org.junit.Before;
import org.junit.Test;

public class RosterTest {

  private Roster roster;
  private PacketListener rosterListener;

  @Before
  public void setUp() {
    Connection connection = EasyMock.createNiceMock(Connection.class);
    EasyMock.expect(connection.getUser()).andStubReturn("alice@example.org/Saros");

    Capture<PacketListener> listeners = Capture.newInstance(CaptureType.ALL);
    connection.addPacketListener(
        EasyMock.capture(listeners), EasyMock.anyObject(PacketFilter.class));
    EasyMock.expectLastCall().anyTimes();
    EasyMock.replay(connection);

    roster = new Roster(connection);
    // the first registered listener handles roster packets
    rosterListener = listeners.getValues().get(0);
  }

  private static RosterPacket push(String from) {
    RosterPacket packet = new RosterPacket();
    packet.setType(IQ.Type.SET);
    packet.setFrom(from);
    packet.addRosterItem(new RosterPacket.Item("mallory@evil.org", "Mallory"));
    return packet;
  }

  @Test
  public void testPushFromForeignSenderIsIgnored() {
    rosterListener.processPacket(push("mallory@evil.org/x"));

    assertEquals(0, roster.getEntryCount());
  }

  @Test
  public void testPushFromServerOrOwnAccountIsApplied() {
    rosterListener.processPacket(push(null));
    assertEquals(1, roster.getEntryCount());

    assertTrue(roster.isValidRosterSender("Alice@example.org"));
    assertFalse(roster.isValidRosterSender("alice@example.org.evil.org"));
  }
}
