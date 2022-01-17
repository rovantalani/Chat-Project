/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package labbassistentchatt3;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import javax.swing.*;

/**
 *
 * @author rovan
 */


public class LabbAssistentChatt3 {

    public static void main(String[] args) {
        final int port = 12348;
        //Starta först som server sn som client
        final boolean startServer = false;
        if (startServer) {
            new Server(port);
        } else {
            new Client(port);
        }
    }
}


class Server {

    Server(int port) {
        ServerSocket myServerSocket = null;  //(to make it available in the finally clause)
        try {
            myServerSocket = new ServerSocket(port);
            Socket mySocket = myServerSocket.accept();
            new ChatParticipant(mySocket);
        } catch (IOException e) {
            System.out.println("error in Server: "+e.getMessage());
        } finally {
            try {
                myServerSocket.close();  // to avoid reserving the port forever
            } catch (Exception e) {
                System.out.println("error closing server socket: "+e.getMessage());
            }
        }
    }
}

class Client {

    Client(int port) {
        try {
            Socket mySocket = new Socket("localhost", port);
            new ChatParticipant(mySocket);
        } catch (IOException e) {
            System.out.println("error in client: "+e.getMessage());
        }
    }
}

class ChatParticipant implements ObjectStreamListener, ActionListener {

    private ObjectOutputStream myObjectOutputStream;
    private JTextArea displayArea;
    private JTextField inputField;
    private JButton endButton;
    private JFrame theFrame;


    ChatParticipant(Socket mySocket) {
        startGUI();
        setupStreams(mySocket);
    }

    private void startGUI() {
        JFrame theFrame = new JFrame("Chat!");
        theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        displayArea = new JTextArea(20, 50);
        inputField = new JTextField(50);
        endButton= new JButton();
        inputField.addActionListener(this);
        endButton.addActionListener(this);

        theFrame.setLayout(new BorderLayout());
        theFrame.add(displayArea, BorderLayout.CENTER);
        theFrame.add(inputField, BorderLayout.SOUTH);
        theFrame.add(endButton, BorderLayout.NORTH);

        theFrame.pack();
        theFrame.setVisible(true);
    }

    private void setupStreams(Socket mySocket) {
        try {
            OutputStream myOutputStream = mySocket.getOutputStream();
            InputStream myInputStream = mySocket.getInputStream();
            myObjectOutputStream = new ObjectOutputStream(myOutputStream);
            ObjectInputStream myObjectInputStream = new ObjectInputStream(myInputStream);
            new ObjectStreamManager(0, myObjectInputStream, this);
        } catch (Exception e) {
            System.out.println("error in setupStreams: "+e.getMessage());
        }
    }

    @Override   // message from other user
    public void objectReceived(int number, Object object, Exception e) {
        if (e == null) {
            if (object.equals("End")) {
                JOptionPane.showMessageDialog(null, "Din vän har loggat ut");
                System.exit(0);
                closeWindow();
                
              //  if (JOptionPane.showConfirmDialog(null, "Är du säker på att du vill avsluta chatten?", "", JOptionPane.YES_NO_CANCEL_OPTION) == JOptionPane.YES_OPTION) {
               // }
                }
                else{    
            display((String) object);}
        } 
        else {
            System.out.println("error in objectReceived: "+e.getMessage());
        }
    }
    public void closeWindow(){
        try{
        myObjectOutputStream.close();
        //myInputStream.close();
       // myOutput.close();
      //  myInput.close();
        theFrame.dispose();
        System.exit(0);
        }
        catch(Exception e){
            System.out.println("error in objectReceived: "+e.getMessage());
        }
        }

    

    @Override   // message from local user
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == endButton) {
            try{
                myObjectOutputStream.writeObject("End");
                System.exit(0);

            }
            catch(IOException ex){
                System.out.println("error in send: "+ex.getMessage());
            }
                            System.exit(0);

        }
        else{
        String message = inputField.getText() + "\n";
        display("I say: " + message);
        send(message);
        inputField.setText("");
        }
    }

    private void display(String message) {
        displayArea.append(message);
    }

    private void send(String message) {
        try {
            myObjectOutputStream.writeObject(message);
        } catch (IOException e) {
            System.out.println("error in send: "+e.getMessage());
        }
    }
}

// Here ends the chat. The rest is just the ObjectStreamManger.
/**
 *
 * @author joachimparrow
 *
 * This is the interface for the listener. It must have a method objectReceived.
 * Whenever reading from the stream results in an object being read or exception
 * being thrown, the object or exception is forwarded to the listener through
 * objectReceived().
 *
 *
 */
interface ObjectStreamListener {

    /**
     * This method is called whenever an object is received or an exception is
     * thrown.
     *
     * @param number The number of the manager as defined in its constructor
     * @param object The object received on the stream
     * @param exception The exception thrown when reading from the stream. Can
     * be IOException or ClassNotFoundException. One of name and exception will
     * always be null. In case of an exception the manager and stream are
     * closed.
     *
     */
    public void objectReceived(int number, Object object, Exception exception);
}

/**
 * @author joachimparrow 2010 This is to read from an input stream in a separate
 * thread, and call a callback when something arrives.
 *
 */
class ObjectStreamManager {

    private final ObjectInputStream theStream;
    private final ObjectStreamListener theListener;
    private final int theNumber;
    private volatile boolean stopped = false;

    /**
     *
     * This creates and starts a stream manager for a stream. The manager will
     * continually read from the stream and forward objects through the
     * objectReceived() method of the ObjectStreamListener parameter
     *
     *
     * @param number The number you give to the manager. It will be included in
     * all calls to readObject. That way you can have the same callback serving
     * several managers since for each received object you get the identity of
     * the manager.
     * @param stream The stream on which the manager should listen.
     * @param listener The object that has the callback objectReceived()
     *
     *
     */
    public ObjectStreamManager(int number, ObjectInputStream stream, ObjectStreamListener listener) {
        theNumber = number;
        theStream = stream;
        theListener = listener;
        new InnerListener().start();  // start to listen on a new thread.
    }

    // This private method accepts an object/exception pair and forwards them
    // to the callback, including also the manager number. The forwarding is scheduled
    // on the Swing thread through an anonymous inner class.
    private void callback(final Object object, final Exception exception) {
        SwingUtilities.invokeLater(
                new Runnable() {
            public void run() {
                if (!stopped) {
                    theListener.objectReceived(theNumber, object, exception);
                    if (exception != null) {
                        closeManager();
                    }
                }
            }
        });
    }

    // This is where the actual reading takes place.
    private class InnerListener extends Thread {

        @Override
        public void run() {
            while (!stopped) {                            // as long as no one stopped me
                try {
                    callback(theStream.readObject(), null); // read an object and forward it
                } catch (Exception e) {                 // if Exception then forward it
                    callback(null, e);
                }
            }
            try {                   // I have been stopped: close stream
                theStream.close();
            } catch (IOException e) {
            }

        }
    }

    /**
     * Stop the manager and close the stream.
     *
     */
    public void closeManager() {
        stopped = true;
    }
}      // end of ObjectStreamManager
