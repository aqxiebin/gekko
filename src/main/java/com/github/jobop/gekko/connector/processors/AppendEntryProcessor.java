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
 *
 * Created by CuttleFish on 2020/7/2.
 */
package com.github.jobop.gekko.connector.processors;

import com.alipay.remoting.AsyncContext;
import com.alipay.remoting.BizContext;
import com.github.jobop.gekko.core.election.GekkoLeaderElector;
import com.github.jobop.gekko.enums.ResultEnums;
import com.github.jobop.gekko.protocols.GekkoInboundProtocol;
import com.github.jobop.gekko.protocols.message.api.AppendEntryReq;
import com.github.jobop.gekko.protocols.message.api.AppendEntryResp;

/**
 * process the push req from leader
 */
public class AppendEntryProcessor extends DefaultProcessor<AppendEntryReq> {
    GekkoLeaderElector elector;

    public AppendEntryProcessor(GekkoInboundProtocol helper, GekkoLeaderElector elector) {
        super(helper);
        this.elector = elector;
    }

    /**
     * @param bizCtx
     * @param asyncCtx
     * @param request
     */
    public void handleRequest(BizContext bizCtx, AsyncContext asyncCtx, AppendEntryReq request) {
        helper.handleAppendEntry(request, entry -> {
            if (entry.getPos() != -1) {
                asyncCtx.sendResponse(AppendEntryResp.builder().index(entry.getEntryIndex()).resultCode(ResultEnums.SUCCESS).build());
            }
        });
    }

    public String interest() {
        return AppendEntryReq.class.getName();
    }
}
