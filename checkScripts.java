package classes;

//import packages
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class checkScripts 
{
    //Class variables
    String[] scripts;
    List<File> scriptFiles;
    File[] scriptFileArray;
    String logNumber;
    scriptArray logScripts;
    File directory;
    Writer writer;
    String errors = "";
    //Class Constructor
    public checkScripts(scriptArray sA, String logNo, File dir)
    {
        //Script Array
        logScripts = sA;
        scripts = sA.allScripts;
        scriptFiles = Arrays.asList(sA.allFiles);
        //Log Number
        logNumber = logNo;
        //Directory
        directory = dir;
    }
  
    public boolean scriptCheck() throws IOException
    {
        //declare local variables
        boolean match = false;
        boolean regularMatch = false;
        boolean patternCheck = false;
        String fileName;
        int fileValue = 0;
        boolean testPrepPassed = true;

        File[] scriptFileArray = new File[scriptFiles.size()];
        scriptFileArray = scriptFiles.toArray(scriptFileArray);
        String[] letters = new String[26];
        String[] numbers = new String[10];

        System.out.println("Checking scripts for correct filenames and script content");
        //For all files in Script Array
        for (int i = 0; i < logScripts.allScripts.length; i++)
        {
            fileName = logScripts.allScripts[i].substring(0, logScripts.allScripts[i].length() - 4);
       
            //Create a ScriptTemplate array with the following items
            //$RDlog[a-z]_ddl.sql
            //$RDlog[a-z]_dml.sql
            //$RDlog[a-z]_df.sql
            //$RDlog[a-z]_dd.sql
            //$RDlog[a-z]_dd_cat.sql
            //$RDlog[a-z]_ext_ddl.sql
            //$RDlog[a-z]_ext_df.sql
            //$RDlog[a-z]_ext_dml.sql
            //drive[0-9][0-9][0-9]_$RDlog.sql
            //$RDlog[a-z]_dd_cs.sql
            //$RDlog[a-z]_df_cs.sql
            //$RDlog[a-z]_dml_cs.sql
            //Drive[0-9][0-9][0-9]_$RDlog_cs.sql
            String[] scriptTemplate = new String[13];
            scriptTemplate[0] = logNumber + "[a-z]_ddl.sql";
            scriptTemplate[1] = logNumber + "[a-z]_dml.sql";
            scriptTemplate[2] = logNumber + "[a-z]_df.sql";
            scriptTemplate[3] = logNumber + "[a-z]_dd.sql";
            scriptTemplate[4] = logNumber + "[a-z]_dd_cat.sql";
            scriptTemplate[5] = logNumber + "[a-z]_ext_ddl.sql";
            scriptTemplate[6] = logNumber + "[a-z]_ext_df.sql";
            scriptTemplate[7] = logNumber + "[a-z]_ext_dml.sql";
            scriptTemplate[8] = "drive[0-9][0-9][0-9]_" + logNumber + ".sql";
            scriptTemplate[9] = logNumber + "[a-z]_dd_cs.sql";
            scriptTemplate[10] = logNumber + "[a-z]_df_cs.sql";
            scriptTemplate[11] = logNumber + "[a-z]_dml_cs.sql";
            scriptTemplate[12] = "drive[0-9][0-9][0-9]_" + logNumber + ".cs.sql";

            //Create a ScriptContent array with the following items
            //script_audit.script_name%TYPE := ‘$filename-minus_extention’;
            //IF tl_script_audit.script_already_run(l_script_name) = ‘N’;
            //tl_script_audit.write_audit(l_script_name);
            //tl_script_audit.script_success(l_scraud_id);
            String[] scriptContent = new String[4];
            scriptContent[0] = "script_audit.script_name\\%TYPE\\s*:=\\s*";
            scriptContent[1] = "IF\\s*tl_script_audit.script_already_run\\(l_script_name\\)\\s*=\\s*'N'";
            scriptContent[2] = "tl_script_audit.write_audit\\(l_script_name\\);";
            scriptContent[3] = "tl_script_audit.script_success\\(l_scraud_id\\);";

            //Match Boolean equals False
            match = false;
            String type = logScripts.scriptType(scriptFileArray[i].getName());
            //For all regular expressions
            for (int j = 0 ; j < scriptTemplate.length; j++)
            {
                //Check to see if there is a match
                //If there is a match, { set the Match variable to True }
                Pattern scriptName = Pattern.compile(scriptTemplate[j], Pattern.CASE_INSENSITIVE);
                Matcher nameMatcher = scriptName.matcher(scriptFileArray[i].getName());

                if (nameMatcher.matches())
                {
                    int typeMatcher = scriptFileArray[i].getName().indexOf(type);
                    if (typeMatcher > - 1)
                    {
                        int correctLogNumber = scripts[i].indexOf(logNumber);
                        if (correctLogNumber > -1) 
                        {
                            match = true;
                        }
                    }
                }
            }
            //If Match equals False
            if (match == false)
            {
                testPrepPassed = false;
                errors = errors + "ERROR: " + logScripts.allScripts[i] + " File name does not meet the standard naming convention." + System.lineSeparator();
                System.out.println("ERROR: " + logScripts.allScripts[i] + " File name does not meet the standard naming convention.");
                //ERROR: $filename: File name does not meet the standard naming
                //convention.
            }
            
            //RegularMatch equals False
            regularMatch = false;
            int auditCheck = 0;         
            //For regular expressions in files (except ddl and drive scripts)
            if (!(type).equals("drive"))
            {
                if (!(type).equals("ddl"))
                {
                    for (int k = 0; k < scriptContent.length; k++)
                    {
                        //Need to read the file content
                        BufferedReader bReader = new BufferedReader(new FileReader(scriptFileArray[i]));
                        int lineCount = 0;

                        String line = "";
                        while ((line = bReader.readLine()) !=null) 
                        {
                            //Check to see if there is a match
                            //If there is a match, {set the RegularMatch variable to True }
                            int posFound = -1;
                            lineCount++;
                    
                            if (k == 0) 
                            {
                                //This pattern is unique as it's the only one that involves the log number, hence a slightly different check
                                Pattern r = Pattern.compile(scriptContent[k], Pattern.CASE_INSENSITIVE);
                                Matcher m = r.matcher(line);
                                int k0Check = line.indexOf(fileName);
                                if (m.find())
                                {
                                    if (k0Check > -1)
                                    {
                                        //The log number and the pattern are correct, so setting the found variable to equal the line will resolve this
                                        posFound = lineCount;
                                    }
                                    else 
                                    {
                                        //There is a mistake here so report an error
                                        errors = errors + "ERROR: " + logScripts.allScripts[i] + " : Wrong scriptname for auditing given. Expecting " + fileName + " to be written as the internal scriptname." + System.lineSeparator();
                                        System.out.println("ERROR: " + logScripts.allScripts[i] + " : Wrong scriptname for auditing given. Expecting " + fileName + " to be written as the internal scriptname.");
                                    }                                   
                                }
                            }
                            else 
                            {
                                //Code checks that the line matches the specified pattern. If it finds a match on at least one line it returns true.
                                Pattern r = Pattern.compile(scriptContent[k], Pattern.CASE_INSENSITIVE);
                                Matcher m = r.matcher(line);
                                if (m.find()) 
                                {
                                    posFound = lineCount;
                                }
                            }
                            if (posFound > -1)
                            {
                                //We've found a match, so increment a value that will allow us to check if all four patterns have been found.
                                auditCheck++;
                            }
                        }
                    }
                    if (auditCheck == 4)
                    {
                        //We know that all four templates have been met, so assign this variable to equal true.
                        regularMatch = true;
                    }
                    //If RegularMatch equals False
                    if (regularMatch == false)
                    {
                        testPrepPassed = false;
                        //ERROR: $filename : Expecting to find script_audit checks and updates.
                        errors = errors + "ERROR: " + logScripts.allScripts[i] + " : Expecting to find script_audit checks and updates." + System.lineSeparator();
                        System.out.println("ERROR: " + logScripts.allScripts[i] + " : Expecting to find script_audit checks and updates.");
                    }
                }
            }
            
            //If Script equals DDL
            if ((type).equals("ddl"))
            {
                //PatternCheck equals False
                patternCheck = false;
                //Check Script to see if it contains CREATE, DROP, ALTER or REPLACE
                BufferedReader bReader = new BufferedReader(new FileReader(scriptFileArray[i]));
                int lineCount = 0;
                String line = "";
        
                while ((line = bReader.readLine()) !=null)
                {
                    line = line.toLowerCase(Locale.US);
                    lineCount++;
                    int posFound = line.indexOf("create");
                    //If Script contains CREATE
                    if (posFound > - 1)
                    {
                        patternCheck = true;
                    }
                    posFound = -1;
                    //Check Script to see if it contains DROP
                    posFound = line.indexOf("drop");
                    //If Script contains DROP
                    if (posFound > - 1)
                    {
                        patternCheck = true;
                    }
                    posFound = -1;              
                    //Check Script to see if it contains ALTER
                    posFound = line.indexOf("alter");
                    //If Script contains ALTER
                    if (posFound > - 1)
                    {
                        patternCheck = true;
                    }
                    posFound = -1;  
                    //Check Script to see if it contains REPLACE
                    posFound = line.indexOf("replace");
                    //If Script contains REPLACE
                    if (posFound > - 1)
                    {
                        //PatternCheck equals True
                        patternCheck = true;
                    }
                }
                //If PatternCheck equals False
                if (patternCheck == false)
                {
                    testPrepPassed = false;
                    //ERROR: $Filename: Expecting to find
                    //CREATE|DROP|ALTER|REPLACE in a ddl script
                    errors = errors + "ERROR: " + logScripts.allScripts[i] + " Expecting to find CREATE|DROP|ALTER|REPLACE in a ddl script" + System.lineSeparator();
                    System.out.println("ERROR: " + logScripts.allScripts[i] + " Expecting to find CREATE|DROP|ALTER|REPLACE in a ddl script");
                }
            }
            //Else
            else
            {
                BufferedReader bReader = new BufferedReader(new FileReader(scriptFileArray[i]));
                int lineCount = 0;
                String line = "";
                //PatternCheck equals False
                patternCheck = false;
        
                while ((line = bReader.readLine()) !=null)
                {
                    line = line.toLowerCase(Locale.US);
                    lineCount++;
                    //Check Script to see if it contains SELECT
                    int posFound = line.indexOf("select");
                    //If Script contains SELECT
                    if (posFound > - 1)
                    {
                        patternCheck = true;
                    }
                    posFound = -1;
                    //Check Script to see if it contains INSERT
                    posFound = line.indexOf("insert");     
                    //If Script contains INSERT
                    if (posFound > - 1)
                    {
                        patternCheck = true;
                    }
                    posFound = -1;
                    //Check Script to see if it contains UPDATE
                    posFound = line.indexOf("update");
                    //If Script contains UPDATE
                    if (posFound > - 1)
                    {
                        patternCheck = true;
                    }
                    posFound = -1;
                    //Check Script to see if it contains DELETE
                    posFound = line.indexOf("delete");
                    //If Script contains DELETE
                    if (posFound > - 1)
                    {
                        //PatternCheck equals True
                        patternCheck = true;
                    }
                }
                //If PatternCheck equals False
                if (patternCheck == false)
                {
                    testPrepPassed = false;
                    //ERROR: $Filename: Expecting to find
                    //SELECT|INSERT|UPDATE|DELETE in the script
                    errors = errors + "ERROR: " + logScripts.allScripts[i] + " Expecting to find SELECT|INSERT|UPDATE|DELETE in the script" + System.lineSeparator();
                    System.out.println("ERROR: " + logScripts.allScripts[i] + " Expecting to find SELECT|INSERT|UPDATE|DELETE in the script");
                }
            }
        }
        //Write errors out to a script error file
        try 
        {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(directory + "/" + "scripterrors.txt"), "utf-8"));
            writer.write(errors);
        } 
        catch (IOException ex) 
        {
            System.out.println("Could not write to scripterrors file.");
            // Report
        } 
        finally 
        {
           try {writer.close();} catch (Exception ex) {/*ignore*/}
        }
        //return result
        return testPrepPassed;
    }
}