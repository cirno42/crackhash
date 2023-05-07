package ru.nsu.anikolotov.crackhash.manager.config;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MarshallingMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import ru.nsu.anikolotov.crackhash.manager.dto.CrackHashManagerRequest;
import ru.nsu.anikolotov.crackhash.manager.dto.CrackHashWorkerResponse;
import ru.nsu.anikolotov.crackhash.manager.mq.MQConstants;


@EnableRabbit
@Configuration
public class RabbitConfiguration {

    @Bean
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public DirectExchange crackHashExchange() {
        return new DirectExchange(MQConstants.CRACK_HASH_EXCHANGE);
    }

    @Bean
    public Queue managerRequestQueue() {
        return new Queue(MQConstants.MANAGER_REQUEST_QUEUE, true);
    }

    @Bean
    public Queue workerResponseQueue() {
        return new Queue(MQConstants.WORKER_RESPONSE_QUEUE, true);
    }

    @Bean
    public Binding managerRequestBinding(DirectExchange crackHashExchange, Queue managerRequestQueue) {
        return BindingBuilder.bind(managerRequestQueue).to(crackHashExchange).withQueueName();
    }

    @Bean
    public Binding workerResponseBinding(DirectExchange crackHashExchange, Queue workerResponseQueue) {
        return BindingBuilder.bind(workerResponseQueue).to(crackHashExchange).withQueueName();
    }

    @Bean
    public MessageConverter xmlMessageConverter() {
        var marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(
                CrackHashManagerRequest.class,
                CrackHashWorkerResponse.class
        );
        return new MarshallingMessageConverter(marshaller);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                               MessageConverter xmlMessageConverter) {
        final SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(xmlMessageConverter);
        return factory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter xmlMessageConverter) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(xmlMessageConverter);
        return rabbitTemplate;
    }
}
