package classes;

import java.io.File;

public class responseDeskData 
{
    //declare variables
    String[] pvcsObjects;
    String[] attachments;
    String[] attachmentTypes;
    String logType = "";
    File[] attachmentFiles;
    //Class Constructor
    public responseDeskData(String[] pVCS, String[] attach, String lt, File[] af, String[] at)
    {
        //equivalent values
        pvcsObjects = pVCS;
        attachments = attach;
        logType = lt;
        attachmentFiles = af;
        attachmentTypes = at;
    }
  
    public String[] pvcsObjects()
    {
        //return this value
        return this.pvcsObjects;
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