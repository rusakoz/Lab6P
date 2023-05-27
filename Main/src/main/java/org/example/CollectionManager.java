package org.example;

import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.example.CommandManager.AntiRecursionScript;
import org.example.CommandManager.Command;
import org.example.CommandManager.Invoker;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import org.slf4j.*;

@EqualsAndHashCode
@Setter
@Getter
public class CollectionManager {
    private Date date;
    private LinkedHashSet<Flat> collection;
    private List<Flat> beans = null;
    private File file;
    private List<String> history;

    private static String path = System.getenv("lab");
    private Dotenv dotenv = Dotenv.load();
    private String path2 = dotenv.get("HELLO");

    private static final Logger log = LoggerFactory.getLogger(CollectionManager.class);


    public CollectionManager(){
        this.history = new ArrayList<>();
        this.date = new Date();
        this.collection = new LinkedHashSet<>();
    }

    public void Read() {

        if (path == null){
            new InputOutput().Output("Переменная окружения отсутствует или не определена, дальнейшая работа приложения невозможна\nЗадайте переменную окружения 'lab' с путем до файла");
            System.exit(-1);
        }
        //Path paths;
        //{
            //try {
                //paths = Paths.get(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
                //paths = Path.of(paths.toAbsolutePath() + dotenv.get("HEL"));
            //} catch (URISyntaxException e) {
                //throw new RuntimeException(e);
            //}
        //}
        //System.out.println(paths);
        //File.createNewFile();


        file = new File(path);
        try {
            if(!file.canRead() || !file.canWrite()) throw new SecurityException();
        } catch (SecurityException e) {
            new InputOutput().Output("Файл отсутствует или недоступен для чтения и записи\nХотите повторить попытку, иначе остановится программа?(Y/N)");
            Scanner scanner = new Scanner(System.in);
            String str = null;
            while(!scanner.hasNext("[YyNn]")){
                new InputOutput().Output("Введите Y или N");
                scanner.nextLine();
            }
            str = scanner.nextLine();
            if (Objects.equals(str, "Y") | Objects.equals(str, "y")){
                path = System.getenv("lab");
                Read();
                return;
            }
            else if(Objects.equals(str, "N") | Objects.equals(str, "n")){
                new InputOutput().Output("Программа остановлена");
                System.exit(0);
            }

        }

        try {
            if(file.length() == 0) throw new CsvException();
        } catch (CsvException e) {
            new InputOutput().Output("Файл пустой");
            return;
        }

        try {

            /*
            URL u = getClass().getClassLoader().getResource(path);

            if (u == null){
                System.out.println("Не был найден необходимый файл, дальнейшая работа приложения невозможна");
                System.exit(-1);
            }

            InputStream r = getClass().getClassLoader().getResourceAsStream(path);
            InputStreamReader rr = new InputStreamReader(r, StandardCharsets.UTF_8);
            System.out.println(r);
            */

             beans = new CsvToBeanBuilder<Flat>(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8))
                    .withType(Flat.class)
                    .withSeparator(',')
                     .withThrowExceptions(false) //если кол-во элементов строки не совпадает с кол-вом столбцов, то не выскочит CsvRequiredFieldEmptyException
                    .build()
                     .parse();

             /*
            final List<Flat> users = beans.parse();//2
            users.forEach((user) -> {
                logger.info("Parsed data:" + user.toString());
            });

            beans.getCapturedExceptions().forEach((exception) -> { //3
                logger.error("Inconsistent data:" +
                        String.join("", exception.getLine()), exception);//4
            });
             */

        } catch (Exception e) {
            System.out.println(e.getMessage());
            new InputOutput().OutputErr("Файл отсутствует");
        }

        collection = new LinkedHashSet<>(beans);

        int count = 1;
        Set<Integer> set = new HashSet<>();
        for (Flat a: beans){
            if (beans.size() == 0) break;
            int id = a.getId();
            set.add(a.getId());
            while (count > set.size()) {
                a.setId(++id);
                set.add(id);
            }
            count++;
        }
        set.clear();

        for (Flat a: beans) {
            new Validators().validatorFlat(a, "-----------------" + "\n" + "Ошибка ввода данных под id: " + a.getId() + "\n" + "-----------------");
        }

        new InputOutput().Output("Коллекция успешно загружена");
        beans.clear();

        /*
        for (Flat a : collection) {
            System.out.print( a.getId() + " | " + a.getName() + " | " + a.getCoordinates().getX() + " | " + a.getCoordinates().getY() + " | " +
                     a.getCreationDate() + " | " + a.getArea() + " | " + a.getNumberOfRooms() + " | " + a.getTimeToMetroByTransport() + " | " +
                     a.getView() + " | " + a.getHouse().getName() + " | " + a.getHouse().getYear() + " | " + a.getHouse().getNumberOfFloors()
                            +"\n" );
        }
        */
    }

    public boolean Write() {

        file = new File(path);

        try {
            if(!file.canRead() || !file.canWrite()) throw new SecurityException();
        } catch (SecurityException e) {
            new InputOutput().OutputErr("Файл недоступен для записи");
            return false;
        }

        try {
            if(!file.isFile()) throw new IOException();
        } catch (IOException e) {
            new InputOutput().OutputErr("Файл для сохранения коллекции не является файлом");
            return false;
        }


        beans = new ArrayList<>(getCollection());

        try (PrintWriter writer = new PrintWriter(path, StandardCharsets.UTF_8)){
            StatefulBeanToCsv<Flat> beanToCsv = new StatefulBeanToCsvBuilder<Flat>(writer)
                    .withSeparator(',')
                    .build();
            beanToCsv.write(beans);
            beans.clear();
        } catch (IOException | CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
            new InputOutput().OutputErr("При записи коллекции в файл произошла ошибка");
            return false;
        }
        return true;
    }

    public void add(Flat a){
        collection.add(a);
        new InputOutput().Output("Новый элемент был успешно добавлен");
    }

    public void addNewElement(){
        add(newElementFromScanner(newId()));
        history("add");
    }

    public int newId(){

        Set<Integer> set = new HashSet<>();
        int id;
        for (Flat a: collection){
            set.add(a.getId());
        }
        int size = set.size();
        if (collection.size() + 1 < Integer.MAX_VALUE) {
            id = collection.size() + 1;
            set.add(id);
        }else {
            id = 0;
            set.add(id);
        }
        while(size == set.size()){
            if(id+1 < Integer.MAX_VALUE) {
                set.add(++id);
            }else {
                id = 0;
                set.add(id);
            }
        }
        set.clear();
        return id;
    }

    public Flat newElementFromScanner(int id){
        Scanner scanner = new Scanner(System.in);
        Date date = new Date();

        String name = null;
        //Coordinates
        Coordinates coordinates;
        Integer X;
        float Y;
        //Coordinates
        Long area = (long) -1;
        int numberOfRooms = -1;
        float timeToMetroByTransport = -1;
        View view = null;
        //House
        House house = null;
        String nameHouse;
        long yearHouse = -1;
        long numberOfFloorsHouse = -1;
        //House

        new InputOutput().Output("Введите имя");
        if (scanner.hasNext()) {
            while (!scanner.hasNext("([А-Я][а-я]+)|([A-Z][a-z]+)")) {
                new InputOutput().Output("Неправильно введено имя, проверьте формат: Aaaaaaa");
                scanner.nextLine();
            }
            name = scanner.nextLine();
            new InputOutput().Output("Success");
        }
        new InputOutput().Output("Введите координату X, пример: 12345");
        while (!scanner.hasNextInt()) {
            new InputOutput().Output("Неправильно введена координата X, проверьте формат: 12345");
            scanner.next();
        }
        X = scanner.nextInt();
        new InputOutput().Output("Success");


        new InputOutput().Output("Введите кординату Y, пример: 12345,1");
        while (!scanner.hasNextFloat()) {
            new InputOutput().Output("Неправильно введена координата Y, проверьте формат: 12345,1");
            scanner.next();
        }
        Y = scanner.nextFloat();
        new InputOutput().Output("Success");
        coordinates = new Coordinates(X, Y);


        new InputOutput().Output("Введите область(>нуля!), пример: 12345");
        while (!(area > 0)){
            while (!scanner.hasNextLong()){
                new InputOutput().Output("Область введена неправильно, проверьте формат: 12345(>нуля!)");
                scanner.next();
            }
            area = scanner.nextLong();
            if(area <= 0){
                new InputOutput().Output("Область введена неправильно, проверьте формат: 12345(>нуля!)");
            }else new InputOutput().Output("Success");
        }


        new InputOutput().Output("Введите кол-во комнат, пример: 12345(>нуля!)");
        while (!(numberOfRooms > 0)){
            while (!scanner.hasNextInt()){
                new InputOutput().Output("Кол-во комнат введено неправильно, проверьте формат: 12345(>нуля!)");
                scanner.next();
            }
            numberOfRooms = scanner.nextInt();
            if(numberOfRooms <= 0){
                new InputOutput().Output("Кол-во комнат введено неправильно, проверьте формат: 12345(>нуля!)");
            }else new InputOutput().Output("Success");
        }


        new InputOutput().Output("Введите время до метро, пример: 12345,1(>нуля!");
        while (!(timeToMetroByTransport > 0)){
            while (!scanner.hasNextFloat()){
                new InputOutput().Output("Время до метро введено неправильно, проверьте формат: 12345,1(>нуля!)");
                scanner.next();
            }
            timeToMetroByTransport = scanner.nextFloat();
            if(timeToMetroByTransport <= 0){
                new InputOutput().Output("Время до метро введено неправильно, проверьте формат: 12345,1(>нуля!)");
            }else new InputOutput().Output("Success");
        }


        new InputOutput().Output("Введите вид, список доступных видов: YARD, PARK, NORMAL");
        if (scanner.hasNext()) {
            String str = scanner.nextLine();
            while (view == null) {
                switch (str) {
                    case "YARD" -> {
                        view = View.YARD;
                        new InputOutput().Output("Success");
                    }
                    case "PARK" -> {
                        view = View.PARK;
                        new InputOutput().Output("Success");
                    }
                    case "NORMAL" -> {
                        view = View.NORMAL;
                        new InputOutput().Output("Success");
                    }
                    default -> {
                        if (scanner.hasNext())new InputOutput().Output("Вид введен неверно, проверьте формат: YARD, PARK, NORMAL");
                        str = scanner.nextLine();
                    }
                }
            }
        }

        new InputOutput().Output("Введите название дома, пример: Ааааа");
        while (!scanner.hasNext("([А-Я][а-я]+)|([A-Z][a-z]+)")) {
            new InputOutput().Output("Неправильно введено название дома, проверьте формат: Aaaaa");
            scanner.nextLine();
        }
        nameHouse = scanner.nextLine();
        new InputOutput().Output("Success");


        new InputOutput().Output("Введите возраст дома, пример: 12345(от 1 до 578)");
        while (!(yearHouse > 0 & yearHouse <= 578)){
            while (!scanner.hasNextLong()){
                new InputOutput().Output("Возраст дома введен неправильно, проверьте формат: 12345(от 1 до 578!)");
                scanner.next();
            }
            yearHouse = scanner.nextLong();
            if(yearHouse <= 0 | yearHouse > 578){
                new InputOutput().Output("Взраст дома введен неправильно, проверьте формат: 12345(от 1 до 578!)");
            }else new InputOutput().Output("Success");
        }


        new InputOutput().Output("Введите кол-во дверей дома, пример: 12345(>нуля!)");
        while (!(numberOfFloorsHouse > 0)){
            while (!scanner.hasNextLong()){
                new InputOutput().Output("Кол-во дверей дома введено неправильно, проверьте формат: 12345(>нуля!)");
                scanner.next();
            }
            numberOfFloorsHouse = scanner.nextLong();
            if(numberOfFloorsHouse <= 0){
                new InputOutput().Output("Кол-во дверей дома введено неправильно, проверьте формат: 12345(>нуля!)");
            }else new InputOutput().Output("Success");
        }

        house = new House(nameHouse, yearHouse, numberOfFloorsHouse);
        return new Flat(id, name, coordinates, date, area, numberOfRooms, timeToMetroByTransport, view, house);

    }

    public void remove(int id){
        if (!collection.removeIf(a -> a.getId() == id)) {
            new InputOutput().Output("Элемента под id = " + id + " нет в коллекции");
        }else new InputOutput().Output("Элемент коллекции под id = " + id + " был успешно удален");
        history("remove");
    }

    public void info(){
        new InputOutput().Output("Тип коллекции - 'LinkedHashSet' | Дата инициализации - " + date + " | Кол-во элементов - " + collection.size());
        history("info");
    }

    public void show(){
        if (collection.isEmpty()) {
            new InputOutput().Output("Коллекция пустая");
        }else {
            collection.forEach(a -> new InputOutput().Output("id: " + a.getId() + "\nname: " + a.getName() +
                    " | кол-во комнат: " + a.getNumberOfRooms() + " | время до метро: " + a.getTimeToMetroByTransport() +
                    " | область: " + a.getArea() + " | дата создания элемента: " + a.getCreationDate() +
                    " | координата X: " + a.getCoordinates().getX() + " | координата Y: " + a.getCoordinates().getY() +
                    " | название дома: " + a.getHouse().getName() + " | возраст дома: " + a.getHouse().getYear() +
                    " | кол-во этажей: " + a.getHouse().getNumberOfFloors() + " | вид: " + a.getView()));
        }
        history("show");
    }

    public void update(int id){
        if(collection.removeIf(a -> a.getId() == id)){
            add(newElementFromScanner(id));
        }else new InputOutput().Output("Элемента под id = " + id + " нет в коллекции");
        history("update");
    }

    public void clear(){
        collection.clear();
        new InputOutput().Output("Коллекция успешно очищена");
        history("clear");
    }

    public void save(){
        if(Write()) {
            new InputOutput().Output("Коллекция успешно сохранена");
        }else new InputOutput().Output("Коллекция не была сохранена");
        history("save");
    }

    public void executeScript(String fileName) {
        history("execute_script");
        File file2 = new File(fileName);
        Invoker invoker = new Invoker(this);


        try {
            if(!file2.canRead() || !file2.canWrite()) throw new SecurityException();
        } catch (SecurityException e) {
            System.err.println("Файл недоступен для чтения");
            return;
        }

        try {
            if(file2.length() == 0) throw new CsvException();
        } catch (CsvException e) {
            System.err.println("Файл пуст");
            return;
        }

        try {
            Scanner sc = new Scanner(new FileInputStream(Path.of(fileName).toFile()), StandardCharsets.UTF_8);
            AntiRecursionScript.add("execute_script "+fileName);
            int f = AntiRecursionScript.getSet().size();

            while (sc.hasNext()) {
                String scan = null;
                try {
                    scan = sc.nextLine();
                    if (scan.matches("execute_script .*")){
                        AntiRecursionScript.add(scan);

                        if (f == AntiRecursionScript.getSet().size()){
                            new InputOutput().Output("Был обнаружен зацикливающий скрипт, выполнение которого было пропущено");
                            return;
                        }else f++;
                    }
                    if(scan.isEmpty()) continue;
                    String[] tokens = scan.split(" ");
                    Command command = invoker.getCommands().get(tokens[0]);

                    command.execute(tokens);

                } catch (NullPointerException e) {
                    new InputOutput().Output("Команда: '" + scan + "' введена неверно выполнение скрипта было остановлено");
                    return;
                } catch (NoSuchElementException e) {
                    new InputOutput().Output("не-не");

                }
            }
            } catch(FileNotFoundException e){
                new InputOutput().Output("Файл отсутствует");
            }
        new InputOutput().Output("Выполнение скрипта окончено");
    }

    public void addIfMax(){
        Flat flat = newElementFromScanner(newId());
        if (flat.compareTo(Collections.max(collection)) > 0){
            add(flat);
        }else new InputOutput().Output("Объект не больше максимального, поэтому не был добавлен в коллекцию");
        history("add_if_max");
    }

    public void addIfMin(){
        Flat flat = newElementFromScanner(newId());
        if (flat.compareTo(Collections.min(collection)) > 0){
            add(flat);
        }else new InputOutput().Output("Объект не больше минимального, поэтому не был добавлен в коллекцию");
        history("add_if_min");
    }

    public void history(String commandName){
        if (history.size() < 15) {
            history.add(commandName);
        }else {
            history.remove(0);
            history.add(commandName);
        }
    }
    public void historyOutput(){
        history("history");
        history.forEach(a -> new InputOutput().Output(a));
    }

    public void sumOfTimeToMetroByTransport(){
        float sum = 0;
        for (Flat a:collection) {
            sum += a.getTimeToMetroByTransport();
        }
        new InputOutput().Output(String.valueOf(sum));
        history("sum_of_time_to_metro_by_transport");
    }

    public void groupCountingByCreationDate(){
        HashMap<Date, List<Flat>> hashMap = new HashMap<>();

        for (Flat a:collection) {
            if (!hashMap.containsKey(a.getCreationDate())){
                List<Flat> list = new ArrayList<>();
                list.add(a);
                hashMap.put(a.getCreationDate(), list);
            }else{
                hashMap.get(a.getCreationDate()).add(a);
            }
        }
        hashMap.forEach((a, b)-> System.out.println(a +" "+ b));
        history("groupCountingByCreationDate");
    }

    public void help(){
        history("help");
    }

    //public void countLessThanHouse(){
    //}
}
