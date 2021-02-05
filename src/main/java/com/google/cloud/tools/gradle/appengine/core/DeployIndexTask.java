/*
 * Copyright 2017 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.google.cloud.tools.gradle.appengine.core;

import com.google.cloud.tools.appengine.AppEngineException;
import com.google.cloud.tools.appengine.operations.Gcloud;
import org.gradle.api.tasks.TaskAction;

/** Task to deploy datastore index configuration. */
public class DeployIndexTask extends GcloudTask {

  private DeployExtension deployExtension;
  private Gcloud gcloud;

  public void setDeployExtension(DeployExtension deployExtension) {
    this.deployExtension = deployExtension;
  }

  @Override
  public void setGcloud(Gcloud gcloud) {
    this.gcloud = gcloud;
  }

  /** Task entrypoint : deploy index.yaml. */
  @TaskAction
  public void deployAction() throws AppEngineException {
    gcloud
        .newDeployment(CloudSdkOperations.getDefaultHandler(getLogger()))
        .deployIndex(deployExtension.toDeployProjectConfigurationConfiguration());
  }
}
