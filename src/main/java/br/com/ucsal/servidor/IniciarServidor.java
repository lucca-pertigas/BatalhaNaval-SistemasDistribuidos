package br.com.ucsal.servidor;

import br.com.ucsal.comum.BatalhaNavalServer;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class IniciarServidor {

    public static void main(String[] args) throws Exception {

        Registry locateRegistry = LocateRegistry.createRegistry(1099);

        BatalhaNavalServer batalhaNavalServer = new BatalhaNavalServerImpl();

        locateRegistry.rebind("BatalhaNaval", batalhaNavalServer);

        System.out.println("Servidor funcionando");
    }
}
