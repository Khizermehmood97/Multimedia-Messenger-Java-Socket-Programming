/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ChatClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Khizer Mehmood
 */
public class ChatClient {

    private final int serverPort;
    private final String serverName;
    private Socket socket;
    private OutputStream serverOut;
    private InputStream serverIn;
    private BufferedReader bufferedIn;
    
    private ArrayList<UserStatusListener> userStatusListeners = new ArrayList<>(); 
    private ArrayList<MessageListener> messageListeners = new ArrayList<>();
    
    public ChatClient (String ip, int port) {
        this.serverName = ip;
        this.serverPort = port;
    }
    
     public static void main(String[] args) throws IOException {
       ChatClient client = new ChatClient("localhost", 4444);
      
       client.addUserStatusListener(new UserStatusListener() {
         @Override
         public void online (String login) {
             System.out.println("ONLINE: "+login);
         }
         @Override
         public void offline (String login) {
             System.out.println("OFFLINE: "+login);
         }
     });
       
       client.addMessageListener(new MessageListener() {
           @Override
           public void onMessage (String fromLogin, String msgBody) {
               System.out.println("Message Recieved from: "+fromLogin+" --> "+msgBody);
           }
       });
      
       
       if (!client.connect()) {
           System.out.println("Connection failed.");
       }else {
           System.out.println("Connection Successful.");
           
           if (client.login("guest","guest")) {
               System.out.println("Login Successfull.");
               
               client.msg("demo", "hello world!");
               
           }else {
               System.out.println("Login failed.");
           }
           
          
         // client.logoff();
       }
    }
     
     private void getsocketclose () throws IOException {
         this.socket.close();
     }

    private boolean connect()  {
        try {
            this.socket = new Socket(serverName, serverPort);
            System.out.println("Client port is "+ socket.getLocalPort());
            this.serverOut = socket.getOutputStream();
            this.serverIn = socket.getInputStream();
            this.bufferedIn = new BufferedReader(new InputStreamReader(serverIn));
          
            return true;
        } catch (IOException ex) {
            Logger.getLogger(ChatClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    private boolean login(String loginID, String password) throws IOException {
        String cmd = "login "+ loginID +" "+password+"\n";
        serverOut.write(cmd.getBytes());
        
        String response = bufferedIn.readLine();
        System.out.println("Response line : "+response);
       
        if("ok login".equalsIgnoreCase(response)) {
            startMessageReader();
            return true;
        }else {
            return false;
        }
    }
    
    
    private void startMessageReader() {
        Thread t = new Thread() {
          @Override
          public void run(){
              readMessageLoop();
          }  
        };
        t.start();
    }
    
    private void readMessageLoop() {
             
        try {String line;
             while((line = bufferedIn.readLine()) != null){
              String[] tokens = line.split(" ");
            if (tokens != null && tokens.length > 0) {
                String cmd = tokens[0];
                if("online".equalsIgnoreCase(cmd)) {
                    handleOnline(tokens);
                } else if ("offline".equalsIgnoreCase(cmd)) {
                    handleOffline(tokens);
                } else if ("msg".equalsIgnoreCase(cmd)) {
                    String[] tokensmsg = line.split(" ", 3);
                    handleMessage(tokensmsg);
                    
                }
            }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            try {
                socket.close();
            } catch (IOException ex1) {
                ex1.printStackTrace();
            }
        }
    }

   
    private void logoff () throws IOException {
        String cmd = "quit\n";
        serverOut.write(cmd.getBytes());
    }
    
     private void msg(String sendTo, String msgBody) throws IOException {
        String cmd = "msg "+sendTo+" "+msgBody+"\n";
        serverOut.write(cmd.getBytes());
    }
    
    private void handleMessage(String[] tokensmsg) {
        String login = tokensmsg[1];
        String msgBody = tokensmsg[2];
        
        for(MessageListener listener : messageListeners) {
            listener.onMessage(login, msgBody);
        }
    }
     
    public void addUserStatusListener(UserStatusListener listener) {
        userStatusListeners.add(listener);
    }
    
    public void removeUserStatusListener(UserStatusListener listener) {
        userStatusListeners.remove(listener);
    }

    public void addMessageListener(MessageListener listener) {
        messageListeners.add(listener);
    }
    
    public void removeMessageListener(MessageListener listener) {
        messageListeners.remove(listener);
    }
    
    private void handleOnline(String[] tokens) {
        String login = tokens[1];
        for (UserStatusListener listener : userStatusListeners) {
            listener.online(login);
        }
    }

    private void handleOffline(String[] tokens) {
         String login = tokens[1];
        for (UserStatusListener listener : userStatusListeners) {
            listener.offline(login);
        }
    }


}
