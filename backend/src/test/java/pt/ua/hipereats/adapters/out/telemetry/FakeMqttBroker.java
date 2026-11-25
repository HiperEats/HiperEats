package pt.ua.hipereats.adapters.out.telemetry;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The few MQTT 3.1.1 packets the subscriber needs, with control over what a real broker does not let
 * a test choose: how long the connection takes and which subscriptions are refused.
 */
final class FakeMqttBroker implements AutoCloseable {

    private static final int CONNECT = 1;
    private static final int SUBSCRIBE = 8;
    private static final int PINGREQ = 12;
    private static final int DISCONNECT = 14;
    private static final byte GRANTED_QOS_0 = 0;
    private static final byte REFUSED = (byte) 0x80;

    private final ServerSocket server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
    private final Set<Socket> open = ConcurrentHashMap.newKeySet();
    private final AtomicInteger connects = new AtomicInteger();
    private final AtomicInteger subscribes = new AtomicInteger();
    private final Queue<Byte> subscribeAnswers = new ConcurrentLinkedQueue<>();
    private volatile Duration connectDelay = Duration.ZERO;

    FakeMqttBroker() throws IOException {
        Thread.ofVirtual().start(this::acceptConnections);
    }

    String url() {
        return "tcp://" + server.getInetAddress().getHostAddress() + ":" + server.getLocalPort();
    }

    /** How long the broker takes to answer the next connections. */
    void delayConnections(Duration delay) {
        connectDelay = delay;
    }

    /** Refuses the next subscription (as an ACL does), keeping the connection up. */
    void refuseNextSubscription() {
        subscribeAnswers.add(REFUSED);
    }

    /** Closes every connection, as a broker that stops. */
    void dropConnections() throws IOException {
        for (Socket socket : open) {
            socket.close();
        }
    }

    /** Connections asked for so far (CONNECT packets). */
    int connects() {
        return connects.get();
    }

    /** Subscriptions asked for so far (SUBSCRIBE packets). */
    int subscribes() {
        return subscribes.get();
    }

    int openConnections() {
        return open.size();
    }

    private void acceptConnections() {
        while (!server.isClosed()) {
            try {
                Socket socket = server.accept();
                open.add(socket);
                Thread.ofVirtual().start(() -> serve(socket));
            } catch (IOException e) {
                return; // close() was called
            }
        }
    }

    private void serve(Socket socket) {
        try (socket) {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            while (true) {
                int type = in.readUnsignedByte() >> 4;
                byte[] body = in.readNBytes(remainingLength(in));
                switch (type) {
                    case CONNECT -> {
                        connects.incrementAndGet();
                        // On another thread, so a client that gives up meanwhile is noticed at once.
                        Duration delay = connectDelay;
                        Thread.ofVirtual().start(() -> acceptClient(socket, delay));
                    }
                    case SUBSCRIBE -> {
                        subscribes.incrementAndGet();
                        Byte answer = subscribeAnswers.poll();
                        send(socket, 0x90, 3, body[0], body[1], answer == null ? GRANTED_QOS_0 : answer);
                    }
                    case PINGREQ -> send(socket, 0xD0, 0);
                    case DISCONNECT -> {
                        return;
                    }
                    default -> { /* not needed by these tests */ }
                }
            }
        } catch (IOException e) {
            // The client closed the connection.
        } finally {
            open.remove(socket);
        }
    }

    private static void acceptClient(Socket socket, Duration delay) {
        try {
            Thread.sleep(delay);
            send(socket, 0x20, 2, 0, 0);
        } catch (IOException e) {
            // The client gave up before the answer.
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static int remainingLength(DataInputStream in) throws IOException {
        int length = 0;
        int shift = 0;
        int digit;
        do {
            digit = in.readUnsignedByte();
            length |= (digit & 0x7F) << shift;
            shift += 7;
        } while ((digit & 0x80) != 0);
        return length;
    }

    private static void send(Socket socket, int... bytes) throws IOException {
        byte[] packet = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            packet[i] = (byte) bytes[i];
        }
        socket.getOutputStream().write(packet);
    }

    @Override
    public void close() throws IOException {
        server.close();
        dropConnections();
    }
}
