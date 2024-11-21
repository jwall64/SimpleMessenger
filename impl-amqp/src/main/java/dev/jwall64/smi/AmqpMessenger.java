package dev.jwall64.smi;

import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.jwall64.mdcfg.ByteBufferInputStream;
import dev.jwall64.mdcfg.serializer.ObjectSerializer;
import dev.jwall64.mdcfg.serializer.Serializer;
import dev.jwall64.midnightlib.event.EventHandler;
import dev.jwall64.midnightlib.event.HandlerList;
import dev.jwall64.midnightlib.registry.Identifier;
import dev.jwall64.midnightlib.registry.Registry;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

public class AmqpMessenger implements Messenger {

    private static final Logger log = LoggerFactory.getLogger(AmqpMessenger.class);
    private final String url;
    private final String username;
    private final String password;
    private final String vhost;
    private final boolean durable;
    private final int expiry;

    private final DeliverCallback callback;
    private final Map<Identifier, HandlerList<Message>> queues = new ConcurrentHashMap<>();

    private Connection connection;
    private Channel channel;

    private CompletableFuture<?> connectionFuture;

    public AmqpMessenger(String url, String username, String password, String vhost, boolean durable, int expiry) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.vhost = vhost;
        this.durable = durable;
        this.expiry = expiry;

        this.callback = (tag, delivery) -> {
            Identifier queue = Identifier.parseOrDefault(delivery.getEnvelope().getRoutingKey(), "smi");
            HandlerList<Message> subscribers = queues.get(queue);
            if (subscribers == null) return;

            subscribers.invoke(new Message(queue, ByteBuffer.wrap(delivery.getBody())));
        };

        connectionFuture = CompletableFuture.runAsync(() -> {

            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(url);

            if (username != null) factory.setUsername(username);
            if (password != null) factory.setPassword(password);
            if (vhost != null) factory.setPassword(vhost);

            try {
                connection = factory.newConnection();
                channel = connection.createChannel();
            } catch (Exception ex) {
                log.error("Unable to connect to AMQP server!", ex);
            }

            connectionFuture = null;
        });

    }

    private Channel getChannel() {
        if(connectionFuture != null) {
            connectionFuture.join();
        }
        return channel;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getVirtualHost() {
        return vhost;
    }

    public boolean isDurable() {
        return durable;
    }

    public int getChannelExpiry() {
        return expiry;
    }

    @Override
    public MessengerType<?> getType() {
        return TYPE;
    }

    @Override
    public void publish(Message msg) {
        addQueue(msg.channel);
        try {
            AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder().expiration(Objects.toString(expiry));
            getChannel().basicPublish("", msg.channel.toString(), builder.build(), getBytes(msg.payload));
        } catch (IOException ex) {
            log.error("An error occurred while publishing a message!", ex);
        }
    }

    @Override
    public void publish(Message msg, long ttl) {
        addQueue(msg.channel);
        try {
            getChannel().basicPublish("", msg.channel.toString(), null, getBytes(msg.payload));
        } catch (IOException ex) {
            log.error("An error occurred while publishing a message!", ex);
        }
    }

    @Override
    public void subscribe(Identifier channel, Object listener, EventHandler<Message> handler) {
        addQueue(channel);
        queues.get(channel).register(listener, handler);
    }

    @Override
    public void close() {
        try {
            getChannel().close();
            connection.close();
        } catch (IOException | TimeoutException ex) {
            log.error("Unable to close connection!");
        }
    }

    private void addQueue(Identifier queue) {
        queues.computeIfAbsent(queue, q -> {
            try {
                Map<String, Object> args = new HashMap<>();
                if (expiry > 0) args.put("x-expires", expiry);

                Channel channel = getChannel();

                channel.queueDeclare(queue.toString(), durable, false, false, args);
                channel.basicConsume(queue.toString(), true, callback, tag -> { });

            } catch (IOException ex) {
                log.error("An error occurred while subscribing to queue!", ex);
            }
            return new HandlerList<>();
        });
    }

    private byte[] getBytes(ByteBuffer buffer) throws IOException {
        byte[] data;
        if(buffer.hasArray()) {
            data = buffer.array();
        } else {
            data = new byte[buffer.remaining()];
            try(ByteBufferInputStream is = new ByteBufferInputStream(buffer)) {
                int bytesRead = 0;
                while(bytesRead < data.length) {
                    int justRead = is.read(data, bytesRead, data.length - bytesRead);
                    if(justRead == -1) {
                        log.error("Encountered end of stream while publishing a packet!");
                        break;
                    }

                    bytesRead += justRead;
                }
            }
        }
        return data;
    }

    public static final Serializer<AmqpMessenger> SERIALIZER = ObjectSerializer.create(
            Serializer.STRING.entry("url", AmqpMessenger::getUrl),
            Serializer.STRING.entry("username", AmqpMessenger::getUsername).optional(),
            Serializer.STRING.entry("password", AmqpMessenger::getPassword).optional(),
            Serializer.STRING.entry("virtual_host", AmqpMessenger::getVirtualHost).optional(),
            Serializer.BOOLEAN.entry("durable", AmqpMessenger::isDurable).orElse(false),
            Serializer.INT.entry("channel_expiry", AmqpMessenger::getChannelExpiry).orElse(60000),
            AmqpMessenger::new
    );

    public static final Identifier ID = new Identifier("smi", "amqp");
    public static final MessengerType<AmqpMessenger> TYPE = new MessengerType<>(AmqpMessenger.class, SERIALIZER);

    public static void register(Registry<Identifier, MessengerType<?>> registry) {
        registry.register(ID, TYPE);
    }
}
