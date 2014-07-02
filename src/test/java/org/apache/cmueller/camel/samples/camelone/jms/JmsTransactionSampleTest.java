package org.apache.cmueller.camel.samples.camelone.jms;

import java.sql.SQLException;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.usage.SystemUsage;
import org.apache.activemq.usage.TempUsage;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class JmsTransactionSampleTest extends CamelSpringTestSupport {

    private BrokerService broker;

    @Before
    @Override
    public void setUp() throws Exception {
        broker = new BrokerService();
        broker.setPersistent(false);
        broker.setUseJmx(false);
        broker.setBrokerName("localhost");
        broker.addConnector("tcp://localhost:61616");
        SystemUsage systemUsage = new SystemUsage();
        TempUsage tempUsage = new TempUsage();
        tempUsage.setLimit(52428800L);
        systemUsage.setTempUsage(tempUsage);
        broker.setSystemUsage(systemUsage);
        broker.start();

        super.setUp();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();

        if (broker != null) {
            broker.stop();
        }
    }

    @Test
    public void moneyShouldBeTransfered() {
        template.sendBodyAndHeader("activemq:queue:transaction.incoming.one", "Camel rocks!", "amount", "100");

        Exchange exchange = consumer.receive("activemq:queue:transaction.outgoing.one", 5000);
        assertNotNull(exchange);
    }

    @Test
    public void moneyShouldNotTransfered() {
        template.sendBodyAndHeader("activemq:queue:transaction.incoming.two", "Camel rocks!", "amount", "100");

        Exchange exchange = consumer.receive("activemq:queue:ActiveMQ.DLQ", 5000);
        assertNotNull(exchange);
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("activemqTx:queue:transaction.incoming.one").transacted("PROPAGATION_REQUIRED")
                        .to("bean:businessService?method=computeOffer").to("activemqTx:queue:transaction.outgoing.one");

                from("activemqTx:queue:transaction.incoming.two").transacted("PROPAGATION_REQUIRED")
                        .throwException(new SQLException("forced exception for test"))
                        .to("activemqTx:queue:transaction.outgoing.two");
            }
        };
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("META-INF/spring/JmsTransactionSampleTest-context.xml");
    }
}