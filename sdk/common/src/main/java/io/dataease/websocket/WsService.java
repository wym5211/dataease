package io.dataease.websocket;



public interface WsService {

    <T> void releaseMessage(WsMessage<T> wsMessage);

}
