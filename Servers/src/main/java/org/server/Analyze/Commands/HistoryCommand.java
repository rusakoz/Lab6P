package org.server.Analyze.Commands;

import lombok.NoArgsConstructor;
import org.server.Analyze.CollectionManager;
import org.server.Analyze.Command;
/**
 * Класс описывающий команду History
 */
@NoArgsConstructor
public class HistoryCommand implements Command {
    CollectionManager cm;
    public HistoryCommand(CollectionManager cm){
        this.cm = cm;
    }
    @Override
    public String Arg(){
        return "";
    }
    @Override
    public String Descr(){
        return "история последних 15 команд";
    }
    @Override
    public void execute(String[] args) {
        cm.historyOutput();
    }
}