package com.tsurugidb.tools.common.connection;

import java.nio.ByteBuffer;

import com.tsurugidb.tsubakuro.channel.common.connection.wire.Response;
import com.tsurugidb.tsubakuro.channel.common.connection.wire.Wire;
import com.tsurugidb.tsubakuro.exception.CoreServiceCode;
import com.tsurugidb.tsubakuro.exception.CoreServiceException;
import com.tsurugidb.tsubakuro.util.FutureResponse;

class MockWire implements Wire {

    @Override
    public FutureResponse<? extends Response> send(int serviceId, ByteBuffer payload) {
        return FutureResponse.raises(new CoreServiceException(CoreServiceCode.UNSUPPORTED_OPERATION));
    }

    @Override
    public void close() {
        return;
    }
}
