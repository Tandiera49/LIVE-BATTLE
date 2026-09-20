package com.livebattle.game;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class TikTokEventServer {

    public interface Listener {
        void onEvent(String type, String username, String giftId,
                     String giftName, int count);
    }

    private static final int PORT = 8765;

    private final Listener listener;
    private volatile boolean running;
    private ServerSocket serverSocket;
    private Thread serverThread;

    public TikTokEventServer(Listener listener) {
        this.listener = listener;
    }

    public void start() {
        if (running) {
            return;
        }

        running = true;

        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(
                        PORT,
                        16,
                        InetAddress.getByName("127.0.0.1")
                );

                System.out.println("[TikTokEventServer] listening on 127.0.0.1:" + PORT);

                while (running) {
                    try {
                        Socket socket = serverSocket.accept();
                        handle(socket);
                    } catch (IOException ignored) {
                        if (running) {
                            ignored.printStackTrace();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, "TikTokEventServer");

        serverThread.setDaemon(true);
        serverThread.start();
    }

    private void handle(Socket socket) {
        try (Socket s = socket;
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(
                             s.getInputStream(),
                             StandardCharsets.UTF_8
                     ));
             OutputStream out = s.getOutputStream()) {

            String requestLine = reader.readLine();

            if (requestLine == null) {
                return;
            }

            int contentLength = 0;
            String line;

            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                String lower = line.toLowerCase();

                if (lower.startsWith("content-length:")) {
                    try {
                        contentLength = Integer.parseInt(
                                line.substring(line.indexOf(':') + 1).trim()
                        );
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            char[] bodyChars = new char[contentLength];
            int total = 0;

            while (total < contentLength) {
                int read = reader.read(bodyChars, total, contentLength - total);
                if (read < 0) {
                    break;
                }
                total += read;
            }

            String body = new String(bodyChars, 0, total);
            Map<String, String> data = parseForm(body);

            String type = value(data, "type");
            String username = value(data, "username");
            String giftId = value(data, "giftId");
            String giftName = value(data, "giftName");

            int count = 1;

            try {
                count = Math.max(1, Integer.parseInt(value(data, "count")));
            } catch (Exception ignored) {
            }

            if (listener != null && type != null && !type.isEmpty()) {
                listener.onEvent(
                        type,
                        username,
                        giftId,
                        giftName,
                        count
                );
            }

            byte[] response = (
                    "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/plain; charset=utf-8\r\n" +
                    "Content-Length: 2\r\n" +
                    "Connection: close\r\n" +
                    "\r\n" +
                    "OK"
            ).getBytes(StandardCharsets.UTF_8);

            out.write(response);
            out.flush();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Map<String, String> parseForm(String body) {
        Map<String, String> result = new HashMap<>();

        if (body == null || body.isEmpty()) {
            return result;
        }

        String[] pairs = body.split("&");

        for (String pair : pairs) {
            int equals = pair.indexOf('=');

            String key;
            String value;

            if (equals >= 0) {
                key = pair.substring(0, equals);
                value = pair.substring(equals + 1);
            } else {
                key = pair;
                value = "";
            }

            try {
                key = URLDecoder.decode(key, "UTF-8");
                value = URLDecoder.decode(value, "UTF-8");
            } catch (Exception ignored) {
            }

            result.put(key, value);
        }

        return result;
    }

    private static String value(Map<String, String> data, String key) {
        String value = data.get(key);
        return value == null ? "" : value;
    }

    public void stop() {
        running = false;

        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }

        serverSocket = null;
        serverThread = null;
    }
}
