package com.picture.backend.manager.websocket.disruptor;

import cn.hutool.json.JSONUtil;
import com.lmax.disruptor.WorkHandler;
import com.picture.backend.manager.websocket.PictureEditHandler;
import com.picture.backend.manager.websocket.model.PictureEditRequestMessage;
import com.picture.backend.manager.websocket.model.PictureEditResponseMessage;
import com.picture.backend.manager.websocket.model.enums.PictureEditMessageTypeEnum;
import com.picture.backend.model.entity.User;
import com.picture.backend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;

@Slf4j
@Component
public class PictureEditEventWorkHandler implements WorkHandler<PictureEditEvent> {

    @Resource
    @Lazy
    private PictureEditHandler pictureEditHandler;

    @Resource
    private UserService userService;

    @Override
    public void onEvent(PictureEditEvent event) throws Exception {
        PictureEditRequestMessage req = event.getPictureEditRequestMessage();
        WebSocketSession session = event.getSession();
        User user = event.getUser();
        Long pictureId = event.getPictureId();

        String type = req.getType();
        PictureEditMessageTypeEnum enumType = PictureEditMessageTypeEnum.getEnumByValue(type);
        if (enumType == PictureEditMessageTypeEnum.EDIT_ACTION) {
            pictureEditHandler.handleEditActionMessage(req, session, user, pictureId);
        } else {
            PictureEditResponseMessage err = new PictureEditResponseMessage();
            err.setType(PictureEditMessageTypeEnum.ERROR.getValue());
            err.setMessage("消息类型错误");
            err.setUser(userService.getUserVO(user));
            session.sendMessage(new TextMessage(JSONUtil.toJsonStr(err)));
        }
    }
}
