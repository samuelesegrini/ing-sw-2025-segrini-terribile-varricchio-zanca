package it.polimi.ingsw.common.network.rmi;

import it.polimi.ingsw.common.message.Message;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface IServerRemote extends Remote {
    String SERVICE_NAME = "GalaxyTruckerGameServerRMI";

    String registerClient(IClientRemoteListener clientListener) throws RemoteException;
    void unregisterClient(String clientSessionToken) throws RemoteException;
    void dispatchClientCommand(String clientSessionToken, Message command) throws RemoteException;
    void pingServer() throws RemoteException;
}