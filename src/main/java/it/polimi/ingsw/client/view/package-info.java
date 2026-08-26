/**
 * Ways of playing, of which there are two.
 *
 * <p>Requirement C3 asks for the interface to be chosen at startup, which only means something
 * if the rest of the client cannot tell which was picked. So there is one method here and
 * nothing else: everything a screen needs it reads from
 * {@link it.polimi.ingsw.client.state.ClientState}, and everything it wants done it sends as a
 * command.
 */
package it.polimi.ingsw.client.view;
