/**
 * Turning the model into the views the protocol carries.
 *
 * <p>This is the mapping layer architecture § 3.1 said projections would cost, and it buys
 * the thing the rest of the design rests on: the client never receives a {@code Ship}, so no
 * information leaks by accident, no domain class has to be {@code Serializable}, and the wire
 * format can change without the model noticing.
 *
 * <p>Everything here is a pure function of the model. Nothing decides, nothing mutates, and
 * calling it twice gives the same answer twice.
 */
package it.polimi.ingsw.server.projection;
