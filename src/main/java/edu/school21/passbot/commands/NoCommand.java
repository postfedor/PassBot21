package edu.school21.passbot.commands;

import edu.school21.passbot.commandsfactory.Command;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Component
@Scope("prototype")
public class NoCommand implements Command {
    @Setter
    @Getter
    Long chatId;
    public NoCommand() {
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getName2() {
        return null;
    }
    @Override
    public SendMessage execute() {
        SendMessage response = new SendMessage();
        response.setChatId(chatId);
        response.setText("Командна не найдена.\nВведите /help чтобы посмотреть доступные команды");
        return response;
    }
}