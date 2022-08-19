package edu.school21.passbot.prototype.gateway.bot;

import edu.school21.passbot.prototype.admin.CallbackHandler;
import edu.school21.passbot.prototype.admin.ListRequestsAdminCommand;
import edu.school21.passbot.prototype.gateway.commandsfactory.Command;
import edu.school21.passbot.prototype.gateway.commandsfactory.CommandsFactory;
import edu.school21.passbot.prototype.gateway.config.BotConfig;
import lombok.SneakyThrows;
import org.apache.shiro.session.InvalidSessionException;
import org.apache.shiro.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.session.TelegramLongPollingSessionBot;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class PassBot extends TelegramLongPollingSessionBot {
    private static final Logger logger = LoggerFactory.getLogger(PassBot.class);
    private final BotConfig config;
    private final CommandsFactory commandsFactory;
    private final CallbackHandler callbackHandler;
    public PassBot(BotConfig config, CommandsFactory commandsFactory, CallbackHandler callbackHandler) {
        this.config = config;
        this.commandsFactory = commandsFactory;
        this.callbackHandler = callbackHandler;
        callbackHandler.setPassBot(this);
    }

    @Override
    public void onUpdateReceived(Update update, Optional<Session> optional) {
        if (!optional.isPresent())
            throw new InvalidSessionException("Session not found");

        Session session = optional.get();
        SendMessage response = null;
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            response = manageMessage(message, session);
        }
        else if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            response = manageCallback(callbackQuery);
        }
        if (response != null) {
            sendMessage(response);
        }
    }

    private SendMessage manageCallback(CallbackQuery callbackQuery) {
        return callbackHandler.handle(callbackQuery);
    }

//    @SneakyThrows
    private SendMessage manageMessage(Message message, Session session) {
        Command command = (Command) session.getAttribute("command");
        SendMessage response;

        if (command == null) {
            command = commandsFactory.getCommandByName(message.getChatId(), message.getText());
            if (command.getClass().equals(ListRequestsAdminCommand.class)) {
                ((ListRequestsAdminCommand) command).setPassBot(this);
            }
            command.init();
            if (command.isError()) {
                response = new SendMessage();
                response.setChatId(command.getChatId());
                response.setText(command.getResponseText());
                return response;
            }
            session.setAttribute("command", command);
        }
        else {
            command.addArgument(message.getText());
            if (command.isError()) {
                response = new SendMessage();
                response.setChatId(command.getChatId());
                response.setText(command.getResponseText());
                session.setAttribute("command", null);
                return response;
            }
        }
        if (command.isReady()) {
            response = command.execute();
            session.setAttribute("command", null);
        } else
            response = command.getNextPrompt();
        return response;
    }
    public void sendMessage(SendMessage message) {
        try {
            execute(message);
            logger.info("Message sent \"{}\" ", message.toString());
        } catch (TelegramApiException e) {
            logger.error("Failed to send message \"{}\"", message.toString());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @SneakyThrows
    private void setCommands() {
        List<BotCommand> commandsList = new ArrayList<>();
        commandsList.add(
                new BotCommand("start", "запустить бота и зарегистрироваться"));
        commandsList.add(
                new BotCommand("new", "создать новую заявку на посещение гостя"));
        commandsList.add(
                new BotCommand("list", "показать все активные заявки"));
        commandsList.add(
                new BotCommand("help", "показать все доступные команды"));
        this.execute(new SetMyCommands(
                commandsList, new BotCommandScopeDefault(), null));
    }
}

