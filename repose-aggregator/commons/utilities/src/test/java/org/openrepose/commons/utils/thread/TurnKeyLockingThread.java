/*
 * #%L
 * Repose
 * %%
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.openrepose.commons.utils.thread;

/**
 *
 * 
 */
public class TurnKeyLockingThread extends Thread implements KeyedStackLockTestThread {

    protected final KeyedStackLock lockReference;
    protected final Object key;
    /// TODO: Review.  Should any of these be marked as volatile?  Or should their updates and reads be synchronized?
    protected boolean finished, passed, run, shouldStop, lock;

    public TurnKeyLockingThread(KeyedStackLock lockReference, Object key) {
        this.lockReference = lockReference;
        this.key = key;

        finished = false;
        passed = false;
        run = false;
        shouldStop = false;
        lock = true;

        super.start();
    }

    protected void toggleLockState() {
        if (lock) {
            passed = lockReference.tryLock(key);
        } else {
            lockReference.unlock(key);
        }

        lock = !lock;
    }

    @Override
    public synchronized void exec() {
        run = true;
        notify();
    }

    @Override
    public void kill() {
        shouldStop = true;
    }

    @Override
    public void run() {
        while (!shouldStop) {
            synchronized (this) {
                if (!run) {
                    try {
                        wait();
                    } catch (InterruptedException ie) {
                        return;
                    }
                } else {
                    finished = false;
                    toggleLockState();
                    finished = true;

                    run = false;
                }
            }
        }
    }

    @Override
    public boolean started() {
        return run;
    }

    @Override
    public boolean finished() {
        return finished;
    }

    @Override
    public boolean passed() {
        return passed;
    }
}
