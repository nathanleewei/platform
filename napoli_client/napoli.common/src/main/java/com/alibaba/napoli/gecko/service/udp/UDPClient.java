/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
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
package com.alibaba.napoli.gecko.service.udp;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import com.alibaba.napoli.gecko.service.exception.NotifyRemotingException;


/**
 * UDP客户端
 * 
 * @author boyan
 * @Date 2010-8-26
 * 
 */
public interface UDPClient extends UDPController {

    /**
     * 发送UDP消息到指定IP
     * 
     * @param buffer
     */
    public void send(InetSocketAddress inetSocketAddress, ByteBuffer buffer) throws NotifyRemotingException;

}