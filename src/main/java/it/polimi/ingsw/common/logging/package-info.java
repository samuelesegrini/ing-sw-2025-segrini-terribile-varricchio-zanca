/**
 * Turning the trace on, and deciding where it lands.
 *
 * <p>Both jars are built from one source tree and both would otherwise get one logging
 * configuration, but they cannot have the same one: the server owns its terminal and the
 * client is drawing a ship on it. So the shared {@code logback.xml} is the quiet, console,
 * {@code INFO} case, and each entry point says at startup which of the two it is.
 */
package it.polimi.ingsw.common.logging;
