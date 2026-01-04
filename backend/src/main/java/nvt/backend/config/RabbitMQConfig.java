package nvt.backend.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String HEARTBEAT_EXCHANGE = "vehicle.heartbeat";
    public static final String TELEMETRY_EXCHANGE = "vehicle.telemetry";
    public static final String STATE_EXCHANGE = "vehicle.state";

    public static final String HEARTBEAT_QUEUE = "vehicle.heartbeat.queue";
    public static final String TELEMETRY_QUEUE = "vehicle.telemetry.queue";
    public static final String STATE_QUEUE = "vehicle.state.queue";

    @Bean
    public TopicExchange heartbeatExchange() {
        return new TopicExchange(HEARTBEAT_EXCHANGE);
    }

    @Bean
    public TopicExchange telemetryExchange() {
        return new TopicExchange(TELEMETRY_EXCHANGE);
    }

    @Bean
    public TopicExchange stateExchange() {
        return new TopicExchange(STATE_EXCHANGE);
    }

    @Bean
    public Queue heartbeatQueue() {
        return QueueBuilder.durable(HEARTBEAT_QUEUE).build();
    }

    @Bean
    public Queue telemetryQueue() {
        return QueueBuilder.durable(TELEMETRY_QUEUE).build();
    }

    @Bean
    public Queue stateQueue() {
        return QueueBuilder.durable(STATE_QUEUE).build();
    }

    @Bean
    public Binding heartbeatBinding(Queue heartbeatQueue, TopicExchange heartbeatExchange) {
        return BindingBuilder.bind(heartbeatQueue).to(heartbeatExchange).with("vehicle.*.heartbeat");
    }

    @Bean
    public Binding telemetryBinding(Queue telemetryQueue, TopicExchange telemetryExchange) {
        return BindingBuilder.bind(telemetryQueue).to(telemetryExchange).with("vehicle.*.telemetry");
    }

    @Bean
    public Binding stateBinding(Queue stateQueue, TopicExchange stateExchange) {
        return BindingBuilder.bind(stateQueue).to(stateExchange).with("vehicle.*.state");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        return factory;
    }
}