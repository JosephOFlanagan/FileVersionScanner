package classes;

import java.io.File;

public class scriptArray 
{
    //This class will hold all scripts
    String[] allScripts;
    File[] allFiles;
    //Class Constructor
    public scriptArray(String[] aScript, File[] aFile)
    {
        allScripts = aScript;
        allFiles = aFile;
    }
  
    //This will return all the script names
    public String[] allScripts()
    {
        return this.allScripts;
    }
  
    //This will return all Script files
    public File[] allFiles()
    {
        return this.allFiles;
    }
  
    //This method checks for customer specific scripts, this will help with determining the script type in the checkScripts method
    public String scriptType(String script)
    {
        //We need to shorten the script down to just the type, which is what we are interested in, but if it's customer specific it will be longer.
        String csCheck1 = script.substring(script.length() - 10);
        //Should this be a customer specific script, the shortened script should contain "cs" in the following characters.
        String csCheck2 = csCheck1.substring(4,6);
        //Create two blank strings
        String type1 = "";
        String type2 = "";
        if ((csCheck2).equals("cs"))
        {
            //Need to check whether this is a drive script or not
            if ((script.substring(0,8).equals("drive_cs")))
            {
                type2 = "drive_cs";
            } 
            //This will return the type based on the existence of cs in the script name
            type1 = csCheck1;
            type2 = type1.substring(0,3);
        }
        else
        {
            //Need to check whether this is a drive script or not
            if ((script.substring(0,5).equals("drive")))
            {
                type2 = "drive";
            } 
            else
            {
                //It's not customer specific, so the type section will be shorter.
                type1 = script.substring(script.length() - 7);
                type2 = type1.substring(0,3);
            }
        }
        //Since dd is a two letter type as opposed to a three letter type then the type needs to be shortened.
        if ((type2).equals ("_dd"))
        {
            type2 = "dd";
        }
        //return type
        return type2;          
    }
}