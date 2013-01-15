/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

function createGUID() {
	return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
		var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
		return v.toString(16);
	});
}

function createCMISForm(rootFolderUrl, id, cmisaction, callback) {
    var frameName = "cmis" + createGUID();
    
    var targetFrame = document.createElement("iframe");
    targetFrame.frameBorder = targetFrame.width = targetFrame.height = 0;
    targetFrame.name = frameName;
    document.body.appendChild(targetFrame);
    targetFrame.addEventListener("load", function() { callback(); document.body.removeChild(targetFrame); }, false);

    var cmisForm = targetFrame.contentDocument.createElement("form");
    cmisForm.action = rootFolderUrl + "?objectId=" + encodeURIComponent(id);
    cmisForm.target = frameName;
    cmisForm.method = "POST";
    
    targetFrame.appendChild(cmisForm);
    
    var cmisActionInput = targetFrame.contentDocument.createElement("input");
    cmisActionInput.name = "cmisaction";
    cmisActionInput.value = cmisaction;
    
    cmisForm.appendChild(cmisActionInput);

    return cmisForm;
}