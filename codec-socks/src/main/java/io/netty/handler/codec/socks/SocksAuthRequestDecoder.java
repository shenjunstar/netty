/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.handler.codec.socks;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.CharsetUtil;

import java.util.List;

/**
 * Decodes {@link ByteBuf}s into {@link SocksAuthRequest}.
 * Before returning SocksRequest decoder removes itself from pipeline.
 */
public class SocksAuthRequestDecoder extends ReplayingDecoder<SocksAuthRequestDecoder.State> {
    private static final String name = "SOCKS_AUTH_REQUEST_DECODER";

    /**
     * @deprecated Will be removed at the next minor version bump.
     */
    @Deprecated
    public static String getName() {
        return name;
    }

    private SocksSubnegotiationVersion version;
    private int fieldLength;
    private String username;
    private String password;
    private SocksRequest msg = SocksCommonUtils.UNKNOWN_SOCKS_REQUEST;

    public SocksAuthRequestDecoder() {
        super(State.CHECK_PROTOCOL_VERSION);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) throws Exception {
        switch (state()) {
            case CHECK_PROTOCOL_VERSION: {
                version = SocksSubnegotiationVersion.fromByte(byteBuf.readByte());
                if (version != SocksSubnegotiationVersion.AUTH_PASSWORD) {
                    break;
                }
                checkpoint(State.READ_USERNAME);
            }
            case READ_USERNAME: {
                fieldLength = byteBuf.readByte();
                username = byteBuf.readBytes(fieldLength).toString(CharsetUtil.US_ASCII);
                checkpoint(State.READ_PASSWORD);
            }
            case READ_PASSWORD: {
                fieldLength = byteBuf.readByte();
                password = byteBuf.readBytes(fieldLength).toString(CharsetUtil.US_ASCII);
                msg = new SocksAuthRequest(username, password);
            }
        }
        ctx.pipeline().remove(this);
        out.add(msg);
    }

    enum State {
        CHECK_PROTOCOL_VERSION,
        READ_USERNAME,
        READ_PASSWORD
    }
}
