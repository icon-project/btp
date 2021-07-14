/*
 * Copyright 2021 ICON Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iconloop.btp.lib;

import score.Address;
import score.annotation.External;

public interface OwnerManager {
    /**
     * Registers the Owner.
     * Called by contract owner or registered Owner
     *
     * @param _addr Address of the Owner
     * @throws IllegalStateException if caller is not contract owner or registered Owner
     * @throws IllegalArgumentException if given address is already registered or contract owner
     */
    @External
    void addOwner(Address _addr) throws IllegalStateException, IllegalArgumentException;

    /**
     * Unregisters the Owner.
     * Called by the operator to manage the BTP network.
     *
     * @param _addr Address
     * @throws IllegalStateException if caller is not contract owner or registered Owner
     * @throws IllegalArgumentException if given address is not registered or contract owner
     */
    @External
    void removeOwner(Address _addr);

    /**
     * Get registered the Owners.
     *
     * @return A list of Owners. ( Address of Owners )
     */
    @External(readonly = true)
    Address[] getOwners();

    /**
     * Return given address is registered as owner
     *
     * @param _addr Address
     * @return boolean true if registered, otherwise false
     */
    @External(readonly = true)
    boolean isOwner(Address _addr);
}
