package depsolver;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import java.util.*;
import java.io.*;


public class Main {
    private static HashSet<List<String>> seenSet = new HashSet<>();
    private static HashMap<String, Integer> solutions = new HashMap<>();
    private static HashMap<String, Integer> biggestConflicts = new HashMap<>();
    private static List<String> commands = new ArrayList<>();


    /**
     * The main function finds calculates all the ways to solve the dependency issue then prints the cheapest method to the console in JSON format
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        TypeReference<List<Package>> repoType = new TypeReference<List<Package>>() {};
        List<Package> repo = JSON.parseObject(readFile(args[0]), repoType);
        TypeReference<List<String>> strListType = new TypeReference<List<String>>() {};
        List<String> initial = JSON.parseObject(readFile(args[1]), strListType);
        List<String> constraints = JSON.parseObject(readFile(args[2]), strListType);
        verifyAndSearch(initial, repo, constraints);
        printCheapestSolution();
    }

    /**
     * Removes any and all bad packages then runs the search function on the new list
     *
     * @param initial
     * @param repo
     * @param constraints
     */
    private static void verifyAndSearch(List<String> initial, List<Package> repo, List<String> constraints)    {
        if(!valid(initial, repo))   {
            List<String> fixed = rmBadPackages(initial, repo);
            while(!valid(fixed,repo)) {
                fixed = rmBadPackages(fixed, repo);
            }
            search(fixed, repo, fixed, constraints);
        } else {
            search(initial, repo, initial, constraints);
        }
    }

    /**
     * Builds a string based on a given set of commands
     * @return the new string
     */
    private static String commands()    {
        String rt = "";
        for(String s : commands) {
            rt+= s+",";
        }
        return rt;
    }

    private static void search(List<String> set, List<Package> repo, List<String> fixed, List<String> constraints) {
        if(!valid(set, repo)) { return; }
        if(seenSet.contains(set)) { return; }
        if(finalState(set, constraints)) {
            int opCost = solutionCost(repo);
            Integer costOfSolution = new Integer(opCost);
            String commands = commands();
            solutions.put(commands, costOfSolution);
            return;
        }
        seenSet.add(set);
        updatePackages(set, repo, fixed, constraints);
    }




    private static boolean valid(List<String> set, List<Package> repo) {
        boolean foundDep = false;
        boolean foundConflict = false;

        for(Package pack : repo) {
            if(set.contains(pack.getName() + "=" + pack.getVersion())) {
                for(List<String> depnds : pack.getDepends()) {
                    for(String deps : depnds) {
                        String[] depSplit = splitPackage(deps);
                        String compareOperator = depSplit[2];
                        if(!foundDep) {
                            for(String str : set) {
                                String[] strSplit = splitPackage(str);
                                if(depSplit[0].equals(strSplit[0])) {
                                    switch(compareOperator) {
                                        case "="  : if(deps.equals(str))
                                                        foundDep =  true;
                                                    break;
                                        case "<"  : if(!depSplit[1].equals(strSplit[1]) && versionCompare(depSplit[1], strSplit[1]) == true)
                                                        foundDep =  true;
                                                    break;
                                        case "<=" : if(versionCompare(depSplit[1], strSplit[1]))
                                                        foundDep =  true;
                                                    break;
                                        case ">"  : if(!depSplit[1].equals(strSplit[1]) && versionCompare(strSplit[1], depSplit[1]) == true)
                                                        foundDep =  true;
                                                    break;
                                        case ">=" : if(versionCompare(strSplit[1], depSplit[1]))
                                                        foundDep =  true;
                                                    break;
                                        default : foundDep = true;
                                                    break;
                                    }
                                }
                            }
                        }
                    }
                    if(!foundDep) { return false; }
                }

                for(String str : pack.getConflicts()) {
                    String[] cSplit = splitPackage(str);
                    String compareOperator = cSplit[2];
                    if(!foundConflict) {
                        for(String nxt : set) {
                            String[] tSplit = splitPackage(nxt);
                            if(cSplit[0].equals(tSplit[0])) {
                                switch(compareOperator) {
                                    case "=" : if(str.equals(nxt)) {
                                                    foundConflict = true;
                                                    Integer value = biggestConflicts.get(nxt);
                                                    if(value != null)
                                                        biggestConflicts.put(nxt, value++);
                                                    else
                                                        biggestConflicts.put(nxt, 1);
                                                }
                                                break;
                                    case "<" : if(!cSplit[1].equals(tSplit[1]) && versionCompare(cSplit[1], tSplit[1]) == true) {
                                                    foundConflict =  true;
                                                    Integer value = biggestConflicts.get(nxt);
                                                    if(value != null)
                                                        biggestConflicts.put(nxt, value++);
                                                    else
                                                        biggestConflicts.put(nxt, 1);
                                                }
                                                break;
                                    case "<=" : if(versionCompare(cSplit[1], tSplit[1])) {
                                                    foundConflict =  true;
                                                    Integer value = biggestConflicts.get(nxt);
                                                    if(value != null)
                                                        biggestConflicts.put(nxt, value++);
                                                    else
                                                        biggestConflicts.put(nxt, 1);
                                                }
                                                break;
                                    case ">" : if(!cSplit[1].equals(tSplit[1]) && versionCompare(tSplit[1], cSplit[1]) == true) {
                                                    foundConflict =  true;
                                                    Integer value = biggestConflicts.get(nxt);
                                                    if(value != null)
                                                        biggestConflicts.put(nxt, value++);
                                                    else
                                                        biggestConflicts.put(nxt, 1);
                                                }
                                                break;
                                    case ">=" : if(versionCompare(tSplit[1], cSplit[1])) {
                                                    foundConflict =  true;
                                                    Integer value = biggestConflicts.get(nxt);
                                                    if(value != null)
                                                        biggestConflicts.put(nxt, value++);
                                                    else
                                                        biggestConflicts.put(nxt, 1);
                                                }
                                                break;
                                    default : foundConflict = true;
                                              Integer value = biggestConflicts.get(nxt);
                                              if(value != null)
                                                  biggestConflicts.put(nxt, value++);
                                              else
                                                  biggestConflicts.put(nxt, 1);
                                              break;
                                }
                            }
                        }
                    }
                }
                if(foundConflict) { return false; }
            }
        }
        return true;
    }

    private static boolean finalState(List<String> set, List<String> constraints) {
        for(String str : constraints) {
            String operator = Character.toString(str.charAt(0));
            str = str.substring(1);
            String[] consNameVers = splitPackage(str);
            String consName = consNameVers[0];
            String consVersion = consNameVers[1];
            String compareOperator = consNameVers[2];

            boolean constraintMet = false;
            if(operator.equals("+")) {
                for(String pack : set) {
                    String[] pSplit = splitPackage(pack);
                    if(consName.equals(pSplit[0])) {
                        switch(compareOperator) {
                            case "=" : if(consVersion.equals(pSplit[1]))
                                            constraintMet = true;
                                        break;
                            case "<"  : break;
                            case "<=" : break;
                            case ">"  : break;
                            case ">=" : break;
                            default   : constraintMet = true;
                                        break;
                        }
                    }
                }
            }
            if(!constraintMet) {
                return false;
            }
            if(operator.equals("-")) {
                int i = 0;
                for(String p : set) {
                    String[] pSplit = splitPackage(p);
                    switch(compareOperator) {
                        case "=" : if(consName.equals(pSplit[0]) && consVersion.equals(pSplit[1]))
                                        i++;
                                    break;
                        case "<"  : break;
                        case "<=" : break;
                        case ">"  : break;
                        case ">=" : break;
                        default   : break;
                    }
                }
                if(i > 0) { return false; }
            }
        }
        return true;
    }

    private static List<String> rmBadPackages(List<String> set, List<Package> repo) {
        List<String> redudent = new ArrayList<>();

        int counter = 0;
        for(Package pack : repo) {
            if(counter >= set.size()) { break; }
            boolean packageRemoved = false;
            if(set.contains(pack.getName() + "=" + pack.getVersion())) {
                counter++;
                for(List<String> clause : pack.getDepends()) {
                    boolean foundDep = false;
                    for(String q : clause) {
                        String[] depSplit = splitPackage(q);
                        String compareSymbol = depSplit[2];
                        if(!q.contains("=")) {
                            if(!foundDep) {
                                for(String s : set) {
                                    String[] sSplit = splitPackage(s);
                                    if(depSplit[0].equals(sSplit[0])) {
                                        switch(compareSymbol) {
                                            case "="  : if(q.equals(s))
                                                            foundDep =  true;
                                                        break;
                                            case "<"  : if(!depSplit[1].equals(sSplit[1]) && versionCompare(depSplit[1], sSplit[1]) == true)
                                                            foundDep =  true;
                                                        break;
                                            case "<=" : if(versionCompare(depSplit[1], sSplit[1]))
                                                            foundDep =  true;
                                                        break;
                                            case ">"  : if(!depSplit[1].equals(sSplit[1]) && versionCompare(sSplit[1], depSplit[1]) == true)
                                                            foundDep =  true;
                                                        break;
                                            case ">=" : if(versionCompare(sSplit[1], depSplit[1]))
                                                            foundDep =  true;
                                                        break;
                                            default   :     foundDep = true;
                                                        break;
                                        }
                                    }
                                }
                            }
                        } else {
                            if(set.contains(q)) {
                                foundDep = true;
                            }
                        }
                    }
                    if(!foundDep) {
                        packageRemoved = true;
                        set.remove(pack.getName() + "=" + pack.getVersion());
                        commands.add("-"+ pack.getName() + "=" + pack.getVersion());
                    }
                }
                if(!packageRemoved) {
                    boolean foundConflict = false;
                    for(String str : pack.getConflicts()) {
                        String[] consSplit = splitPackage(str);
                        String compareOperator = consSplit[2];
                        if(!foundConflict) {
                            for(String t : set) {
                                String[] tSplit = splitPackage(t);
                                if(consSplit[0].equals(tSplit[0])) {
                                    switch(compareOperator) {
                                        case "="  : if(str.equals(t))
                                                        foundConflict = true;
                                                    break;
                                        case "<"  : if(!consSplit[1].equals(tSplit[1]) && versionCompare(consSplit[1], tSplit[1]) == true)
                                                        foundConflict =  true;
                                                    break;
                                        case "<=" : if(versionCompare(consSplit[1], tSplit[1]))
                                                        foundConflict =  true;
                                                    break;
                                        case ">"  : if(!consSplit[1].equals(tSplit[1]) && versionCompare(tSplit[1], consSplit[1]) == true)
                                                        foundConflict =  true;
                                                    break;
                                        case ">=" : if(versionCompare(tSplit[1], consSplit[1]))
                                                        foundConflict =  true;
                                                    break;
                                        default   : foundConflict = true;
                                                    break;
                                    }
                                }
                            }
                        }
                    }
                    if(foundConflict) {
                        redudent.add(pack.getName() + "=" + pack.getVersion());
                        set.remove(pack.getName() + "=" + pack.getVersion());
                        commands.add("-"+ pack.getName() + "=" + pack.getVersion());
                    }
                }
            }
        }

        return set;
    }

    /**
     * Removes out of date version of packages and replace them with the newest version
     *
     * @param set
     * @param repo
     * @param fixed
     * @param constraints
     */
    private static void updatePackages(List<String> set, List<Package> repo, List<String> fixed, List<String> constraints)    {
        for (Package pack : repo) {
            if(!set.contains(pack.getName() + "=" + pack.getVersion()) &&
                    !commands.contains("-" + pack.getName() + "=" + pack.getVersion())) {
                set.add(pack.getName() + "=" + pack.getVersion());
                commands.add("+" + pack.getName() + "=" + pack.getVersion());
                search(set, repo, fixed, constraints);
                set.remove(pack.getName() + "=" + pack.getVersion());
                commands.remove("+" + pack.getName() + "=" + pack.getVersion());
            } else if(fixed.contains(pack.getName() + "=" + pack.getVersion())) {
                set.remove(pack.getName() + "=" + pack.getVersion());
                commands.add("-" + pack.getName() + "=" + pack.getVersion());
                search(set, repo, fixed, constraints);
                set.add(pack.getName() + "=" + pack.getVersion());
                commands.remove("-" + pack.getName() + "=" + pack.getVersion());
            } else {

            }
        }
    }

    /**
     *
     * @param v1
     * @param v2
     * @return
     */
    private static boolean versionCompare(String v1, String v2) {
        String v1Tmp = v1.replace("0", "");
        String v2Tmp = v2.replace("0", "");
        if(!v1Tmp.isEmpty()) { v1 = v1Tmp; }
        if(!v2Tmp.isEmpty()) { v2 = v2Tmp; }
        List<String> v1_Array = new ArrayList(Arrays.asList(v1.split("\\.")));
        List<String> v2_Array = new ArrayList(Arrays.asList(v2.split("\\.")));
        pad(v1_Array, v2_Array);
        return bestVersion(v1_Array, v2_Array);
    }

    /**
     * This function is used to make sure the two lists are of the same length
     *
     * @param v1_Array
     * @param v2_Array
     */
    public static void pad(List<String> v1_Array, List<String> v2_Array)    {
        while(v1_Array.size() != v2_Array.size()) {
            if(v1_Array.size() > v2_Array.size()) {
                v2_Array.add("0");
            } else {
                v1_Array.add("0");
            }
        }
    }

    /**
     * Checks which version is better dependant on the contents of the array. Returns true is array 1 is better or
     * the arrays are the same, if array 2 is better returns false.
     *
     * @param vers1Arr
     * @param vers2Arr
     * @return bool of better list
     */
    public static boolean bestVersion(List<String> vers1Arr, List<String> vers2Arr) {
        if (vers1Arr.equals(vers2Arr))  { return true;  }
        for(int i = 0; i < vers1Arr.size(); i++) {
            if(Integer.parseInt(vers1Arr.get(i)) > Integer.parseInt(vers2Arr.get(i))) {
                return true;
            }
            if(Integer.parseInt(vers2Arr.get(i)) > Integer.parseInt(vers1Arr.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     *
     * @param str
     * @return result
     */
    private static String[] splitPackage(String str) {
        boolean matched = false;
        String[] nameVer;
        String name ="";
        String version = "";
        String compareOperator = "";
        ArrayList<String> operatorsS1 = new ArrayList();
        operatorsS1.add("<"); operatorsS1.add("<"); operatorsS1.add("=");

        for (int i = 0; i < operatorsS1.size(); i++)    {
            if (str.contains(operatorsS1.get(i))) {
                nameVer = str.split(operatorsS1.get(i));
                name = nameVer[0];
                version = nameVer[1];
                compareOperator = operatorsS1.get(i);
                matched = true;
            }
        }

        ArrayList<String> operatorsS2 = new ArrayList();
        operatorsS2.add("<="); operatorsS2.add(">=");
        for (int i = 0; i < operatorsS2.size(); i++)    {
            if (str.contains(operatorsS2.get(i))) {
                if (i == 0)
                    str = str.replace("<", "");
                else
                    str = str.replace(">", "");
                nameVer = str.split("=");
                name = nameVer[0];
                version = nameVer[1];
                compareOperator = operatorsS2.get(i);
                matched = true;
            }
        }
        if(!matched)
            name = str;

        String[] result = { name, version, compareOperator  };
        return result;
    }

    /**
     * Calculates teh cost of a solution
     *
     * @param repo
     * @return the cost
     */
    private static int solutionCost(List<Package> repo) {
        int cost = 0;

        for(String str : commands) {
            if(str.contains("-")) {
                cost += 1111111111;
            } else {
                for(Package pack : repo) {
                    String nameVers = pack.getName() + "=" + pack.getVersion();
                    if(str.substring(1).equals(nameVers)) {
                        cost += pack.getSize();
                    }
                }
            }
        }
        return cost;
    }

    /**
     * This function determines what the cheapest solution by comparing the costs
     *
     * @return A list of the cheapest solution
     */
    private static List<String> getCheapestSolution()   {
        List<String> result = new ArrayList<>();
        int resultCost = 0;
        for(Map.Entry<String, Integer> entry : solutions.entrySet()) {
            if(entry.getValue() < resultCost || result.size() == 0) {
                result = Arrays.asList(entry.getKey().split(","));
                resultCost = entry.getValue();
            }
        }
        return result;
    }

    /**
     * Outputs the cheapest solution in JSON format
     */
    private static void printCheapestSolution() {
        List<String> result = getCheapestSolution();
        System.out.println(JSON.toJSON(result));
    }

    /**
     * Reads the contents of a given file
     *
     * @param filename
     * @return a string with the contents of the selected file
     * @throws IOException
     */
    static String readFile(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        StringBuilder sb = new StringBuilder();
        br.lines().forEach(line -> sb.append(line));
        return sb.toString();
    }
}