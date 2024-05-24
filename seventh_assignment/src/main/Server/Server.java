package org.example;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 5555;
    private static Set<PrintWriter> clientWriters = new HashSet<>();
    private static List<String> chatHistory = new ArrayList<>();
    private static final String FILES_DIRECTORY = "data";

    public static void main(String[] args) throws Exception {
        System.out.println("Server starting...");

        // Create the directory if it does not exist
        File directory = new File(FILES_DIRECTORY);
        if (!directory.exists()) {
            directory.mkdirs();
            System.out.println("Created directory: " + FILES_DIRECTORY);
        } else {
            System.out.println("Using existing directory: " + FILES_DIRECTORY);
        }

        ServerSocket listener = new ServerSocket(PORT);
        try {
            while (true) {
                new org.example.Server.Handler(listener.accept()).start();
            }
        } finally {
            listener.close();
        }
    }

    private static class Handler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String command = in.readLine();
                if ("CHAT".equalsIgnoreCase(command)) {
                    handleChat();
                } else if ("FILE".equalsIgnoreCase(command)) {
                    handleFile();
                }
            } catch (IOException e) {
                System.out.println(e);
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println(e);
                }
            }
        }

        private void handleChat() throws IOException {
            synchronized (clientWriters) {
                clientWriters.add(out);
                for (String message : chatHistory) {
                    out.println(message);
                }
            }

            String message;
            while (true) {
                if (socket.isClosed() || socket.isInputShutdown() || socket.isOutputShutdown()) {
                    System.out.println("Client disconnected");
                    break;
                }

                try {
                    if ((message = in.readLine()) != null) {
                        synchronized (chatHistory) {
                            chatHistory.add(message);
                        }
                        synchronized (clientWriters) {
                            for (PrintWriter writer : clientWriters) {
                                if (writer != out) { // Skip sending to the sender
                                    writer.println(message);
                                }
                            }
                        }
                    } else {
                        break; // Client has disconnected
                    }
                } catch (IOException e) {
                    System.out.println("Connection lost: " + e.getMessage());
                    break;
                }
            }

            // Clean up
            synchronized (clientWriters) {
                clientWriters.remove(out);
            }
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Error closing socket: " + e.getMessage());
            }
        }

        private void handleFile() throws IOException {
            String command;
            while ((command = in.readLine()) != null) {
                if (command.startsWith("GET_FILES")) {
                    listFiles();
                } else if (command.startsWith("DOWNLOAD_FILE")) {
                    String fileName = command.substring(14).trim();
                    File file = new File(FILES_DIRECTORY, fileName);
                    if (file.exists() && !file.isDirectory()) {
                        out.println("FILE_FOUND");
                        sendFile(file);
                    } else {
                        out.println("FILE_NOT_FOUND");
                    }
                }
            }
        }

        private void listFiles() throws IOException {
            File folder = new File(FILES_DIRECTORY);
            File[] listOfFiles = folder.listFiles();
            if (listOfFiles != null) {
                for (File file : listOfFiles) {
                    if (file.isFile()) {
                        out.println(file.getName());
                    }
                }
            }
            out.println("END_OF_LIST");
        }

        private void sendFile(File file) throws IOException {
            byte[] buffer = new byte[4096];
            FileInputStream fis = new FileInputStream(file);
            OutputStream os = socket.getOutputStream();
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
            fis.close();
        }
    }
}
