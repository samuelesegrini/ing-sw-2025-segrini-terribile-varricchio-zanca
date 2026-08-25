/**
 * The server side: the authoritative game model, the rules that drive it, and the
 * networking that lets clients play it.
 *
 * <p>No type in here is ever sent to a client. What crosses the network is a
 * projection built for the view, which is what keeps the wire format free to change
 * independently of the model and keeps hidden information hidden.
 */
package it.polimi.ingsw.server;
