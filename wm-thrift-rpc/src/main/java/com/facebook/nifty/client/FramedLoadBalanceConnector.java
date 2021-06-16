package com.facebook.nifty.client;

import com.facebook.nifty.duplex.TDuplexProtocolFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;


public class FramedLoadBalanceConnector extends AbstractLoadBalanceConnector<FramedClientChannel> {
    // TFramedTransport framing appears at the front of the message
    private static final int LENGTH_FIELD_OFFSET = 0;

    // TFramedTransport framing is four bytes long
    private static final int LENGTH_FIELD_LENGTH = 4;

    // TFramedTransport framing represents message size *not including* framing so no adjustment
    // is necessary
    private static final int LENGTH_ADJUSTMENT = 0;

    // The client expects to see only the message *without* any framing, this strips it off
    private static final int INITIAL_BYTES_TO_STRIP = LENGTH_FIELD_LENGTH;

    /**
     * IGEN_MAX_FRAME_SIZE default 50m
     */
    private static final int IGEN_MAX_FRAME_SIZE = 52428800;

    public FramedLoadBalanceConnector(TDuplexProtocolFactory protocolFactory, LoadBalancerClient loadBalancer, String discoveryName) {
        super(protocolFactory, loadBalancer, discoveryName);
    }


    @Override
    public FramedClientChannel newThriftClientChannel(Channel nettyChannel, NettyClientConfig clientConfig) {
        FramedClientChannel channel = new FramedClientChannel(nettyChannel, clientConfig.getTimer(), getProtocolFactory());
        ChannelPipeline cp = nettyChannel.getPipeline();
        TimeoutHandler.addToPipeline(cp);
        cp.addLast("thriftHandler", channel);
        return channel;
    }

    @Override
    public ChannelPipelineFactory newChannelPipelineFactory(final int maxFrameSize, NettyClientConfig clientConfig) {
        return new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline()
                    throws Exception {
                ChannelPipeline cp = Channels.pipeline();
                TimeoutHandler.addToPipeline(cp);

                cp.addLast("frameEncoder", new LengthFieldPrepender(LENGTH_FIELD_LENGTH));
                cp.addLast(
                        "frameDecoder",
                        new LengthFieldBasedFrameDecoder(
                                //netty maxFrameSize 默认为16M，大于16M报错，调高为50M
                                //maxFrameSize,
                                IGEN_MAX_FRAME_SIZE,
                                LENGTH_FIELD_OFFSET,
                                LENGTH_FIELD_LENGTH,
                                LENGTH_ADJUSTMENT,
                                INITIAL_BYTES_TO_STRIP));
                if (clientConfig.sslClientConfiguration() != null) {
                    cp.addFirst("ssl", clientConfig.sslClientConfiguration().createHandler(getAddress()));
                }
                return cp;
            }
        };
    }
}

