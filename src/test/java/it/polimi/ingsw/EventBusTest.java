package it.polimi.ingsw;

import it.polimi.ingsw.common.event.EventBus;
import it.polimi.ingsw.common.event.MessageHandler;
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
    }

    static class AnotherTestMessage implements Message {
        final int value;
        AnotherTestMessage(int value) { this.value = value; }
    }

    static class SpecialTestMessage extends TestMessage {
        SpecialTestMessage(String content) { super(content); }
    }

    interface TestEventInterface extends Message {}
    static class InterfaceImplTestMessage implements TestEventInterface {
        final String data = "InterfaceData";
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
        private void handlePrivate(TestMessage message){ // Test private method handling
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


    @BeforeEach
    void setUp() {
        // Using a unique prefix for each test run helps if logs are interleaved
        eventBus = new EventBus(2, "test-eb-" + System.nanoTime());
    }

    @AfterEach
    void tearDown() {
        if (eventBus != null) {
            eventBus.shutdown();
        }
    }

    @Test
    void registerAndPostSimpleEvent() throws InterruptedException {
        SimpleListener listener = new SimpleListener();
        eventBus.register(listener);

        TestMessage event = new TestMessage("hello");
        eventBus.post(event);

        // Give async handler time to complete
        Thread.sleep(100); // Adjust if needed, or use CountDownLatch

        assertTrue(listener.specificHandlerCalled, "Handler method was not called.");
        assertEquals("hello", listener.receivedContent, "Handler did not receive correct content.");
    }

    @Test
    void postToCorrectHandlerWhenMultipleRegistered() throws InterruptedException {
        MultiHandlerListener listener = new MultiHandlerListener();
        eventBus.register(listener);

        eventBus.post(new TestMessage("msg1"));
        eventBus.post(new AnotherTestMessage(100));
        eventBus.post(new TestMessage("msg2"));

        Thread.sleep(100);

        assertEquals(2, listener.testMessagesHandled, "Incorrect number of TestMessage handled.");
        assertEquals(1, listener.anotherTestMessagesHandled, "Incorrect number of AnotherTestMessage handled.");
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
    void polymorphicDispatchToSuperclassHandler() throws InterruptedException {
        PolymorphicListener listener = new PolymorphicListener();
        eventBus.register(listener);

        eventBus.post(new SpecialTestMessage("special"));
        Thread.sleep(100);

        assertTrue(listener.baseHandlerCalled, "Base handler (for TestMessage) should be called for SpecialTestMessage.");
        assertTrue(listener.specialHandlerCalled, "Special handler (for SpecialTestMessage) should be called.");
    }

    @Test
    void polymorphicDispatchToInterfaceHandler() throws InterruptedException {
        PolymorphicListener listener = new PolymorphicListener();
        eventBus.register(listener);

        eventBus.post(new InterfaceImplTestMessage());
        Thread.sleep(100);

        assertTrue(listener.interfaceHandlerCalled, "Interface handler (for TestEventInterface) should be called for InterfaceImplTestMessage.");
    }

    @Test
    void handlerMethodCanBePrivate() throws InterruptedException {
        PrivateMethodListener listener = new PrivateMethodListener();
        eventBus.register(listener);
        eventBus.post(new TestMessage("for private"));
        Thread.sleep(100);
        assertTrue(listener.handlerCalled.get(), "Private handler method was not called.");
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
        assertEquals("multi-dispatch", goodListener.receivedContent);
    }

    @Test
    void registeringNullListenerThrowsException() {
        assertThrows(NullPointerException.class, () -> eventBus.register(null));
    }

    @Test
    void postingNullEventThrowsException() {
        assertThrows(NullPointerException.class, () -> eventBus.post(null));
    }
}