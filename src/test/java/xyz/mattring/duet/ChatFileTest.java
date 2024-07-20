package xyz.mattring.duet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatFileTest {

    @Test
    void testMessageTimestampProcessing() {
        String now = ChatFile.now();
        String formattedMsg = ChatFile.MSG_TEMPLATE_WITH_HEADER.formatted("Fred", now, "Hello World!");
        System.out.println(formattedMsg);
        String timestamp = ChatFile.parseTimestampFromMessage(formattedMsg).get();
        assertEquals(now, timestamp);
        System.out.println(ChatFile.timestampToLong(timestamp));
    }
}