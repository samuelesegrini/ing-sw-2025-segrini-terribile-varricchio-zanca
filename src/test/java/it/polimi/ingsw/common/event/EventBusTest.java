package it.polimi.ingsw.common.event;

import it.polimi.ingsw.common.message.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class EventBusTest {

    private EventBus eventBus;

    // --- Test Message Classes ---
    static class TestMessage implements Message {
        final String content;
        TestMessage(String content) { this.content = content; }
        
        @Override
        public String toString() {
            return "TestMessage{content='" + content + "'}";
        }
    }

    static class AnotherTestMessage implements Message {
        final int value;
        AnotherTestMessage(int value) { this.value = value; }
        
        @Override
        public String toString() {
            return "AnotherTestMessage{value=" + value + "}";
        }
    }

    static class SpecialTestMessage extends TestMessage {
        SpecialTestMessage(String content) { super(content); }
        
        @Override
        public String toString() {
            return "SpecialTestMessage{content='" + content + "'}";
        }
    }

    interface TestEventInterface extends Message {}
    
    static class InterfaceImplTestMessage implements TestEventInterface {
        final String data = "InterfaceData";
        
        @Override
        public String toString() {
            return "InterfaceImplTestMessage{data='" + data + "'}";
        }
    }

    // --- Test Listener Classes ---
    static class SimpleListener {
        boolean specificHandlerCalled = false;
        String receivedContent = null;

        @MessageHandler
        public void handleTestMessage(TestMessage message) {
            specificHandlerCalled = true;
            receivedContent = message.content;
        }
    }

    static class MultiHandlerListener {
        int testMessagesHandled = 0;
        int anotherTestMessagesHandled = 0;

        @MessageHandler
        public void handleTest(TestMessage message) {
            testMessagesHandled++;
        }

        @MessageHandler
        public void handleAnother(AnotherTestMessage message) {
            anotherTestMessagesHandled++;
        }
    }

    static class PolymorphicListener {
        boolean baseHandlerCalled = false;
        boolean specialHandlerCalled = false;
        boolean interfaceHandlerCalled = false;

        @MessageHandler
        public void handleBaseMessage(TestMessage message) { // Handles TestMessage and SpecialTestMessage
            baseHandlerCalled = true;
        }

        @MessageHandler
        public void handleSpecialMessage(SpecialTestMessage message) { // Only handles SpecialTestMessage
            specialHandlerCalled = true;
        }

        @MessageHandler
        public void handleInterfaceMessage(TestEventInterface message) { // Handles InterfaceImplTestMessage
            interfaceHandlerCalled = true;
        }
    }

    static class AsyncListener {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean handlerCalled = new AtomicBoolean(false);
        String threadName = null;

        @MessageHandler
        public void handleAsync(TestMessage message) {
            threadName = Thread.currentThread().getName();
            handlerCalled.set(true);
            latch.countDown();
        }
    }

    static class PrivateMethodListener {
        AtomicBoolean handlerCalled = new AtomicBoolean(false);
        
        @MessageHandler
        private void handlePrivate(TestMessage message) { // Test private method handling
            handlerCalled.set(true);
        }
    }

    static class ErrorThrowingListener {
        AtomicBoolean handlerCalled = new AtomicBoolean(false);
        
        @MessageHandler
        public void willThrowError(TestMessage message) {
            handlerCalled.set(true);
            throw new RuntimeException("Test error in handler");
        }
    }
    
    static class InvalidHandlerListener {
        // Handler with no parameters (invalid)
        @MessageHandler
        public void invalidNoParams() {
            // This should be skipped during registration
        }
        
        // Handler with multiple parameters (invalid)
        @MessageHandler
        public void invalidMultipleParams(TestMessage msg1, AnotherTestMessage msg2) {
            // This should be skipped during registration
        }
        
        // Handler with non-Message parameter (invalid)
        @MessageHandler
        public void invalidParamType(String notAMessage) {
            // This should be skipped during registration
        }
    }

    @BeforeEach
    void setUp() {
        // Using a unique prefix for each test run helps if logs are interleaved
        eventBus = new EventBus(2, "test-eb-" + System.nanoTime());
    }

    @AfterEach
    void tearDown() {
        eventBus.shutdown();
    }

    @Test
    void registerAndPostMessage() throws InterruptedException {
        SimpleListener listener = new SimpleListener();
        eventBus.register(listener);
        
        TestMessage message = new TestMessage("hello");
        eventBus.post(message);
        
        // Wait for async processing
        Thread.sleep(100);
        
        assertTrue(listener.specificHandlerCalled, "Handler should be called after posting message");
        assertEquals("hello", listener.receivedContent, "Handler should receive the correct message content");
    }

    @Test
    void multipleHandlersForDifferentMessages() throws InterruptedException {
        MultiHandlerListener listener = new MultiHandlerListener();
        eventBus.register(listener);
        
        eventBus.post(new TestMessage("first"));
        eventBus.post(new AnotherTestMessage(42));
        
        // Wait for async processing
        Thread.sleep(100);
        
        assertEquals(1, listener.testMessagesHandled, "TestMessage handler should be called once");
        assertEquals(1, listener.anotherTestMessagesHandled, "AnotherTestMessage handler should be called once");
    }

    @Test
    void polymorphicMessageHandling() throws InterruptedException {
        PolymorphicListener listener = new PolymorphicListener();
        eventBus.register(listener);
        
        // Post a base message - should trigger only base handler
        eventBus.post(new TestMessage("base"));
        Thread.sleep(50);
        
        assertTrue(listener.baseHandlerCalled, "Base handler should be called for base message");
        assertFalse(listener.specialHandlerCalled, "Special handler should not be called for base message");
        
        // Reset flags
        listener.baseHandlerCalled = false;
        listener.specialHandlerCalled = false;
        
        // Post a special message - should trigger both handlers
        eventBus.post(new SpecialTestMessage("special"));
        Thread.sleep(50);
        
        assertTrue(listener.baseHandlerCalled, "Base handler should be called for special message");
        assertTrue(listener.specialHandlerCalled, "Special handler should be called for special message");
        
        // Post an interface implementation message
        listener.interfaceHandlerCalled = false;
        eventBus.post(new InterfaceImplTestMessage());
        Thread.sleep(50);
        
        assertTrue(listener.interfaceHandlerCalled, "Interface handler should be called for interface implementation message");
    }

    @Test
    void unregisterListenerStopsReceivingEvents() throws InterruptedException {
        SimpleListener listener = new SimpleListener();
        eventBus.register(listener);
        eventBus.post(new TestMessage("first"));
        Thread.sleep(100); // Ensure first event is processed

        assertTrue(listener.specificHandlerCalled, "Handler should be called before unregister.");
        listener.specificHandlerCalled = false; // Reset for next check

        eventBus.unregister(listener);
        eventBus.post(new TestMessage("second"));
        Thread.sleep(100); // Give time for potential (incorrect) processing

        assertFalse(listener.specificHandlerCalled, "Handler should not be called after unregister.");
    }

    @Test
    void postEventWithNoHandlers() {
        // No exception should be thrown, just logged (if logging level is appropriate)
        assertDoesNotThrow(() -> eventBus.post(new TestMessage("lonely event")));
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS) // Ensure async operation completes
    void asyncHandlingExecutesOnDifferentThread() throws InterruptedException {
        AsyncListener listener = new AsyncListener();
        eventBus.register(listener);

        String mainThreadName = Thread.currentThread().getName();
        eventBus.post(new TestMessage("async test"));

        assertTrue(listener.latch.await(500, TimeUnit.MILLISECONDS), "Async handler did not complete in time.");
        assertTrue(listener.handlerCalled.get(), "Async handler was not called.");
        assertNotNull(listener.threadName, "Thread name in handler was not set.");
        assertNotEquals(mainThreadName, listener.threadName, "Handler ran on the posting thread, expected async.");
        assertTrue(listener.threadName.startsWith("test-eb-"), "Handler thread name has unexpected prefix.");
    }
    
    @Test
    void privateMethodHandlersAreSupported() throws InterruptedException {
        PrivateMethodListener privateListener = new PrivateMethodListener();
        eventBus.register(privateListener);
        
        eventBus.post(new TestMessage("private method test"));
        Thread.sleep(100); // Wait for async processing
        
        assertTrue(privateListener.handlerCalled.get(), "Private method handler should be called");
    }

    @Test
    void eventBusContinuesDispatchingAfterHandlerError() throws InterruptedException {
        ErrorThrowingListener errorListener = new ErrorThrowingListener();
        SimpleListener goodListener = new SimpleListener();

        eventBus.register(errorListener);
        eventBus.register(goodListener);

        TestMessage event = new TestMessage("multi-dispatch");
        eventBus.post(event); // This will cause an error log for errorListener

        Thread.sleep(100); // Give async handlers time

        assertTrue(errorListener.handlerCalled.get(), "Error throwing handler should have been called.");
        assertTrue(goodListener.specificHandlerCalled, "Good listener should still be called after another handler throws an error.");
    }

    @Test
    void registeringNullListenerThrowsException() {
        assertThrows(NullPointerException.class, () -> eventBus.register(null));
    }

    @Test
    void postingNullEventThrowsException() {
        assertThrows(NullPointerException.class, () -> eventBus.post(null));
    }
    
    @Test
    void invalidHandlerMethods() throws InterruptedException {
        // This test verifies that invalid handler methods are properly skipped
        InvalidHandlerListener invalidListener = new InvalidHandlerListener();
        
        // This should not throw an exception, just log warnings about invalid handlers
        assertDoesNotThrow(() -> eventBus.register(invalidListener));
        
        // Post some messages to verify nothing happens
        eventBus.post(new TestMessage("test"));
        eventBus.post(new AnotherTestMessage(123));
        
        Thread.sleep(100); // Give time for async processing
        
        // No assertions needed - the test passes if no exceptions are thrown
        // The logs should contain warnings about invalid handler methods
    }
    
    @Test
    void multipleListenersForSameMessageType() throws InterruptedException {
        SimpleListener listener1 = new SimpleListener();
        SimpleListener listener2 = new SimpleListener();
        
        eventBus.register(listener1);
        eventBus.register(listener2);
        
        eventBus.post(new TestMessage("shared message"));
        
        Thread.sleep(100); // Wait for async processing
        
        assertTrue(listener1.specificHandlerCalled, "First listener should receive the message");
        assertTrue(listener2.specificHandlerCalled, "Second listener should receive the message");
        assertEquals("shared message", listener1.receivedContent);
        assertEquals("shared message", listener2.receivedContent);
    }
    
    @Test
    void shutdownTerminatesThreadPool() throws InterruptedException {
        // This test verifies that the shutdown method properly terminates the thread pool
        
        // First verify the EventBus is working
        AsyncListener listener = new AsyncListener();
        eventBus.register(listener);
        
        eventBus.post(new TestMessage("before shutdown"));
        assertTrue(listener.latch.await(500, TimeUnit.MILLISECONDS), "Handler should be called before shutdown");
        
        // Create a new latch for the second message
        listener.latch = new CountDownLatch(1);
        listener.handlerCalled.set(false);
        
        // Now shut down the EventBus
        eventBus.shutdown();
        
        // Try to post another message - it might be accepted but won't be processed
        // (Behavior depends on executor service implementation)
        eventBus.post(new TestMessage("after shutdown"));
        
        // The latch should never count down because the executor is shut down
        assertFalse(listener.latch.await(200, TimeUnit.MILLISECONDS), 
                "Message should not be processed after shutdown");
    }
} 