package org.client.CommandManager.Commands;

import lombok.NoArgsConstructor;
import org.client.CommandManager.Command;
import org.client.InputOutput;

@NoArgsConstructor
public class ExitCommand implements Command {

    @Override
    public String Arg(){
        return "";
    }
    @Override
    public String Descr(){
        return "завершить работу программы(без сохранения)";
    }
    @Override
    public void execute(String[] args) {
        new HistoryCommand().clear();
        new InputOutput().OutputErr("Программа остановлена");
        System.exit(0);
    }
}
