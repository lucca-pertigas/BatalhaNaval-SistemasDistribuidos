package br.com.ucsal.server;

import br.com.ucsal.common.remote.BatalhaNavalServer;
import br.com.ucsal.server.remote.BatalhaNavalServerImpl;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class IniciarServidor {

    public static void main(String[] args) throws Exception {

        System.setProperty("java.rmi.server.hostname", "192.168.1.70");

        Registry locateRegistry = LocateRegistry.createRegistry(1099);

        BatalhaNavalServer batalhaNavalServer = new BatalhaNavalServerImpl();

        locateRegistry.rebind("BatalhaNaval", batalhaNavalServer);

        System.out.println("Servidor funcionando");
    }
}