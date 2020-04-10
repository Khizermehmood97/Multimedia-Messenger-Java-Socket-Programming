/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ChatServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Khizer Mehmood
 */
public class ServerMain {

    public static void main(String[] args) {
        int port = 4444;
        Server server = new Server (port);
        server.start();

    }

}
