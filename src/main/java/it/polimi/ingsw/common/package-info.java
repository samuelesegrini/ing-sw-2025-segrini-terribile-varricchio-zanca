/**
 * What both sides know, and neither side owns.
 *
 * <p>The vocabulary the protocol is written in: colours, geometry, kinds, the commands and
 * events themselves, the views they carry, and the transport that moves them. Everything here
 * is depended on by {@link it.polimi.ingsw.server} and {@link it.polimi.ingsw.client}, and
 * depends on neither — which {@code LayeringTest} asserts by reading the imports rather than
 * by trusting this sentence.
 *
 * <p><b>No game state, and no authority.</b> That is a narrower rule than "no logic", and
 * deliberately so: a dozen value types carry small predicates — whether two connectors join,
 * whether a shield stops a hit — so that a view can grey out an illegal placement before
 * somebody clicks it. The server then revalidates as though the client had computed nothing.
 * The test is whether the server's answer could ever depend on trusting this code. It cannot.
 */
package it.polimi.ingsw.common;
