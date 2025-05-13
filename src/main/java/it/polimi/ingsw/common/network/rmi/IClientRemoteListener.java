package it.polimi.ingsw.common.network.rmi;

import it.polimi.ingsw.common.message.Message;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IClientRemoteListener extends Remote {
    void onMessageFromServer(Message message) throws RemoteException;
    void pingClient() throws RemoteException;
}