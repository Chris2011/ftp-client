/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.github.chris2011.netbeans.plugins.ftp.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.openide.awt.StatusDisplayer;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;

class FtpFileChildren extends ChildFactory<FtpFile> {

    private static final Comparator<FtpFile> FILE_COMPARATOR = Comparator
        .comparing(FtpFile::isDirectory).reversed()
        .thenComparing(FtpFile::getName, String.CASE_INSENSITIVE_ORDER);

    private final FtpExplorerTopComponent explorerComponent;
    private final FtpFile parentFile;

    FtpFileChildren(FtpFile parentFile, FtpExplorerTopComponent explorerComponent) {
        this.parentFile = parentFile;
        this.explorerComponent = explorerComponent;
    }

    @Override
    protected boolean createKeys(List<FtpFile> toPopulate) {
        if (!explorerComponent.isConnected()) {
            return true;
        }

        try {
            List<FtpFile> files = new ArrayList<>(explorerComponent.listFiles(parentFile.getPath()));
            Collections.sort(files, FILE_COMPARATOR);
            toPopulate.addAll(files);
        } catch (IOException ex) {
            StatusDisplayer.getDefault().setStatusText("Failed to list " + parentFile.getPath() + ": " + ex.getMessage());
        }
        return true;
    }

    @Override
    protected Node createNodeForKey(FtpFile file) {
        return new FtpFileNode(file, explorerComponent);
    }
}
