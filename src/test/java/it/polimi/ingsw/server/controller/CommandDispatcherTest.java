package it.polimi.ingsw.server.controller;

import it.polimi.ingsw.common.message.EventPublisher;
import it.polimi.ingsw.common.message.event.Event;
import it.polimi.ingsw.common.message.request.Request;
import it.polimi.ingsw.common.message.request.RequestContext;
import it.polimi.ingsw.common.message.response.ClientContext;
import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.validation.ValidationResult;
import it.polimi.ingsw.server.core.GameSessionManager;
import it.polimi.ingsw.server.core.PlayerSessionRegistry;
import it.polimi.ingsw.server.network.ServerNetworkManager;
import it.polimi.ingsw.server.network.ServerNetworkInterface;
import it.polimi.ingsw.common.message.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class CommandDispatcherTest {

    private CommandDispatcher commandDispatcher;
    private GameSessionManager sessionManager;
    private PlayerSessionRegistry playerRegistry;
    private ServerNetworkManager networkManager;
    private EventPublisher eventPublisher;
    private ConcurrentHashMap<String, String> networkClientToGamePlayerMap;
    private TestNetworkAdapter testAdapter;

    @BeforeEach
    void setUp() throws Exception {
        playerRegistry = new PlayerSessionRegistry();
        networkManager = new ServerNetworkManager();
        eventPublisher = new TestEventPublisher(); // Create test event publisher
        sessionManager = new GameSessionManager(networkManager, playerRegistry, eventPublisher);
        networkClientToGamePlayerMap = new ConcurrentHashMap<>();

        // Create test adapter to intercept messages
        testAdapter = new TestNetworkAdapter();

        commandDispatcher = new CommandDispatcher(
                sessionManager, playerRegistry, networkManager, networkClientToGamePlayerMap);
    }

    @AfterEach
    void tearDown() {
        if (commandDispatcher != null) {
            commandDispatcher.shutdown();
        }
        if (sessionManager != null) {
            sessionManager.shutdown();
        }
        if (testAdapter != null) {
            testAdapter.stop();
        }
    }

    @Test
    void testConstructor() {
        assertNotNull(commandDispatcher);
        assertNotNull(commandDispatcher.getEventPublisher());
        assertEquals("EventPublisherImpl",
                commandDispatcher.getEventPublisher().getClass().getSimpleName());
    }

    @Test
    @Timeout(10)
    void testDispatchRequestAsync() throws InterruptedException {
        String clientId = "test-client-async";
        setupTestClient(clientId);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(1);

        TestableRequest testRequest = new TestableRequest(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        testRequest.setExecutionLatch(startLatch);
        testRequest.setCompletionLatch(completeLatch);

        // Dispatch and verify it returns immediately
        long startTime = System.currentTimeMillis();
        commandDispatcher.dispatch(testRequest, clientId);
        long dispatchTime = System.currentTimeMillis() - startTime;

        assertTrue(dispatchTime < 100, "Dispatch should return immediately");

        // Verify request is processed asynchronously
        assertTrue(startLatch.await(5, TimeUnit.SECONDS),
                "Request should be processed asynchronously");

        completeLatch.countDown(); // Allow test request to complete
    }

    @Test
    @Timeout(10)
    void testProcessRequestSuccess() throws InterruptedException {
        String clientId = "test-client-success";
        setupTestClient(clientId);

        CountDownLatch executedLatch = new CountDownLatch(1);
        UUID correlationId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        TestableResponse expectedResponse = new TestableResponse(correlationId);

        TestableRequest successRequest = new TestableRequest(correlationId);
        successRequest.setExecutionLatch(executedLatch);
        successRequest.setResponseToReturn(expectedResponse);

        commandDispatcher.dispatch(successRequest, clientId);

        assertTrue(executedLatch.await(5, TimeUnit.SECONDS),
                "Request should be executed");

        // Verify response was sent
        Thread.sleep(100); // Small wait to allow message sending
        List<Message> sentMessages = testAdapter.getSentMessages();
        assertEquals(1, sentMessages.size());
        assertTrue(sentMessages.get(0) instanceof TestableResponse);
        assertEquals(correlationId, ((TestableResponse) sentMessages.get(0)).getCorrelationId());
    }

    @Test
    @Timeout(10)
    void testProcessRequestWithNullResponse() throws InterruptedException {
        String clientId = "test-client-null";
        setupTestClient(clientId);

        CountDownLatch executedLatch = new CountDownLatch(1);
        TestableRequest nullResponseRequest = new TestableRequest(UUID.fromString("550e8400-e29b-41d4-a716-446655440002"));
        nullResponseRequest.setExecutionLatch(executedLatch);
        nullResponseRequest.setResponseToReturn(null);

        commandDispatcher.dispatch(nullResponseRequest, clientId);

        assertTrue(executedLatch.await(5, TimeUnit.SECONDS));

        // Verify no message was sent
        Thread.sleep(100);
        assertTrue(testAdapter.getSentMessages().isEmpty());
    }

    @Test
    @Timeout(10)
    void testProcessRequestWithException() throws InterruptedException {
        String clientId = "test-client-error";
        setupTestClient(clientId);

        CountDownLatch executedLatch = new CountDownLatch(1);
        UUID correlationId = UUID.fromString("550e8400-e29b-41d4-a716-446655440003");
        TestableRequest errorRequest = new TestableRequest(correlationId);
        errorRequest.setExecutionLatch(executedLatch);
        errorRequest.setShouldThrowException(true);

        commandDispatcher.dispatch(errorRequest, clientId);

        assertTrue(executedLatch.await(5, TimeUnit.SECONDS));

        // Verify ErrorResponse was sent
        Thread.sleep(100);
        List<Message> sentMessages = testAdapter.getSentMessages();
        assertEquals(1, sentMessages.size());
        assertTrue(sentMessages.get(0) instanceof ErrorResponse);
        ErrorResponse errorResponse = (ErrorResponse) sentMessages.get(0);
        assertEquals(correlationId, errorResponse.getCorrelationId());
        assertEquals(ErrorResponse.INTERNAL_ERROR, errorResponse.getErrorCode());
        assertTrue(errorResponse.getErrorMessage().contains("Test exception"));
    }

    @Test
    @Timeout(15)
    void testMultipleRequestsProcessedConcurrently() throws InterruptedException {
        int numRequests = 10;
        CountDownLatch allStartedLatch = new CountDownLatch(numRequests);
        CountDownLatch releaseAllLatch = new CountDownLatch(1);
        List<TestNetworkAdapter> adapters = new ArrayList<>();

        // Create multiple clients with separate adapters
        for (int i = 0; i < numRequests; i++) {
            String clientId = "concurrent-client-" + i;
            TestNetworkAdapter adapter = new TestNetworkAdapter();
            adapters.add(adapter);
            setupTestClientWithAdapter(clientId, adapter);
        }

        long startTime = System.currentTimeMillis();

        // Send all requests
        for (int i = 0; i < numRequests; i++) {
            String clientId = "concurrent-client-" + i;
            UUID requestId = UUID.randomUUID();
            TestableRequest request = new TestableRequest(requestId);
            request.setExecutionLatch(allStartedLatch);
            request.setCompletionLatch(releaseAllLatch);
            // FIX: Set a response so the request generates a response
            request.setResponseToReturn(new TestableResponse(requestId));
            commandDispatcher.dispatch(request, clientId);
        }

        // Verify all requests started (concurrent processing)
        assertTrue(allStartedLatch.await(10, TimeUnit.SECONDS),
                "All requests should start processing concurrently");

        long concurrentTime = System.currentTimeMillis() - startTime;

        // Release all requests
        releaseAllLatch.countDown();

        // Wait a bit for responses to be sent
        Thread.sleep(500); // Increased wait time

        // Verify responses were distributed across adapters
        int totalResponses = adapters.stream()
                .mapToInt(adapter -> adapter.getSentMessages().size())
                .sum();
        assertEquals(numRequests, totalResponses, "All responses should be sent");

        // Time to start all requests should be much less than sequential processing
        assertTrue(concurrentTime < 2000,
                "Concurrent processing should be faster than sequential");

        // Cleanup adapters
        adapters.forEach(TestNetworkAdapter::stop);
    }
    @Test
    @Timeout(10)
    void testRequestProcessingWithClientMapping() throws InterruptedException {
        String clientId = "mapped-client";
        String playerId = "player-123";
        setupTestClient(clientId);
        networkClientToGamePlayerMap.put(clientId, playerId);

        CountDownLatch executedLatch = new CountDownLatch(1);
        ContextTestableRequest contextRequest = new ContextTestableRequest(clientId, executedLatch);

        commandDispatcher.dispatch(contextRequest, clientId);

        assertTrue(executedLatch.await(5, TimeUnit.SECONDS));
        assertTrue(contextRequest.wasContextValid());
        assertEquals(clientId, contextRequest.getReceivedSenderId());
    }

    @Test
    @Timeout(10)
    void testShutdownStopsProcessing() throws InterruptedException {
        CountDownLatch beforeShutdownLatch = new CountDownLatch(1);
        CountDownLatch afterShutdownLatch = new CountDownLatch(1);
        CountDownLatch releaseBeforeShutdown = new CountDownLatch(1);

        String clientId = "shutdown-test-client";
        setupTestClient(clientId);

        // Request that waits before completing
        TestableRequest beforeShutdownRequest = new TestableRequest(UUID.randomUUID());
        beforeShutdownRequest.setExecutionLatch(beforeShutdownLatch);
        beforeShutdownRequest.setCompletionLatch(releaseBeforeShutdown);

        commandDispatcher.dispatch(beforeShutdownRequest, clientId);

        // Wait for first request to start
        assertTrue(beforeShutdownLatch.await(5, TimeUnit.SECONDS));

        // Shutdown dispatcher
        commandDispatcher.shutdown();

        // FIX: Add a small delay after shutdown to ensure it's fully processed
        Thread.sleep(100);

        // Request after shutdown - should not cause exception
        TestableRequest afterShutdownRequest = new TestableRequest(UUID.randomUUID());
        afterShutdownRequest.setExecutionLatch(afterShutdownLatch);

        // FIX: Wrap in try-catch to handle expected RejectedExecutionException
        boolean exceptionThrown = false;
        try {
            commandDispatcher.dispatch(afterShutdownRequest, clientId);
        } catch (Exception e) {
            // Expected behavior - dispatcher should reject new requests after shutdown
            exceptionThrown = true;
            assertTrue(e instanceof RuntimeException || e.getCause() instanceof java.util.concurrent.RejectedExecutionException,
                    "Should throw RejectedExecutionException or wrap it");
        }

        // Release first request
        releaseBeforeShutdown.countDown();
        // Request after shutdown should not be processed OR should throw exception
        if (!exceptionThrown) {
            assertFalse(afterShutdownLatch.await(2, TimeUnit.SECONDS),
                    "Requests should not be processed after shutdown");
        }
        // If exception was thrown, that's also acceptable behavior
    }

    @Test
    @Timeout(10)
    void testCreateContextWithAllParameters() throws InterruptedException {
        String clientId = "context-test-client";
        String playerId = "context-player-456";
        setupTestClient(clientId);
        networkClientToGamePlayerMap.put(clientId, playerId);

        CountDownLatch executedLatch = new CountDownLatch(1);
        ContextTestableRequest request = new ContextTestableRequest(clientId, executedLatch);

        commandDispatcher.dispatch(request, clientId);

        assertTrue(executedLatch.await(5, TimeUnit.SECONDS));
        assertTrue(request.wasContextValid());
        assertEquals(clientId, request.getReceivedSenderId());
    }

    @Test
    @Timeout(10)
    void testNetworkClientWithoutAdapter() throws InterruptedException {
        String clientId = "no-adapter-client";
        // Don't setup adapter for this client

        CountDownLatch executedLatch = new CountDownLatch(1);
        TestableResponse response = new TestableResponse(UUID.randomUUID());
        TestableRequest request = new TestableRequest(UUID.randomUUID());
        request.setExecutionLatch(executedLatch);
        request.setResponseToReturn(response);

        commandDispatcher.dispatch(request, clientId);

        assertTrue(executedLatch.await(5, TimeUnit.SECONDS));

        // Verify no message was sent (no adapter)
        Thread.sleep(100);
        assertTrue(testAdapter.getSentMessages().isEmpty());
    }

    @Test
    @Timeout(10)
    void testNetworkAdapterSendFailure() throws InterruptedException {
        String clientId = "failing-client";
        TestNetworkAdapter failingAdapter = new TestNetworkAdapter();
        failingAdapter.setShouldFailSending(true);

        // Setup client with failing adapter
        setupTestClientWithAdapter(clientId, failingAdapter);

        CountDownLatch executedLatch = new CountDownLatch(1);
        TestableResponse response = new TestableResponse(UUID.randomUUID());
        TestableRequest request = new TestableRequest(UUID.randomUUID());
        request.setExecutionLatch(executedLatch);
        request.setResponseToReturn(response);

        commandDispatcher.dispatch(request, clientId);

        assertTrue(executedLatch.await(5, TimeUnit.SECONDS));

        // Verify message sending was attempted but failed
        Thread.sleep(100);
        assertTrue(failingAdapter.getSentMessages().isEmpty());
        assertTrue(failingAdapter.getSendAttempts() > 0);
    }

    @Test
    @Timeout(10)
    void testComplexErrorScenario() throws InterruptedException {
        String clientId = "complex-error-client";
        setupTestClient(clientId);

        CountDownLatch executedLatch = new CountDownLatch(1);
        ComplexErrorRequest errorRequest = new ComplexErrorRequest(UUID.randomUUID(), executedLatch);

        commandDispatcher.dispatch(errorRequest, clientId);

        assertTrue(executedLatch.await(5, TimeUnit.SECONDS));

        // Verify ErrorResponse was sent with correct details
        Thread.sleep(100);
        List<Message> sentMessages = testAdapter.getSentMessages();
        assertEquals(1, sentMessages.size());
        assertTrue(sentMessages.get(0) instanceof ErrorResponse);
        ErrorResponse errorResponse = (ErrorResponse) sentMessages.get(0);
        assertEquals(errorRequest.getCorrelationId(), errorResponse.getCorrelationId());
        assertTrue(errorResponse.getErrorMessage().contains("Complex error scenario"));
    }

    @Test
    @Timeout(10)
    void testThreadPoolStressTest() throws InterruptedException {
        int numClients = 20;
        CountDownLatch allProcessedLatch = new CountDownLatch(numClients);
        List<TestNetworkAdapter> adapters = new ArrayList<>();

        // Setup many clients with separate adapters
        for (int i = 0; i < numClients; i++) {
            String clientId = "stress-client-" + i;
            TestNetworkAdapter adapter = new TestNetworkAdapter();
            adapters.add(adapter);
            setupTestClientWithAdapter(clientId, adapter);
        }

        // Send many requests quickly
        for (int i = 0; i < numClients; i++) {
            String clientId = "stress-client-" + i;
            UUID requestId = UUID.randomUUID();
            TestableRequest request = new TestableRequest(requestId);
            request.setExecutionLatch(allProcessedLatch);
            // FIX: Ensure all requests generate responses
            request.setResponseToReturn(new TestableResponse(requestId));
            commandDispatcher.dispatch(request, clientId);
        }

        // All should be processed within reasonable time
        assertTrue(allProcessedLatch.await(15, TimeUnit.SECONDS),
                "All stress test requests should be processed");

        // Verify all responses were sent
        Thread.sleep(500); // Increased wait time
        int totalResponses = adapters.stream()
                .mapToInt(adapter -> adapter.getSentMessages().size())
                .sum();
        assertEquals(numClients, totalResponses, "All responses should be sent");

        // Cleanup adapters
        adapters.forEach(TestNetworkAdapter::stop);
    }

    // Helper methods
    private void setupTestClient(String clientId) {
        setupTestClientWithAdapter(clientId, testAdapter);
    }

    private void setupTestClientWithAdapter(String clientId, ServerNetworkInterface adapter) {
        try {
            Field mapField = ServerNetworkManager.class.getDeclaredField("clientToAdapterMap");
            mapField.setAccessible(true);
            @SuppressWarnings("unchecked")
            ConcurrentHashMap<String, ServerNetworkInterface> clientMap =
                    (ConcurrentHashMap<String, ServerNetworkInterface>) mapField.get(networkManager);
            clientMap.put(clientId, adapter);
        } catch (Exception e) {
            throw new RuntimeException("Failed to setup test client", e);
        }
    }
}

// Test EventPublisher implementation
class TestEventPublisher implements EventPublisher {
    private final List<Object> publishedEvents = Collections.synchronizedList(new ArrayList<>());

    public void publishEvent(Object event) {
        publishedEvents.add(event);
    }

    public List<Object> getPublishedEvents() {
        return new ArrayList<>(publishedEvents);
    }

    public void clearEvents() {
        publishedEvents.clear();
    }

    @Override
    public void publishEvent(Event event) {

    }

    @Override
    public void publishEventToClient(Event event, String clientId) {

    }

    @Override
    public void publishEventToGame(Event event, String gameId) {

    }
}

// Test helper classes - all at the same level
class TestNetworkAdapter implements ServerNetworkInterface {
    private final List<Message> sentMessages = Collections.synchronizedList(new ArrayList<>());
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicInteger sendAttempts = new AtomicInteger(0);
    private boolean shouldFailSending = false;

    @Override
    public boolean sendMessageToClient(String networkClientId, Message message) {
        sendAttempts.incrementAndGet();
        if (running.get() && !shouldFailSending) {
            sentMessages.add(message);
            return true;
        }
        return false;
    }

    @Override
    public void broadcastMessage(Message message) {
        // No-op for test implementation
    }

    @Override
    public void setOnClientConnected(Consumer<String> onClientConnectedHandler) {
        // No-op for test implementation
    }

    @Override
    public void setOnClientDisconnected(Consumer<String> onClientDisconnectedHandler) {
        // No-op for test implementation
    }

    @Override
    public void setOnMessageReceived(BiConsumer<String, Message> onMessageReceivedHandler) {
        // No-op for test implementation
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public void startServer(int port) {
        // No-op for test implementation
    }

    @Override
    public void stopServer() {
        // No-op for test implementation
    }

    public List<Message> getSentMessages() {
        return new ArrayList<>(sentMessages);
    }

    public int getSendAttempts() {
        return sendAttempts.get();
    }

    public void setShouldFailSending(boolean shouldFailSending) {
        this.shouldFailSending = shouldFailSending;
    }

    public void stop() {
        running.set(false);
    }
}

class TestableRequest implements Request {
    private final UUID correlationId;
    private CountDownLatch executionLatch;
    private CountDownLatch completionLatch;
    private Response responseToReturn;
    private boolean shouldThrowException = false;

    public TestableRequest(UUID correlationId) {
        this.correlationId = correlationId;
    }

    @Override
    public UUID getCorrelationId() {
        return correlationId;
    }

    @Override
    public ValidationResult validate() {
        return new ValidationResult(true, null, null);
    }

    @Override
    public Response execute(RequestContext context) {
        if (executionLatch != null) {
            executionLatch.countDown();
        }

        if (completionLatch != null) {
            try {
                completionLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        if (shouldThrowException) {
            throw new RuntimeException("Test exception for error handling");
        }

        return responseToReturn;
    }

    public void setExecutionLatch(CountDownLatch executionLatch) {
        this.executionLatch = executionLatch;
    }

    public void setCompletionLatch(CountDownLatch completionLatch) {
        this.completionLatch = completionLatch;
    }

    public void setResponseToReturn(Response responseToReturn) {
        this.responseToReturn = responseToReturn;
    }

    public void setShouldThrowException(boolean shouldThrowException) {
        this.shouldThrowException = shouldThrowException;
    }

    @Override
    public UUID getMessageId() {
        return null;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return null;
    }
}

class ContextTestableRequest implements Request {
    private final String expectedSenderId;
    private final CountDownLatch executedLatch;
    private final UUID correlationId;
    private boolean contextValid = false;
    private String receivedSenderId;

    public ContextTestableRequest(String expectedSenderId, CountDownLatch executedLatch) {
        this.expectedSenderId = expectedSenderId;
        this.executedLatch = executedLatch;
        this.correlationId = UUID.randomUUID();
    }

    @Override
    public UUID getCorrelationId() {
        return correlationId;
    }

    @Override
    public ValidationResult validate() {
        return new ValidationResult(true, null, null);
    }

    @Override
    public Response execute(RequestContext context) {
        try {
            assertNotNull(context, "Context should not be null");
            assertNotNull(context.getSenderId(), "Sender ID should not be null");
            assertNotNull(context.getSessionManager(), "Session manager should not be null");
            assertNotNull(context.getPlayerRegistry(), "Player registry should not be null");
            assertNotNull(context.getEventPublisher(), "Event publisher should not be null");
            assertNotNull(context.getNetworkManager(), "Network manager should not be null");

            receivedSenderId = context.getSenderId();
            assertEquals(expectedSenderId, receivedSenderId, "Sender ID should match expected");

            contextValid = true;
        } catch (Exception e) {
            contextValid = false;
            throw e;
        } finally {
            executedLatch.countDown();
        }
        return null;
    }

    public boolean wasContextValid() {
        return contextValid;
    }

    public String getReceivedSenderId() {
        return receivedSenderId;
    }

    @Override
    public UUID getMessageId() {
        return null;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return null;
    }
}

class ComplexErrorRequest implements Request {
    private final UUID correlationId;
    private final CountDownLatch executedLatch;

    public ComplexErrorRequest(UUID correlationId, CountDownLatch executedLatch) {
        this.correlationId = correlationId;
        this.executedLatch = executedLatch;
    }

    @Override
    public UUID getCorrelationId() {
        return correlationId;
    }

    @Override
    public ValidationResult validate() {
        return new ValidationResult(true, null, null);
    }

    @Override
    public Response execute(RequestContext context) {
        try {
            // Simulate some processing before throwing exception
            Thread.sleep(10);
            throw new IllegalStateException("Complex error scenario with detailed message");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted during complex error", e);
        } finally {
            executedLatch.countDown();
        }
    }

    @Override
    public UUID getMessageId() {
        return null;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return null;
    }
}

class TestableResponse implements Response {
    private final UUID correlationId;

    public TestableResponse(UUID correlationId) {
        this.correlationId = correlationId;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    @Override
    public boolean isSuccess() {
        return true;
    }

    @Override
    public String getErrorMessage() {
        return "";
    }

    @Override
    public String getErrorCode() {
        return "";
    }

    @Override
    public void handleOnClient(ClientContext context) {
        // No-op for test implementation
    }

    @Override
    public UUID getMessageId() {
        return null;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return null;
    }
}