package it.polimi.ingsw.common.message;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.*;

class BaseMessageTest {

    // Concrete implementation of BaseMessage for testing
    static class ConcreteMessage extends BaseMessage {
        private static final long serialVersionUID = 1L;
        private final String data;

        public ConcreteMessage(String data) {
            super();
            this.data = data;
        }

        public String getData() {
            return data;
        }
    }

    @Test
    void baseMessageImplementsMessageInterface() {
        assertTrue(Message.class.isAssignableFrom(BaseMessage.class),
                "BaseMessage should implement Message interface");
    }

    @Test
    void baseMessageIsSerializable() {
        assertTrue(Serializable.class.isAssignableFrom(BaseMessage.class),
                "BaseMessage should implement Serializable");
    }

    @Test
    void baseMessageTimestampIsSetOnConstruction() {
        long beforeCreation = System.currentTimeMillis();
        ConcreteMessage message = new ConcreteMessage("test");
        long afterCreation = System.currentTimeMillis();

        assertTrue(message.getTimestamp() >= beforeCreation,
                "Timestamp should be set at or after object creation time");
        assertTrue(message.getTimestamp() <= afterCreation,
                "Timestamp should be set at or before after-creation time");
    }

    @Test
    void concreteMessagesRetainTheirData() {
        ConcreteMessage message = new ConcreteMessage("test data");
        assertEquals("test data", message.getData());
        
        // Test multiple instances
        ConcreteMessage message2 = new ConcreteMessage("other data");
        assertEquals("other data", message2.getData(), "Second message should have its own data");
        assertEquals("test data", message.getData(), "Original message should retain its data");
    }
    
    @Test
    void consecutiveMessagesHaveDifferentTimestamps() throws InterruptedException {
        ConcreteMessage message1 = new ConcreteMessage("first");
        
        // Sleep a small amount to ensure different timestamps
        Thread.sleep(5);
        
        ConcreteMessage message2 = new ConcreteMessage("second");
        
        assertNotEquals(message1.getTimestamp(), message2.getTimestamp(),
                "Messages created at different times should have different timestamps");
        
        assertTrue(message1.getTimestamp() < message2.getTimestamp(),
                "Later message should have a higher timestamp value");
    }
    
    @Test
    void baseMessageCanBeSerialized() throws IOException, ClassNotFoundException {
        ConcreteMessage original = new ConcreteMessage("serialization test");
        long originalTimestamp = original.getTimestamp();
        
        // Serialize
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
        objectStream.writeObject(original);
        objectStream.close();
        
        // Deserialize
        ByteArrayInputStream byteInputStream = new ByteArrayInputStream(byteStream.toByteArray());
        ObjectInputStream objectInputStream = new ObjectInputStream(byteInputStream);
        ConcreteMessage deserialized = (ConcreteMessage) objectInputStream.readObject();
        objectInputStream.close();
        
        // Check data and timestamp are preserved
        assertEquals("serialization test", deserialized.getData(), 
                "Data field should be preserved during serialization");
        assertEquals(originalTimestamp, deserialized.getTimestamp(), 
                "Timestamp should be preserved during serialization");
    }
    
    @Test
    void multipleInstancesAreIndependent() {
        ConcreteMessage message1 = new ConcreteMessage("first data");
        ConcreteMessage message2 = new ConcreteMessage("second data");
        
        // Verify instances maintain separate data
        assertNotEquals(message1.getData(), message2.getData());
        
        // Also check timestamps (should differ but this is not guaranteed if created very quickly)
        assertNotSame(message1, message2, "Different instances should be different objects");
    }
} 