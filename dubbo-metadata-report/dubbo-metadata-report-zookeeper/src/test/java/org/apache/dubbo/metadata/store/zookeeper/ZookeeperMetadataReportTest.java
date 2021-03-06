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
package org.apache.dubbo.metadata.store.zookeeper;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.metadata.definition.ServiceDefinitionBuilder;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.identifier.MetadataIdentifier;
import org.apache.dubbo.remoting.zookeeper.curator.CuratorZookeeperTransporter;

import com.google.gson.Gson;
import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * 2018/10/9
 */
public class ZookeeperMetadataReportTest {
    private TestingServer zkServer;
    private ZookeeperMetadataReport zookeeperMetadataReport;
    private URL registryUrl;
    private ZookeeperMetadataReportFactory zookeeperMetadataReportFactory;

    @Before
    public void setUp() throws Exception {
        int zkServerPort = NetUtils.getAvailablePort();
        this.zkServer = new TestingServer(zkServerPort, true);
        this.registryUrl = URL.valueOf("zookeeper://localhost:" + zkServerPort);

        zookeeperMetadataReportFactory = new ZookeeperMetadataReportFactory();
        zookeeperMetadataReportFactory.setZookeeperTransporter(new CuratorZookeeperTransporter());
        this.zookeeperMetadataReport = (ZookeeperMetadataReport) zookeeperMetadataReportFactory.createMetadataReport(registryUrl);
    }

    @After
    public void tearDown() throws Exception {
        zkServer.stop();
    }

    private void deletePath(MetadataIdentifier metadataIdentifier, ZookeeperMetadataReport zookeeperMetadataReport) {
        String category = zookeeperMetadataReport.toRootDir() + metadataIdentifier.getUniqueKey(MetadataIdentifier.KeyTypeEnum.PATH);
        zookeeperMetadataReport.deletePath(category);
    }

    @Test
    public void testStoreProvider() throws ClassNotFoundException, InterruptedException {
        String interfaceName = "org.apache.dubbo.metadata.store.zookeeper.ZookeeperMetadataReport4TstService";
        String version = "1.0.0.zk.md";
        String group = null;
        String application = "vic.zk.md";
        MetadataIdentifier providerMetadataIdentifier = storePrivider(zookeeperMetadataReport, interfaceName, version, group, application);

        String fileContent = zookeeperMetadataReport.zkClient.getContent(zookeeperMetadataReport.getNodePath(providerMetadataIdentifier));
        fileContent = waitSeconds(fileContent, 3500, zookeeperMetadataReport.getNodePath(providerMetadataIdentifier));
        Assert.assertNotNull(fileContent);

        deletePath(providerMetadataIdentifier, zookeeperMetadataReport);
        fileContent = zookeeperMetadataReport.zkClient.getContent(zookeeperMetadataReport.getNodePath(providerMetadataIdentifier));
        fileContent = waitSeconds(fileContent, 1000, zookeeperMetadataReport.getNodePath(providerMetadataIdentifier));
        Assert.assertNull(fileContent);


        providerMetadataIdentifier = storePrivider(zookeeperMetadataReport, interfaceName, version, group, application);
        fileContent = zookeeperMetadataReport.zkClient.getContent(zookeeperMetadataReport.getNodePath(providerMetadataIdentifier));
        fileContent = waitSeconds(fileContent, 3500, zookeeperMetadataReport.getNodePath(providerMetadataIdentifier));
        Assert.assertNotNull(fileContent);

        Gson gson = new Gson();
        FullServiceDefinition fullServiceDefinition = gson.fromJson(fileContent, FullServiceDefinition.class);
        Assert.assertEquals(fullServiceDefinition.getParameters().get("paramTest"), "zkTest");
    }


    @Test
    public void testConsumer() throws ClassNotFoundException, InterruptedException {
        String interfaceName = "org.apache.dubbo.metadata.store.zookeeper.ZookeeperMetadataReport4TstService";
        String version = "1.0.0.zk.md";
        String group = null;
        String application = "vic.zk.md";
        MetadataIdentifier consumerMetadataIdentifier = storeConsumer(zookeeperMetadataReport, interfaceName, version, group, application);

        String fileContent = zookeeperMetadataReport.zkClient.getContent(zookeeperMetadataReport.getNodePath(consumerMetadataIdentifier));
        fileContent = waitSeconds(fileContent, 3500, zookeeperMetadataReport.getNodePath(consumerMetadataIdentifier));
        Assert.assertNotNull(fileContent);

        deletePath(consumerMetadataIdentifier, zookeeperMetadataReport);
        fileContent = zookeeperMetadataReport.zkClient.getContent(zookeeperMetadataReport.getNodePath(consumerMetadataIdentifier));
        fileContent = waitSeconds(fileContent, 1000, zookeeperMetadataReport.getNodePath(consumerMetadataIdentifier));
        Assert.assertNull(fileContent);

        consumerMetadataIdentifier = storeConsumer(zookeeperMetadataReport, interfaceName, version, group, application);
        fileContent = zookeeperMetadataReport.zkClient.getContent(zookeeperMetadataReport.getNodePath(consumerMetadataIdentifier));
        fileContent = waitSeconds(fileContent, 3000, zookeeperMetadataReport.getNodePath(consumerMetadataIdentifier));
        Assert.assertNotNull(fileContent);
        Assert.assertEquals(fileContent, "{\"paramConsumerTest\":\"zkCm\"}");
    }


    private MetadataIdentifier storePrivider(ZookeeperMetadataReport zookeeperMetadataReport, String interfaceName, String version, String group, String application) throws ClassNotFoundException, InterruptedException {
        URL url = URL.valueOf("xxx://" + NetUtils.getLocalAddress().getHostName() + ":4444/" + interfaceName + "?paramTest=zkTest&version=" + version + "&application="
                + application + (group == null ? "" : "&group=" + group));

        MetadataIdentifier providerMetadataIdentifier = new MetadataIdentifier(interfaceName, version, group, Constants.PROVIDER_SIDE, application);
        Class interfaceClass = Class.forName(interfaceName);
        FullServiceDefinition fullServiceDefinition = ServiceDefinitionBuilder.buildFullDefinition(interfaceClass, url.getParameters());

        zookeeperMetadataReport.storeProviderMetadata(providerMetadataIdentifier, fullServiceDefinition);
        Thread.sleep(2000);
        return providerMetadataIdentifier;
    }

    private MetadataIdentifier storeConsumer(ZookeeperMetadataReport zookeeperMetadataReport, String interfaceName, String version, String group, String application) throws ClassNotFoundException, InterruptedException {
        URL url = URL.valueOf("xxx://" + NetUtils.getLocalAddress().getHostName() + ":4444/" + interfaceName + "?version=" + version + "&application="
                + application + (group == null ? "" : "&group=" + group));

        MetadataIdentifier consumerMetadataIdentifier = new MetadataIdentifier(interfaceName, version, group, Constants.CONSUMER_SIDE, application);
        Class interfaceClass = Class.forName(interfaceName);

        Map<String, String> tmp = new HashMap<>();
        tmp.put("paramConsumerTest", "zkCm");
        zookeeperMetadataReport.storeConsumerMetadata(consumerMetadataIdentifier, tmp);
        Thread.sleep(2000);

        return consumerMetadataIdentifier;
    }

    private String waitSeconds(String value, long moreTime, String path) throws InterruptedException {
        if (value == null) {
            Thread.sleep(moreTime);
            return zookeeperMetadataReport.zkClient.getContent(path);
        }
        return value;
    }
}
