/*
 * Copyright 2022 ICON Foundation
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

package foundation.icon.btp.xcall;

import org.junit.jupiter.api.Test;

public class CSUnitTest {

    @Test
    void ordered() throws Exception {
        /** sendCallMessageFromDAppProxy */
        //sendCallMessage via DAppProxySample
        //BMC.Message(_to.net, sn, CSMessage(CSRequest))
        //TODO CallService.CallMessageSent(caller, _to, sn, _data)

        /** handleBTPMessageShouldEmitCallMessage */
        //handleBTPMessage via MockForBSH
        //CallService.CallMessage()

        /** executeCallWithoutSuccessResponse */
        //executeCall
        //DAppProxySample.MessageReceived

        /** sendCallMessageWithRollback */
        //sendCallMessage via DAppProxySample
        //BMC.Message(_to.net, sn, CSMessage(CSRequest))
        //TODO CallService.CallMessageSent(caller, _to, sn, _data)

        /** executeCallWithFailureResponse */
        //handleBTPMessage(CSMessage(last CSRequest)) via MockForBSH
        //BMC.Message(_to.net, sn, CSMessage(CSResponse(FAILURE))
        //TODO CallService.CallMessage
        //executeCall => it should be rollback
        //TODO assertEquals(Message._sn)

        /** executeRollbackWithFailureResponse */
        //executeRollback
        //DAppProxySample.MessageReceived
        //CallService.CallRequestCleared

        /** handleBTPMessageWithSuccessResponse */
        //sendCallMessageWithRollback
        //handleBTPMessage(CSMessage(CSResponse(SUCCESS))) via MockForBSH
        //CallService.CallRequestCleared
        //ensure notExists CallService.RollbackMessage

        /** verifyAccruedFees */
        //accruedFees

        /** handleBTPErrorTest */
        //sendCallMessageWithRollback
        //handleBTPError via MockForBSH
        //CallService.RollbackMessage
        //ensure notExists CallService.CallRequestCleared

        /** executeRollbackWithBTPError */
        //executeRollback
        //DAppProxySample.MessageReceived
        //CallService.CallRequestCleared
    }

    @Test
    void isolated() throws Exception {
        /** handleBTPMessageWithSuccessResponseButNoRequestEntry */
        //handleBTPMessage via MockForBSH
        //ensure notExists CallService.CallRequestCleared

        /** handleBTPMessageWithFailureResponseButNoRequestEntry */
        //handleBTPMessage via MockForBSH
        //ensure notExists CallService.RollbackMessage

        /** maxPayloadsTest */
        //sendCallMessage via DAppProxySample
        //ensure revert

        /** fixedFeesTest */
        //setFixedFees
        //CallService.FixedFeesUpdated

        /** executeRollbackEarlyCallShouldFail */
        //executeRollback
        //ensure revert

        /** handleBTPMessageWithFailureResponse */
        //handleBTPMessage(CSMessage(CSResponse(FAILURE))) via MockForBSH
        //CallService.RollbackMessage
        //ensure notExists CallService.CallRequestCleared

    }

    @Test
    void sendCallMessageShouldSuccess() throws Exception {
        //BMC.Message(_to.net, sn, CSMessage(CSRequest))
        //CallService.CallMessageSent(caller, _to, sn, _data)

        //if _rollback.length > 0 then requests.set(sn, CallRequest)
    }

    @Test
    void sendCallMessageShouldRevert() throws Exception {
        //IfCallerIsEOA
        //_data.length > MAX_DATA_SIZE => maxPayloadsTest
        //_rollback.length > MAX_ROLLBACK_SIZE => maxPayloadsTest
        //payValue < fee.TotalFee
    }


}
