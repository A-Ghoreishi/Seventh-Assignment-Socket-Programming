package org.example;

import java.io.*;
import java.net.*;
import java.util.*;

public class Client {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 5555;
    private static final String DOWNLOADS_DIRECTORY = "Data";

    public static void main(String[] args) throws Exception {
        Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
        BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
        Scanner scanner = new Scanner(System.in);

        System.out.println("Select an option:");
        System.out.println("1. Join Group Chat");
        System.out.println("2. Download File");

        int choice = scanner.nextInt();
        scanner.nextLine(); // consume the newline character

        if (choice == 1) {
            output.println("CHAT");
            joinChat(socket, input, output, scanner);
        } else if (choice == 2) {
            output.println("FILE");
            handleFileTransfer(socket, input, output, scanner);
        }

        socket.close();
    }

    private static void joinChat(Socket socket, BufferedReader input, PrintWriter output, Scanner scanner) {
        Thread listenerThread = new Thread(new Runnable() {
            public void run() {
                try {
                    String message;
                    while ((message = input.readLine()) != null) {
                        System.out.println(message);
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            }
        });

        listenerThread.start();

        try {
            while (true) {
                String message = scanner.nextLine();
                if (message != null) {
                    output.println(message);
                }
            }
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Failed to close socket.");
            }
        }
    }

    private static void handleFileTransfer(Socket socket, BufferedReader input, PrintWriter output, Scanner scanner) throws IOException {
        System.out.println("Type 'GET_FILES' to list files or 'DOWNLOAD_FILE <filename>' to download a file.");

        Thread listenerThread = new Thread(new Runnable() {
            public void run() {
                try {
                    String message;
                    while ((message = input.readLine()) != null) {
                        if (message.equals("END_OF_LIST")) {
                            break;
                        }
                        System.out.println(message);
                    }
                } catch (IOException e) {
                    System.out.println(e);
                }
            }
        });

        listenerThread.start();

        while (true) {
            String command = scanner.nextLine();
            output.println(command);
            if (command.startsWith("DOWNLOAD_FILE")) {
                String fileName = command.substring(14).trim();
                downloadFile(socket, fileName);
            }
        }
    }

    private static void downloadFile(Socket socket, String fileName) {
        try {
            InputStream is = socket.getInputStream();
            File downloadsDir = new File(DOWNLOADS_DIRECTORY);
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs();
            }

            FileOutputStream fos = new FileOutputStream(new File(DOWNLOADS_DIRECTORY, fileName));
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            fos.close();
            System.out.println("File downloaded: " + fileName);
        } catch (IOException e) {
            System.out.println("File download failed: " + e.getMessage());
        }
    }
}
