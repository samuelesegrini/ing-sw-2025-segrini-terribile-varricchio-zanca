package it.polimi.ingsw.common.event;

import it.polimi.ingsw.common.message.Message;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EventBus {
    private static final Logger LOGGER = Logger.getLogger(EventBus.class.getName());

    // Maps a Message class to a list of methods that can handle it.
    private final Map<Class<? extends Message>, List<HandlerMethodSubscription>> handlerSubscriptions = new ConcurrentHashMap<>();

    // Executor service to handle events asynchronously
    private final ExecutorService asyncEventExecutor;

    /**
     * Creates an EventBus with a fixed-size thread pool for asynchronous event handling.
     * @param threadPoolSize The number of threads in the pool.
     * @param threadNamePrefix Prefix for naming threads in the pool (e.g., "client-eventbus", "server-gameX-eventbus")
     */
    public EventBus(int threadPoolSize, String threadNamePrefix) {
        // Custom ThreadFactory to name threads for easier debugging
        ThreadFactory namedThreadFactory = new ThreadFactory() {
            private int count = 0;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName(threadNamePrefix + "-" + count++);
                t.setDaemon(true); // Allow JVM to exit if only these threads are running
                return t;
            }
        };
        this.asyncEventExecutor = Executors.newFixedThreadPool(threadPoolSize, namedThreadFactory);
    }

    /**
     * Registers all @MessageHandler methods on the given listener object.
     * @param listener The object containing handler methods.
     */
    public void register(Object listener) {
        Objects.requireNonNull(listener, "Listener object cannot be null.");
        Class<?> listenerClass = listener.getClass();

        for (Method method : listenerClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(MessageHandler.class)) {
                if (method.getParameterCount() != 1) {
                    LOGGER.log(Level.WARNING, "Method {0} in class {1} is annotated with @MessageHandler but does not have exactly one parameter. Skipping.",
                            new Object[]{method.getName(), listenerClass.getName()});
                    continue;
                }

                Class<?> parameterType = method.getParameterTypes()[0];
                if (!Message.class.isAssignableFrom(parameterType)) {
                    LOGGER.log(Level.WARNING, "Method {0} in class {1} is annotated with @MessageHandler but its parameter type {2} does not implement Message. Skipping.",
                            new Object[]{method.getName(), listenerClass.getName(), parameterType.getName()});
                    continue;
                }

                @SuppressWarnings("unchecked") // Checked by isAssignableFrom
                Class<? extends Message> messageType = (Class<? extends Message>) parameterType;
                method.setAccessible(true); // Allow calling private/protected methods

                handlerSubscriptions.computeIfAbsent(messageType, k -> new ArrayList<>())
                        .add(new HandlerMethodSubscription(listener, method));

                LOGGER.log(Level.INFO, "Registered handler: {0}.{1} for message type {2}",
                        new Object[]{listenerClass.getSimpleName(), method.getName(), messageType.getSimpleName()});
            }
        }
    }

    /**
     * Unregisters all handler methods for the given listener object.
     * @param listener The object to unregister.
     */
    public void unregister(Object listener) {
        Objects.requireNonNull(listener, "Listener object cannot be null.");
        handlerSubscriptions.values().forEach(subscriptions ->
                subscriptions.removeIf(subscription -> subscription.listenerInstance == listener)
        );
        // Optional: Clean up empty lists from the map
        handlerSubscriptions.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        LOGGER.log(Level.INFO, "Unregistered all handlers for listener: {0}", listener.getClass().getSimpleName());
    }

    /**
     * Posts an event to all registered handlers capable of handling it or its supertypes/interfaces.
     * Handlers are invoked asynchronously.
     * @param event The event object to post.
     */
    public void post(Message event) {
        Objects.requireNonNull(event, "Event cannot be null.");
        LOGGER.log(Level.FINER, "Posting event: {0}", event.getClass().getSimpleName());

        // Check if executor service is shut down
        if (asyncEventExecutor.isShutdown()) {
            LOGGER.log(Level.WARNING, "Cannot post event: {0} - EventBus executor service is shut down", 
                event.getClass().getSimpleName());
            return;
        }

        List<HandlerMethodSubscription> applicableHandlers = findApplicableHandlers(event.getClass());

        if (applicableHandlers.isEmpty()) {
            LOGGER.log(Level.FINE, "No handlers registered for event type {0} or its supertypes/interfaces.", event.getClass().getName());
        }

        for (HandlerMethodSubscription subscription : applicableHandlers) {
            asyncEventExecutor.submit(() -> {
                try {
                    subscription.handlerMethod.invoke(subscription.listenerInstance, event);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    LOGGER.log(Level.SEVERE, "Error dispatching event " + event.getClass().getSimpleName() +
                            " to " + subscription.listenerInstance.getClass().getSimpleName() +
                            "." + subscription.handlerMethod.getName(), cause);
                } catch (Exception e) { // Catch any other unexpected runtime exceptions from handler
                    LOGGER.log(Level.SEVERE, "Unexpected error in handler " +
                            subscription.listenerInstance.getClass().getSimpleName() +
                            "." + subscription.handlerMethod.getName() + " for event " +
                            event.getClass().getSimpleName(), e);
                }
            });
        }
    }

    /**
     * Finds all handlers that can handle the given message class, including handlers for its
     * superclasses and implemented interfaces that extend {@link Message}.
     * @param messageClass The class of the message being posted.
     * @return A list of applicable handler subscriptions.
     */
    private List<HandlerMethodSubscription> findApplicableHandlers(Class<?> messageClass) {
        List<HandlerMethodSubscription> applicableHandlers = new ArrayList<>();
        Class<?> currentClass = messageClass;

        // Iterate up the class hierarchy
        while (currentClass != null && currentClass != Object.class) {
            if (Message.class.isAssignableFrom(currentClass)) {
                @SuppressWarnings("unchecked")
                Class<? extends Message> msgClassKey = (Class<? extends Message>) currentClass;
                List<HandlerMethodSubscription> handlersForClass = handlerSubscriptions.get(msgClassKey);
                if (handlersForClass != null) {
                    applicableHandlers.addAll(handlersForClass);
                }
            }
            // Check interfaces implemented by the current class
            for (Class<?> iface : currentClass.getInterfaces()) {
                if (Message.class.isAssignableFrom(iface)) {
                    @SuppressWarnings("unchecked")
                    Class<? extends Message> ifaceKey = (Class<? extends Message>) iface;
                    List<HandlerMethodSubscription> handlersForInterface = handlerSubscriptions.get(ifaceKey);
                    if (handlersForInterface != null) {
                        applicableHandlers.addAll(handlersForInterface);
                    }
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        // Deduplicate if a handler method is registered for multiple compatible types explicitly (though unlikely with current registration)
        // For now, assuming distinct registrations. If A extends B and handler is for A and for B, it will be called twice if event is A.
        // This behavior is often desired (specific handler + general handler). If not, add deduplication logic here.
        return applicableHandlers;
    }


    /**
     * Shuts down the asynchronous event executor.
     * It's important to call this when the application is shutting down
     * to allow threads to terminate gracefully.
     */
    public void shutdown() {
        LOGGER.info("Shutting down EventBus executor service...");
        asyncEventExecutor.shutdown();
        try {
            if (!asyncEventExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                asyncEventExecutor.shutdownNow();
                if (!asyncEventExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    LOGGER.severe("EventBus executor service did not terminate.");
                }
            }
        } catch (InterruptedException e) {
            asyncEventExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOGGER.info("EventBus executor service shutdown complete.");
    }

    /**
     * Helper class to store listener instance and its handler method.
     */
    private static class HandlerMethodSubscription {
        final Object listenerInstance;
        final Method handlerMethod;

        HandlerMethodSubscription(Object listenerInstance, Method handlerMethod) {
            this.listenerInstance = listenerInstance;
            this.handlerMethod = handlerMethod;
        }

        // Optional: Override equals and hashCode if you store these in a Set or need distinct checks
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            HandlerMethodSubscription that = (HandlerMethodSubscription) o;
            return Objects.equals(listenerInstance, that.listenerInstance) &&
                    Objects.equals(handlerMethod, that.handlerMethod);
        }

        @Override
        public int hashCode() {
            return Objects.hash(listenerInstance, handlerMethod);
        }
    }
}