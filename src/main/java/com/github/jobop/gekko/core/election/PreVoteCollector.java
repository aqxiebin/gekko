
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
 * Created by CuttleFish on 2020/7/17.
 */

package com.github.jobop.gekko.core.election;

import com.alipay.remoting.InvokeCallback;
import com.github.jobop.gekko.core.metadata.NodeState;
import com.github.jobop.gekko.enums.RoleEnum;
import com.github.jobop.gekko.enums.VoteResultEnums;
import com.github.jobop.gekko.protocols.message.node.VoteResp;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Data
public class PreVoteCollector implements InvokeCallback {
    static Executor executor = Executors.newCachedThreadPool();
    NodeState nodeState;
    private long voteTerm;

    private Set<String> agreeSet = Collections.synchronizedSet(new HashSet<>());


    private volatile AtomicBoolean available = new AtomicBoolean(true);
    GekkoLeaderElector elector;

    public PreVoteCollector(NodeState nodeState, GekkoLeaderElector elector) {
        this.nodeState = nodeState;
        this.voteTerm = nodeState.getTerm();
        agreeSet.add(nodeState.getSelfId());
        this.elector = elector;
    }

    @Override
    public void onResponse(Object result) {
        VoteResp resp = (VoteResp) result;
        if (resp.getTerm() != this.voteTerm) {
            log.warn("this vote term has expired! term=" + this.getVoteTerm());
            return;
        }
        if (this.voteTerm < nodeState.getTerm() + 1) {
            log.warn("this pre vote term has expired! term=" + this.getVoteTerm());
            return;
        }

        if (available.get() == true) {
            if (resp.getResult() == VoteResultEnums.AGREE) {
                agreeSet.add(resp.getVoteMemberId());
                //become a leader
                if (agreeSet.size() > (nodeState.getPeersMap().size() / 2)) {
                    //upgrade to leader and disable this collector
                    if (available.compareAndSet(true, false)) {
//                        this.nodeState.getTermAtomic().compareAndSet(this.voteTerm, this.voteTerm + 1);
                        reqToRealVote();
                    }
                }
            }
        }


    }

    private void reqToRealVote() {
        nodeState.setRole(RoleEnum.CANDIDATE);
        nodeState.getTermAtomic().incrementAndGet();
        VoteCollector voteCollector = new VoteCollector(nodeState, elector);
        elector.getVoteCollectors().add(new WeakReference<>(voteCollector));
        elector.getClient().reqVote(voteCollector);
        //when no outer trigger the reset,it will reset by itself
        elector.resetElectionTimeout();
    }

    @Override
    public void onException(Throwable e) {
        log.error("", e);
    }

    @Override
    public Executor getExecutor() {
        return executor;
    }

    public void disAble() {
        if (this.available.compareAndSet(true, false)) {
            agreeSet.clear();
            agreeSet = null;
        }
    }
}
