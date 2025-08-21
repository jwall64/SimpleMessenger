package dev.jwall64.smi;

import com.rabbitmq.client.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.jwall64.mdcfg.ByteBufferInputStream;
import dev.jwall64.mdcfg.registry.Identifier;
import dev.jwall64.mdcfg.registry.Registry;
import dev.jwall64.mdcfg.serializer.InlineSerializer;
import dev.jwall64.mdcfg.serializer.ObjectSerializer;
import dev.jwall64.mdcfg.serializer.SerializeContext;
import dev.jwall64.mdcfg.serializer.SerializeResult;
import dev.jwall64.mdcfg.serializer.Serializer;
import dev.jwall64.midnightlib.event.HandlerList;

public class AmqpMessenger implements SerializableMessenger {

    private static final Logger log =
        LoggerFactory.getLogger(AmqpMessenger.class);

    private final String hostname;
    private final Integer port;
    private final String username;
    private final String password;
    private final String vhost;
    private final String exchange;
    private final String exchangeType;
    private final boolean durable;

    private final Map<String, HandlerList<Message>> queues =
        new ConcurrentHashMap<>();

    private Connection connection;
    private Channel channel;
    private DefaultConsumer consumer;

    private CompletableFuture<?> connectionFuture;

    public AmqpMessenger(String hostname, Integer port, String username,
                         String password, String vhost, String exchange,
                         String exchangeType, boolean durable) {
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.password = password;
        this.vhost = vhost;
        this.durable = durable;
        this.exchange = exchange;
        this.exchangeType = exchangeType;

        connectionFuture = CompletableFuture.runAsync(() -> {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(hostname);

            if (port != null)
                factory.setPort(port);
            if (username != null)
                factory.setUsername(username);
            if (password != null)
                factory.setPassword(password);
            if (vhost != null)
                factory.setVirtualHost(vhost);

            try {
                connection = factory.newConnection();
                channel = connection.createChannel();

                channel.exchangeDeclare(exchange, exchangeType, durable);

                this.consumer = new DefaultConsumer(channel) {
                    @Override
                    public void handleDelivery(String consumerTag,
                                               Envelope envelope,
                                               AMQP.BasicProperties properties,
                                               byte[] body) throws IOException {

                        String queue = envelope.getRoutingKey();
                        HandlerList<Message> subscribers = queues.get(queue);
                        if (subscribers != null)
                            subscribers.invoke(
                                new Message(queue, ByteBuffer.wrap(body)));
                    }
                };

            } catch (Exception ex) {
                log.error("Unable to connect to AMQP server!", ex);
            }

            connectionFuture = null;
        });
    }

    private Channel getChannel() {
        if (connectionFuture != null) {
            connectionFuture.join();
        }
        return channel;
    }

    public String getHostname() { return hostname; }

    public int getPort() { return port; }

    public String getUsername() { return username; }

    public String getPassword() { return password; }

    public String getVirtualHost() { return vhost; }

    public String getExchange() { return exchange; }

    public String getExchangeType() { return exchangeType; }

    public boolean isDurable() { return durable; }

    @Override
    public MessengerType<?> getType() {
        return TYPE;
    }

    @Override
    public void publish(Message msg) {
        try {
            getChannel().basicPublish(exchange, msg.channel, null,
                                      getBytes(msg.payload));
        } catch (IOException ex) {
            log.error("An error occurred while publishing a message!", ex);
        }
    }

    @Override
    public void publish(Message msg, long ttl) {
        try {
            AMQP.BasicProperties.Builder builder =
                new AMQP.BasicProperties.Builder().deliveryMode(2).expiration(
                    Objects.toString(ttl));
            getChannel().basicPublish(exchange, msg.channel, builder.build(),
                                      getBytes(msg.payload));
        } catch (IOException ex) {
            log.error("An error occurred while publishing a message!", ex);
        }
    }

    @Override
    public void subscribe(String routingKey, Object listener,
                          Consumer<Message> handler) {
        queues
            .computeIfAbsent(
                routingKey,
                key -> {
                    try {
                        Channel channel = getChannel();

                        AMQP.Queue.DeclareOk res =
                            channel.queueDeclare(durable ? key : "", durable,
                                                 !durable, !durable, null);

                        channel.queueBind(res.getQueue(), exchange, key);
                        channel.basicConsume(res.getQueue(), true, consumer);

                    } catch (IOException ex) {
                        log.error(
                            "An error occurred while subscribing to queue!",
                            ex);
                    }

                    return new HandlerList<>();
                })
            .register(listener, handler::accept);
    }

    @Override
    public void unsubscribeAll(String channel) {
        HandlerList<Message> hl = queues.get(channel);
        if (hl != null) {
            hl.unregisterAll();
        }
    }

    @Override
    public void unsubscribe(String channel, Object listener) {
        HandlerList<Message> hl = queues.get(channel);
        if (hl != null) {
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

    private byte[] getBytes(ByteBuffer buffer) throws IOException {
        byte[] data;
        if (buffer.hasArray()) {
            data = buffer.array();
        } else {
            data = new byte[buffer.remaining()];
            try (ByteBufferInputStream is = new ByteBufferInputStream(buffer)) {
                int bytesRead = 0;
                while (bytesRead < data.length) {
                    int justRead =
                        is.read(data, bytesRead, data.length - bytesRead);
                    if (justRead == -1) {
                        log.error("Encountered end of stream while "
                                  + "publishing a packet!");
                        break;
                    }

                    bytesRead += justRead;
                }
            }
        }
        return data;
    }
    //
    // private static class EnvSerializer<T> implements Serializer<T> {
    //
    //     private final InlineSerializer<T> inlineSerializer;
    //
    //     EnvSerializer(InlineSerializer<T> inlineSerializer) {
    //         this.inlineSerializer = inlineSerializer;
    //     }
    //
    //     @Override
    //     public <O> SerializeResult<O> serialize(SerializeContext<O> ctx,
    //                                             T value) {
    //         return inlineSerializer.serialize(ctx, value);
    //     }
    //
    //     @Override
    //     public <O> SerializeResult<T> deserialize(SerializeContext<O> ctx,
    //                                               O value) {
    //
    //         if (ctx.isString(value)) {
    //             return inlineSerializer.deserialize(ctx, value);
    //         } else if (ctx.isMap(value)) {
    //             return ctx.asString(ctx.get("env", value)).map(str -> {
    //                 String env = System.getenv(str);
    //                 if (env == null) {
    //                     env = "";
    //                 }
    //                 return inlineSerializer.deserialize(ctx,
    //                 ctx.toString(env));
    //             });
    //         }
    //
    //         return SerializeResult.failure("Not value or environment key! ("
    //         +
    //                                        value + ")");
    //     }
    // }
    //
    public static final Serializer<AmqpMessenger> SERIALIZER =
        ObjectSerializer.create(
            Serializer.STRING.orEnv().entry("hostname",
                                            AmqpMessenger::getHostname),
            Serializer.INT.orEnv()
                .entry("port", AmqpMessenger::getPort)
                .optional(),
            Serializer.STRING.orEnv()
                .entry("username", AmqpMessenger::getUsername)
                .optional(),
            Serializer.STRING.orEnv()
                .entry("password", AmqpMessenger::getPassword)
                .optional(),
            Serializer.STRING.orEnv()
                .entry("virtual_host", AmqpMessenger::getVirtualHost)
                .optional(),
            Serializer.STRING.orEnv()
                .entry("exchange", AmqpMessenger::getExchange)
                .orElse("smi"),
            Serializer.STRING.orEnv()
                .entry("exchange_type", AmqpMessenger::getExchangeType)
                .orElse("direct"),
            Serializer.BOOLEAN.orEnv()
                .entry("durable", AmqpMessenger::isDurable)
                .orElse(false),
            AmqpMessenger::new);

    public static final Identifier ID = new Identifier("smi", "amqp");
    public static final MessengerType<AmqpMessenger> TYPE =
        new MessengerType<AmqpMessenger>() {
            @Override
            public Class<AmqpMessenger> getMessengerClass() {
                return AmqpMessenger.class;
            }

            @Override
            public Serializer<AmqpMessenger> getSerializer() {
                return SERIALIZER;
            }
        };

    public static void register(
        Registry<Identifier, MessengerType<?>> registry) {
        registry.register(ID, TYPE);
    }
}
