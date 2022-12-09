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
package org.apache.rocketmq.streams.core.util;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.protocol.ResponseCode;
import org.apache.rocketmq.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.srvutil.ServerUtil;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.apache.rocketmq.tools.command.topic.UpdateStaticTopicSubCommand;
import org.apache.rocketmq.tools.command.topic.UpdateTopicSubCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RocketMQUtil {
    private static final Logger logger = LoggerFactory.getLogger(RocketMQUtil.class.getName());

    private static final List<String> existStateCompactTopic = new ArrayList<>();

    public static void createStaticCompactTopic(DefaultMQAdminExt mqAdmin, String topicName, int queueNum, Set<String> clusters) throws Exception {
        if (check(mqAdmin, topicName)) {
            logger.info("topic[{}] already exist.", topicName);
            return;
        }

        if (clusters == null || clusters.size() == 0) {
            clusters = getCluster(mqAdmin);
        }


        for (String cluster : clusters) {
            createStaticTopicWithCommand(topicName, queueNum, new HashSet<>(), cluster, mqAdmin.getNamesrvAddr());
            logger.info("【step 1】create static topic:[{}] in cluster:[{}] success, logic queue num:[{}].", topicName, cluster, queueNum);

            update2CompactTopicWithCommand(topicName, cluster, mqAdmin.getNamesrvAddr());
            logger.info("【step 2】update static topic to compact topic success. topic:[{}], cluster:[{}]", topicName, cluster);
        }

        existStateCompactTopic.add(topicName);
        logger.info("create static-compact topic [{}] success, queue num [{}]", topicName, queueNum);
    }

    public static void createStaticTopic(DefaultMQAdminExt mqAdmin, String topicName, int queueNum) throws Exception {
        if (check(mqAdmin, topicName)) {
            logger.info("topic[{}] already exist.", topicName);
            return;
        }

        Set<String> clusters = getCluster(mqAdmin);
        for (String cluster : clusters) {
            createStaticTopicWithCommand(topicName, queueNum, new HashSet<>(), cluster, mqAdmin.getNamesrvAddr());
            logger.info("create static topic:[{}] in cluster:[{}] success, logic queue num:[{}].", topicName, cluster, queueNum);
        }

        existStateCompactTopic.add(topicName);
    }

    private static void createStaticTopicWithCommand(String topic, int queueNum, Set<String> brokers, String cluster, String nameservers) throws Exception {
        UpdateStaticTopicSubCommand cmd = new UpdateStaticTopicSubCommand();
        Options options = ServerUtil.buildCommandlineOptions(new Options());
        String[] args;
        if (cluster != null) {
            args = new String[]{
                    "-c", cluster,
                    "-t", topic,
                    "-qn", String.valueOf(queueNum),
                    "-n", nameservers
            };
        } else {
            String brokerStr = String.join(",", brokers);
            args = new String[]{
                    "-b", brokerStr,
                    "-t", topic,
                    "-qn", String.valueOf(queueNum),
                    "-n", nameservers
            };
        }

        final CommandLine commandLine = ServerUtil.parseCmdLine("mqadmin " + cmd.commandName(), args, cmd.buildCommandlineOptions(options), new PosixParser());

        String namesrvAddr = commandLine.getOptionValue('n');
        System.setProperty(MixAll.NAMESRV_ADDR_PROPERTY, namesrvAddr);

        cmd.execute(commandLine, options, null);
    }

    private static void update2CompactTopicWithCommand(String topic, String cluster, String nameservers) throws Exception {
        UpdateTopicSubCommand command = new UpdateTopicSubCommand();
        Options options = ServerUtil.buildCommandlineOptions(new Options());
        String[] args = new String[]{
                "-c", cluster,
                "-t", topic,
                "-n", nameservers
//                todo 发布版本还不支持
//                , "-a", "+delete.policy=COMPACTION"
        };

        final CommandLine commandLine = ServerUtil.parseCmdLine("mqadmin " + command.commandName(), args, command.buildCommandlineOptions(options), new PosixParser());
        String namesrvAddr = commandLine.getOptionValue('n');
        System.setProperty(MixAll.NAMESRV_ADDR_PROPERTY, namesrvAddr);

        command.execute(commandLine, options, null);
    }



    public static Set<String> getCluster(DefaultMQAdminExt mqAdmin) throws Exception {
        ClusterInfo clusterInfo = mqAdmin.examineBrokerClusterInfo();
        return clusterInfo.getClusterAddrTable().keySet();
    }

    private static boolean check(DefaultMQAdminExt mqAdmin, String topicName) {
        if (existStateCompactTopic.contains(topicName)) {
            return true;
        }

        try {
            mqAdmin.examineTopicRouteInfo(topicName);
            existStateCompactTopic.add(topicName);
            return true;
        } catch (RemotingException | InterruptedException e) {
            logger.error("examine topic route info error.", e);
            throw new RuntimeException("examine topic route info error.", e);
        } catch (MQClientException exception) {
            if (exception.getResponseCode() == ResponseCode.TOPIC_NOT_EXIST) {
                logger.info("topic[{}] does not exist, create it.", topicName);
            } else {
                throw new RuntimeException(exception);
            }
        }
        return false;
    }

    public static boolean checkWhetherExist(String topic) {
        return existStateCompactTopic.contains(topic);
    }
}
