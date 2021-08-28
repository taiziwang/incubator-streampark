/*
 * Copyright (c) 2021 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.streamx.flink.k8s

import com.streamxhub.streamx.flink.k8s.enums.FlinkK8sExecuteMode
import io.fabric8.kubernetes.client.{DefaultKubernetesClient, KubernetesClient}
import org.apache.flink.client.deployment.{ClusterClientFactory, DefaultClusterClientServiceLoader}
import org.apache.flink.client.program.ClusterClient
import org.apache.flink.configuration.{Configuration, DeploymentOptions}
import org.apache.flink.kubernetes.KubernetesClusterDescriptor
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions

import javax.annotation.Nullable
import scala.util.Try

/**
 * author:Al-assad
 */
object KubernetesRetriever {

  /**
   * get new KubernetesClient
   */
  def newK8sClient(): KubernetesClient = {
    new DefaultKubernetesClient()
  }

  /**
   * check connection of kubernetes clutser
   */
  def checkK8sConnection(): Boolean = {
    Try(newK8sClient().getVersion != null).getOrElse(false)
  }


  private val clusterClientServiceLoader = new DefaultClusterClientServiceLoader()

  /**
   * get new flink cluster client of kubernetes mode
   */
  def newFinkClusterClient(clusterId: String,
                           @Nullable namespace: String,
                           executeMode: FlinkK8sExecuteMode.Value): ClusterClient[String] = {
    // build flink config
    val flinkConfig = new Configuration()
    flinkConfig.setString(DeploymentOptions.TARGET, executeMode.toString)
    flinkConfig.setString(KubernetesConfigOptions.CLUSTER_ID, clusterId)
    if (Try(namespace.isEmpty).getOrElse(true)) {
      flinkConfig.setString(KubernetesConfigOptions.NAMESPACE, KubernetesConfigOptions.NAMESPACE.defaultValue())
    } else {
      flinkConfig.setString(KubernetesConfigOptions.NAMESPACE, namespace)
    }
    // retrive flink cluster client
    val clientFactory: ClusterClientFactory[String] = clusterClientServiceLoader.getClusterClientFactory(flinkConfig)
    val clusterProvider: KubernetesClusterDescriptor = clientFactory.createClusterDescriptor(flinkConfig)
      .asInstanceOf[KubernetesClusterDescriptor]
    val flinkClient: ClusterClient[String] = clusterProvider
      .retrieve(flinkConfig.getString(KubernetesConfigOptions.CLUSTER_ID))
      .getClusterClient
    flinkClient
  }


}
