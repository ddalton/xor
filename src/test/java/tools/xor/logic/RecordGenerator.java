package tools.xor.logic;

import tools.xor.GeneratorDriver;
import tools.xor.generator.DefaultGenerator;
import tools.xor.service.exim.CSVLoader;
import tools.xor.util.graph.StateGraph;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONObject;

public class RecordGenerator extends DefaultGenerator implements Iterator<JSONObject>, GeneratorDriver
{
    private StateGraph.ObjectGenerationVisitor visitor;
    private int total;
    private int counter;
    private SimpleDateFormat dateFormatter;

    // This format should also be configured in the entity generator config file
    private static String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    String[] owners = {"johnd", "johnw", "vivianr"};

    public RecordGenerator (String[] arguments)
    {
        super(arguments);
    }

    public RecordGenerator (int argument)
    {
        super(argument);
    }

    @Override
    public void init (Connection connection, StateGraph.ObjectGenerationVisitor visitor)
    {
        this.dateFormatter = new SimpleDateFormat(DATE_FORMAT);
        this.visitor = visitor;

        // We are going to generate 20 records;
        this.total = 20;
        this.counter = 0;
    }

    @Override
    public boolean isDirect ()
    {
        return true;
    }

    @Override
    public boolean hasNext ()
    {
        return this.counter < this.total;
    }

    @Override
    public JSONObject next ()
    {
        this.counter++;
        visitor.setContext(this.counter);

        // generate data for the following fields
        // name, displayname, description, ownedBy, dtype, createdon

        JSONObject json = new JSONObject();
        Map<String, Object> record = new HashMap<>();
        json.put(CSVLoader.CSVState.normalize("name"), "TID_" + counter);
        json.put(CSVLoader.CSVState.normalize("displayname"), "Task " + counter);
        json.put(CSVLoader.CSVState.normalize("description"), "This is task number - " + counter);
        json.put(CSVLoader.CSVState.normalize("ownedBy"), owners[(int)(Math.random() * 3)]);
        json.put(CSVLoader.CSVState.normalize("dtype"), "Task");
        json.put(CSVLoader.CSVState.normalize("createdon"), dateFormatter.format(new Date()));

        return json;
    }
}
