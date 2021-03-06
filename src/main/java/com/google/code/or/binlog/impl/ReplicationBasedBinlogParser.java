/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.code.or.binlog.impl;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.or.binlog.BinlogEventParser;
import com.google.code.or.binlog.impl.event.BinlogEventV4HeaderImpl;
import com.google.code.or.common.util.MySQLConstants;
import com.google.code.or.io.XInputStream;
import com.google.code.or.net.Transport;
import com.google.code.or.net.TransportInputStream;
import com.google.code.or.net.impl.EventInputStream;
import com.google.code.or.net.impl.packet.EOFPacket;
import com.google.code.or.net.impl.packet.ErrorPacket;
import com.google.code.or.net.impl.packet.OKPacket;

/**
 *
 * @author Jingqi Xu
 */
public class ReplicationBasedBinlogParser extends AbstractBinlogParser {
	//
	private static final Logger LOGGER = LoggerFactory.getLogger(ReplicationBasedBinlogParser.class);

	//
	protected Transport transport;


	/**
	 *
	 */
	public ReplicationBasedBinlogParser() {
	}

	@Override
	protected void doStart() throws Exception {
		// NOP
	}

	@Override
	protected void doStop(long timeout, TimeUnit unit) throws Exception {
		// NOP
	}

	/**
	 *
	 */
	public Transport getTransport() {
		return transport;
	}

	public void setTransport(Transport transport) {
		this.transport = transport;
	}

	@Override
	public String getBinlogFileName() {
		return binlogFileName;
	}

	public void setBinlogFileName(String binlogFileName) {
		this.binlogFileName = binlogFileName;
	}

	/**
	 *
	 */
	@Override
	protected void doParse() throws Exception {
		//
		final TransportInputStream is = this.transport.getInputStream();
		final EventInputStream es = new EventInputStream(is);

		final Context context = new Context(this);

		BinlogEventV4HeaderImpl header;
		while(isRunning()) {
			header = es.getNextBinlogHeader();

			// Parse the event body
			if(this.eventFilter != null && !this.eventFilter.accepts(header, context)) {
				this.defaultParser.parse(is, header, context);
			} else {
				BinlogEventParser parser = getEventParser(header.getEventType());
				if(parser == null) parser = this.defaultParser;
				parser.parse(es, header, context);
			}

			if ( header.getEventType() == MySQLConstants.FORMAT_DESCRIPTION_EVENT )
				es.setChecksumEnabled(context.getChecksumEnabled());

			es.finishEvent(header);
		}
	}
}
