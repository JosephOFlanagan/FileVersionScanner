package classes;

import java.io.File;

public class fileData 
{
    //declare variables
    String[] objects;
    String[] attachments;
    String[] attachmentTypes;
    String logType = "";
    File[] attachmentFiles;
    //Class Constructor
    public responseDeskData(String[] obj, String[] attach, String lt, File[] af, String[] at)
    {
        //equivalent values
        objects = obj;
        attachments = attach;
        logType = lt;
        attachmentFiles = af;
        attachmentTypes = at;
    }
  
    public String[] objects()
    {
        //return this value
        return this.objects;
    }
    public String[] attachments()
    {
        //return these valuess
        return this.attachments;
    }
    public String logType()
    {
        //return this value
        return this.logType;
    }
    public String[] attachmentTypes() 
    {
        //return these values
        return this.attachmentTypes;
    }
  
    public File[] attachmentFiles() 
    {
        //return this value
        return this.attachmentFiles;
    }
}
