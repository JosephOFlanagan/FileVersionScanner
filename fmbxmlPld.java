package classes;

import java.util.List;

public class fmbxmlPld
{
    //FMX Array
    List<String> fmbxmlArray;
    //PLL Text Array
    List<String> pldArray;

    public fmbxmlPld(List<String> fX, List<String> pL)
    {
        //FMX Array
        fmbxmlArray = fX;
        //PLL Text Array
        pldArray = pL;
    }
    //FMX Array
    public List<String> fmbxmlArray()
    {
        //Return value
        return this.fmbxmlArray;
    }
    //PLL Text Array
    public List<String> pldArray()
    {
        //Return value
        return this.pldArray;
    }
}