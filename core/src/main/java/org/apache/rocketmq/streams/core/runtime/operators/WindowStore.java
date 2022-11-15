/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.streams.core.runtime.operators;


import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.streams.core.function.ValueMapperAction;
import org.apache.rocketmq.streams.core.state.StateStore;
import org.apache.rocketmq.streams.core.util.Pair;
import org.apache.rocketmq.streams.core.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class WindowStore<K, V> {
    private static final Logger logger = LoggerFactory.getLogger(WindowStore.class.getName());

    private StateStore stateStore;
    private ValueMapperAction<byte[], WindowState<K, V>> bytes2State;
    private ValueMapperAction<WindowState<K, V>, byte[]> state2Bytes;


    public WindowStore(StateStore stateStore,
                       ValueMapperAction<byte[], WindowState<K, V>> bytes2State,
                       ValueMapperAction<WindowState<K, V>, byte[]> state2Bytes) {
        this.stateStore = stateStore;
        this.bytes2State = bytes2State;
        this.state2Bytes = state2Bytes;
    }

    public void put(MessageQueue stateTopicMessageQueue, WindowKey windowKey, WindowState<K, V> value) throws Throwable {
        logger.debug("put key into store, key: " + windowKey);
        byte[] keyBytes = this.key2Byte(windowKey);
        byte[] valueBytes = this.state2Bytes.convert(value);

        this.stateStore.put(stateTopicMessageQueue, keyBytes, valueBytes);
    }

    public WindowState<K, V> get(WindowKey windowKey) throws Throwable {
        byte[] bytes = this.key2Byte(windowKey);
        byte[] valueBytes = this.stateStore.get(bytes);
        return deserializerState(valueBytes);
    }

    public List<Pair<WindowKey, WindowState<K, V>>> searchLessThanWatermark(WindowKey windowKey) throws Throwable {
        List<Pair<byte[], byte[]>> windowStateBytes = this.stateStore.searchStateLessThanWatermark(windowKey.getOperatorName(), windowKey.getWindowEnd(), this::byte2WindowKey);

        return deserializerState(windowStateBytes);
    }

    public List<Pair<WindowKey, WindowState<K, V>>> searchMatchKeyPrefix(WindowKey windowKey) throws Throwable {
        List<Pair<byte[], byte[]>> pairs = this.stateStore.searchStateLessThanWatermark(windowKey.getOperatorName(), Long.MAX_VALUE, this::byte2WindowKey);

        return deserializerState(pairs);
    }

    public void deleteByKey(WindowKey windowKey) throws Throwable {
        if (windowKey == null) {
            return;
        }
        byte[] keyBytes = this.key2Byte(windowKey);
        this.stateStore.delete(keyBytes);
    }

    private WindowKey byte2WindowKey(byte[] source) {
        String str = new String(source, StandardCharsets.UTF_8);
        String[] split = Utils.split(str);
        return new WindowKey(split[0], split[1],  Long.parseLong(split[2]), Long.parseLong(split[3]));
    }

    private byte[] key2Byte(WindowKey windowKey) {
        if (windowKey == null) {
            return new byte[0];
        }

        return windowKey.toString().getBytes(StandardCharsets.UTF_8);
    }

    private List<Pair<WindowKey, WindowState<K, V>>> deserializerState(List<Pair<byte[], byte[]>> windowStateBytes)  throws Throwable{
        List<Pair<WindowKey, WindowState<K, V>>> result = new ArrayList<>();
        if (windowStateBytes == null || windowStateBytes.size() ==0) {
            return result;
        }


        for (Pair<byte[], byte[]> pair : windowStateBytes) {
            byte[] keyBytes = pair.getKey();
            WindowKey key = this.byte2WindowKey(keyBytes);
            WindowState<K, V> state = this.deserializerState(pair.getValue());

            Pair<WindowKey, WindowState<K, V>> temp = new Pair<>(key, state);
            result.add(temp);
        }
        return result;
    }

    private WindowState<K, V> deserializerState(byte[] source) throws Throwable {
        if (source == null) {
            return null;
        }

        return this.bytes2State.convert(source);
    }

}
