/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.groovy.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.groovy.GroovyScriptExecutingMessageProcessor;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.splitter.MethodInvokingSplitter;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class GroovySplitterTests {

	@Autowired
	private MessageChannel referencedScriptInput;

	@Autowired
	private MessageChannel inlineScriptInput;

	@Autowired
	@Qualifier("groovySplitter.handler")
	private MessageHandler groovySplitterMessageHandler;

	@Test
	public void referencedScript() {
		QueueChannel replyChannel = new QueueChannel();
		replyChannel.setBeanName("returnAddress");
		Message<?> message = MessageBuilder.withPayload("x,y,z").setReplyChannel(replyChannel).build();
		this.referencedScriptInput.send(message);
		assertEquals("x", replyChannel.receive(0).getPayload());
		assertEquals("y", replyChannel.receive(0).getPayload());
		assertEquals("z", replyChannel.receive(0).getPayload());
		assertNull(replyChannel.receive(0));
	}

	@Test
	public void inlineScript() {
		QueueChannel replyChannel = new QueueChannel();
		replyChannel.setBeanName("returnAddress");
		Message<?> message = MessageBuilder.withPayload("a   b c").setReplyChannel(replyChannel).build();
		this.inlineScriptInput.send(message);
		assertEquals("a", replyChannel.receive(0).getPayload());
		assertEquals("b", replyChannel.receive(0).getPayload());
		assertEquals("c", replyChannel.receive(0).getPayload());
		assertNull(replyChannel.receive(0));
	}

	@Test
	public void testInt2433VerifyRiddingOfMessageProcessorsWrapping() {
		assertTrue(this.groovySplitterMessageHandler instanceof MethodInvokingSplitter);
		@SuppressWarnings("rawtypes")
		MessageProcessor messageProcessor = TestUtils.getPropertyValue(this.groovySplitterMessageHandler,
				"messageProcessor", MessageProcessor.class);
		//before it was MethodInvokingMessageProcessor
		assertTrue(messageProcessor instanceof GroovyScriptExecutingMessageProcessor);
	}

}
