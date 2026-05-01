package com.picture.backend.manager.websocket;

import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.picture.backend.manager.websocket.disruptor.PictureEditEventProducer;
import com.picture.backend.manager.websocket.model.PictureEditRequestMessage;
import com.picture.backend.manager.websocket.model.PictureEditResponseMessage;
import com.picture.backend.manager.websocket.model.enums.PictureEditActionEnum;
import com.picture.backend.manager.websocket.model.enums.PictureEditMessageTypeEnum;
import com.picture.backend.model.entity.User;
import com.picture.backend.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PictureEditHandler extends TextWebSocketHandler {
    @Resource
    private UserService userService;

    @Resource
    private PictureEditEventProducer pictureEditEventProducer;

    // 保存所有连接的会话，key: pictureId, value: 用户会话集合
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        pictureSessions.putIfAbsent(pictureId, ConcurrentHashMap.newKeySet());
        pictureSessions.get(pictureId).add(session);

        PictureEditResponseMessage msg = new PictureEditResponseMessage();
        msg.setType(PictureEditMessageTypeEnum.INFO.getValue());
        msg.setMessage(String.format("%s加入编辑", user.getUserName()));
        msg.setUser(userService.getUserVO(user));
        broadcastToPicture(pictureId, msg);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        PictureEditRequestMessage req = com.fasterxml.jackson.databind.json.JsonMapper.builder()
                .build().readValue(message.getPayload(), PictureEditRequestMessage.class);
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long pictureId = (Long) attributes.get("pictureId");
        pictureEditEventProducer.publishEvent(req, session, user, pictureId);
    }

    /**
     * CRDT 模式：接收所有编辑操作，广播给其他人（不验证编辑者）
     */
    public void handleEditActionMessage(PictureEditRequestMessage req, WebSocketSession session, User user, Long pictureId) throws Exception {
        String editAction = req.getEditAction();
        PictureEditActionEnum actionEnum = PictureEditActionEnum.getEnumByValue(editAction);
        if (actionEnum == null) return;

        PictureEditResponseMessage msg = new PictureEditResponseMessage();
        msg.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
        msg.setMessage(String.format("%s执行%s", user.getUserName(), actionEnum.getText()));
        msg.setEditAction(editAction);
        msg.setUser(userService.getUserVO(user));
        // 广播给除发送者外的所有人（发送者已本地执行）
        broadcastToPicture(pictureId, msg, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Map<String, Object> attributes = session.getAttributes();
        Long pictureId = (Long) attributes.get("pictureId");
        User user = (User) attributes.get("user");

        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (sessionSet != null) {
            sessionSet.remove(session);
            if (sessionSet.isEmpty()) {
                pictureSessions.remove(pictureId);
            }
        }

        PictureEditResponseMessage msg = new PictureEditResponseMessage();
        msg.setType(PictureEditMessageTypeEnum.INFO.getValue());
        msg.setMessage(String.format("%s离开编辑", user.getUserName()));
        msg.setUser(userService.getUserVO(user));
        broadcastToPicture(pictureId, msg);
    }

    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage msg, WebSocketSession excludeSession) throws Exception {
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (CollUtil.isNotEmpty(sessionSet)) {
            ObjectMapper mapper = new ObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);
            mapper.registerModule(module);
            String json = mapper.writeValueAsString(msg);
            TextMessage textMessage = new TextMessage(json);
            for (WebSocketSession session : sessionSet) {
                if (excludeSession != null && excludeSession.equals(session)) continue;
                if (session.isOpen()) session.sendMessage(textMessage);
            }
        }
    }

    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage msg) throws Exception {
        broadcastToPicture(pictureId, msg, null);
    }
}
