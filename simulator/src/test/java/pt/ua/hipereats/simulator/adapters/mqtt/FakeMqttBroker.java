package pt.ua.hipereats.simulator.adapters.mqtt;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The few MQTT 3.1.1 packets the gateway needs, with control over what a real broker does not let a
 * test choose, such as refusing a subscription while the connection stays up.
 */
final class FakeMqttBroker implements AutoCloseable {

    private static final int CONNECT = 1;
    private static final int PUBLISH = 3;
    private static final int SUBSCRIBE = 8;
    private static final int PINGREQ = 12;
    private static final int DISCONNECT = 14;
    private static final int REFUSED = 0x80;

    private final ServerSocket server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
    private final Set<Socket> open = ConcurrentHashMap.newKeySet();
    private final AtomicInteger connects = new AtomicInteger();
    private final AtomicInteger subscribes = new AtomicInteger();
    private final AtomicInteger refusals = new AtomicInteger();

    FakeMqttBroker() throws IOException {
        Thread.ofVirtual().start(this::acceptConnections);
    }

    String url() {
        return "tcp://" + server.getInetAddress().getHostAddress() + ":" + server.getLocalPort();
    }

    /** Refuses the next subscription (as an ACL does), keeping the connection up. */
    void refuseNextSubscription() {
        refusals.incrementAndGet();
    }

    /** Connections asked for so far (CONNECT packets). */
    int connects() {
        return connects.get();
    }

    /** Subscriptions asked for so far (SUBSCRIBE packets). */
    int subscribes() {
        return subscribes.get();
    }

    /** Sends a message (QoS 0) to every connected client. */
    void publish(String topic, String payload) throws IOException {
        byte[] name = topic.getBytes(StandardCharsets.UTF_8);
        byte[] content = payload.getBytes(StandardCharsets.UTF_8);
        int length = 2 + name.length + content.length;
        ByteArrayOutputStream packet = new ByteArrayOutputStream();
        packet.write(PUBLISH << 4);
        do {
            int digit = length % 128;
            length /= 128;
            packet.write(length > 0 ? digit | 0x80 : digit);
        } while (length > 0);
        packet.write(name.length >> 8);
        packet.write(name.length);
        packet.writeBytes(name);
        packet.writeBytes(content);
        for (Socket socket : open) {
            send(socket, packet.toByteArray());
        }
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
                        send(socket, bytes(0x20, 2, 0, 0));
                    }
                    case SUBSCRIBE -> {
                        subscribes.incrementAndGet();
                        boolean refused = refusals.getAndUpdate(left -> Math.max(0, left - 1)) > 0;
                        send(socket, bytes(0x90, 3, body[0], body[1], refused ? REFUSED : 1));
                    }
                    case PINGREQ -> send(socket, bytes(0xD0, 0));
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

    private static byte[] bytes(int... values) {
        byte[] packet = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            packet[i] = (byte) values[i];
        }
        return packet;
    }

    private static void send(Socket socket, byte[] packet) throws IOException {
        synchronized (socket) {
            socket.getOutputStream().write(packet);
        }
    }

    @Override
    public void close() throws IOException {
        server.close();
        for (Socket socket : open) {
            socket.close();
        }
    }
}
