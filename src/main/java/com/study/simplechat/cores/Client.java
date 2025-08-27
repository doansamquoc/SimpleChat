package com.study.simplechat.cores;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.SwingUtilities;
import javax.swing.UnsupportedLookAndFeelException;

import com.study.simplechat.configs.Config;
import com.study.simplechat.configs.ThemeConfig;
import com.study.simplechat.views.Login;

public class Client {
    private static Client instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private Thread listenerThread;
    private MessageListener messageListener;

    private static final int PORT = Integer.parseInt(Config.get("sv.port"));
    private static final String HOST = Config.get("sv.host");

    private Client() throws IOException {
        socket = new Socket(HOST, PORT);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        listenerThread = new Thread(this::listenForMessages);
        listenerThread.start();
    }

    public static Client getInstance() throws IOException {
        if (instance == null) {
            instance = new Client();
        }
        return instance;
    }

    public PrintWriter getWriter() {
        return out;
    }

    public BufferedReader getReader() {
        return in;
    }

    public void close() throws IOException {
        socket.close();
        instance = null;
    }

    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    private void listenForMessages() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (messageListener != null) {
                    messageListener.onMessage(line);
                }
            }
        } catch (IOException e) {
            System.out.println("Disconnected.");
        }
    }

    public interface MessageListener {
        void onMessage(String msg);
    }

    public static void main(String[] args) throws IOException {
        // Initalizing client instance
        Client.getInstance();

        // Initializing theme
        try {
            new ThemeConfig();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        // Start client
        Login login = new Login();
        SwingUtilities.invokeLater(() -> {
            login.setVisible(true);
        });
    }
}
