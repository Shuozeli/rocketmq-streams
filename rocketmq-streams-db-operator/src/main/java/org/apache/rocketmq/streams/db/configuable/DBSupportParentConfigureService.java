///*
// * Licensed to the Apache Software Foundation (ASF) under one or more
// * contributor license agreements.  See the NOTICE file distributed with
// * this work for additional information regarding copyright ownership.
// * The ASF licenses this file to You under the Apache License, Version 2.0
// * (the "License"); you may not use this file except in compliance with
// * the License.  You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package org.apache.rocketmq.streams.db.configuable;
//
//import com.google.auto.service.AutoService;
//import java.util.Properties;
//import org.apache.rocketmq.streams.common.configurable.IConfigurableService;
//import org.apache.rocketmq.streams.common.model.ServiceName;
//import org.apache.rocketmq.streams.configurable.service.AbstractSupportParentConfigureService;
//import org.apache.rocketmq.streams.configurable.service.ConfigurableServcieType;
//
//@AutoService(IConfigurableService.class)
//@ServiceName(ConfigurableServcieType.DEFAULT_SERVICE_NAME)
//public class DBSupportParentConfigureService extends AbstractSupportParentConfigureService {
//
//    @Override
//    protected void initBeforeInitConfigurable(Properties property) {
//        this.parentConfigureService = new DBConfigureService(property);
//        this.configureService = new DBConfigureService(property);
//
//    }
//}
