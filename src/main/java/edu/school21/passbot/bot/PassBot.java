package edu.school21.passbot.bot;

import edu.school21.passbot.admin.CallbackHandler;
import edu.school21.passbot.repositories.UserDataCache;
import edu.school21.passbot.commandsfactory.Command;
import edu.school21.passbot.commandsfactory.CommandsFactory;
import edu.school21.passbot.config.PassBotConfig;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Component
public class PassBot extends TelegramLongPollingBot {
    private static final Logger logger = LoggerFactory.getLogger(PassBot.class);
    private final PassBotConfig config;
    private final CommandsFactory commandsFactory;
    private final CallbackHandler callbackHandler;
    private final UserDataCache usersDataCache;
    public PassBot(PassBotConfig config, CommandsFactory commandsFactory,
                   CallbackHandler callbackHandler, UserDataCache usersDataCache) {
        this.config = config;
        this.commandsFactory = commandsFactory;
        this.callbackHandler = callbackHandler;
        this.usersDataCache = usersDataCache;
        callbackHandler.setPassBot(this);
    }

    @Override
    public void onUpdateReceived(Update update) {
        List<SendMessage> responses = null;
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            responses = manageMessage(message);
        }
        else if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            responses = manageCallback(callbackQuery);
        }
        if (responses != null && !responses.isEmpty()) {
            for (SendMessage response : responses)
                if (response != null)
                    sendMessage(response);
        }
    }

    private List<SendMessage> manageCallback(CallbackQuery callbackQuery) {
        return callbackHandler.handle(callbackQuery);
    }

    private List<SendMessage> manageMessage(Message message) {
        Long chatId = message.getChatId();
        Command command = usersDataCache.getCommand(chatId);
        List<SendMessage> responses = new LinkedList<>();

        if (command == null) {
            command = commandsFactory.getCommandByName(message.getChatId(), message.getText());
            command.onCreate();
            usersDataCache.setCommand(chatId, command);
        }
        else {
            command.addArgument(message.getText());
        }
        if (command.isReady()) {
            responses = command.execute();
            usersDataCache.clearCommand(chatId);
        } else
            responses.add(command.getNextPrompt());
        return responses;
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
                new BotCommand("start", "запустить бота и ввести свой ник в интре"));
        commandsList.add(
                new BotCommand("register", "ввести свои ФИО"));
        commandsList.add(
                new BotCommand("new", "создать новую заявку на посещение гостя"));
        commandsList.add(
                new BotCommand("list", "показать все мои заявки"));
        commandsList.add(
                new BotCommand("listall", "показать все активные заявки (только для сотрудников школы)"));
        commandsList.add(
                new BotCommand("help", "показать все доступные команды"));
        this.execute(new SetMyCommands(
                commandsList, new BotCommandScopeDefault(), null));
    }
}


