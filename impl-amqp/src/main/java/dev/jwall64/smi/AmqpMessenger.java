package dev.jwall64.smi;

import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.jwall64.mdcfg.ByteBufferInputStream;
import dev.jwall64.mdcfg.serializer.ObjectSerializer;
import dev.jwall64.mdcfg.serializer.Serializer;
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
import java.util.function.Consumer;

public class AmqpMessenger implements SerializableMessenger {

    private static final Logger log = LoggerFactory.getLogger(AmqpMessenger.class);

    private final String hostname;
    private final Integer port;
    private final String username;
    private final String password;
    private final String vhost;
    private final String exchange;
    private final boolean durable;
    private final int expiry;


    private final Map<String, HandlerList<Message>> queues = new ConcurrentHashMap<>();

    private Connection connection;
    private Channel channel;
    private DefaultConsumer consumer;

    private CompletableFuture<?> connectionFuture;


    public AmqpMessenger(String hostname, Integer port, String username, String password, String vhost, String exchange, boolean durable, int expiry) {
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.password = password;
        this.vhost = vhost;
        this.durable = durable;
        this.exchange = exchange;
        this.expiry = expiry;


        connectionFuture = CompletableFuture.runAsync(() -> {

            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(hostname);

            if (port != null) factory.setPort(port);
            if (username != null) factory.setUsername(username);
            if (password != null) factory.setPassword(password);
            if (vhost != null) factory.setVirtualHost(vhost);

            try {
                connection = factory.newConnection();
                channel = connection.createChannel();

                channel.exchangeDeclare(exchange, BuiltinExchangeType.FANOUT, durable);

                this.consumer = new DefaultConsumer(channel) {

                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {

                        String queue = envelope.getRoutingKey();
                        HandlerList<Message> subscribers = queues.get(queue);
                        if (subscribers != null) subscribers.invoke(new Message(queue, ByteBuffer.wrap(body)));

                        channel.basicAck(envelope.getDeliveryTag(), false);
                    }
                };

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

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
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

    public String getExchange() {
        return exchange;
    }

    public boolean isDurable() {
        return durable;
    }

    public int getQueueExpiry() {
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
            getChannel().basicPublish(exchange, msg.channel, null, getBytes(msg.payload));
        } catch (IOException ex) {
            log.error("An error occurred while publishing a message!", ex);
        }
    }

    @Override
    public void publish(Message msg, long ttl) {
        addQueue(msg.channel);
        try {
            AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder().deliveryMode(2).expiration(Objects.toString(ttl));
            getChannel().basicPublish(exchange, msg.channel, builder.build(), getBytes(msg.payload));
        } catch (IOException ex) {
            log.error("An error occurred while publishing a message!", ex);
        }
    }

    @Override
    public void subscribe(String channel, Object listener, Consumer<Message> handler) {
        addQueue(channel);
        queues.get(channel).register(listener, handler::accept);
    }

    @Override
    public void unsubscribeAll(String channel) {
        HandlerList<Message> hl = queues.get(channel);
        if(hl != null) {
            hl.unregisterAll();
        }
    }

    @Override
    public void unsubscribe(String channel, Object listener) {
        HandlerList<Message> hl = queues.get(channel);
        if(hl != null) {
            hl.unregisterAll(listener);
        }
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

    @Override
    public boolean isClosed() {
        return !connection.isOpen();
    }

    private void addQueue(String queue) {
        if(!queues.containsKey(queue)) {
            try {
                Map<String, Object> args = new HashMap<>();
                if (expiry > 0) args.put("x-expires", expiry);

                Channel channel = getChannel();

                channel.queueDeclare(queue, durable, false, false, args);
                channel.queueBind(queue, exchange, queue);

                channel.basicConsume(queue, false, consumer);

                queues.put(queue, new HandlerList<>());

            } catch (IOException ex) {
                log.error("An error occurred while subscribing to queue!", ex);
            }
        }
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
            Serializer.STRING.entry("hostname", AmqpMessenger::getHostname),
            Serializer.INT.entry("port", AmqpMessenger::getPort).optional(),
            Serializer.STRING.entry("username", AmqpMessenger::getUsername).optional(),
            Serializer.STRING.entry("password", AmqpMessenger::getPassword).optional(),
            Serializer.STRING.entry("virtual_host", AmqpMessenger::getVirtualHost).optional(),
            Serializer.STRING.entry("exchange", AmqpMessenger::getExchange).orElse("smi"),
            Serializer.BOOLEAN.entry("durable", AmqpMessenger::isDurable).orElse(false),
            Serializer.INT.entry("queue_expiry", AmqpMessenger::getQueueExpiry).orElse(0),
            AmqpMessenger::new
    );

    public static final Identifier ID = new Identifier("smi", "amqp");
    public static final MessengerType<AmqpMessenger> TYPE = new MessengerType<AmqpMessenger>() {
        @Override
        public Class<AmqpMessenger> getMessengerClass() {
            return AmqpMessenger.class;
        }

        @Override
        public Serializer<AmqpMessenger> getSerializer() {
            return SERIALIZER;
        }
    };

    public static void register(Registry<Identifier, MessengerType<?>> registry) {
        registry.register(ID, TYPE);
    }
}
